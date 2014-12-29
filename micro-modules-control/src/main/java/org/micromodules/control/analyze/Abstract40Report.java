package org.micromodules.control.analyze;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.googlecode.jatl.Html;
import org.jgrapht.DirectedGraph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Iterables.filter;
import static org.micromodules.control.analyze.Abstract35Graph.EdgeType.*;
import static org.micromodules.control.analyze.Abstract35Graph.NodeType.*;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-25 5:23 PM
 */
abstract class Abstract40Report extends Abstract38GraphRenderer {
    private File dir;

    public final void setDir(final File dir) {
        this.dir = dir;
    }

    protected File report() throws IOException {
        if (!dir.isDirectory()) {
            checkState(dir.mkdirs(), "Failed to make dir: %s", dir);
        }

//        export(graphScanner().startList(ModuleNode).build(), dir, "index-modules");

        final File indexFile = new File(dir, "index.html");
        try (final Writer indexWriter = new FileWriter(indexFile)) {
            final Html index = new Html(indexWriter);
            index.html().body();
            new Runnable() {

                @SuppressWarnings("CodeBlock2Expr")
                @Override
                public void run() {
                    //noinspection ConstantConditions,ConstantIfStatement
                    if (true) {
                        new TableReport<Node>("All Modules Report")
                                .addColumn("Module", (html, node) -> {
                                    final ModuleSpec spec = getModuleSpecByNode(node);
                                    if (spec.isDeprecated()) {
                                        html.s();
                                    }
                                    node.appendToHtml(html);
                                    if (spec.isDeprecated()) {
                                        html.end();
                                    }
                                    html.br();
                                    listGraph(html,
                                            start().from(node).forward().by(SubModule).to(ModuleNode).single().graph(),
                                            and(not(node), ModuleNode),
                                            "Sub Modules"
                                    );
                                    listGraph(html,
                                            start().from(node).forward().by(DependsOn).to(ModuleNode).single()
                                                    .useStart().then().backward().by(DependsOn).to(ModuleNode).single()
                                                    .unmaskedEdgesGraph(),
                                            and(not(node), ModuleNode),
                                            "Related Modules"
                                    );
                                    html.br();
                                })
                                .addColumn("Comment", (html, node) -> {
                                    final ModuleSpec spec = getModuleSpecByNode(node);
                                    spec.getComments().forEach(commentLine -> html.text(commentLine).br());
                                })
                                .addColumn("Contract", (html, node) -> {
                                    listGraph(html,
                                            start().from(node).forward().by(Contract).to(CodeNode).single().graph(),
                                            CodeNode);

                                })
                                .addColumn("Implementation", (html, node) -> {
                                    listGraph(html,
                                            start().from(node).forward().by(Implementation).to(CodeNode).single().graph(),
                                            CodeNode);
                                })
                                .addColumn("DependsOn", (html, node) -> {
                                    listGraph(html,
                                            start().from(node).forward().by(DependsOn).to(ModuleNode).single().graph(),
                                            not(node),
                                            "Direct Dependencies");
                                    final GraphPathBacktrace sumModulesQuery = start().from(node).forward().by(SubModule).to(ModuleNode).recursive();
                                    final ImmutableSet<Node> subModulesSet = sumModulesQuery.set();
                                    listGraph(html, sumModulesQuery.then().forward().by(DependsOn).to(ModuleNode).single().unmaskedEdgesGraph(),
                                            and(not(in(subModulesSet)), ModuleNode),
                                            "Hierarchy Dependencies"
                                    );
                                })
                                .addColumn("UsedBy", (html, node) -> {
                                    listGraph(html,
                                            start().from(node).backward().by(DependsOn).to(ModuleNode).single().graph(),
                                            not(node),
                                            "Direct Usages");
                                })
                                .addColumn("JarDependencies", (html, node) -> {
                                    listGraph(html,
                                            start()
                                                    .from(node).forward().by(Implementation).by(Contract).to(CodeNode).single()
                                                    .useFinish().then().forward().by(Uses).to(JarNode).single().graph(),
                                            JarNode);

                                })
                                .addColumn("Problems", (html, node) -> {
                                    listGraph(html,
                                            start().from(node).forward().by(Contract).by(Implementation).to(CodeNode).single()
                                                    .useFinish().then().forward().by(Uses).to(CodeNode).single()
                                                    .useFinish().then().backward().by(Contract).by(Implementation).to(
                                                    and(
                                                            not(in(
                                                                    start().from(node).backward().by(SubModule).to(ModuleNode).recursive()
                                                                            .then().forward().by(AllowedDependency).to(ModuleNode).single()
                                                                            .useFinish().then().forward().by(SubModule).to(ModuleNode).single()
                                                                            .useFinish().set()
                                                            )),
                                                            not(node)
                                                    )
                                            ).single().backtrace().graph(),
                                            and(not(node), ModuleNode),
                                            "Dependency rule violation");
                                    listGraph(html,
                                            start().from(node).forward().by(Contract).by(Implementation).to(CodeNode).single()
                                                    .useFinish().then().forward().by(Uses).to(CodeNode).single()
                                                    .useFinish().then().backward().by(Implementation).to(and(not(node), ModuleNode)).single()
                                                    .backtrace().graph(),
                                            and(not(node), ModuleNode),
                                            "Contract rule violation");
                                    listGraph(html,
                                            start().from(node).forward().by(Contract).by(Implementation).to(CodeNode).single()
                                                    .useFinish().then().backward().by(Contract).by(Implementation).to(and(not(node), ModuleNode)).single()
                                                    .backtrace().graph(),
                                            and(not(node), ModuleNode),
                                            "Collision rule violation");
                                })
                                .render(index, filter(getCompleteGraph().vertexSet(), ModuleNode));
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
                                .render(index, start().from(JarNode).getStartSet());
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
                    final File svgFile = export(graph, dir, "" + imgIndex.incrementAndGet());
                    html.a().attr("target", "_new").attr("href", svgFile.getName()).text(title).end();
                }

            }.run();
            index.endAll();
        }
        return indexFile;
    }

    protected static class TableReport<T> {
        private final String title;
        private final List<TableColumn<T>> columnList = new ArrayList<>();

        public TableReport(final String title) {
            this.title = title;
        }

        public TableReport<T> addColumn(final String title, final TableCellRenderer<T> cellRenderer) {
            this.columnList.add(new TableColumn<>(title, cellRenderer));
            return this;
        }

        public void render(final Html html, final Iterable<T> rowList) {
            //noinspection SpellCheckingInspection
            html.table()
                    .attr("border", "1px")
                    .attr("cellpadding", "0").
                    attr("cellspacing", "0");
            html.caption().text(this.title).end();
            {
                html.tr();
                columnList.forEach(column -> html.th().text(column.getTitle()).end());
                for (final T row : rowList) {
                    html.tr();
                    columnList.forEach(column -> {
                        html.td().attr("nowrap", "true").attr("valign", "top");
                        if (column.getCellRenderer() != null) {
                            column.getCellRenderer().render(html, row);
                        }
                        html.end();
                    });
                    html.end();
                }
                html.end();
            }
            html.end();
        }

        public static interface TableCellRenderer<T> {
            void render(Html html, T node);
        }

        private static class TableColumn<T> {
            private final String title;
            private final TableCellRenderer<T> cellRenderer;

            private TableColumn(final String title, final TableCellRenderer<T> cellRenderer) {
                this.title = title;
                this.cellRenderer = cellRenderer;
            }

            public String getTitle() {
                return title;
            }

            public TableCellRenderer<T> getCellRenderer() {
                return cellRenderer;
            }
        }

    }


}
