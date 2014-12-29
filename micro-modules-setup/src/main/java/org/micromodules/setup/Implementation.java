package org.micromodules.setup;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-29 1:53 PM
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(TYPE)
public @interface Implementation {
    Class<? extends Module> value();
}
