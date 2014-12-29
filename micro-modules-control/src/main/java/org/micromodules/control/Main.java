package org.micromodules.control;

import org.micromodules.control.analyze.ModulesAnalyzer;
import org.micromodules.control.graph.GraphRenderer;
import org.micromodules.control.graph.ModulesGraph;
import org.micromodules.control.report.ModulesReport;
import org.micromodules.control.scan.ClasspathRelations;
import org.micromodules.control.spec.ModulesSpecification;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * @author dmitry.mamonov
 *         Created: 2014-11-14 9:56 PM
 */
public class Main {
    public static void main(final String... args)  {
        final ClasspathRelations classpathRelations = ClasspathRelations.createFrom(Thread.currentThread().getContextClassLoader(), args);
        final ModulesSpecification modulesSpecification = ModulesSpecification.createFrom(classpathRelations);
        final ModulesGraph modulesGraph = ModulesGraph.createFrom(classpathRelations, modulesSpecification);
        final ModulesAnalyzer modulesAnalyzer = ModulesAnalyzer.createFrom(modulesGraph);
        final GraphRenderer graphRenderer = Boolean.parseBoolean(System.getProperty("micro.render_graph_to_png", "false"))
                ? GraphRenderer.createDonAndPngRenderer()
                : GraphRenderer.createDonOnlyRenderer();
        final ModulesReport modulesReport = ModulesReport.createFrom(modulesGraph, modulesAnalyzer, graphRenderer);

        final File reportIndexFile;
        try {
            reportIndexFile = modulesReport.generateReportTo(new File(System.getProperty("micro.out_dir", ".micro-report")));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        if (Boolean.parseBoolean(System.getProperty("micromodules.open_index", "false"))) {
            try {
                Desktop.getDesktop().open(reportIndexFile);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
