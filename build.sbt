name := "Refinery"

organization := "SEEK"

scalaVersion := "2.13.4"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "2.3.0",
  "org.scalameta" %% "munit" % "0.7.19" % Test,
  "com.lihaoyi" %% "ujson" % "1.2.2" % Test,
)
testFrameworks += new TestFramework("munit.Framework")
