package org.micromodules.control.spec;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.micromodules.setup.Module;

/**
* @author dmitry.mamonov
*         Created: 2014-12-29 5:22 PM
*/
public interface ModuleSpec {
    default String getId(){
        return getModule().getName();
    }
    Class<? extends Module> getModule();

    boolean isDeprecated();
    ImmutableList<String> getComments();
    ImmutableSet<Class<?>> getImplementationClasses();
    ImmutableSet<Class<?>> getContractClasses();
    ImmutableSet<Class<?>> getAllClasses();
    ImmutableSet<Class<? extends Module>> getAllowedDependencies();
}
