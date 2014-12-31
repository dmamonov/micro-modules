package org.micromodules.test;

import org.junit.Test;
import org.micromodules.control.Main;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-30 7:00 PM
 */
public class CliTest {
    @Test
    public void testCliReport() throws Exception {
        Main.main("org.micromodules.test.project");
    }
}
