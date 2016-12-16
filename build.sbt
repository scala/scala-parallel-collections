import com.typesafe.tools.mima.plugin.{MimaPlugin, MimaKeys}

scalaModuleSettings

name               := "scala-parallel-collections"

version            := "1.0.0-SNAPSHOT"

scalaVersion       := crossScalaVersions.value.head

resolvers += "scala-pr" at "https://scala-ci.typesafe.com/artifactory/scala-pr-validation-snapshots/"

crossScalaVersions := Seq("2.13.0-SNAPSHOT")

scalacOptions      ++= Seq("-deprecation", "-feature")

// important!! must come here (why?)
scalaModuleOsgiSettings

OsgiKeys.exportPackage := Seq(s"scala.collection.parallel.*;version=${version.value}")

mimaPreviousVersion := None

lazy val root = project.in( file(".") )

fork in Test := true

libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test"

testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-v")
