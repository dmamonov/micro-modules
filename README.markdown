#Micro Modules Framework

Allow to control code dependencies at class level precision without affecting current project structure.

##Inspiration

Working with a distributed team on a project of a several hundred thousands lines of code
I found it hard to control code usages at class level. Once most of logic is accessible
everywhere it started to be usual problem when, for example, developers use user interface models
in core business logic. Idea of this framework is to get rid of such problems without increasing
project complexity and not even require any changes in code structure at first step.

##Alternatives Overview

*Maven* - de facto standard tool for managing dependencies of a compilation units (aka jars).
Maven is fine to manage dependencies in big, but it starts to be insane to try to describe
each business logic module (which is usually several classes) as a separate maven module.

*DI/IoC Frameworks* - (Spring, Guice, etc.) is more about decoupling and wiring logic together.
They helps a lot on their field, especially in wiring everything with everything in a every way you like,
independently if it's rational or not. They just allow.

*OSGi* - is just a monster from my point of view. Yet it require to completely reorganize project which
is not acceptable for me.

*Java Modularity* - still a concept and has no meaning in production use currently.

*Summary* - this tools misses concept of deny usages in general.

 ##Micro Modules Framework Concept

 It is all about:
  - grouping classes spread across packages into distinct modules,
  - making hierarchies of this modules,
  - defining allowed dependencies between them
  - and checking all of this fit together properly.

 ###Micro Module Definition

Modules definition is required to be an *inner* java class placed into parent class called '__module__' or '__modules__'.
This is convention intended to place all module definition on top of each package and allow to describe several
modules placed in a same package using convinient way.

