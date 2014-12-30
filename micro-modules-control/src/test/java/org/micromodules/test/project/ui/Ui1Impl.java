package org.micromodules.test.project.ui;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-26 9:58 PM
 */
public class Ui1Impl implements Ui1 {
    public void wrongUseLoginFromSameLevel(){
        System.out.println(Ui2.class);
    }
}
