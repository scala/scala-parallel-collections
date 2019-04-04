# Scala parallel collections

This Scala standard module contains the package
`scala.collection.parallel`, with all of the parallel collections that
used to be part of the Scala standard library.

For Scala 2.13, this module is a separate JAR that can be
omitted from projects that do not use parallel collections.

## Maintenance status

This module is maintained by the Scala team at Lightbend.  If you are
interested in participating, please jump right in on issues and pull
requests.

## Usage

To depend on scala-parallel-collections in sbt, add this to your `build.sbt`:

```
libraryDependencies +=
  "org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0"
```

In your code, adding this import:

```
import scala.collection.parallel.CollectionConverters._
```

will enable use of the `.par` method as in earlier Scala versions.

### Cross-building: dependency

This module is published only for the Scala 2.13.x series, so in a
cross-built project, the dependency should take this form:

```scala
libraryDependencies ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, major)) if major >= 13 =>
      Seq("org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0")
    case _ =>
      Seq()
  }
}
```

This way of testing `scalaVersion` is robust across varying Scala
version number formats (nightlies, milestones, release candidates,
community build, etc).

### Cross-building: source compatibility

Using `.par` is problematic in a cross-built project, since in Scala
2.13 the `CollectionConverters._` import shown above is necessary, but
in earlier Scala versions, that import will not compile.

You may able to avoid the problem by directly constructing your
parallel collections rather than going through `.par`.  For other
possible workarounds, see
https://github.com/scala/scala-parallel-collections/issues/22,
which is still under discussion.

## Releasing

As with other Scala standard modules, build and release infrastructure
is provided by the
[sbt-scala-modules](https://github.com/scala/sbt-scala-modules/) sbt
plugin.
