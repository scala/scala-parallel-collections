Global / cancelable := true
publish / skip := true // in root

Global / scalacOptions ++= (
  if (isDotty.value) Seq("-language:implicitConversions")  // TODO check again on 3.0.0-M1
  else               Seq()
)

lazy val commonSettings: Seq[Setting[_]] =
  ScalaModulePlugin.scalaModuleSettings ++ Seq(
    Compile / compile / scalacOptions --= (if (isDotty.value) Seq("-Xlint")
                                           else Seq()),
    Compile / compile / scalacOptions ++= (if (isDotty.value) Seq()
                                           else Seq("-Werror")),
  )

lazy val core = project.in(file("core"))
  .settings(commonSettings)
  .settings(
    name := "scala-parallel-collections",
    // don't run Dottydoc, it errors and isn't needed anyway
    Compile / doc / sources := (if (isDotty.value) Seq() else (Compile / doc/ sources).value),
    Compile / packageDoc / publishArtifact := !isDotty.value,
  )

lazy val junit = project.in(file("junit"))
  .settings(commonSettings)
  .settings(
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test,
    // for javax.xml.bind.DatatypeConverter, used in SerializationStabilityTest
    libraryDependencies += "javax.xml.bind" % "jaxb-api" % "2.3.1" % Test,
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-v"),
    Test / fork := true,
    publish / skip := true,
    // https://github.com/sbt/sbt/pull/5919 adds this to sbt itself,
    // so we should revisit once sbt 1.4.1 is available
    Test / unmanagedSourceDirectories += {
      val major = CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((0 | 3, _)) => "3"
        case _ => "2"
      }
      baseDirectory.value / "src" / "test" / s"scala-$major"
    },
  ).dependsOn(testmacros, core)

lazy val scalacheck = project.in(file("scalacheck"))
  .settings(commonSettings)
  .settings(
    libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.15.0-M1" withDottyCompat(scalaVersion.value),
    Test / fork := true,
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-workers", "1", "-minSize", "0", "-maxSize", "4000", "-minSuccessfulTests", "5"),
    publish / skip := true
  ).dependsOn(core)

lazy val testmacros = project.in(file("testmacros"))
  .settings(commonSettings)
  .settings(
    libraryDependencies += scalaOrganization.value % "scala-compiler" % scalaVersion.value,
    publish / skip := true,
    // https://github.com/sbt/sbt/pull/5919 adds this to sbt itself,
    // so we should revisit once sbt 1.4.1 is available
    Compile / unmanagedSourceDirectories += {
      val major = CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((0 | 3, _)) => "3"
        case _ => "2"
      }
      baseDirectory.value / "src" / "main" / s"scala-$major"
    },
  )
