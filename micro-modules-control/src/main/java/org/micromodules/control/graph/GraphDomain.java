package org.micromodules.control.graph;

import com.googlecode.jatl.Html;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.Edge;
import org.jgrapht.DirectedGraph;
import org.micromodules.setup.Contract;

import java.util.Map;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-29 3:54 PM
 */
@Contract(__modules__.ModulesGraphModule.class)
public final class GraphDomain {
    public enum NodeType implements Predicate<Node>, com.google.common.base.Predicate<Node> {
        CodeNode,
        PackageNode,
        ModuleNode,
        JarNode;

        public Node named(final String name) {
            return new Node(name, this);
        }

        @Override
        public boolean test(final Node node) {
            return node.getType() == this;
        }

        @Override
        public boolean apply(final Node node) {
            return test(node);
        }
    }

    public enum EdgeType implements com.google.common.base.Predicate<NodeEdge> {
        SubModule,
        Contains,
        Uses,
        DependsOn,
        AllowedDependency(true),
        RequiredDependency,
        Contract,
        Implementation;

        private final boolean allowLoopDependency;

        EdgeType() {
            this(false);
        }

        EdgeType(final boolean allowLoopDependency) {
            this.allowLoopDependency = allowLoopDependency;
        }

        public final void createEdge(final DirectedGraph<Node, NodeEdge> dg, final Node source, final Node target) {
            if (!source.equals(target) || allowLoopDependency) {
                dg.addEdge(source, target, new NodeEdge(this, source, target));
            }
        }

        @Override
        public boolean apply(final NodeEdge edge) {
            return edge.getType() == this;
        }
    }

    public static final class NodeEdge implements Edge {
        private final EdgeType type;
        private final Node source;
        private final Node target;


        private NodeEdge(final EdgeType type, final Node source, final Node target) {
            this.type = type;
            this.source = source;
            this.target = target;
        }

        public EdgeType getType() {
            return type;
        }

        @Override
        public Node getSource() {
            return source;
        }

        @Override
        public Node getTarget() {
            return target;
        }

        @Override
        public void setSource(final Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setTarget(final Object o) {
            throw new UnsupportedOperationException();
        }


        @Override
        public AttributeMap getAttributes() {
            throw new UnsupportedOperationException();
        }

        @Override
        @Deprecated
        public Map changeAttributes(final Map map) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setAttributes(final AttributeMap attributeMap) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof NodeEdge)) {
                return false;
            }

            final NodeEdge nodeEdge = (NodeEdge) o;

            if (source != null ? !source.equals(nodeEdge.source) : nodeEdge.source != null) {
                return false;
            }
            if (target != null ? !target.equals(nodeEdge.target) : nodeEdge.target != null) {
                return false;
            }
            //noinspection RedundantIfStatement
            if (type != nodeEdge.type) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = type != null ? type.hashCode() : 0;
            result = 31 * result + (source != null ? source.hashCode() : 0);
            result = 31 * result + (target != null ? target.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return source+" --"+type+"-> "+target;
        }
    }

    public static class Node implements com.google.common.base.Predicate<Node>, Comparable<Node> {
        private final String name;
        private final NodeType type;

        private Node(final String name, final NodeType type) {
            this.name = name.replace('/', '.');
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public NodeType getType() {
            return type;
        }

        public void appendToHtml(final Html html) {
            html.span().attr("title", toString()).text(name.replaceAll("^.*[.$]", "")).end();
        }

        @Override
        public boolean apply(final Node node) {
            return this.equals(node);
        }

        @Override
        public int compareTo(@SuppressWarnings("NullableProblems") final Node other) {
            checkNotNull(other, " other required");
            final int typeCompare = this.type.compareTo(other.type);
            if (typeCompare != 0) {
                return typeCompare;
            } else {
                return this.name.compareTo(other.name);
            }
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Node)) {
                return false;
            }

            final Node micro = (Node) o;

            //noinspection ConstantConditions
            if (name != null ? !name.equals(micro.name) : micro.name != null) {
                return false;
            }
            //noinspection RedundantIfStatement
            if (type != micro.type) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            //noinspection ConstantConditions
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (type != null ? type.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return '"' + name + ":" + type.name().replace("Node", "") + '"';
        }
    }
}
