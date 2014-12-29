package org.micromodules.control.analyze;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.micromodules.setup.ClassesFilter;
import org.micromodules.setup.ClassesPattern;
import org.micromodules.setup.DependenciesSetup;
import org.micromodules.setup.Module;
import org.micromodules.setup.ModuleSetup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.quote;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-25 5:23 PM
 */
abstract class Abstract10Domain extends Abstract00Core {
    class MapToSet<K, V> extends LinkedHashMap<K, Set<V>> {
        @Override
        public Set<V> get(final Object key) {
            final Set<V> value = super.get(key);
            if (value!=null){
                return value;
            } else {
                final Set<V> newValue = new LinkedHashSet<V>(){
                    @Override
                    public boolean add(final V o) {
                        //noinspection SimplifiableIfStatement
                        if (o!=null) {
                            return super.add(o);
                        } else {
                            return false;
                        }
                    }
                };
                //noinspection unchecked
                super.put((K)key, newValue);
                return newValue;
            }
        }
    }

    interface ModuleSpec {
        default String getId(){
            return getModule().getName();
        }
        Class<? extends Module> getModule();

        boolean isDeprecated();
        ImmutableList<String>  getComments();
        ImmutableSet<Class<?>> getImplementationClasses();
        ImmutableSet<Class<?>> getContractClasses();
        ImmutableSet<Class<?>> getAllClasses();
        ImmutableSet<String> getActualDependencies();
        ImmutableSet<Class<? extends Module>> getAllowedDependencies();
    }

    class ModuleSetupImpl implements ModuleSetup {
        private Class<? extends Module> moduleClass;
        private final MapToSet<String, Class<?>> classesByPackage;
        private final String name;
        private String deprecated = null;
        private final List<String> comments = new ArrayList<>();
        private boolean packageMustBeBlank = false;
        private final Set<Class<?>> contractInclude = new LinkedHashSet<>();
        private final Set<Class<?>> contractExclude = new LinkedHashSet<>();
        private final Set<Class<?>> implementationInclude = new LinkedHashSet<>();
        private final Set<Class<?>> implementationExclude = new LinkedHashSet<>();
        private final Set<Class<? extends Module>> allowedDependencies = new LinkedHashSet<>();

        public ModuleSetupImpl(final Class<? extends Module> moduleClass, final MapToSet<String, Class<?>> classesByPackage) {
            this.moduleClass = checkNotNull(moduleClass);
            this.classesByPackage = checkNotNull(classesByPackage);
            this.name = moduleClass.getName();
        }

        @Override
        public ModuleSetup comment(final String comment) {
            this.comments.add(comment);
            return this;
        }

        @Override
        public ClassesFilter<ModuleSetup> contract() {
            return createClassesPattern(contractInclude, contractExclude);
        }

        @Override
        public ClassesFilter<ModuleSetup> implementation() {
            return createClassesPattern(implementationInclude, implementationExclude);
        }
        private ClassesFilter<ModuleSetup> createClassesPattern(final Set<Class<?>> include, final Set<Class<?>> exclude) {
            return new ClassesFilter<ModuleSetup>() {
                @Override
                public ClassesPattern<ModuleSetup> include() {
                    return createClassesPattern(include);
                }

                @Override
                public ClassesPattern<ModuleSetup> exclude() {
                    return createClassesPattern(exclude);
                }

                private ClassesPattern<ModuleSetup> createClassesPattern(final Set<Class<?>> classesSet) {
                    //noinspection Convert2Lambda
                    return new ClassesPattern<ModuleSetup>() {
                        @Override
                        public ModuleSetup matchByPattern(final Pattern regexp) {
                            final Set<Class<?>> classesInPackage = classesByPackage.get(moduleClass.getPackage().getName());
                            if (classesInPackage != null) {
                                boolean matched = false;
                                for (final Class<?> clazz : classesInPackage) {
                                    if (regexp.matcher(clazz.getSimpleName().replace("[$].*","")).matches()) {
                                        classesSet.add(clazz);
                                        matched = true;
                                    }
                                }
                                checkState(matched, "Nothing matched to pattern: " + regexp + " of module " + moduleClass.getName());
                            }
                            return ModuleSetupImpl.this;
                        }

                        @Override
                        public ModuleSetup allInPackage() {
                            return matchByPattern(compile(".*"));
                        }

                        @Override
                        public ModuleSetup matchByPrefix(final String prefix) {
                            return matchByPattern(compile(quote(prefix)+".*"));
                        }

                        @Override
                        public ModuleSetup matchBySuffix(final String suffix) {
                            return matchByPattern(compile(".*"+quote(suffix)));
                        }

                        @Override
                        public ModuleSetup matchByName(final String simpleClassName) {
                            return matchByPattern(compile(quote(simpleClassName)));
                        }
                    };
                }

                @Override
                public ModuleSetup none() {
                    return ModuleSetupImpl.this;
                }
            };
        }



