package org.micromodules.control;

import org.micromodules.control.graph.__modules__.ModulesGraphModule;
import org.micromodules.control.report.__modules__.ReportModule;
import org.micromodules.control.scan.__modules__.ClasspathModule;
import org.micromodules.control.spec.__modules__.SpecificationModule;
import org.micromodules.control.util.__modules__.UtilModule;
import org.micromodules.setup.Module;
import org.micromodules.setup.ModuleSetup;
import org.micromodules.setup.__modules__.SetupModule;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-24 4:58 PM
 */
@SuppressWarnings("UnusedDeclaration")
public interface  __modules__ {
    public static class ControlSuperModule implements Module {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("super module")
                    .dependencies().grant(SetupModule.class)
                    .dependencies().grant(UtilModule.class);
        }
    }

    public static class CliModule extends ControlSuperModule {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("console entry point")
                    .dependencies().grant(ReportModule.class)
                    .dependencies().grant(SpecificationModule.class)
                    .dependencies().grant(ClasspathModule.class)
                    .dependencies().grant(ModulesGraphModule.class);
        }
    }
}