package org.micromodules.control.analyze;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import org.micromodules.setup.Module;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-25 5:23 PM
 */
abstract class Abstract20Scan extends Abstract10Domain {
    private final List<ModuleSetupImpl> moduleSetupSet = new ArrayList<>();
    private final List<ModuleSpec> moduleSpecSet = new ArrayList<>();
    private final Set<Class<?>> classSet = new LinkedHashSet<>();
    private final MapToSet<String, Class<?>> packageToClassMap = new MapToSet<>();

    protected Iterable<ModuleSetupImpl> listModuleSetup() {
        return checkNotNull(moduleSetupSet, "modules not scanned");
    }

    protected Iterable<ModuleSpec> listModuleSpec() {
        return checkNotNull(moduleSpecSet, "modules not scanned");
    }

    protected Iterable<Class<?>> listClasses() {
        return checkNotNull(classSet, "modules not scanned");
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
                    classSet.add(clazz);
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
