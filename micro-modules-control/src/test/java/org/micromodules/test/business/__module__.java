package org.micromodules.test.business;

import org.micromodules.setup.ModuleSetup;
import org.micromodules.test.__module__.BusinessLayer;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-26 9:56 PM
 */
public interface  __module__ {
    class Business1Module extends BusinessLayer {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("Business module one")
                    .contract().include().matchByName("Business1")
                    .implementation().include().matchByName("Business1Impl");
        }
    }

    class Business2Module extends BusinessLayer {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("Business module two")
                    .contract().include().matchByName("Business2")
                    .implementation().include().matchByName("Business2Impl");
        }
    }
}
