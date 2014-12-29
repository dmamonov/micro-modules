package org.micromodules.control.graph;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.jgrapht.DirectedGraph;
import org.jgrapht.ext.DOTExporter;
import org.micromodules.control.scan.ClasspathRelations;
import org.micromodules.control.spec.ModulesSpecification;
import org.micromodules.setup.Contract;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-29 4:42 PM
 */
@Contract(__modules__.ModulesGraphModule.class)
public class GraphRenderer {
    private static final DefaultExecutor executor = new DefaultExecutor();
    private final boolean renderGraphToPng;

    public static GraphRenderer createDonAndPngRenderer(){
        return new GraphRenderer(true);

    }

    public static GraphRenderer createDonOnlyRenderer(){
        return new GraphRenderer(false);
    }

    private GraphRenderer(final boolean renderGraphToPng) {
        this.renderGraphToPng = renderGraphToPng;
    }

    public File export(final DirectedGraph<GraphDomain.Node, GraphDomain.NodeEdge> g, final File dir, final String name) {
        final DOTExporter<GraphDomain.Node, GraphDomain.NodeEdge> exporter = new DOTExporter<>(
                GraphDomain.Node::toString,
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
                if (renderGraphToPng) {
                    if (g.vertexSet().size() <= 100) {
                        System.out.println("  rendering: " + svgFile);
                        final CommandLine command = new CommandLine("dot");
                        command.addArguments(new String[]{dotFile.getAbsolutePath(), "-T" + "svg", "-o", svgFile.getAbsolutePath()});
                        executor.execute(command);
                    } else {
                        System.out.println("  graph is too big, skipping: " + svgFile);
                    }
                }

            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return svgFile;

    }


    public static ModulesGraph createBySpecificationAndClasspathRelations(ModulesSpecification modulesSpecification, ClasspathRelations classpathRelations) {
        return null;
    }

}
