ore-play
========

ore-play is a WIP port of the Sponge [Ore](https://github.com/SpongePowered/Ore) project, originally written in Python
using Django. This implementation emphasizes readability, good OOP practices, and encapsulated polymorphic structures 
by separating application and database logic from abstract concepts wherever possible.
 
ore-play is written in Scala using the [Play](https://www.playframework.com/) framework.

# Running

Running ore-play is relatively simple. If you are using IntelliJ you should be able to import the `build.sbt` file and
just run it. Dependencies will be automatically resolved.

**With Activator:**
    * Download and install the latest [Activator](https://www.lightbend.com/activator/download) distribution.
    * Execute `activator run` in the project root.

# Contributing

ore-play's code-style is not unlike the rest of the Sponge projects code-style with some leniency for readability.
Namely, the differences include:
    * Non-uniform whitespace (see [schema.sql](app/models/db/schema.scala)
    * Inline conditionals / statements
    * Public fields permitted on database properties in models
    