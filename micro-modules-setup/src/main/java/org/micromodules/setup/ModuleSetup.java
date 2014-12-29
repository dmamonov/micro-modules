package org.micromodules.setup;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-20 10:45 PM
 */
public interface ModuleSetup {

    ModuleSetup comment(final String comment);

    ClassesFilter<ModuleSetup> contract();

    ClassesFilter<ModuleSetup> implementation();

    DependenciesSetup dependencies();

    void packageMustBeBlank();

    ModuleSetup deprecated(final String comment);

    ModuleSetup addContentFromDifferentPackage(final Module.Partial partialModule);

}
