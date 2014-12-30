package org.micromodules.test.project.business;

import com.google.common.collect.ImmutableList;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-26 9:58 PM
 */
public class Business1Impl implements Business1 {
    public void wrongUseOfImplementationClass(){
        System.out.println(Business2Impl.class);
    }

    public void showGuavaUsage(){
        System.out.println(ImmutableList.class);
    }
}
