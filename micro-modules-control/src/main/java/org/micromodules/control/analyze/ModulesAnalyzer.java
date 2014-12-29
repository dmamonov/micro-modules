package org.micromodules.control.analyze;

import com.google.common.collect.ImmutableSet;
import org.micromodules.control.graph.GraphDomain.Node;
import org.micromodules.control.graph.GraphQuery.GraphPathFinish;
import org.micromodules.control.graph.ModulesGraph;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static org.micromodules.control.graph.GraphDomain.EdgeType.*;
import static org.micromodules.control.graph.GraphDomain.NodeType.*;
import static org.micromodules.control.util.Predicates2.and;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-29 5:52 PM
 */
@org.micromodules.setup.Contract(__modules__.AnalyzeModule.class)
public class ModulesAnalyzer {
    private final ModulesGraph graph;

    public static ModulesAnalyzer createFrom(final ModulesGraph graph) {
        return new ModulesAnalyzer(graph);
    }

    private ModulesAnalyzer(final ModulesGraph graph) {
        checkNotNull(graph, "graph required");
        this.graph = graph;
    }

    public GraphPathFinish getSuperModules(final Node module) {
        return graph.query().from(module).backward().by(SubModule).to(ModuleNode).recursive();
    }

    public GraphPathFinish getSubModules(final Node module) {
        return graph.query().from(module).forward().by(SubModule).to(ModuleNode).recursive();
    }

    public GraphPathFinish getRelatedModules(final Node module) {
        return graph.query().from(module).forward().by(Dependency).to(ModuleNode).single()
                .useStart().then().backward().by(Dependency).to(ModuleNode).single();
    }

    public GraphPathFinish getModuleContractClasses(final Node module) {
        return graph.query().from(module).forward().by(ContractClass).to(CodeNode).single();
    }

    public GraphPathFinish getModuleImplementationClasses(final Node module) {
        return graph.query().from(module).forward().by(ImplementationClass).to(CodeNode).single();
    }

    public GraphPathFinish getModuleAllClasses(final Node module) {
        return graph.query().from(module).forward().by(ContractClass.or(ImplementationClass)).to(CodeNode).single();
    }

    public GraphPathFinish getModuleDirectDependencies(final Node module) {
        return graph.query().from(module).forward().by(Dependency).to(ModuleNode).single();
    }

    public GraphPathFinish getModuleHierarchyDependencies(final Node module) {
        return getSubModules(module).then().forward().by(Dependency).to(ModuleNode).single();
    }

    public GraphPathFinish getModuleUsedBy(final Node module) {
        return graph.query().from(module).backward().by(Dependency).to(ModuleNode).single();
    }

    public GraphPathFinish getModuleJarDependencies(final Node module) {
        return graph.query()
                .from(module).forward().by(ImplementationClass).by(ContractClass).to(CodeNode).single()
                .useFinish().then().forward().by(UsesClass).to(JarNode).single();
    }


    public GraphPathFinish getModuleDependencyRuleViolation(final Node module) {
        final GraphPathFinish superModules = getSuperModules(module);
        final ImmutableSet<Node> superModulesSet = superModules.set(ModuleNode);
        return graph.query()
                .from(module).forward().by(ContractClass).by(ImplementationClass).to(CodeNode).single()
                .useFinish().then().forward().by(UsesClass).to(CodeNode).single()
                .useFinish().then().backward().by(ContractClass).by(ImplementationClass).to(
                        and(
                                not(in(
                                        superModules
                                                .then().forward().by(Allowed).to(ModuleNode).single()
                                                .useFinish().then().forward().by(SubModule).to(ModuleNode).single()
                                                .set(and(ModuleNode, not(in(superModulesSet))))
                                )),
                                not(module)
                        )
                ).single().backtrace();
    }
    public GraphPathFinish getModuleAllowedDependencies(final Node module){
        final ImmutableSet<Node> superModulesSet = getSuperModules(module).set(ModuleNode);
        return graph.query().from(superModulesSet).forward().by(Allowed).to(ModuleNode).single().useFinish();
    }


    public GraphPathFinish getModuleContractRuleViolation(final Node module) {
        return graph.query()
                .from(module).forward().by(ContractClass).by(ImplementationClass).to(CodeNode).single()
                .useFinish().then().forward().by(UsesClass).to(CodeNode).single()
                .useFinish().then().backward().by(ImplementationClass).to(and(not(module), ModuleNode)).single()
                .backtrace();
    }

    public GraphPathFinish getModuleCollisionRuleViolation(final Node module) {
        return graph.query()
                .from(module).forward().by(ContractClass).by(ImplementationClass).to(CodeNode).single()
                .useFinish().then().backward().by(ContractClass).by(ImplementationClass).to(and(not(module), ModuleNode)).single()
                .backtrace();
    }
    
    public boolean isSuperModule(final Node module){
        return graph.query().from(module).forward().by(SubModule).to(ModuleNode).single().useFinish().set().size()>0;
    }
}
