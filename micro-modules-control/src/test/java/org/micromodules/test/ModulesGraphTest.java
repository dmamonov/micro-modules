package org.micromodules.test;

import org.micromodules.control.graph.ModulesGraph;
import org.micromodules.control.scan.ClasspathRelations;
import org.micromodules.control.spec.ModulesSpecification;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-30 7:30 PM
 */
public class ModulesGraphTest {
    private final ModulesGraph modulesGraph;

    public ModulesGraphTest() {
        final ClasspathRelations classpathRelations = ClasspathRelations.createFrom(Thread.currentThread().getContextClassLoader(), "org.micromodules.test.project");
        final ModulesSpecification modulesSpecification = ModulesSpecification.createFrom(classpathRelations);
        this.modulesGraph = ModulesGraph.createFrom(classpathRelations, modulesSpecification);
    }
}
