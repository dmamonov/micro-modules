package org.micromodules.test;

import org.micromodules.setup.Module;
import org.micromodules.setup.ModuleSetup;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-26 9:56 PM
 */
public class __module__ {
    public static class EntireApplication implements Module {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("Show modules hierarchy");
        }
    }

    public static class StandaloneLayer extends EntireApplication {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("Standalone logic level");
        }
    }

    public static class CommonLayer implements Module {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("Common logic layer")
                    .dependencies().grant(StandaloneLayer.class)
                    .dependencies().grant(CommonLayer.class);
        }
    }

    public static class BusinessLayer extends EntireApplication {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("Business layer")
                    .dependencies().grant(StandaloneLayer.class)
                    .dependencies().grant(CommonLayer.class)
                    .dependencies().grant(BusinessLayer.class);
        }
    }

    public static class UiLayer extends EntireApplication {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("User interface layer")
                    .dependencies().grant(StandaloneLayer.class)
                    .dependencies().grant(CommonLayer.class)
                    .dependencies().grant(BusinessLayer.class)
                    .comment("UI components must be independent");
        }
    }
}
