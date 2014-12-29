package org.micromodules.control.util;

import org.micromodules.control.__modules__.ControlSuperModule;
import org.micromodules.setup.ModuleSetup;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-24 4:58 PM
 */
@SuppressWarnings("UnusedDeclaration")
public class __modules__ {
    public static class UtilModule extends ControlSuperModule {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("Logic not found in core libraries");
        }
    }
}