package org.micromodules.control.graph;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.micromodules.control.analyze.ModulesAnalyzer;
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
        final ModulesGraph modulesGraph = new ModulesGraph(classpathRelations, modulesSpecification);
        modulesGraph.simplify();
        return modulesGraph;
    }

    private ModulesGraph(final ClasspathRelations classpathRelations, final ModulesSpecification modulesSpecification) {
        checkNotNull(classpathRelations, "classpathRelations required");
        checkNotNull(modulesSpecification, "modulesSpecification required");

        classpathRelations.getClassesSet().forEach(clazz -> {
            final Node packageNode = PackageNode.named(clazz.getPackage().getName());
            final Node codeNode = NodeType.CodeNode.named(clazz.getName());
            addNode(packageNode);
            addNode(codeNode);
            createEdge(packageNode, ContainsSubClass, codeNode);
        });
        ImmutableList.copyOf(System.getProperty("java.class.path", "").split("[:;]")).forEach(jarPath -> {
            final String[] pathItems = jarPath.split("[/\\\\]");
            if (pathItems.length > 0) {
                final String jarName = pathItems[pathItems.length - 1];
                if (jarName.endsWith(".jar")) {
                    this.addNode(JarNode.named(jarName));
                }
            }
        });
        classpathRelations.getClassesSet().forEach(useClazz -> {
            if (useClazz.getSimpleName().equals("__modules__") || (!useClazz.isInterface() && Module.class.isAssignableFrom(useClazz))){
                return;
            }
            final Node clazzNode = CodeNode.named(useClazz.getName());
            classpathRelations.getClassToDependencyClassMap().get(useClazz.getName()).forEach(dependencyClazzName -> {
                final Node dependencyClazzNode = CodeNode.named(dependencyClazzName);
                if (containsNode(dependencyClazzNode)) {
                    createEdge(clazzNode, UsesClass, dependencyClazzNode);
                } else {
                    final Node jarNode = JarNode.named(classpathRelations.getJarName(dependencyClazzNode.getName()));
                    addNode(jarNode);
                    createEdge(clazzNode, UsesJar, jarNode);
                }
            });
            classpathRelations.getClassContainsClasses(useClazz.getName())
                    .forEach(nestedClazzName -> createEdge(clazzNode, ContainsSubClass, CodeNode.named(nestedClazzName)));
        });
        modulesSpecification.getModuleSpecSet().forEach(spec -> {
            final Node moduleNode = NodeType.ModuleNode.named(spec.getId());
            addNode(moduleNode);
            spec.getImplementationClasses().forEach(impl -> createEdge(moduleNode, ImplementationClass, CodeNode.named(impl.getName())));
            spec.getContractClasses().forEach(contract -> createEdge(moduleNode, ContractClass, CodeNode.named(contract.getName())));
            { //add relation to module group:
                final Class<?> superClazz = spec.getModule().getSuperclass();
                if (Module.class.isAssignableFrom(superClazz)) {
                    final Node superModuleNode = ModuleNode.named(superClazz.getName());
                    addNode(superModuleNode);
                    createEdge(superModuleNode, SubModule, moduleNode);
                }
            }
        });


        modulesSpecification.getModuleSpecSet().forEach(spec -> {
            final Node moduleNode = ModuleNode.named(spec.getId());
            spec.getAllowedDependencies().forEach(allowedDependencyClazz -> {
                final Node allowedDependencyNode = ModuleNode.named(allowedDependencyClazz.getName());
                createEdge(moduleNode, Granted, allowedDependencyNode);
            });

        });


        modulesSpecification.getModuleSpecSet().forEach(spec -> {
            final Node moduleNode = ModuleNode.named(spec.getId());
            query().from(moduleNode).forward().by(ContractClass).by(ImplementationClass).to(CodeNode).single()
                    .useFinish().then().forward().by(UsesClass).to(CodeNode).single()
                    .useFinish().then().backward().by(ContractClass).by(ImplementationClass).to(and(ModuleNode, not(moduleNode))).single()
                    .useFinish().set().forEach(dependsOnModuleNode -> createEdge(moduleNode, Dependency, dependsOnModuleNode)
            );
        });

        this.nodeToModuleSpecMap = Maps.uniqueIndex(modulesSpecification.getModuleSpecSet(), spec -> ModuleNode.named(spec.getId()));

        //cleanup useless nodes:
        removeNode(JarNode.named("rt.jar"));
        removeNode(JarNode.named("default"));
    }

    private void simplify() {
        final ModulesAnalyzer analyzer = ModulesAnalyzer.createFrom(this);
        query().from(ModuleNode).getStartSet().forEach(module -> {
            final boolean isSuperModule = analyzer.isSuperModule(module);
            System.out.println("Simplify " + (isSuperModule ? "Super" : "") + " module: " + module);
            final ImmutableSet<Node> moduleClassesSet = analyzer.getModuleAllClasses(module).set(CodeNode);
            if (isSuperModule) {
                if (moduleClassesSet.size() > 0) {
                    System.out.println("  Super module must not contain classes: " + moduleClassesSet);
                    moduleClassesSet.forEach(code -> createEdge(module, RuleSuperModuleMustNotContainClasses, code));
                }
            } else {
                if (moduleClassesSet.isEmpty()) {
                    System.out.println("  Regular Module should contain classes");
                    createEdgeForce(module, HasProblem, ProblemNode.named("Not super module should contain at least one class"));
                }
            }
            {
                final ImmutableSet<Node> superModulesSet = analyzer.getSuperModules(module).set(ModuleNode);
                System.out.println("  Super modules: " + superModulesSet);
                final ImmutableSet<Node> directlyGrantedModulesSet = analyzer.getModuleDirectlyGrantedDependencies(module).set(ModuleNode);
                System.out.println("  Directly granted dependencies: " + directlyGrantedModulesSet);
                final ImmutableSet<Node> grantedModulesSet = query().from(directlyGrantedModulesSet).forward().by(SubModule).to(ModuleNode).recursive().set(ModuleNode);
                System.out.println("  All granted dependencies: " + grantedModulesSet);
                final ImmutableSet<Node> actualDependencySet = analyzer.getModuleDirectDependencies(module).useFinish().set(ModuleNode);
                System.out.println("  Actual dependencies: " + actualDependencySet);
                final Sets.SetView<Node> notAllowedActualDependenciesSet = Sets.difference(actualDependencySet, grantedModulesSet);
                if (notAllowedActualDependenciesSet.size() > 0) {
                    System.out.println("  Contains not allowed dependencies: " + notAllowedActualDependenciesSet);
                    notAllowedActualDependenciesSet.forEach(dependencyModule -> createEdge(module, NotAllowed, dependencyModule));
                }
                final Sets.SetView<Node> allowedActualDependenciesSet = Sets.intersection(actualDependencySet, grantedModulesSet);
                if (allowedActualDependenciesSet.size() > 0) {
                    System.out.println("  Contains allowed dependencies: " + allowedActualDependenciesSet);
                    allowedActualDependenciesSet.forEach(dependencyModule -> createEdge(module, Allowed, dependencyModule));
                }
            }
        });
    }

    public GraphQuery.GraphPathStart query() {
        return GraphQuery.start(graph);
    }

    public ModulesGraph addNode(final Node node) {
        this.graph.addVertex(node);
        return this;
    }

    private ModulesGraph removeNode(final Node node) {
        this.graph.removeVertex(node);
        return this;
    }

    private boolean containsNode(final Node node) {
        return graph.containsVertex(node);
    }

    public ModulesGraph createEdge(final Node from, final EdgeType by, final Node to) {
        by.createEdge(this.graph, from, to);
        return this;
    }

    public ModulesGraph createEdgeForce(final Node from, final EdgeType by, final Node to) {
        return addNode(from).addNode(to).createEdge(from, by, to);
    }

    public ModuleSpec getModuleSpecByNode(final Node node) {
        return nodeToModuleSpecMap.get(node);
    }
}
