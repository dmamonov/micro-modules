package org.micromodules.control.analyze;

import org.micromodules.setup.Module;
import org.micromodules.setup.ModuleSetup;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-24 4:58 PM
 */
@SuppressWarnings("UnusedDeclaration")
public class __modules__ {
    public static class AnalyzeModule implements Module {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("Analysis and reporting");
        }
    }
}