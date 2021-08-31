ThisBuild / crossScalaVersions := Seq("2.13.6", "3.0.1")
ThisBuild / scalaVersion := (ThisBuild / crossScalaVersions).value.head

Global / cancelable := true
publish / skip := true // in root

lazy val commonSettings: Seq[Setting[_]] =
  Seq(scalaModuleAutomaticModuleName := Some("scala.collection.parallel")) ++
  ScalaModulePlugin.scalaModuleSettings ++ Seq(
    versionPolicyIntention := Compatibility.BinaryAndSourceCompatible,
    Compile / compile / scalacOptions --= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) => Seq("-Xlint")
      case _            => Seq()
    }),
    Compile / compile / scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) => Seq()
      case _            => Seq("-Werror"),
    }),
  )

lazy val core = project.in(file("core"))
  .settings(commonSettings)
  .settings(
    name := "scala-parallel-collections",
    Compile / doc / autoAPIMappings := true,
  )

lazy val junit = project.in(file("junit"))
  .settings(commonSettings)
  .settings(
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test,
    libraryDependencies += "junit" % "junit" % "4.13.2" % Test,
    // for javax.xml.bind.DatatypeConverter, used in SerializationStabilityTest
    libraryDependencies += "javax.xml.bind" % "jaxb-api" % "2.3.1" % Test,
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-v"),
    Test / fork := true,
    publish / skip := true,
  ).dependsOn(testmacros, core)

lazy val scalacheck = project.in(file("scalacheck"))
  .settings(commonSettings)
  .settings(
    libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.15.4",
    Test / fork := true,
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-workers", "1", "-minSize", "0", "-maxSize", "4000", "-minSuccessfulTests", "5"),
    publish / skip := true
  ).dependsOn(core)

lazy val testmacros = project.in(file("testmacros"))
  .settings(commonSettings)
  .settings(
    libraryDependencies += (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) => scalaOrganization.value %% "scala3-compiler" % scalaVersion.value
      case _            => scalaOrganization.value % "scala-compiler" % scalaVersion.value
    }),
    publish / skip := true,
  )
