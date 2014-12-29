package org.micromodules.setup;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-20 11:26 PM
 */
public interface ClassesFilter<T> {
    ClassesPattern<T> include();
    ClassesPattern<T> exclude();
    T none();
}
