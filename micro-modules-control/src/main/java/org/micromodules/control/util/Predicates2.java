package org.micromodules.control.util;

import com.google.common.base.Predicate;
import org.micromodules.setup.Contract;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-29 3:58 PM
 */
@Contract(__modules__.UtilModule.class)
public class Predicates2 {
    public static <T> Predicate<T> and(final Predicate<T> left, final Predicate<T> right) {
        return new Predicate<T>() {
            @Override
            public boolean apply(final T input) {
                return left.apply(input) && right.apply(input);
            }
        };
    }

    public static <T> Predicate<T> or(final Predicate<T> left, final Predicate<T> right) {
        return new Predicate<T>() {
            @Override
            public boolean apply(final T input) {
                return left.apply(input) || right.apply(input);
            }
        };
    }
}
