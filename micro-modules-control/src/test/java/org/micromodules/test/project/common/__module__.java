package org.micromodules.test.project.common;

import org.micromodules.setup.ModuleSetup;
import org.micromodules.test.project.__module__.CommonLayer;
import org.micromodules.test.project.__module__.StandaloneLayer;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-26 9:56 PM
 */
public interface __module__ {


    class Common1Module extends CommonLayer {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("Some basic logic")
                    .contract().include().matchByName("Common1")
                    .implementation().include().matchByName("Common1Impl");
        }
    }


}
