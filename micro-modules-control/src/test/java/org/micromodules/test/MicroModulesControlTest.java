package org.micromodules.test;

import org.micromodules.control.Main;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-29 2:45 PM
 */
public class MicroModulesControlTest extends ModulesSpecificationTest {
    //@Test()
    public void testControlReport() {
        Main.main("org.micromodules.test.project");
    }
}
