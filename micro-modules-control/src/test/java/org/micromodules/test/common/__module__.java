package org.micromodules.test.common;

import org.micromodules.setup.ModuleSetup;
import org.micromodules.test.__module__.CommonLayer;
import org.micromodules.test.__module__.StandaloneLayer;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-26 9:56 PM
 */
public class __module__ {
    public static class Standalone1Module extends StandaloneLayer {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("Logic without logal dependencies")
                    .contract().include().matchByName("Standalone1")
                    .implementation().include().matchByName("Standalone1Impl");
        }
    }

    public static class Common1Module extends CommonLayer {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("Some basic logic")
                    .contract().include().matchByName("Common1")
                    .implementation().include().matchByName("Common1Impl");
        }
    }


}
