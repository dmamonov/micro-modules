package org.micromodules.control;

import org.micromodules.setup.Contract;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-29 7:05 PM
 */
@Contract(__modules__.CliModule.class)
public class SelfReportMain {
    public static void main(final String[] args) {
        System.setProperty("micromodules.output_dir", ".self-report");
        Main.main("org.micromodules.setup", "org.micromodules.control");
    }
}
