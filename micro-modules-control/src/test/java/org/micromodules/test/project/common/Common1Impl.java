package org.micromodules.test.project.common;

import org.micromodules.test.project.standalone.Standalone1ContractByName;
import org.micromodules.test.project.ui.Ui1;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-26 9:58 PM
 */
public class Common1Impl implements Common1 {
    public void wrongUseOfUpperLevelModule(){
        System.out.println(Ui1.class);
    }

    public void properUsageOfStandaloneLogic(){
        System.out.println(Standalone1ContractByName.class);
    }
}
