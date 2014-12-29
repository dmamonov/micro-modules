package org.micromodules.control.graph;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DirectedMaskSubgraph;
import org.jgrapht.graph.MaskFunctor;
import org.micromodules.control.graph.GraphDomain.*;
import org.micromodules.setup.Contract;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.alwaysTrue;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.collect.Iterables.*;
import static org.micromodules.control.graph.GraphQuery.GraphDirection.Backward;
import static org.micromodules.control.graph.GraphQuery.GraphDirection.Forward;
import static org.micromodules.control.util.Predicates2.or;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-29 3:54 PM
 */
@Contract(__modules__.ModulesGraphModule.class)
public class GraphQuery  {
    public interface GraphPathStart {
        GraphPathDirection from(Node node);

        @SuppressWarnings("UnusedDeclaration")
        GraphPathDirection from(Iterable<Node> nodes);

        GraphPathDirection from(Predicate<Node> nodeFilter);
    }

    public interface GraphPathDirection {
        GraphPathEdgeFilter forward();

        GraphPathEdgeFilter backward();

        ImmutableSet<Node> getStartSet();
    }

    public interface GraphPathEdgeFilter extends GraphPathNodeFilter {
        GraphPathEdgeFilter by(Predicate<NodeEdge> edgeFilter);
    }

    public interface GraphPathNodeFilter extends GraphPathStep {
        GraphPathNodeFilter to(Predicate<Node> nodeFilter);
    }

    public interface GraphPathStep {
        GraphPathBacktrace recursive();

        GraphPathBacktrace single();
    }

    public interface GraphPathBacktrace extends GraphPathFinish {
        GraphPathFinish backtrace();
    }

    public interface GraphPathFinish {
        GraphPathFinish filterStart(Predicate<Node> predicate);
        default GraphPathFinish useStart() {
            return filterStart(alwaysTrue());
        }

        GraphPathFinish filterPath(Predicate<Node> predicate);

        @SuppressWarnings("UnusedDeclaration")
        default GraphPathFinish usePath() {
            return filterPath(alwaysTrue());
        }


        GraphPathFinish filterFinish(Predicate<Node> predicate);
        default GraphPathFinish useFinish() {
            return filterFinish(alwaysTrue());
        }

        ImmutableSet<Node> set();

        DirectedGraph<Node, NodeEdge> graph();

        DirectedGraph<Node, NodeEdge> unmaskedEdgesGraph();

        GraphPathDirection then();
    }

    protected enum GraphDirection {
        Forward {
            @Override
            Set<NodeEdge> go(final DirectedGraph<Node, NodeEdge> g, final Node fromNode) {
                return g.outgoingEdgesOf(fromNode);
            }

            @Override
            Node from(final NodeEdge edge) {
                return edge.getSource();
            }

            @Override
            Node to(final NodeEdge edge) {
                return edge.getTarget();
            }
        },
        Backward {
            @Override
            Set<NodeEdge> go(final DirectedGraph<Node, NodeEdge> g, final Node fromNode) {
                return g.incomingEdgesOf(fromNode);
            }

            @Override
            Node from(final NodeEdge edge) {
                return edge.getTarget();
            }

            @Override
            Node to(final NodeEdge edge) {
                return edge.getSource();
            }
        };

        abstract Set<NodeEdge> go(DirectedGraph<Node, NodeEdge> g, Node fromNode);

        abstract Node to(NodeEdge edge);

        abstract Node from(NodeEdge edge);
    }

