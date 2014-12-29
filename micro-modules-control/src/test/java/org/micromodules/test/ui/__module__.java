package org.micromodules.test.ui;

import org.micromodules.setup.ModuleSetup;
import org.micromodules.test.__module__.UiLayer;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-26 9:56 PM
 */
public class __module__ {
    public static class Ui1Module extends UiLayer {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("User interface one")
                    .contract().include().matchByName("Ui1")
                    .implementation().include().matchByName("Ui1Impl");
        }
    }

    public static class Ui2Module extends UiLayer {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("User interface two")
                    .contract().include().matchByName("Ui2")
                    .contract().include().matchByName("UiCollision")
                    .implementation().include().matchByName("Ui2Impl");
        }
    }

    public static class Ui3Module extends UiLayer {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("User interface three")
                    .contract().include().matchByName("Ui3")
                    .contract().include().matchByName("UiCollision")
                    .implementation().include().matchByName("Ui3Impl");
        }
    }
}
