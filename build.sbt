// Initialized via https://github.com/portable-scala/sbt-crossproject

lazy val refinery =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .settings(
      crossScalaVersions := Seq(
        "2.13.4",
      ),
      libraryDependencies ++= Seq(
        "org.typelevel" %%% "cats-core" % "2.3.0",
      ),
    )
