lazy val scala3Version = "3.0.1"
lazy val scala213Version = "2.13.6"

ThisBuild / scalaVersion := scala3Version
ThisBuild / crossScalaVersions := Seq(scala3Version, scala213Version)

lazy val refinery = (project in file("."))
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.6.1",
      "org.scalameta" %% "munit" % "0.7.27" % Test,
      "com.lihaoyi" %% "ujson" % "1.4.0" % Test,
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    name := "Refinery",
    description := "Contextual validation for Scala",
    licenses := List(
      "MIT License" -> url("https://github.com/seek-oss/refinery/blob/main/LICENSE"),
    ),
    homepage := Some(url("https://github.com/koterpillar/refinery/")),
    organization := "com.koterpillar",
    organizationName := "Koterpillar",
    organizationHomepage := Some(url("https://koterpillar.com/")),
    developers := List(
      Developer(
        id = "refinery-developers",
        name = "Refinery Developers",
        email = "noreply@example.com",
        url = url("https://github.com/koterpillar/refinery/"),
      ),
    ),
    sonatypeCredentialHost := "s01.oss.sonatype.org",
  )

def scala32[A](version: String)(scala3: => A, scala2: => A): A =
  CrossVersion.partialVersion(version) match {
    case Some((3, _))  => scala3
    case Some((2, 13)) => scala2
    case _             => throw new Exception("Unexpected Scala version.")
  }

// https://docs.scala-lang.org/scala3/guides/migration/plugin-kind-projector.html
// https://stackoverflow.com/a/67582387
ThisBuild / libraryDependencies ++= scala32(scalaVersion.value)(
  scala3 = Nil,
  scala2 =
    Seq(compilerPlugin("org.typelevel" % "kind-projector" % "0.13.0" cross CrossVersion.full)),
)
ThisBuild / scalacOptions ++= scala32(scalaVersion.value)(
  scala3 = Seq("-Ykind-projector:underscores"),
  scala2 = Seq("-Xsource:3", "-P:kind-projector:underscore-placeholders"),
)

ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches +=
  RefPredicate.StartsWith(Ref.Tag("v"))

ThisBuild / githubWorkflowBuild += WorkflowStep.Sbt(
  List("scalafmtSbtCheck", "scalafmtCheckAll"),
  name = Some("Check Formatting"),
)

ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("ci-release"),
    env = Map(
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}",
    ),
  ),
)
