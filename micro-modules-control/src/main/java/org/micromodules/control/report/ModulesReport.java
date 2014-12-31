package org.micromodules.control.report;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.googlecode.jatl.Html;
import org.jgrapht.DirectedGraph;
import org.micromodules.control.analyze.ModulesAnalyzer;
import org.micromodules.control.graph.GraphDomain.Node;
import org.micromodules.control.graph.GraphDomain.NodeEdge;
import org.micromodules.control.graph.GraphRenderer;
import org.micromodules.control.graph.ModulesGraph;
import org.micromodules.control.spec.ModuleSpec;
import org.micromodules.setup.Contract;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Iterables.filter;
import static org.micromodules.control.graph.GraphDomain.EdgeType.*;
import static org.micromodules.control.graph.GraphDomain.NodeType.*;
import static org.micromodules.control.util.Predicates2.and;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-29 4:01 PM
 */
@Contract(__modules__.ReportModule.class)
public class ModulesReport extends AbstractReport{
    private final ModulesGraph graph;
    private final ModulesAnalyzer analyzer;
    private final GraphRenderer renderer;

    public static ModulesReport createFrom(final ModulesGraph graph, final GraphRenderer renderer){
        return new ModulesReport(graph, renderer);
    }

    private ModulesReport(final ModulesGraph graph, final GraphRenderer renderer) {
        checkNotNull(graph, "graph required");
        checkNotNull(renderer, "renderer required");
        this.graph = graph;
        this.analyzer = ModulesAnalyzer.createFrom(graph);
        this.renderer = renderer;
    }

