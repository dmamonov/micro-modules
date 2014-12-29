package org.micromodules.test.common;

import org.micromodules.test.ui.Ui1;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-26 9:58 PM
 */
public class Common1Impl implements Common1 {
    public void wrongUseOfUpperLevelModule(){
        System.out.println(Ui1.class);
    }

    public void properUsageOfStandaloneLogic(){
        System.out.println(Standalone1.class);
    }
}
