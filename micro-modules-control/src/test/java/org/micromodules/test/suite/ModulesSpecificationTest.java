package org.micromodules.test.suite;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.micromodules.control.scan.ClasspathRelations;
import org.micromodules.control.spec.ModuleSpec;
import org.micromodules.control.spec.ModulesSpecification;
import org.micromodules.test.project.standalone.Standalone1ContractByAnnotation;
import org.micromodules.test.project.standalone.Standalone1ContractByName;
import org.micromodules.test.project.standalone.Standalone1ImplByAnnotation;
import org.micromodules.test.project.standalone.Standalone1ImplByName;

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
    public void testStandalone1ModuleSpec() throws Exception {
        final Optional<ModuleSpec> common1ModuleOptional = modulesSpecification.getModuleSpecSet().stream()
                .filter(spec -> spec.getModule().equals(org.micromodules.test.project.standalone.__module__.Standalone1Module.class))
                .findFirst();
        assertEquals(true, common1ModuleOptional.isPresent());
        final ModuleSpec standalone1ModuleSpec = common1ModuleOptional.get();
        assertEquals(ImmutableSet.of(Standalone1ContractByName.class, Standalone1ContractByAnnotation.class), standalone1ModuleSpec.getContractClasses());
        assertEquals(ImmutableSet.of(Standalone1ImplByName.class, Standalone1ImplByAnnotation.class), standalone1ModuleSpec.getImplementationClasses());
        assertEquals(false, standalone1ModuleSpec.isDeprecated());
    }
}
