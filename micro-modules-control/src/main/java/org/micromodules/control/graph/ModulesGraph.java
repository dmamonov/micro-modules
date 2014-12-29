package org.micromodules.control.graph;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.micromodules.control.graph.GraphDomain.EdgeType;
import org.micromodules.control.graph.GraphDomain.Node;
import org.micromodules.control.graph.GraphDomain.NodeEdge;
import org.micromodules.control.graph.GraphDomain.NodeType;
import org.micromodules.control.scan.ClasspathRelations;
import org.micromodules.control.spec.ModuleSpec;
import org.micromodules.control.spec.ModulesSpecification;
import org.micromodules.setup.Module;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.not;
import static org.micromodules.control.graph.GraphDomain.EdgeType.*;
import static org.micromodules.control.graph.GraphDomain.NodeType.*;
import static org.micromodules.control.util.Predicates2.and;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-29 3:43 PM
 */
@org.micromodules.setup.Contract(__modules__.ModulesGraphModule.class)
public class ModulesGraph {
    private final DirectedGraph<Node, NodeEdge> graph = new DefaultDirectedGraph<>(NodeEdge.class);
    private final ImmutableMap<Node, ModuleSpec> nodeToModuleSpecMap;

    public static ModulesGraph createFrom(final ClasspathRelations classpathRelations, final ModulesSpecification modulesSpecification) {
        return new ModulesGraph(classpathRelations, modulesSpecification);
    }

    private ModulesGraph(final ClasspathRelations classpathRelations, final ModulesSpecification modulesSpecification) {
        checkNotNull(classpathRelations, "classpathRelations required");
        checkNotNull(modulesSpecification, "modulesSpecification required");

        classpathRelations.getClassesSet().forEach(clazz -> {
            final Node packageNode = PackageNode.named(clazz.getPackage().getName());
            final Node codeNode = NodeType.CodeNode.named(clazz.getName());
            addVertex(packageNode);
            addVertex(codeNode);
            createEdge(packageNode, Contains, codeNode);
        });
        ImmutableList.copyOf(System.getProperty("java.class.path", "").split("[:;]")).forEach(jarPath -> {
            final String[] pathItems = jarPath.split("[/\\\\]");
            if (pathItems.length > 0) {
                final String jarName = pathItems[pathItems.length - 1];
                if (jarName.endsWith(".jar")) {
                    this.addVertex(JarNode.named(jarName));
                }
            }
        });
        classpathRelations.getClassesSet().forEach(useClazz -> {
            final Node clazzNode = CodeNode.named(useClazz.getName());
            classpathRelations.getClassToDependencyClassMap().get(useClazz.getName()).forEach(dependencyClazzName -> {
                final Node dependencyClazzNode = CodeNode.named(dependencyClazzName);
                if (containsVertex(dependencyClazzNode)) {
                    createEdge(clazzNode, Uses, dependencyClazzNode);
                } else {
                    final Node jarNode = JarNode.named(classpathRelations.getJarName(dependencyClazzNode.getName()));
                    addVertex(jarNode);
                    createEdge(clazzNode, Uses, jarNode);
                }
            });
            classpathRelations.getClassContainsClasses(useClazz.getName())
                    .forEach(nestedClazzName -> createEdge(clazzNode, Contains, CodeNode.named(nestedClazzName)));
        });
        modulesSpecification.getModuleSpecSet().forEach(spec -> {
            final Node moduleNode = NodeType.ModuleNode.named(spec.getId());
            addVertex(moduleNode);
            spec.getImplementationClasses().forEach(impl -> createEdge(moduleNode, Implementation, CodeNode.named(impl.getName())));
            spec.getContractClasses().forEach(contract -> createEdge(moduleNode, Contract, CodeNode.named(contract.getName())));
            { //add relation to module group:
                final Class<?> superClazz = spec.getModule().getSuperclass();
                if (Module.class.isAssignableFrom(superClazz)) {
                    final Node superModuleNode = ModuleNode.named(superClazz.getName());
                    addVertex(superModuleNode);
                    createEdge(superModuleNode, SubModule, moduleNode);
                }
            }
        });


        modulesSpecification.getModuleSpecSet().forEach(spec -> {
            final Node moduleNode = ModuleNode.named(spec.getId());
            spec.getAllowedDependencies().forEach(allowedDependencyClazz -> {
                final Node allowedDependencyNode = ModuleNode.named(allowedDependencyClazz.getName());
                createEdge(moduleNode, AllowedDependency, allowedDependencyNode);
            });

        });


        modulesSpecification.getModuleSpecSet().forEach(spec -> {
            final Node moduleNode = ModuleNode.named(spec.getId());
            query().from(moduleNode).forward().by(Contract).by(Implementation).to(CodeNode).single()
                    .useFinish().then().forward().by(Uses).to(CodeNode).single()
                    .useFinish().then().backward().by(Contract).by(Implementation).to(and(ModuleNode, not(moduleNode))).single()
                    .useFinish().set().forEach(dependsOnModuleNode -> createEdge(moduleNode, DependsOn, dependsOnModuleNode)
            );
        });

        this.nodeToModuleSpecMap = Maps.uniqueIndex(modulesSpecification.getModuleSpecSet(), spec -> ModuleNode.named(spec.getId()));

        //cleanup useless nodes:
        removeVertex(JarNode.named("rt.jar"));
        removeVertex(JarNode.named("default"));
    }

    public GraphQuery.GraphPathStart query() {
        return GraphQuery.start(graph);
    }

    public ModulesGraph addVertex(final Node node) {
        this.graph.addVertex(node);
        return this;
    }

    private ModulesGraph removeVertex(final Node node) {
        this.graph.removeVertex(node);
        return this;
    }

    private boolean containsVertex(final Node node) {
        return graph.containsVertex(node);
    }

    public ModulesGraph createEdge(final Node from, final EdgeType by, final Node to) {
        by.createEdge(this.graph, from, to);
        return this;
    }

    public ModuleSpec getModuleSpecByNode(final Node node) {
        return nodeToModuleSpecMap.get(node);
    }
}
