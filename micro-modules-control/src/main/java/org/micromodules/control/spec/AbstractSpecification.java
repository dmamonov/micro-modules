package org.micromodules.control.spec;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.micromodules.control.scan.ClasspathRelations;
import org.micromodules.setup.ClassesFilter;
import org.micromodules.setup.ClassesPattern;
import org.micromodules.setup.DependenciesSetup;
import org.micromodules.setup.Module;
import org.micromodules.setup.ModuleSetup;

import java.util.ArrayList;
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
 *         Created: 2014-12-29 5:26 PM
 */
abstract class AbstractSpecification {
    protected static class ModuleSetupImpl implements ModuleSetup {
        private Class<? extends Module> moduleClass;
        private final ClasspathRelations classpathRelations;
        private final String name;
        private String deprecated = null;
        private final List<String> comments = new ArrayList<>();
        private boolean packageMustBeBlank = false;
        private final Set<Class<?>> contractInclude = new LinkedHashSet<>();
        private final Set<Class<?>> contractExclude = new LinkedHashSet<>();
        private final Set<Class<?>> implementationInclude = new LinkedHashSet<>();
        private final Set<Class<?>> implementationExclude = new LinkedHashSet<>();
        private final Set<Class<? extends Module>> allowedDependencies = new LinkedHashSet<>();

        public ModuleSetupImpl(final Class<? extends Module> moduleClass, final ClasspathRelations classpathRelations) {
            this.moduleClass = checkNotNull(moduleClass);
            this.classpathRelations = checkNotNull(classpathRelations);
            this.name = moduleClass.getName();

            classpathRelations.getModuleToAnnotatedContractClasses(moduleClass).forEach(contractClazz ->
                    new ClassesFilterImpl(contractClazz.getPackage().getName(), contractInclude, contractExclude)
                            .include().matchByName(contractClazz.getSimpleName()));

            classpathRelations.getModuleToAnnotatedImplementationClasses(moduleClass).forEach(implementationClazz ->
                    new ClassesFilterImpl(implementationClazz.getPackage().getName(), implementationInclude, implementationExclude)
                            .include().matchByName(implementationClazz.getSimpleName()));
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
            return new ClassesFilterImpl(moduleClass.getPackage().getName(), include, exclude);

        }

        private class ClassesFilterImpl implements ClassesFilter<ModuleSetup> {
            private final String basePackageName;
            private final Set<Class<?>> include;
            private final Set<Class<?>> exclude;

            public ClassesFilterImpl(final String basePackageName, final Set<Class<?>> include, final Set<Class<?>> exclude) {
                this.basePackageName = basePackageName;
                this.include = include;
                this.exclude = exclude;
            }

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

                        final Set<Class<?>> classesInPackage = classpathRelations.getPackageToClasses(basePackageName);
                        if (classesInPackage != null) {
                            boolean matched = false;
                            for (final Class<?> clazz : classesInPackage) {
                                if (regexp.matcher(clazz.getName().replaceAll("^.*[.]","").replaceAll("[$].*","")).matches()) {
                                    classesSet.add(clazz);
                                    matched = true;
                                }
                            }
                            checkState(matched, "Nothing matched to pattern: " + regexp + " of module " + moduleClass.getName()+" in package "+basePackageName);
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



        @Override
        public DependenciesSetup dependencies() {
            //noinspection Convert2Lambda
            return new DependenciesSetup() {
                @Override
                public ModuleSetup grant(final Class<? extends Module> moduleClazz) {
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

     /*


    private final Map<String, String> classToModuleMap = new HashMap<>();

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final MapToSet<String, String> classToNestedClassMap = new MapToSet<>();
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final MapToSet<String, String> moduleToDependencyModuleMap = new MapToSet<>();




    private final List<ModuleSetupImpl> moduleSetupSet = new ArrayList<>();

    protected Iterable<ModuleSetupImpl> listModuleSetup() {
        return checkNotNull(moduleSetupSet, "modules not scanned");
    }

    protected Iterable<ModuleSpec> listModuleSpec() {
        return moduleSpecSet;
    }
    */

}