    public static GraphPathStart start(final DirectedGraph<Node, NodeEdge> dg) {
        return new GraphPathStart() {
            @Override
            public GraphPathDirection from(final Node node) {
                return new GraphPathDirectionImpl(of(node), new GraphPathImpl());
            }

            @Override
            public GraphPathDirection from(final Iterable<Node> nodes) {
                return new GraphPathDirectionImpl(copyOf(nodes), new GraphPathImpl());
            }

            @Override
            public GraphPathDirection from(final Predicate<Node> nodeFilter) {
                return new GraphPathDirectionImpl(copyOf(Sets.filter(dg.vertexSet(), nodeFilter)), new GraphPathImpl());
            }

            class GraphPathDirectionImpl implements GraphPathDirection {
                private final ImmutableSet<Node> startSet;
                private final GraphPathImpl graphPath;

                public GraphPathDirectionImpl(final ImmutableSet<Node> startSet, final GraphPathImpl graphPath) {
                    this.startSet = checkNotNull(startSet);
                    this.graphPath = graphPath;
                }

                @Override
                public GraphPathEdgeFilter forward() {
                    return new GraphPathEdgeFilterImpl(startSet, Forward, graphPath);
                }

                @Override
                public GraphPathEdgeFilter backward() {
                    return new GraphPathEdgeFilterImpl(startSet, Backward, graphPath);
                }

                public ImmutableSet<Node> getStartSet() {
                    return startSet;
                }
            }

            class GraphPathEdgeFilterImpl implements GraphPathEdgeFilter, GraphPathBacktrace {
                private final ImmutableSet<Node> startSet;
                private final GraphDirection direction;
                private final GraphPathImpl graphPath;
                private Predicate<NodeEdge> edgeFilter = null;
                private Predicate<Node> nodeFilter = null;

                //TODO [DM] separate this filters into derived instance
                private Predicate<Node> filterStart;
                private Predicate<Node> filterPath;
                private Predicate<Node> filterFinish;

                public GraphPathEdgeFilterImpl(final ImmutableSet<Node> startSet, final GraphDirection direction, final GraphPathImpl graphPath) {
                    this.startSet = checkNotNull(startSet);
                    this.direction = checkNotNull(direction);
                    this.graphPath = checkNotNull(graphPath);
                }

                @Override
                public GraphPathEdgeFilter by(final Predicate<NodeEdge> edgeFilter) {
                    checkNotNull(edgeFilter);
                    this.edgeFilter = this.edgeFilter != null
                            ? or(this.edgeFilter, edgeFilter)
                            : edgeFilter;
                    return this;
                }

                @Override
                public GraphPathNodeFilter to(final Predicate<Node> nodeFilter) {
                    checkNotNull(nodeFilter);
                    this.nodeFilter = this.nodeFilter != null
                            ? or(this.nodeFilter, nodeFilter)
                            : nodeFilter;
                    return this;
                }

                @Override
                public GraphPathBacktrace single() {
                    stepSingleImpl(startSet, new HashSet<>());
                    return this;
                }

                @Override
                public GraphPathBacktrace recursive() {
                    stepRecursiveImpl(startSet, new HashSet<>());
                    return this;
                }

                private ImmutableSet<Node> stepSingleImpl(final ImmutableSet<Node> fromSet, final Set<NodeEdge> passedSet) {
                    final Predicate<NodeEdge> edgePredicate = edgeFilter != null ? edgeFilter : alwaysTrue();
                    final Predicate<Node> nodePredicate = nodeFilter != null ? nodeFilter : alwaysTrue();
                    final Set<NodeEdge> pathStepSet = new HashSet<>();
                    //noinspection CodeBlock2Expr
                    fromSet.forEach(start -> {
                        direction.go(dg, start).stream()
                                .filter(edgePredicate::apply)
                                .forEach(edge -> {
                                    if (!passedSet.contains(edge)) {
                                        if (nodePredicate.apply(direction.to(edge))) {
                                            passedSet.add(edge);
                                            pathStepSet.add(edge);
                                        }
                                    }
                                });
                    });
                    GraphPathEdgeFilterImpl.this.graphPath.createPathEntry(direction).getEdgeSet().addAll(pathStepSet);
                    return copyOf(pathStepSet.stream().map(direction::to).iterator());
                }

                private void stepRecursiveImpl(final ImmutableSet<Node> fromSet, final Set<NodeEdge> passedSet) {
                    final ImmutableSet<Node> toSet = stepSingleImpl(fromSet, passedSet);
                    if (!toSet.isEmpty()) {
                        stepRecursiveImpl(toSet, passedSet);
                    }
                }

                @Override
                public GraphPathFinish backtrace() {
                    this.graphPath.backtrace();
                    return this;
                }

                @Override
                public GraphPathFinish filterStart(final Predicate<Node> predicate) {
                    this.filterStart = predicate != null ? predicate : alwaysTrue();
                    return this;
                }

                @Override
                public GraphPathFinish filterPath(final Predicate<Node> predicate) {
                    this.filterPath = predicate != null ? predicate : alwaysTrue();
                    return this;
                }

                @Override
                public GraphPathFinish filterFinish(final Predicate<Node> predicate) {
                    this.filterFinish = predicate != null ? predicate : alwaysTrue();
                    return this;
                }

                @Override
                public ImmutableSet<Node> set() {
                    if (this.filterStart == null && this.filterPath == null && this.filterFinish == null) {
                        this.filterStart = alwaysTrue();
                        this.filterPath = alwaysTrue();
                        this.filterFinish = alwaysTrue();
                    }
                    final TreeSet<Node> resultSet = new TreeSet<>();
                    if (this.filterStart != null) {
                        resultSet.addAll(Sets.filter(graphPath.getStartSet(), this.filterStart));
                    }
                    if (this.filterPath != null) {
                        resultSet.addAll(Sets.filter(graphPath.getPathSet(), this.filterPath));
                    }
                    if (this.filterFinish != null) {
                        resultSet.addAll(Sets.filter(graphPath.getFinishSet(), this.filterFinish));
                    }
                    //TODO [DM] deal with this state!
                    this.filterStart = null;
                    this.filterPath = null;
                    this.filterFinish = null;
                    return copyOf(resultSet);
                }

                @Override
                public DirectedGraph<Node, NodeEdge> graph() {
                    return graphImpl(true);
                }

                @Override
                public DirectedGraph<Node, NodeEdge> unmaskedEdgesGraph() {
                    return graphImpl(false);
                }

                private DirectedGraph<Node, NodeEdge> graphImpl(final boolean maskEdges) {
                    return new DirectedMaskSubgraph<>(dg, new MaskFunctor<Node, NodeEdge>() {
                        final ImmutableSet<Node> nodeSet = set();
                        final ImmutableSet<NodeEdge> edgeSet = maskEdges ? graphPath.getEdgeSet() : null;

                        @Override
                        public boolean isEdgeMasked(final NodeEdge edge) {
                            //noinspection SimplifiableConditionalExpression
                            return maskEdges ? !edgeSet.contains(edge) : false;
                        }

                        @Override
                        public boolean isVertexMasked(final Node node) {
                            return !nodeSet.contains(node);
                        }
                    });
                }



                @Override
                public GraphPathDirection then() {
                    return new GraphPathDirectionImpl(set(), graphPath.copy());
                }
            }

            class GraphPathEntry {
                private final GraphDirection direction;
                private final Set<NodeEdge> edgeSet = new HashSet<>();

                public GraphPathEntry(final GraphDirection direction) {
                    this.direction = checkNotNull(direction);
                }

                public ImmutableSet<Node> getFromSet() {
                    return copyOf(this.edgeSet.stream().map(direction::from).iterator());
                }

                public ImmutableSet<Node> getToSet() {
                    return copyOf(this.edgeSet.stream().map(direction::to).iterator());
                }

                public Set<NodeEdge> getEdgeSet() {
                    return edgeSet;
                }

                public void backtrace(final ImmutableSet<Node> nextFromSet) {
                    final ImmutableSet<NodeEdge> removeSet = copyOf(filter(this.edgeSet, edge -> !nextFromSet.contains(direction.to(edge))));
                    this.edgeSet.removeAll(removeSet);
                }

                public GraphDirection getDirection() {
                    return direction;
                }

                @Override
                public String toString() {
                    return direction+" "+edgeSet;
                }
            }

            class GraphPathImpl {
                private final List<GraphPathEntry> pathList = new ArrayList<>();

                public GraphPathEntry createPathEntry(final GraphDirection direction) {
                    final GraphPathEntry newEntry = new GraphPathEntry(direction);
                    this.pathList.add(newEntry);
                    return newEntry;
                }

                public ImmutableSet<Node> getStartSet() {
                    return pathList.get(0).getFromSet();
                }

                public ImmutableSet<Node> getPathSet() {
                    return copyOf(concat(copyOf(pathList.subList(0, pathList.size() - 1).stream().map(GraphPathEntry::getToSet).iterator())));
                }

                public ImmutableSet<Node> getFinishSet() {
                    return pathList.get(pathList.size() - 1).getToSet();
                }

                public ImmutableSet<NodeEdge> getEdgeSet() {
                    return copyOf(concat(transform(pathList, GraphPathEntry::getEdgeSet)));
                }

                public void backtrace() {
                    ImmutableSet<Node> nextFromSet = pathList.get(pathList.size() - 1).getFromSet();
                    for (int i = pathList.size() - 2; i >= 0; i--) {
                        final GraphPathEntry entry = pathList.get(i);
                        entry.backtrace(nextFromSet);
                        nextFromSet = entry.getFromSet();
                    }
                }

                public GraphPathImpl copy() {
                    final GraphPathImpl pathCopy = new GraphPathImpl();
                    //noinspection CodeBlock2Expr
                    pathList.forEach(step -> {
                        pathCopy.createPathEntry(step.getDirection()).getEdgeSet().addAll(step.getEdgeSet());
                    });
                    return pathCopy;
                }
            }
        };
    }

}
