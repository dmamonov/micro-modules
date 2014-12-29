package org.micromodules.test.ui;

import org.micromodules.test.business.Business2Impl;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-26 9:59 PM
 */
public class Ui3Impl implements Ui3 {
    public void wrongUsageOfImplementation(){
        System.out.println(Business2Impl.class);
    }
}
