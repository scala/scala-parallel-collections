import ScalaModulePlugin._

version in ThisBuild := "0.1.3-SNAPSHOT"

resolvers in ThisBuild += "scala-integration" at "https://scala-ci.typesafe.com/artifactory/scala-integration/"

scalaVersionsByJvm in ThisBuild := {
  val v213 = "2.13.0-pre-e959f6c"  // Feb 5
  Map(
    8 -> List(v213 -> true),
    11 -> List(v213 -> false))
}

scalacOptions in ThisBuild ++= Seq("-deprecation", "-feature"/*, "-Xfatal-warnings"*/)

cancelable in Global := true

disablePublishing  // in root

// not sure why `in ThisBuild` doesn't work for this; see
// https://github.com/sbt/sbt-header/issues/153
lazy val commonSettings = Seq(
  headerLicense  := Some(HeaderLicense.Custom(
    s"""|Scala (https://www.scala-lang.org)
        |
        |Copyright EPFL and Lightbend, Inc.
        |
        |Licensed under Apache License 2.0
        |(http://www.apache.org/licenses/LICENSE-2.0).
        |
        |See the NOTICE file distributed with this work for
        |additional information regarding copyright ownership.
        |""".stripMargin)))
commonSettings  // in root

/** Create an OSGi version range for standard Scala / Lightbend versioning
  * schemes that describes binary compatible versions. */
def osgiVersionRange(version: String): String =
  if(version contains '-') "${@}" // M, RC or SNAPSHOT -> exact version
  else "${range;[==,=+)}" // Any binary compatible version

/** Create an OSGi Import-Package version specification. */
def osgiImport(pattern: String, version: String): String =
  pattern + ";version=\"" + osgiVersionRange(version) + "\""

lazy val core = project.in(file("core"))
  .settings(scalaModuleSettings)
  .settings(commonSettings)
  .settings(
  name := "scala-parallel-collections",
  OsgiKeys.exportPackage := Seq(
    s"scala.collection.parallel.*;version=${version.value}",
    // The first entry on the classpath is the project's target classes dir but sbt-osgi also passes all
    // dependencies to bnd. Any "merge" strategy for split packages would include the classes from scala-library.
    s"scala.collection;version=${version.value};-split-package:=first",
    s"scala.collection.generic;version=${version.value};-split-package:=first"
  ),
  // Use correct version for scala package imports
  OsgiKeys.importPackage := Seq(osgiImport("scala*", scalaVersion.value), "*"),
  mimaPreviousVersion := None
)

lazy val junit = project.in(file("junit"))
  .settings(commonSettings)
  .settings(
  libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test,
  // for javax.xml.bind.DatatypeConverter, used in SerializationStabilityTest
  libraryDependencies += "javax.xml.bind" % "jaxb-api" % "2.3.1" % Test,
  testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-v"),
  fork in Test := true,
  disablePublishing
).dependsOn(testmacros, core)

lazy val scalacheck = project.in(file("scalacheck"))
  .settings(commonSettings)
  .settings(
  libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.14.0",
  fork in Test := true,
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaCheck, "-workers", "1", "-minSize", "0", "-maxSize", "4000", "-minSuccessfulTests", "5"),
  disablePublishing
).dependsOn(core)

lazy val testmacros = project.in(file("testmacros"))
  .settings(commonSettings)
  .settings(
  libraryDependencies += scalaOrganization.value % "scala-compiler" % scalaVersion.value,
  disablePublishing
)
