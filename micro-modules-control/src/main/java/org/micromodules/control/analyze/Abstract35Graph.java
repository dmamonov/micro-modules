package org.micromodules.control.analyze;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.googlecode.jatl.Html;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.Edge;
import org.jgrapht.DirectedGraph;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.micromodules.setup.Module;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.micromodules.control.analyze.Abstract35Graph.EdgeType.*;
import static org.micromodules.control.analyze.Abstract35Graph.NodeType.*;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-25 7:45 PM
 */
public class Abstract35Graph extends Abstract30Connect {
    private static final DefaultExecutor executor = new DefaultExecutor();
    private final DirectedGraph<Node, NodeEdge> dg = new DefaultDirectedGraph<>(NodeEdge.class);
    private final Map<Node, ModuleSpec> moduleNodeToSpecMap = new HashMap<>();
    private boolean renderGraphToPng = false;

    public final void setRenderGraphToPng(boolean renderGraphToPng) {
        this.renderGraphToPng = renderGraphToPng;
    }

    protected DirectedGraph<Node, NodeEdge> getCompleteGraph() {
        return dg;
    }

    protected ModuleSpec getModuleSpecByNode(final Node moduleNode) {
        checkArgument(moduleNode.getType() == ModuleNode);
        return moduleNodeToSpecMap.get(moduleNode);
    }

    void constructGraph() {
        listClasses().forEach(clazz -> {
            final Node packageNode = PackageNode.named(clazz.getPackage().getName());
            final Node codeNode = NodeType.CodeNode.named(clazz.getName());
            dg.addVertex(packageNode);
            dg.addVertex(codeNode);
            Contains.createEdge(dg, packageNode, codeNode);
        });
        ImmutableList.copyOf(System.getProperty("java.class.path", "").split("[:;]")).forEach(jarPath -> {
            final String[] pathItems = jarPath.split("[/\\\\]");
            if (pathItems.length > 0) {
                final String jarName = pathItems[pathItems.length - 1];
                if (jarName.endsWith(".jar")) {
                    dg.addVertex(JarNode.named(jarName));
                }
            }
        });
        listClasses().forEach(useClazz -> {
            final Node clazzNode = CodeNode.named(useClazz.getName());
            getClassDependencies(useClazz).forEach(dependencyClazzName -> {
                final Node dependencyClazzNode = CodeNode.named(dependencyClazzName);
                if (dg.containsVertex(dependencyClazzNode)) {
                    Uses.createEdge(dg, clazzNode, dependencyClazzNode);
                } else {
                    final Node jarNode = JarNode.named(getJarName(dependencyClazzNode.getName()));
                    dg.addVertex(jarNode);
                    Uses.createEdge(dg, clazzNode, jarNode);
                }
            });
            getClassSubclasses(useClazz).forEach(nestedClazzName -> Contains.createEdge(dg, clazzNode, CodeNode.named(nestedClazzName)));
        });
        listModuleSpec().forEach(spec -> {
            final Node moduleNode = NodeType.ModuleNode.named(spec.getId());
            this.moduleNodeToSpecMap.put(moduleNode, spec);
            dg.addVertex(moduleNode);
            spec.getImplementationClasses().forEach(impl -> Implementation.createEdge(dg, moduleNode, CodeNode.named(impl.getName())));
            spec.getContractClasses().forEach(contract -> Contract.createEdge(dg, moduleNode, CodeNode.named(contract.getName())));
            { //add relation to module group:
                final Class<?> superClazz = spec.getModule().getSuperclass();
                if (Module.class.isAssignableFrom(superClazz)) {
                    final Node superModuleNode = ModuleNode.named(superClazz.getName());
                    dg.addVertex(superModuleNode);
                    SubModule.createEdge(dg, superModuleNode, moduleNode);
                }
            }
        });
        listModuleSpec().forEach(spec -> {
            final Node moduleNode = ModuleNode.named(spec.getId());
            spec.getActualDependencies().forEach(dependencyModuleName -> DependsOn.createEdge(dg, moduleNode, ModuleNode.named(dependencyModuleName)));
            spec.getAllowedDependencies().forEach(allowedDependencyClazz -> {
                final Node allowedDependencyNode = ModuleNode.named(allowedDependencyClazz.getName());
                AllowedDependency.createEdge(dg, moduleNode, allowedDependencyNode);
            });
        });

        //cleanup useless nodes:
        this.dg.removeVertex(JarNode.named("rt.jar"));
        this.dg.removeVertex(JarNode.named("default"));
    }

    protected File export(final DirectedGraph<Node, NodeEdge> g, final File dir, final String name) {
        final DOTExporter<Node, NodeEdge> exporter = new DOTExporter<>(
                Node::toString,
                node -> null,
                edge -> edge.getType().name(),
                node -> ImmutableMap.<String, String>builder().build(),
                edge -> ImmutableMap.<String, String>builder().build()
        );
        final File dotFile = new File(dir, name + ".dot");
        final File svgFile = new File(dir, name + ".svg");
        System.out.println("exporting: " + dotFile);
        final StringWriter inMemoryOut = new StringWriter();
        exporter.export(inMemoryOut, g);
        final String dotText = inMemoryOut.toString();
        try {
            if (!dotFile.exists() || !svgFile.exists() || !new String(Files.readAllBytes(dotFile.toPath())).equals(dotText)) {
                try (final FileWriter out = new FileWriter(dotFile)) {
                    out.write(dotText);
                }
                if (g.vertexSet().size() <= 100) {
                    System.out.println("  rendering: " + svgFile);
                    final CommandLine command = new CommandLine("dot");
                    command.addArguments(new String[]{dotFile.getAbsolutePath(), "-T" + "svg", "-o", svgFile.getAbsolutePath()});
                    executor.execute(command);
                } else {
                    System.out.println("  graph is too big, skipping: " + svgFile);
                }

            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return svgFile;

    }

    enum NodeType implements Predicate<Node>, com.google.common.base.Predicate<Node> {
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

    enum EdgeType implements com.google.common.base.Predicate<NodeEdge> {
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

    static final class NodeEdge implements Edge {
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

    static class Node implements com.google.common.base.Predicate<Node>, Comparable<Node> {
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
