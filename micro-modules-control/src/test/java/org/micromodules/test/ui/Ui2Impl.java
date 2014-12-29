package org.micromodules.test.ui;

import org.micromodules.test.business.Business1;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-26 9:59 PM
 */
public class Ui2Impl implements Ui2 {
    public void properUsageOfBusinessModule(){
        System.out.println(Business1.class);
    }
}
