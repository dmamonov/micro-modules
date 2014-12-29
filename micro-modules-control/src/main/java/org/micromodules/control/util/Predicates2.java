package org.micromodules.control.util;

import com.google.common.base.Predicate;
import org.micromodules.setup.Contract;

import static com.google.common.base.Predicates.not;

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

    public interface BooleanMixin<T> extends Predicate<T> {
        default Predicate<T> and(final Predicate<T> other) {
            return Predicates2.and(this, other);
        }

        default Predicate<T> andNot(final Predicate<T> other) {
            return Predicates2.and(this, not(other));
        }

        default Predicate<T> or(final Predicate<T> other) {
            return Predicates2.or(this, other);
        }
    }
}
