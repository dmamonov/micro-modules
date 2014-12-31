package org.micromodules.test;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.micromodules.control.scan.ClasspathRelations;
import org.micromodules.setup.Module;
import org.micromodules.test.project.__module__;
import org.micromodules.test.project.business.Business1;
import org.micromodules.test.project.business.Business1Impl;
import org.micromodules.test.project.business.Business2Impl;

import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Sets.intersection;
import static org.junit.Assert.assertEquals;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-30 2:31 PM
 */
public class AbstractClasspathRelationsTest {
    public static final ImmutableSet<Class<?>> expectedModuleWrappers = ImmutableSet.<Class<?>>builder()
            .add(__module__.class)
            .add(org.micromodules.test.project.common.__module__.class)
            .add(org.micromodules.test.project.business.__module__.class)
            .add(org.micromodules.test.project.ui.__module__.class)
            .build();
    public static final ImmutableSet<Class<? extends Module>> expectedModules = ImmutableSet.<Class<? extends Module>>builder()
            .add(__module__.EntireApplication.class)
            .add(__module__.StandaloneLayer.class)
            .add(__module__.CommonLayer.class)
            .add(__module__.BusinessLayer.class)
            .add(__module__.UiLayer.class)
            .add(org.micromodules.test.project.common.__module__.Standalone1Module.class)
            .add(org.micromodules.test.project.common.__module__.Common1Module.class)
            .add(org.micromodules.test.project.business.__module__.Business1Module.class)
            .add(org.micromodules.test.project.business.__module__.Business2Module.class)
            .add(org.micromodules.test.project.ui.__module__.Ui1Module.class)
            .add(org.micromodules.test.project.ui.__module__.Ui2Module.class)
            .add(org.micromodules.test.project.ui.__module__.Ui3Module.class)
            .build();
    public static final ImmutableSet<Class<?>> exampleClassesToBeFound = ImmutableSet.<Class<?>>builder()
            .add(Business1.class)
            .add(Business1.SubInterfaceInBusiness1.class)
            .add(Business1.SubInterfaceInBusiness1.SubSubInterfaceInBusiness1.SubSubSubClassInBusiness1.class)
            .add(Business1.SubInterfaceInBusiness1.SubSubInterfaceInBusiness1.SubSubSubClassInBusiness1.class)
            .build();
    private final ClasspathRelations classpathRelations;

    public AbstractClasspathRelationsTest() {
        this.classpathRelations = ClasspathRelations.createFrom(Thread.currentThread().getContextClassLoader(), "org.micromodules.test.project");
    }

    @Test
    public void testAllModulesFound() throws Exception {
        assertEquals(
                classpathRelations.getModulesSet(),
                expectedModules
        );
    }

    @Test
    public void testClassesNotContainsModules() throws Exception {
        assertEquals(ImmutableSet.<Class<?>>of(), copyOf(intersection(classpathRelations.getClassesSet(), expectedModules)));
    }

    @Test
    public void testClassesNotContainsModuleWrappers() throws Exception {
        assertEquals(ImmutableSet.<Class<?>>of(), copyOf(intersection(classpathRelations.getClassesSet(), expectedModuleWrappers)));

    }

    @Test
    public void testSubclassFound() throws Exception {
        assertEquals(exampleClassesToBeFound, copyOf(intersection(classpathRelations.getClassesSet(),exampleClassesToBeFound)));

    }

    @Test
    public void testUsageEventThoughtItWrong() throws Exception {
        final ImmutableSet<String> business1Dependencies = classpathRelations.getClassToDependencyClassMap().get(Business1Impl.class.getName());
        assertEquals(true, business1Dependencies.contains(Business2Impl.class.getName()));

    }
}
