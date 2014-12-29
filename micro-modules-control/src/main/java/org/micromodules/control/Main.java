package org.micromodules.control;

import org.micromodules.control.analyze.ModulesAnalyzer;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * @author dmitry.mamonov
 *         Created: 2014-11-14 9:56 PM
 */
public class Main {
    public static void main(final String[] args) throws IOException {
        final ModulesAnalyzer analyzer = new ModulesAnalyzer();
        analyzer.setDir(new File(System.getProperty("micro.out_dir", ".micro-report")));
        analyzer.setRenderGraphToPng(Boolean.parseBoolean(System.getProperty("micro.render_graph_to_png", "false")));
        final File reportIndexFile = analyzer.analyzeToDir(args);
        if (Boolean.parseBoolean(System.getProperty("micro.open_index", "false"))) {
            Desktop.getDesktop().open(reportIndexFile);
        }
    }
}
