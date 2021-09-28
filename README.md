# Scala parallel collections

This Scala standard module contains the package
`scala.collection.parallel`, with all of the parallel collections that
used to be part of the Scala standard library (in Scala 2.10 through 2.12).

For Scala 3 and Scala 2.13, this module is a separate JAR that can be
omitted from projects that do not use parallel collections.

## Documentation

* https://docs.scala-lang.org/overviews/parallel-collections/overview.html
* https://javadoc.io/doc/org.scala-lang.modules/scala-parallel-collections_2.13

## Maintenance status

This module is community-maintained, under the guidance of the Scala team at Lightbend.  If you are
interested in participating, please jump right in on issues and pull
requests.

## Usage

To depend on scala-parallel-collections in sbt, add this to your `build.sbt`:

```scala
libraryDependencies +=
  "org.scala-lang.modules" %% "scala-parallel-collections" % "<version>"
```

In your code, adding this import:

```scala
import scala.collection.parallel.CollectionConverters._
```

will enable use of the `.par` method as in earlier Scala versions.

### Cross-building: dependency

This module is published only for the Scala 3 and 2.13, so in a
cross-built project, the dependency should take this form:

```scala
libraryDependencies ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, major)) if major <= 12 =>
      Seq()
    case _ =>
      Seq("org.scala-lang.modules" %% "scala-parallel-collections" % "<version>")
  }
}
```

This way of testing `scalaVersion` is robust across varying Scala
version number formats (nightlies, milestones, release candidates,
community build, etc).

### Cross-building: source compatibility

Using `.par` is problematic in a cross-built project, since in Scala
2.13+ the `CollectionConverters._` import shown above is necessary, but
in earlier Scala versions, that import will not compile.

You may able to avoid the problem by directly constructing your
parallel collections rather than going through `.par`.  For other
possible workarounds, see
https://github.com/scala/scala-parallel-collections/issues/22,
which is still under discussion.

## Releasing

As with other Scala standard modules, build and release infrastructure
is provided by the
[sbt-scala-module](https://github.com/scala/sbt-scala-module/) sbt
plugin.
