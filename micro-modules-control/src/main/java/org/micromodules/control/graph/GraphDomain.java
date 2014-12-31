package org.micromodules.control.graph;

import com.google.common.base.Joiner;
import com.googlecode.jatl.Html;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.Edge;
import org.jgrapht.DirectedGraph;
import org.micromodules.control.util.Predicates2.BooleanMixin;
import org.micromodules.setup.Contract;
import org.micromodules.setup.Module;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-29 3:54 PM
 */
@Contract(__modules__.ModulesGraphModule.class)
public final class GraphDomain {
    public enum NodeType implements Predicate<Node>, BooleanMixin<Node> {
        CodeNode,
        PackageNode,
        ModuleNode,
        JarNode,
        ProblemNode;

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

    public enum EdgeType implements BooleanMixin<NodeEdge> {
        SubModule,
        ContainsSubClass,
        UsesClass,
        UsesJar,
        Dependency,
        NotAllowed,
        Allowed,
        Granted(true),
        RequiredDependency,
        ContractClass,
        ImplementationClass,
        RuleSuperModuleMustNotContainClasses,
        HasProblem;

        private final boolean allowLoopDependency;

        EdgeType() {
            this(false);
        }

        EdgeType(final boolean allowLoopDependency) {
            this.allowLoopDependency = allowLoopDependency;
        }

        public final void createEdge(final DirectedGraph<Node, NodeEdge> dg, final Node source, final Node target) {
            if (!source.equals(target) || allowLoopDependency) {
                checkState(dg.containsVertex(source), "No source node in graph: %s", source);
                checkState(dg.containsVertex(target), "No target node in graph: %s", target);
                final NodeEdge existingEdge = dg.getEdge(source, target);
                if (existingEdge!=null){
                    existingEdge.getTypes().add(this);
                } else {
                    dg.addEdge(source, target, new NodeEdge(this, source, target));
                }
            }
        }

        @Override
        public boolean apply(final NodeEdge edge) {
            return edge.getTypes().contains(this);
        }
    }

    public static final class NodeEdge implements Edge {
        private final Set<EdgeType> types = new TreeSet<>();
        private final Node source;
        private final Node target;


        private NodeEdge(final EdgeType type, final Node source, final Node target) {
            this.types.add(type);
            this.source = source;
            this.target = target;
        }

        public Set<EdgeType> getTypes() {
            return types;
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
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof NodeEdge)) {
                return false;
            }

            final NodeEdge nodeEdge = (NodeEdge) other;

            if (!source.equals(nodeEdge.source)) {
                return false;
            }
            //noinspection RedundantIfStatement
            if (!target.equals(nodeEdge.target)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = source.hashCode();
            result = 31 * result + target.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return source+" --"+types+"-> "+target;
        }
    }

    public static class Node implements BooleanMixin<Node>, Comparable<Node> {
        private final String name;
        private final NodeType type;

        private Node(final String name, final NodeType type) {
            this.name = name.replace('/', '.');
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getSimpleName() {
            return name.replaceAll("^.*[.$]", "")+":"+type.name().replace("Node","");
        }

        public NodeType getType() {
            return type;
        }

        public void appendToHtml(final Html html) {
            html.span().attr("title", toString()).text(getSimpleName()).end();
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

        public static Node classNode(final Class<?> clazz) {
            if (!clazz.isInterface() && Module.class.isAssignableFrom(clazz)) {
                return NodeType.ModuleNode.named(clazz.getName());
            } else {
                return NodeType.CodeNode.named(clazz.getName());
            }
        }

        public static String nodesToString(final Collection<Node> nodes){
            return Joiner.on(", ").join(nodes.stream().map(Node::getSimpleName).iterator());
        }
    }
}
