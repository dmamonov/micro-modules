package org.micromodules.control.report;

import com.googlecode.jatl.Html;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-29 6:10 PM
 */
abstract class AbstractReport {
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
