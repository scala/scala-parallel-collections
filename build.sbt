import com.typesafe.tools.mima.plugin.{MimaPlugin, MimaKeys}

resolvers in ThisBuild += "scala-pr" at "https://scala-ci.typesafe.com/artifactory/scala-integration/"

crossScalaVersions in ThisBuild := Seq("2.13.0-pre-e2a2cba")  // March 16, 2017

scalaVersion in ThisBuild       := crossScalaVersions.value.head

version in ThisBuild            := "1.0.0-SNAPSHOT"

scalacOptions in ThisBuild      ++= Seq("-deprecation", "-feature")

cancelable in Global := true

lazy val core = project.in(file("core")).settings(scalaModuleSettings).settings(scalaModuleOsgiSettings).settings(
  name := "scala-parallel-collections",
  OsgiKeys.exportPackage := Seq(s"scala.collection.parallel.*;version=${version.value}"),
  mimaPreviousVersion := None
)

lazy val junit = project.in(file("junit")).settings(
  libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test",
  testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-v"),
  fork in Test := true,
  publishArtifact := false
).dependsOn(testmacros, core)

lazy val scalacheck = project.in(file("scalacheck")).settings(
  libraryDependencies += "org.scalacheck" % "scalacheck_2.12" % "1.13.4",
  fork in Test := true,
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaCheck, "-workers", "1", "-minSize", "0", "-maxSize", "4000", "-minSuccessfulTests", "5"),
  publishArtifact := false
).dependsOn(core)

lazy val testmacros = project.in(file("testmacros")).settings(
  libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  publishArtifact := false
)
