package org.micromodules.control.scan;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import org.micromodules.control.util.MapToSet;
import org.micromodules.setup.Contract;
import org.micromodules.setup.Implementation;
import org.micromodules.setup.Module;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-29 3:48 PM
 */
@Contract(__modules__.ClasspathModule.class)
public class ClasspathRelations {
    private final ImmutableSet<Class<? extends Module>> modulesSet;
    private final ImmutableSet<Class<?>> classesSet;
    private final ImmutableMap<String, ImmutableSet<Class<?>>> packageToClassMap;
    private final ImmutableMap<Class<? extends Module>,ImmutableSet<Class<?>>> moduleToAnnotatedContractClassesMap;
    private final ImmutableMap<Class<? extends Module>,ImmutableSet<Class<?>>> moduleToAnnotatedImplementationClassesMap;
    private final Map<String, String> classToJarCache = new HashMap<>();
    private final ImmutableMap<String, ImmutableSet<String>> classContainsClassesMap;
    private final ImmutableMap<String, ImmutableSet<String>> classToDependencyClassMap;


    public static ClasspathRelations createFrom(final ClassLoader classLoader, final String... packagePrefixes) {
        try {
            return new ClasspathRelations(classLoader, ImmutableSet.copyOf(packagePrefixes));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ClasspathRelations(final ClassLoader classLoader, final ImmutableSet<String> packagePrefixList) throws IOException {
        final ImmutableSet<ClassPath.ClassInfo> allClasses = ClassPath.from(classLoader).getAllClasses();

        final Set<Class<? extends Module>> modulesSetMutable = new HashSet<>();
        final Set<Class<?>> classesSetMutable = new HashSet<>();
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        final MapToSet<String, Class<?>> packageToClassMapMutable = new MapToSet<>();
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        final MapToSet<Class<? extends Module>,Class<?>> moduleToAnnotatedContractClassesMapMutable = new MapToSet<>();
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        final MapToSet<Class<? extends Module>,Class<?>> moduleToAnnotatedImplementationClassesMapMutable = new MapToSet<>();
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        final MapToSet<String, String> classContainsClassesMapMutable = new MapToSet<>();
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        final MapToSet<String, String> classToDependencyClassMapMutable = new MapToSet<>();

        for (final ClassPath.ClassInfo classInfo : allClasses) {
            for(final String packagePrefix: packagePrefixList) {
                if (classInfo.getPackageName().startsWith(packagePrefix)) {
                    final Class<?> clazz = classInfo.load();
                    if (Module.class.isAssignableFrom(clazz) && !clazz.isInterface() && !Module.Partial.class.isAssignableFrom(clazz)) {
                        //noinspection unchecked
                        modulesSetMutable.add((Class<? extends Module>) clazz);
                    }
                    packageToClassMapMutable.get(classInfo.getPackageName()).add(clazz);
                    classesSetMutable.add(clazz);
                    { //ContractClass annotation
                        final Contract contract = clazz.getAnnotation(Contract.class);
                        if (contract!=null) {
                            moduleToAnnotatedContractClassesMapMutable.get(contract.value()).add(clazz);
                        }
                    }
                    { //Implementation annotation
                        final Implementation implementation = clazz.getAnnotation(Implementation.class);
                        if (implementation!=null){
                            moduleToAnnotatedImplementationClassesMapMutable.get(implementation.value()).add(clazz);
                        }
                    }
                    break;
                }
            }
        }

        final ClassPool pool = new ClassPool();
        pool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
        for (final Class<?> clazz : classesSetMutable) {
            new Runnable() {
                @Override
                public void run() {
                    try {
                        final CtClass ctClass = pool.get(clazz.getName());
                        addClassAsNode(ctClass, ctClass);
                    } catch (final NotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }

                void addClassAsNode(final CtClass ctClass, final CtClass trackAs) throws NotFoundException {
                    addRelations(trackAs, ctClass.getClassFile().getConstPool().getClassNames());
                    addRelations(trackAs, ctClass.getRefClasses());
                    addRelations(trackAs, ImmutableList.of(ctClass.getSuperclass().getName()));
                    final CtClass[] interfaces = ctClass.getInterfaces();
                    if (interfaces != null) {
                        for (final CtClass face : interfaces) {
                            addRelations(trackAs, ImmutableList.of(face.getName()));
                        }
                    }

                    final CtClass[] nestedClasses = ctClass.getNestedClasses();
                    if (nestedClasses != null) {
                        for (final CtClass nestedCtClass : nestedClasses) {
                            addClassAsNode(nestedCtClass, trackAs);
                            classContainsClassesMapMutable.get(trackAs.getName()).add(nestedCtClass.getName());
                        }
                    }
                }

                private void addRelations(final CtClass sourceClass, final Iterable refClasses) {
                    if (refClasses != null) {
                        final String sourceClassName = sourceClass.getName();
                        for (final Object classNameObj : refClasses) {
                            final String refClassName = (String) classNameObj;
                            classToDependencyClassMapMutable.get(sourceClassName).add(refClassName);
                        }
                    }
                }
            }.run();
        }

        this.modulesSet = ImmutableSet.copyOf(modulesSetMutable);
        this.classesSet = ImmutableSet.copyOf(classesSetMutable);
        this.packageToClassMap = packageToClassMapMutable.convertToImmutableMap();
        this.moduleToAnnotatedContractClassesMap = moduleToAnnotatedContractClassesMapMutable.convertToImmutableMap();
        this.moduleToAnnotatedImplementationClassesMap = moduleToAnnotatedImplementationClassesMapMutable.convertToImmutableMap();
        this.classContainsClassesMap = classContainsClassesMapMutable.convertToImmutableMap();
        this.classToDependencyClassMap = classToDependencyClassMapMutable.convertToImmutableMap();
    }

    public ImmutableSet<Class<? extends Module>> getModulesSet() {
        return modulesSet;
    }

    public ImmutableSet<Class<?>> getClassesSet() {
        return classesSet;
    }

    public ImmutableSet<Class<?>> getPackageToClasses(final String packageName) {
        return replaceNullWithEmptySet(packageToClassMap.get(packageName));
    }

    public ImmutableSet<Class<?>> getModuleToAnnotatedContractClasses(final Class<? extends Module> moduleClazz) {
        return replaceNullWithEmptySet(moduleToAnnotatedContractClassesMap.get(moduleClazz));
    }

    public ImmutableSet<Class<?>> getModuleToAnnotatedImplementationClasses(final Class<? extends Module> moduleClazz) {
        return replaceNullWithEmptySet(moduleToAnnotatedImplementationClassesMap.get(moduleClazz));
    }

    public ImmutableSet<String> getClassContainsClasses(final String className) {
        return replaceNullWithEmptySet(classContainsClassesMap.get(className));
    }

    private <T> ImmutableSet<T> replaceNullWithEmptySet(final ImmutableSet<T> result){
        return result != null ? result : ImmutableSet.of();
    }

    //TODO [DM] use class as key
    public ImmutableMap<String, ImmutableSet<String>> getClassToDependencyClassMap() {
        return classToDependencyClassMap;
    }

    public String getJarName(final String clazzName) {
        if (!classToJarCache.containsKey(clazzName)) {
            final Class clazz;
            try {
                clazz = Class.forName(clazzName);
            } catch (final ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            final URL resource = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class");
            if (resource != null) {
                final String[] urlItems = resource.toString().split("/");
                for (final String item : urlItems) {
                    if (item.endsWith("!")) {
                        classToJarCache.put(clazzName, item.replaceAll("!", ""));
                        break;
                    }
                }
            }

            if (!classToJarCache.containsKey(clazzName)) {
                classToJarCache.put(clazzName, "default");
            }
        }
        return classToJarCache.get(clazzName);
    }

}
