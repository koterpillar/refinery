ThisBuild / scalaVersion := "2.13.5"

lazy val refinery = (project in file("."))
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.3.0",
      "org.scalameta" %% "munit" % "0.7.19" % Test,
      "com.lihaoyi" %% "ujson" % "1.2.2" % Test,
    ),
    testFrameworks += new TestFramework("munit.Framework"),

    name := "Refinery",
    description := "Contextual validation for Scala",
    licenses := List("MIT License" -> url("https://github.com/seek-oss/refinery/blob/main/LICENSE")),
    homepage := Some(url("https://github.com/koterpillar/refinery/")),
    organization := "com.koterpillar",
    organizationName := "Koterpillar",
    organizationHomepage := Some(url("https://koterpillar.com/")),
    developers := List(
      Developer(
        id = "refinery-developers",
        name = "Refinery Developers",
        email = "noreply@example.com",
        url = "https://github.com/koterpillar/refinery/",
      ),
    ),

    sonatypeCredentialHost := "s01.oss.sonatype.org",
  )

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.3" cross CrossVersion.full)

ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches += 
  RefPredicate.StartsWith(Ref.Tag("v"))

ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("ci-release"),
    env = Map(
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
    )
  )
)
