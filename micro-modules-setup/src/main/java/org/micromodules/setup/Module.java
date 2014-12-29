package org.micromodules.setup;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-20 10:49 PM
 */
public interface Module extends Setup<ModuleSetup> {

    interface Partial extends Module {

    }
}
