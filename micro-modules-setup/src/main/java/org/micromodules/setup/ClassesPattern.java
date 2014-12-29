package org.micromodules.setup;

import java.util.regex.Pattern;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-20 10:43 PM
 */
public interface ClassesPattern<T> {
    T matchByPattern(Pattern regexp);

    T allInPackage();

    T matchByPrefix(final String prefix);

    T matchBySuffix(final String suffix);

    T matchByName(final String simpleClassName);
}
