package org.micromodules.control.analyze;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import org.micromodules.setup.Module;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-25 5:28 PM
 */
abstract class Abstract00Core {
    protected final boolean fineDebug = false;

    abstract ImmutableSet<String> getModuleDependencies(String moduleId);

    abstract ImmutableSet<Class<?>> getAnnotatedContractClasses(Class<? extends Module> moduleClazz);

    abstract ImmutableSet<Class<?>> getAnnotatedImplementationClasses(Class<? extends Module> moduleClazz);

    abstract ImmutableSet<String> getClassSubclasses(final Class<?> clazz);

    abstract Class<?> getClassByName(final String className);

    protected static <T> Predicate<T> and(final Predicate<T> left, final Predicate<T> right) {
        return new Predicate<T>() {
            @Override
            public boolean apply(final T input) {
                return left.apply(input) && right.apply(input);
            }
        };
    }

    protected static <T> Predicate<T> or(final Predicate<T> left, final Predicate<T> right) {
        return new Predicate<T>() {
            @Override
            public boolean apply(final T input) {
                return left.apply(input) || right.apply(input);
            }
        };
    }
}
