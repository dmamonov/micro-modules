package org.micromodules.control.analyze;

import com.google.common.collect.ImmutableSet;

import java.io.File;
import java.io.IOException;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-25 5:24 PM
 */
public class ModulesAnalyzer extends Abstract40Report {

    public File analyzeToDir(final String... packagePrefixes) {
        try {
            this.scan(ImmutableSet.copyOf(packagePrefixes));
            this.connect();
            this.constructGraph();
            return this.report();
        } catch (final IOException | IllegalAccessException | InstantiationException oops) {
            throw new RuntimeException("Failed to analyze project", oops);
        }
    }
}
