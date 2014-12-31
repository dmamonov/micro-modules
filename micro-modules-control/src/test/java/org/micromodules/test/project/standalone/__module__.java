package org.micromodules.test.project.standalone;

import org.micromodules.setup.ModuleSetup;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-31 11:34 AM
 */
public interface __module__ {
    class Standalone1Module extends org.micromodules.test.project.__module__.StandaloneLayer {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("Logic without local dependencies")
                    .contract().include().matchByName("Standalone1ContractByName")
                    .implementation().include().matchByName("Standalone1ImplByName");
        }
    }
    class Standalone2Module extends org.micromodules.test.project.__module__.StandaloneLayer {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("Logic without local dependencies");
        }
    }
}
