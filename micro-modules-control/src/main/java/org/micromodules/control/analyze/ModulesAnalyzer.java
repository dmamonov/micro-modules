package org.micromodules.control.analyze;

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

    public GraphPathFinish getSubModules(final Node node) {
        return graph.query().from(node).forward().by(SubModule).to(ModuleNode).recursive();
    }

    public GraphPathFinish getRelatedModules(final Node node) {
        return graph.query().from(node).forward().by(DependsOn).to(ModuleNode).single()
                .useStart().then().backward().by(DependsOn).to(ModuleNode).single();
    }

    public GraphPathFinish getContract(final Node node) {
        return graph.query().from(node).forward().by(Contract).to(CodeNode).single();
    }

    public GraphPathFinish getImplementation(final Node node) {
        return graph.query().from(node).forward().by(Implementation).to(CodeNode).single();
    }

    public GraphPathFinish getDirectDependencies(final Node node) {
        return graph.query().from(node).forward().by(DependsOn).to(ModuleNode).single();
    }

    public GraphPathFinish getHierarchyDependencies(final Node node) {
        return getSubModules(node).then().forward().by(DependsOn).to(ModuleNode).single();
    }

    public GraphPathFinish getModuleUsedBy(final Node node) {
        return graph.query().from(node).backward().by(DependsOn).to(ModuleNode).single();
    }

    public GraphPathFinish getModuleJarDependencies(final Node node) {
        return graph.query()
                .from(node).forward().by(Implementation).by(Contract).to(CodeNode).single()
                .useFinish().then().forward().by(Uses).to(JarNode).single();
    }


    public GraphPathFinish getModuleDependencyRuleViolation(final Node node) {
        return graph.query()
                .from(node).forward().by(Contract).by(Implementation).to(CodeNode).single()
                .useFinish().then().forward().by(Uses).to(CodeNode).single()
                .useFinish().then().backward().by(Contract).by(Implementation).to(
                        and(
                                not(in(
                                        graph.query().from(node).backward().by(SubModule).to(ModuleNode).recursive()
                                                .then().forward().by(AllowedDependency).to(ModuleNode).single()
                                                .useFinish().then().forward().by(SubModule).to(ModuleNode).single()
                                                .useFinish().set()
                                )),
                                not(node)
                        )
                ).single().backtrace();
    }

    public GraphPathFinish getModuleContractRuleViolation(final Node node) {
        return graph.query()
                .from(node).forward().by(Contract).by(Implementation).to(CodeNode).single()
                .useFinish().then().forward().by(Uses).to(CodeNode).single()
                .useFinish().then().backward().by(Implementation).to(and(not(node), ModuleNode)).single()
                .backtrace();
    }

    public GraphPathFinish getModuleCollisionRuleViolation(final Node node) {
        return graph.query()
                .from(node).forward().by(Contract).by(Implementation).to(CodeNode).single()
                .useFinish().then().backward().by(Contract).by(Implementation).to(and(not(node), ModuleNode)).single()
                .backtrace();
    }
}
