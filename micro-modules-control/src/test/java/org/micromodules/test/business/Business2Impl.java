package org.micromodules.test.business;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-26 9:58 PM
 */
public class Business2Impl implements Business2 {
    public void properUseOfModuleContractClass() {
        System.out.println(Business1.class);
    }
}
