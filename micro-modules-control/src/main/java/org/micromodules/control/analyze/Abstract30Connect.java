package org.micromodules.control.analyze;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-25 5:25 PM
 */
abstract class Abstract30Connect extends Abstract20Scan{
    private final Map<String, String> classToModuleMap = new HashMap<>();
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final MapToSet<String, String> classContainsClassesMap = new MapToSet<>();
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final MapToSet<String, String> classToDependencyClassMap = new MapToSet<>();
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final MapToSet<String, String> moduleToDependencyModuleMap = new MapToSet<>();


    protected void connect() {
        new Runnable(){
            @Override
            public void run() {
                connectClassToModule();
                connectClassToClass();
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

            private void connectClassToClass() {
                final ClassPool pool = new ClassPool();
                pool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
                for (final Class<?> clazz : listClasses()) {
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
                            if (interfaces!=null) {
                                for (final CtClass face : interfaces) {
                                    addRelations(trackAs, ImmutableList.of(face.getName()));
                                }
                            }

                            final CtClass[] nestedClasses = ctClass.getNestedClasses();
                            if (nestedClasses != null) {
                                for (final CtClass nestedCtClass : nestedClasses) {
                                    addClassAsNode(nestedCtClass, trackAs);
                                    classContainsClassesMap.get(ctClass.getName()).add(nestedCtClass.getName());
                                }
                            }
                        }

                        private void addRelations(final CtClass sourceClass, final Iterable refClasses) {
                            if (refClasses != null) {
                                final String sourceClassName = sourceClass.getName();
                                for (final Object classNameObj : refClasses) {
                                    final String refClassName = (String) classNameObj;
                                    classToDependencyClassMap.get(sourceClassName).add(refClassName);
                                    //noinspection ConstantConditions
                                    if (fineDebug) {
                                        System.out.println(sourceClassName + " depends on " + refClassName);
                                    }
                                }
                            }
                        }
                    }.run();
                }
            }



            protected void connectModuleToModule(){
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
    }

    @Override
    ImmutableSet<String> getModuleDependencies(final String moduleId) {
        return ImmutableSet.copyOf(Sets.difference(moduleToDependencyModuleMap.get(moduleId), ImmutableSet.of(moduleId)));
    }

    ImmutableSet<String> getClassDependencies(final Class<?> clazz) {
        return getClassDependencies(clazz.getName());
    }

    ImmutableSet<String> getClassDependencies(final String clazzName){
        return ImmutableSet.copyOf(classToDependencyClassMap.get(clazzName));
    }

    ImmutableSet<String> getClassSubclasses(final Class<?> clazz){
        return getClassSubclasses(clazz.getName());
    }

    ImmutableSet<String> getClassSubclasses(final String clazzName){
        return ImmutableSet.copyOf(classContainsClassesMap.get(clazzName));
    }

    private final Map<String, String> classToJarMap = new HashMap<>();
    String getJarName(final String clazzName){
        if (!classToJarMap.containsKey(clazzName)){
            final Class clazz;
            try {
                clazz = Class.forName(clazzName);
            } catch (final ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            final URL resource = clazz.getResource("/"+clazz.getName().replace('.', '/') + ".class");
            if (resource!=null) {
                final String[] urlItems = resource.toString().split("/");
                for (final String item : urlItems) {
                    if (item.endsWith("!")){
                        classToJarMap.put(clazzName, item.replaceAll("!",""));
                        break;
                    }
                }
            }

            if (!classToJarMap.containsKey(clazzName)) {
                classToJarMap.put(clazzName, "default");
            }
        }
        return classToJarMap.get(clazzName);
    }
}
