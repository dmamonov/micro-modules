package org.micromodules.test;

import org.junit.Test;
import org.micromodules.control.Main;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-29 2:45 PM
 */
public class TestModules {
    @Test
    public void testModules() {
        System.setProperty("micromodules.open_index","true");
        Main.main("org.micromodules.test");
    }
}