        @Override
        public DependenciesSetup dependencies() {
            //noinspection Convert2Lambda
            return new DependenciesSetup() {
                @Override
                public ModuleSetup allow(final Class<? extends Module> moduleClazz) {
                    ModuleSetupImpl.this.allowedDependencies.add(moduleClazz);
                    return ModuleSetupImpl.this;
                }
            };
        }

        @Override
        public void packageMustBeBlank() {
            this.packageMustBeBlank = true;
        }

        @Override
        public ModuleSetup deprecated(final String comment) {
            checkState(this.deprecated==null, "already deprecated");
            this.deprecated = checkNotNull(comment, "comment required");
            return this;
        }

        @Override
        public ModuleSetup addContentFromDifferentPackage(final Module.Partial partialModule) {
            final Class<? extends Module> backupModuleClass = this.moduleClass;
            this.moduleClass = partialModule.getClass();
            partialModule.setup(this);
            this.moduleClass = backupModuleClass;
            return this;
        }

        protected ModuleSpec createSpec() {
            return new ModuleSpec() {
                private final Class<? extends Module> module = ModuleSetupImpl.this.moduleClass;
                private final String deprecated = ModuleSetupImpl.this.deprecated;
                private final ImmutableList<String> comments = ImmutableList.copyOf(ModuleSetupImpl.this.comments);
                private final ImmutableSet<Class<?>> contractClasses = ImmutableSet.copyOf(Sets.difference(
                        ModuleSetupImpl.this.contractInclude,
                        ModuleSetupImpl.this.contractExclude
                ));
                private final ImmutableSet<Class<?>> implementationClasses = ImmutableSet.copyOf(Sets.difference(Sets.difference(
                        ModuleSetupImpl.this.implementationInclude,
                        ModuleSetupImpl.this.implementationExclude
                ), contractClasses));
                private final ImmutableSet<Class<?>> allClasses = ImmutableSet.copyOf(Sets.union(
                        this.contractClasses,
                        this.implementationClasses
                ));

                @Override
                public Class<? extends Module> getModule() {
                    return module;
                }

                @Override
                public boolean isDeprecated() {
                    return this.deprecated !=null;
                }

                @Override
                public ImmutableList<String> getComments() {
                    return this.comments;
                }

                @Override
                public ImmutableSet<Class<?>> getImplementationClasses() {
                    return this.implementationClasses;
                }

                @Override
                public ImmutableSet<Class<?>> getContractClasses() {
                    return this.contractClasses;
                }

                @Override
                public ImmutableSet<Class<?>> getAllClasses() {
                    return this.allClasses;
                }

                @Override
                public ImmutableSet<String> getActualDependencies() {
                    return getModuleDependencies(getId());
                }

                @Override
                public ImmutableSet<Class<? extends Module>> getAllowedDependencies() {
                    return ImmutableSet.copyOf(ModuleSetupImpl.this.allowedDependencies);
                }
            };

        }

        @Override
        public String toString() {
            return "ModuleSetupImpl{\n" +
                    ", name='" + name + "'\n" +
                    ", deprecated='" + deprecated + "'\n" +
                    ", comments=" + comments + "'\n" +
                    ", packageMustBeBlank=" + packageMustBeBlank + "'\n" +
                    ", contractInclude=" + contractInclude + "'\n" +
                    ", contractExclude=" + contractExclude + "'\n" +
                    ", implementationInclude=" + implementationInclude + "'\n" +
                    ", implementationExclude=" + implementationExclude + "'\n" +
                    '}';
        }
    }

}
