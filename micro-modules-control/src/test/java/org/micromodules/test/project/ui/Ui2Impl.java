package org.micromodules.test.project.ui;

import org.micromodules.test.project.business.Business1;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-26 9:59 PM
 */
public class Ui2Impl implements Ui2 {
    public void properUsageOfBusinessModule(){
        System.out.println(Business1.class);
    }
}