    public File generateReportTo(final File dir)throws IOException {
        if (!dir.isDirectory()) {
            checkState(dir.mkdirs(), "Failed to make dir: %s", dir);
        }

        final File indexFile = new File(dir, "index.html");
        try (final Writer indexWriter = new FileWriter(indexFile)) {
            final Html index = new Html(indexWriter);
            index.html().body();
            new Runnable() {

                @SuppressWarnings("CodeBlock2Expr")
                @Override
                public void run() {
                    index.h1().text("TODO drilldown").end();

                    final File allModulesGraph = renderer.export(graph.query().from(ModuleNode).forward().to(ModuleNode).single().unmaskedEdgesGraph(), dir, "all-modules");
                    index.img().attr("src",allModulesGraph.getName()).attr("width","1200px").end();

                    //noinspection ConstantConditions,ConstantIfStatement
                    if (true) {
                        new TableReport<Node>("All Modules Report")
                                .addColumn("Module", (html, node) -> {
                                    final ModuleSpec spec = graph.getModuleSpecByNode(node);
                                    if (spec.isDeprecated()) {
                                        html.s();
                                    }
                                    node.appendToHtml(html);
                                    if (spec.isDeprecated()) {
                                        html.end();
                                    }
                                    html.br();
                                    listGraph(html,
                                            analyzer.getSubModules(node).graph(),
                                            and(not(node), ModuleNode),
                                            "Sub Modules"
                                    );
                                    if (false) {
                                        listGraph(html,
                                                analyzer.getRelatedModules(node).unmaskedEdgesGraph(),
                                                and(not(node), ModuleNode),
                                                "Related Modules"
                                        );
                                    }
                                    listGraph(html,
                                            graph.query().from(node).forward().by(ContractClass.or(ImplementationClass)).to(CodeNode).single()
                                                    .useFinish().then().forward().by(UsesClass).to(CodeNode).single()
                                                    .useFinish().then().backward().by(ContractClass.or(ImplementationClass)).to(ModuleNode).single()
                                                    .unmaskedEdgesGraph(),
                                            ModuleNode.andNot(node),
                                            "Related modules");
                                    html.br();
                                })
                                .addColumn("Comment", (html, node) -> {
                                    final ModuleSpec spec = graph.getModuleSpecByNode(node);
                                    spec.getComments().forEach(commentLine -> html.text(commentLine).br());
                                })
                                .addColumn("Contract", (html, node) -> {
                                    listGraph(html,
                                            analyzer.getModuleContractClasses(node).unmaskedEdgesGraph(),
                                            CodeNode);

                                })
                                .addColumn("Implementation", (html, node) -> {
                                    listGraph(html,
                                            analyzer.getModuleImplementationClasses(node).unmaskedEdgesGraph(),
                                            CodeNode);
                                })
                                .addColumn("DependsOn", (html, node) -> {
                                    listGraph(html,
                                            analyzer.getModuleDirectDependencies(node).graph(),
                                            not(node),
                                            "Direct Dependencies");

                                    listGraph(html, analyzer.getModuleHierarchyDependencies(node).unmaskedEdgesGraph(),
                                            and(not(in(analyzer.getSubModules(node).set())), ModuleNode),
                                            "Hierarchy Dependencies"
                                    );
                                })
                                .addColumn("UsedBy", (html, node) -> {
                                    listGraph(html,
                                            analyzer.getModuleUsedBy(node).graph(),
                                            not(node),
                                            "Direct Usages");
                                })
                                .addColumn("JarDependencies", (html, node) -> {
                                    listGraph(html,
                                            analyzer.getModuleJarDependencies(node).graph(),
                                            JarNode);

                                })
                                .addColumn("Problems", (html, node) -> {
                                    listGraph(html,
                                            analyzer.getModuleDependencyRuleViolation(node).graph(),
                                            and(not(node), ModuleNode),
                                            "Dependency rule violation");
                                    listGraph(html,
                                            analyzer.getModuleContractRuleViolation(node).graph(),
                                            and(not(node), ModuleNode),
                                            "Contract rule violation");
                                    listGraph(html,
                                            analyzer.getModuleCollisionRuleViolation(node).graph(),
                                            and(not(node), ModuleNode),
                                            "Collision rule violation");
                                })
                                .render(index, graph.query().from(ModuleNode).getStartSet());
                    }

                    //noinspection ConstantIfStatement,ConstantConditions
                    if (false) {
                        new TableReport<Node>("All Jars Report")
                                .addColumn("Jar", (html, node) -> index.text(node.getName()))
                                .addColumn("UsedInModules", (html, node) -> {
                                    /*
                                    listGraph(html,
                                            graphScanner().start(node).backward(ModuleNode),
                                            ModuleNode);
                                            */

                                })
                                .addColumn("UsedInClass", (html, node) -> {
                                    /*
                                    listGraph(html,
                                            graphScanner().start(node).backward(CodeNode),
                                            CodeNode);
                                            */
                                })
                                .render(index, graph.query().from(JarNode).getStartSet());
                    }
                }


                final AtomicInteger imgIndex = new AtomicInteger();

                private void listGraph(final Html html, final DirectedGraph<Node, NodeEdge> graph, final Predicate<Node> listFilter) {
                    listGraph(html, graph, listFilter, "show graph");
                }

                private void listGraph(final Html html, final DirectedGraph<Node, NodeEdge> graph, final Predicate<Node> listFilter, final String summary) {
                    final ImmutableSet<Node> nodeList = copyOf(filter(graph.vertexSet(), listFilter));
                    if (nodeList.size() > 0) {
                        renderGraph(html, graph, summary);
                        html.ul();
                        nodeList.forEach(node -> {
                            html.li();
                            node.appendToHtml(html);
                            html.end();
                        });
                        html.end();
                    }
                }

                private void renderGraph(final Html html, final DirectedGraph<Node, NodeEdge> graph, final String title) {
                    final File svgFile = renderer.export(graph, dir, "" + imgIndex.incrementAndGet());
                    html.a().attr("target", "_new").attr("href", svgFile.getName()).text(title).end();
                }

            }.run();
            index.endAll();
        }
        return indexFile;
    }



}
