package org.micromodules.test.project.standalone;

import org.micromodules.setup.Contract;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-31 11:34 AM
 */
@Contract(__module__.Standalone2Module.class)
public class Standalone2 {
    public void wrongUsageOnStandalone1(){
        System.out.println(Standalone1ContractByName.class);
    }
}
