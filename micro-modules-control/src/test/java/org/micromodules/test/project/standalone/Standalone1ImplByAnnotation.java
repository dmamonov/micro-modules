package org.micromodules.test.project.standalone;

import org.micromodules.setup.Implementation;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-31 11:42 AM
 */
@Implementation(__module__.Standalone1Module.class)
public class Standalone1ImplByAnnotation implements Standalone1ContractByAnnotation {
}
