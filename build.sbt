// Initialized via https://github.com/portable-scala/sbt-crossproject

val sharedSettings = Seq(scalaVersion := "2.13.4")

lazy val refinery =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .settings(sharedSettings)
    .settings(
      libraryDependencies += "org.example" %%% "foo" % "1.2.3"
    )
