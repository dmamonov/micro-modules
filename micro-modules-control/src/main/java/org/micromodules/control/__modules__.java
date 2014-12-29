package org.micromodules.control;

import org.micromodules.setup.Module;
import org.micromodules.setup.ModuleSetup;
import org.micromodules.setup.__modules__.SetupModule;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-24 4:58 PM
 */
@SuppressWarnings("UnusedDeclaration")
public class __modules__ {
    public static class CliModule extends ControlSuperModule {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("console entry point")
                    .dependencies().allow(SetupModule.class);
        }
    }

    public static class ControlSuperModule implements Module {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("super module")
                    .dependencies().allow(SetupModule.class);
        }
    }
}