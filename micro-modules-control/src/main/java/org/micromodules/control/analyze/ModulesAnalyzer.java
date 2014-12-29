package org.micromodules.control.analyze;

import org.micromodules.control.graph.ModulesGraph;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-29 5:52 PM
 */
public class ModulesAnalyzer {
    public static ModulesAnalyzer createFrom(final ModulesGraph graph){
        return new ModulesAnalyzer();
    }
}
