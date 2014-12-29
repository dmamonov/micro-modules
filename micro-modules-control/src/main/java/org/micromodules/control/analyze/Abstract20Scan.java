package org.micromodules.control.analyze;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import org.micromodules.setup.Contract;
import org.micromodules.setup.Implementation;
import org.micromodules.setup.Module;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-25 5:23 PM
 */
abstract class Abstract20Scan extends Abstract10Domain {
    private final List<ModuleSetupImpl> moduleSetupSet = new ArrayList<>();
    private final List<ModuleSpec> moduleSpecSet = new ArrayList<>();
    private final Map<String, Class<?>> classMap = new HashMap<>();
    private final MapToSet<String, Class<?>> packageToClassMap = new MapToSet<>();
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final MapToSet<Class<? extends Module>,Class<?>> moduleToAnnotatedContractClassesMap = new MapToSet<>();
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final MapToSet<Class<? extends Module>,Class<?>> moduleToAnnotatedImplementationClassesMap = new MapToSet<>();

    protected Iterable<ModuleSetupImpl> listModuleSetup() {
        return checkNotNull(moduleSetupSet, "modules not scanned");
    }

    protected Iterable<ModuleSpec> listModuleSpec() {
        return moduleSpecSet;
    }

    protected Iterable<Class<?>> listClasses() {
        return classMap.values();
    }

    protected Class<?> getClassByName(final String className) {
        return classMap.get(className);
    }

    @Override
    ImmutableSet<Class<?>> getAnnotatedContractClasses(final Class<? extends Module> moduleClazz) {
        return ImmutableSet.copyOf(moduleToAnnotatedContractClassesMap.get(moduleClazz));
    }

    @Override
    ImmutableSet<Class<?>> getAnnotatedImplementationClasses(final Class<? extends Module> moduleClazz) {
        return ImmutableSet.copyOf(moduleToAnnotatedImplementationClassesMap.get(moduleClazz));
    }

    protected void scan(final ImmutableSet<String> packagePrefixList) throws IOException, IllegalAccessException, InstantiationException {
        final ImmutableSet<ClassPath.ClassInfo> allClasses = ClassPath
                .from(Thread.currentThread().getContextClassLoader())
                .getAllClasses();
        final List<Class<? extends Module>> modules = new ArrayList<>();
        for (final ClassPath.ClassInfo classInfo : allClasses) {
            for(final String packagePrefix: packagePrefixList) {
                if (classInfo.getPackageName().startsWith(packagePrefix)) {
                    final Class<?> clazz = classInfo.load();
                    if (Module.class.isAssignableFrom(clazz) && !clazz.isInterface() && !Module.Partial.class.isAssignableFrom(clazz)) {
                        //noinspection unchecked
                        modules.add((Class<? extends Module>)clazz);
                    }
                    packageToClassMap.get(classInfo.getPackageName()).add(clazz);
                    classMap.put(clazz.getName(), clazz);
                    { //Contract annotation
                        final Contract contract = clazz.getAnnotation(Contract.class);
                        if (contract!=null) {
                            moduleToAnnotatedContractClassesMap.get(contract.value()).add(clazz);
                        }
                    }
                    { //Implementation annotation
                        final Implementation implementation = clazz.getAnnotation(Implementation.class);
                        if (implementation!=null){
                            moduleToAnnotatedImplementationClassesMap.get(implementation.value()).add(clazz);
                        }
                    }
                    break;
                }
            }
        }
        for (final Class<? extends Module> moduleClass: modules) {
            System.out.println("Module: "+moduleClass.getName());
            final ModuleSetupImpl setup = new ModuleSetupImpl(moduleClass, packageToClassMap);
            final Module module;
            try {
                final Constructor<? extends Module> constructor = moduleClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                module = constructor.newInstance();
            } catch (final InvocationTargetException|NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            module.setup(setup);
            moduleSetupSet.add(setup);
            moduleSpecSet.add(setup.createSpec());
        }
    }
}
