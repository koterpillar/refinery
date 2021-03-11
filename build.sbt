ThisBuild / scalaVersion := "2.13.5"

lazy val refinery = (project in file("."))
  .settings(
    licenses := List("MIT License" -> url("https://github.com/seek-oss/refinery/blob/main/LICENSE")),
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.3.0",
      "org.scalameta" %% "munit" % "0.7.19" % Test,
      "com.lihaoyi" %% "ujson" % "1.2.2" % Test,
    ),
    testFrameworks += new TestFramework("munit.Framework"),
  )

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.3" cross CrossVersion.full)

ThisBuild / organization := "com.koterpillar"
ThisBuild / organizationName := "Koterpillar"
ThisBuild / organizationHomepage := Some(url("https://koterpillar.com/"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/koterpillar/refinery"),
      "scm:git@github.com:koterpillar/refinery.git"
  )
)

ThisBuild / name := "Refinery"
ThisBuild / description := "Contextual validation for Scala"
ThisBuild / licenses := List("MIT License" -> url("https://github.com/seek-oss/refinery/blob/main/LICENSE"))
ThisBuild / homepage := Some(url("https://github.com/koterpillar/refinery/"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
  val nexus = "https://s01.oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true
