package org.micromodules.control.scan;

import org.micromodules.control.__modules__.ControlSuperModule;
import org.micromodules.setup.ModuleSetup;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-24 4:58 PM
 */
@SuppressWarnings("UnusedDeclaration")
public interface  __modules__ {
    public static class ClasspathModule extends ControlSuperModule {
        @Override
        public void setup(final ModuleSetup setup) {
            setup.comment("Scan classpath byte code structure");
        }
    }
}