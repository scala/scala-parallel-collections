import com.typesafe.tools.mima.plugin.{MimaPlugin, MimaKeys}

scalaModuleSettings

name               := "scala-parallel-collections"

version            := "1.0.0-SNAPSHOT"

scalaVersion       := crossScalaVersions.value.head

resolvers += "scala-pr" at "https://scala-ci.typesafe.com/artifactory/scala-pr-validation-snapshots/"

// this is the SHA of https://github.com/scala/scala/pull/5603 as of 16 Dec 2016
// (that's the PR that removes the parallel collections)
crossScalaVersions := Seq("2.13.0-6aa1987-SNAPSHOT")

scalacOptions      ++= Seq("-deprecation", "-feature")

// important!! must come here (why?)
scalaModuleOsgiSettings

OsgiKeys.exportPackage := Seq(s"scala.collection.parallel.*;version=${version.value}")

mimaPreviousVersion := None

lazy val root = project.in( file(".") )

fork in Test := true

libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test"

testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-v")
