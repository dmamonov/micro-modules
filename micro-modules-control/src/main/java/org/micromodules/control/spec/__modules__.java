package org.micromodules.control.spec;

import org.micromodules.setup.Module;
import org.micromodules.setup.ModuleSetup;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-24 4:58 PM
 */
@SuppressWarnings("UnusedDeclaration")
public class __modules__ {
    public static class SpecificationModule implements Module {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("setup interface impl.")
                    .implementation().include().allInPackage();
        }
    }
}