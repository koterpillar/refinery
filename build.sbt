name := "Refinery"

organization := "SEEK"

scalaVersion := "2.13.5"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "2.3.0",
  "org.scalameta" %% "munit" % "0.7.19" % Test,
  "com.lihaoyi" %% "ujson" % "1.2.2" % Test,
)
testFrameworks += new TestFramework("munit.Framework")

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.3" cross CrossVersion.full)
