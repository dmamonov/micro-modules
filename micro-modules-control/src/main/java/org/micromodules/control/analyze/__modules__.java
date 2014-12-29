package org.micromodules.control.analyze;

import org.micromodules.control.__modules__.ControlSuperModule;
import org.micromodules.control.graph.__modules__.ModulesGraphModule;
import org.micromodules.setup.ModuleSetup;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-24 4:58 PM
 */
@SuppressWarnings("UnusedDeclaration")
public interface  __modules__ {
    public static class AnalyzeModule extends ControlSuperModule {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("Analysis and reporting")
                    .dependencies().grant(ModulesGraphModule.class);
        }
    }
}