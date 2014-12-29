package org.micromodules.setup;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-22 2:56 PM
 */
public interface DependenciesSetup {
    ModuleSetup allow(Class<? extends Module> moduleClazz);
}
