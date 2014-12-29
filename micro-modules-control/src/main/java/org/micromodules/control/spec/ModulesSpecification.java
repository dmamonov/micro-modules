package org.micromodules.control.spec;

import com.google.common.collect.ImmutableSet;
import org.micromodules.control.scan.ClasspathRelations;
import org.micromodules.setup.Contract;
import org.micromodules.setup.Module;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-29 4:06 PM
 */
@Contract(__modules__.SpecificationModule.class)
public class ModulesSpecification extends AbstractSpecification {
    public static ModulesSpecification createFrom(final ClasspathRelations classpathRelations) {
        return new ModulesSpecification(classpathRelations);
    }

    private final ImmutableSet<ModuleSpec> moduleSpecSet;

    private ModulesSpecification(final ClasspathRelations classpathRelations) {
        checkNotNull(classpathRelations, "classpathRelations required");

        final HashSet<ModuleSpec> moduleSpecSetMutable = new HashSet<>();
        for (final Class<? extends Module> moduleClass : classpathRelations.getModulesSet()) {
            System.out.println("Module: " + moduleClass.getName());
            final ModuleSetupImpl setup = new ModuleSetupImpl(moduleClass, classpathRelations);
            final Module module;
            try {
                final Constructor<? extends Module> constructor = moduleClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                module = constructor.newInstance();
            } catch (final InstantiationException| IllegalAccessException|InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            module.setup(setup);
            moduleSpecSetMutable.add(setup.createSpec());
        }

        this.moduleSpecSet = ImmutableSet.copyOf(moduleSpecSetMutable);

        /*
        new Runnable() {
            @Override
            public void run() {
                connectClassToModule();
                connectModuleToModule();
            }

            private void connectClassToModule() {
                for (final ModuleSetupImpl moduleSetup : listModuleSetup()) {
                    final ModuleSpec spec = moduleSetup.createSpec();
                    for (final Class<?> classInModule : spec.getAllClasses()) {
                        classToModuleMap.put(classInModule.getName(), spec.getId());
                    }
                }
            }


            protected void connectModuleToModule() {
                for (final ModuleSpec moduleSpec : listModuleSpec()) {
                    final String moduleId = moduleSpec.getId();
                    for (final Class<?> classInModule : moduleSpec.getAllClasses()) {
                        for (final String dependencyClassName : classToDependencyClassMap.get(classInModule.getName())) {
                            moduleToDependencyModuleMap.get(moduleId).add(classToModuleMap.get(dependencyClassName));
                        }
                    }
                }
            }
        }.run();
        */
    }

    public ImmutableSet<ModuleSpec> getModuleSpecSet() {
        return moduleSpecSet;
    }
}
