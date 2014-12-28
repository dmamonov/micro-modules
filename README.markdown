#Micro Modules Framework

Allow to control code dependencies at class level precision without affecting current project structure.

##Inspiration

Working with a distributed team on a project of a several hundred thousands lines of code
I found it hard to control code usages at class level. Once most of logic is accessible
everywhere it started to be usual problem when, for example, developers use user interface models
in core business logic. Idea of this framework is to get rid of such problems without increasing
project complexity and not even require any changes in code structure at first step.

##Alternatives Overview

__Maven__ - de facto standard tool for managing dependencies of a compilation units (aka jars).
Maven is fine to manage dependencies in big, but it starts to be insane to try to describe
each business logic module (which is usually several classes) as a separate maven module.

__DI/IoC Frameworks__ - (Spring, Guice, etc.) is more about decoupling and wiring logic together.
They helps a lot on their field, especially in wiring everything with everything in a every way you like,
independently if it's rational or not. They just allow.

__OSGi__ - is just a monster from my point of view. Yet it require to completely reorganize project which
is not acceptable for me.

__Java Modularity__ - still a concept and has no meaning in production use currently.

__Summary__ - this tools miss concept of deny usages in general.

##Micro Modules Framework Concept

It is all about:
  - grouping classes spread across packages into distinct modules,
  - making hierarchies of this modules,
  - defining allowed dependencies between them
  - and checking all of this fit together properly.

###Micro Module Definition

Properly defined module is a java class which apply to this conventions:
  - Module class is always an inner class placed in enclosing class named `__module__` or `__modules__`
  - Module class implements interface `Module` or extends from super module.

    public class __modules__ {
        public static class ExampleModule implements Module {
            public void setup(final ModuleSetup setup) {
               //configure module here.
            }
        }
    }