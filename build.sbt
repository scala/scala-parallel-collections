resolvers in ThisBuild += "scala-integration" at "https://scala-ci.typesafe.com/artifactory/scala-integration/"

scalacOptions in ThisBuild ++= Seq("-deprecation", "-feature"/*, "-Xfatal-warnings"*/)

cancelable in Global := true

skip in publish := true // in root

lazy val commonSettings: Seq[Setting[_]] = Seq()

commonSettings  // in root

lazy val core = project.in(file("core"))
  .settings(ScalaModulePlugin.scalaModuleSettings)
  .settings(commonSettings)
  .settings(
  name := "scala-parallel-collections"
)

lazy val junit = project.in(file("junit"))
  .settings(commonSettings)
  .settings(
  libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test,
  // for javax.xml.bind.DatatypeConverter, used in SerializationStabilityTest
  libraryDependencies += "javax.xml.bind" % "jaxb-api" % "2.3.1" % Test,
  testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-v"),
  fork in Test := true,
  skip in publish := true
).dependsOn(testmacros, core)

lazy val scalacheck = project.in(file("scalacheck"))
  .settings(commonSettings)
  .settings(
  libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.14.3",
  fork in Test := true,
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaCheck, "-workers", "1", "-minSize", "0", "-maxSize", "4000", "-minSuccessfulTests", "5"),
  skip in publish := true
).dependsOn(core)

lazy val testmacros = project.in(file("testmacros"))
  .settings(commonSettings)
  .settings(
  libraryDependencies += scalaOrganization.value % "scala-compiler" % scalaVersion.value,
  skip in publish := true
)
