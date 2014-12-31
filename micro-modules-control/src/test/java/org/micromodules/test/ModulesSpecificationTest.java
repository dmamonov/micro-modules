package org.micromodules.test;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.micromodules.control.scan.ClasspathRelations;
import org.micromodules.control.spec.ModuleSpec;
import org.micromodules.control.spec.ModulesSpecification;
import org.micromodules.test.project.common.Common1;
import org.micromodules.test.project.common.Common1Impl;
import org.micromodules.test.project.common.__module__;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-30 6:15 PM
 */
public class ModulesSpecificationTest {
    private final ModulesSpecification modulesSpecification;

    public ModulesSpecificationTest() {
        final ClasspathRelations classpathRelations = ClasspathRelations.createFrom(Thread.currentThread().getContextClassLoader(), "org.micromodules.test.project");
        this.modulesSpecification = ModulesSpecification.createFrom(classpathRelations);
    }

    @Test
    public void testCliModuleSpec() throws Exception {
        final Optional<ModuleSpec> common1ModuleOptional = modulesSpecification.getModuleSpecSet().stream().filter(spec -> spec.getModule().equals(__module__.Common1Module.class)).findFirst();
        assertEquals(true, common1ModuleOptional.isPresent());
        final ModuleSpec common1ModuleSpec = common1ModuleOptional.get();
        assertEquals(ImmutableSet.<Class<?>>of(Common1.class), common1ModuleSpec.getContractClasses());
        assertEquals(ImmutableSet.<Class<?>>of(Common1Impl.class), common1ModuleSpec.getImplementationClasses());
        assertEquals(false, common1ModuleSpec.isDeprecated());
    }
}
