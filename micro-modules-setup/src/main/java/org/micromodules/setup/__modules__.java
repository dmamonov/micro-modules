package org.micromodules.setup;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-24 4:58 PM
 */
@SuppressWarnings("UnusedDeclaration")
public class __modules__ {
    public static class SetupModule implements Module {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("Configuration contract")
                    .contract().include().allInPackage()
                    .implementation().none();
        }
    }
}