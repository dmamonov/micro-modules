#Micro Modules Framework

Allow to control code dependencies at class level precision without affecting current project structure.

##Inspiration

Working with a distributed team on a project of a several hundred thousands lines of code
I found it hard to control code usages at class level.

Once most of logic is accessible everywhere it started to be usual problem when, for example,
developers use user interface models in core business logic.

Idea of this framework is to get rid of such problems without increasing
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

###Module Definition

Properly defined module is a java class which apply to this conventions:
  - Module is *always* an inner static class placed in enclosing class named `__module__` or `__modules__`
  - Module class implements interface `Module` or extends from super module.
  - Module name matches to class simple name, word `Module` in prefix or suffix is omitted.


Here is blank __HelloWorld__ module.

    package org.hello;

    import org.micromodules.setup.Module;
    import org.micromodules.setup.ModuleSetup;

    public class __modules__ {
        public static class HelloWorldModule implements Module {
            public void setup(final ModuleSetup setup) {
               //configure module here.
            }
        }
    }

Implementing `setup` method module defines:
  - Contract classes, which could be used by classes from other modules
  - Implementation classes, which *must not* be referenced from other modules
  - Allowed dependencies to other modules

More complex example with two modules placed in same package:

    public class __modules__ {
        public static class InputOutputModule implements Module {
            public void setup(final ModuleSetup setup) {
               setup.comment("Handles standard input and output for messages")
                   .contract().include().matchByName("InputOutput")
                   .comment("InputOutput is an interface which abstracts console in this example")
                   .implementation().include().matchByName("InputOutputImpl")
                   .comment("InputOutputImpl is a corresponding implementation");
            }
        }

        public static class HelloWorldModule implements Module {
            public void setup(final ModuleSetup setup) {
               setup.comment("Business logic module intended to print Hello World into console using InputOutput")
                   .contract().include().matchByName("HelloWorld")
                   .implementation().include().matchByName("HelloWorldImpl")
                   .dependencies().allow(InputOutputModule.class)
                   .comment("Dependency to InputOutput module defined directly here");
            }
        }
    }

Based on configuration above, classes from module *HelloWorld* directly allowed to use contract
classes of module *InputOutput*. This helps to find not allowed dependency from *InputOutput* module
classes to *HelloWorld" classes. But while project get bigger, managing all allowed dependencies by bare hands
starts to be tedious and, probably, useless. Modules hierarchy concept helps here.

###Modules Hierarchy

Hierarchy between modules defined using regular java inheritance. Once *SuperModule* and *SubModule* joined into hierarchy,
next rules applies:
  - Super module __must not__ be related with any classes.
  - Super module may define allowed dependencies which will be inherited by sub modules.
  - Once dependency refers to some super module, it extends to all sub modules of such.
  - Super module may allow dependencies to it self in order to allow for sub modules to refer each other.


    public class __modules__ {
        public static class IOLayer implements Module {
            public void setup(final ModuleSetup setup) {
               setup.dependencies().allow(IOLayer.class)
                  .comment("Allow to Input and Output modules to interact");
            }
        }

        public static class InputModule extends IOLayer {
            public void setup(final ModuleSetup setup) {
               ...
            }
        }

        public static class OutputModule extends IOLayer {
            public void setup(final ModuleSetup setup) {
               ...
            }
        }

        public static class CommunicationLayer implements Module {
            public void setup(final ModuleSetup setup) {
               setup.dependencies().allow(IOLayer.class)
                  .comment("Allow to refer to Input and Output modules")
                  .comment("But all sub modules must be independent by default");
            }
        }

        public static class HelloWorldModule extends CommunicationLayer {
            public void setup(final ModuleSetup setup) {
               ...
            }
        }

        public static class GoodByeModule extends CommunicationLayer {
            public void setup(final ModuleSetup setup) {
               setup.dependencies().allow(HelloWorldModule.class)
                   .comment("Thought communication layer modules" +
                            " must not interact each other by default")
                   .comment("For this specific module it is allowed to use HelloWorld module");
            }
        }
    }


###Defining Relations between Modules and Classes

Within `setup` method it is possible to connect classes placed in *current* package
to module *implementation* or *contract*. This is done by matching by simple class name patterns.
Once top level class matches pattern - all nested, inlined and lambda classes will match to.

    setup.comment("A bit more complex )
        .contract().include().matchBySuffix("Service")
        .comment("Suppose interface classes end with 'Service' suffix")
        .contract().exclude().matchByName("AbstractService")
        .comment("Except AbstractService which is abstract class")
        .implementation().include().allInPackage()
        .comment("Rest of classes in package will be treated as implementation")
        ...

In order to include classes from other packages, there should be *partial* module (`Module.Partial`)
defined. Then *partial* module should be used in main module configuration:

     setup.comment("Include context from different package using partial module")
         .addPartialModule(new some.other.package.__modules__.SomePartialModuleFromDifferentPackage())
         ...

All this usable in case when it is important to add *micro modules* layer over existing project
not even changing any class in there, yet not introducing any usages of project classes.

Once it is possible to change existing code it is better to use annotations:
   - `@Contract(__modules__.OutputModule.class) public interface Output {`
   - `@Implementation(__modules__.OutputModule.class) public class OutputImpl {...`


###Configuration Summary
It was short yet almost complete reference for configuration. For more practical examples please check this tests: TODO.

In order to setup *micro modules* in project you only need to add:

    <dependency>
        <groupId>org.micromodules</groupId>
        <artifactId>micromodules-setup</artifactId>
        <version>1.0-SNAPSHOT</version>
        <scope>compile</scope>
    </dependency>

##Micro Modules Control

Once project is properly configured, even partially, it's time to get some benefits of it
using `micro-modules-control` part of the framework.

    <dependency>
        <groupId>org.micromodules</groupId>
        <artifactId>micromodules-control</artifactId>
        <version>1.0-SNAPSHOT</version>
        <scope>runtime</scope>
    </dependency>

It defines `org.micromodules.control.ControlMain` executable class which could be used directly to:
  - check project structure against dependency rules
  - prepare structured report of current modules layout and interaction

Currently supported rule:
  - None of module classes may refer to *implementation* classes of other modules
  - None of module classes may refer to *contract* classes of other modules, which in not allowed by module dependencies configuration
  - TODO Each module must contain at least one class, except it is *super* module
  - TODO Each super module must contain at least on *sub* module
  - TODO Each class may be connected at most to one module
  - TODO Each class (except module setup) must be connected to some module
  - TODO Module configuration classes must be placed properly

Here is example report bases on test classes: TODO

How to run control tool using Maven: TODO

