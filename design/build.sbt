libraryDependencies ++= Seq(
  "org.typelevel" %% "jawn-parser" % "0.14.2",
  "org.scalacheck" %% "scalacheck" % "1.14.0"
)

lazy val webUI = project.in(file("web-ui")).
  enablePlugins(ScalaJSPlugin).
  settings(
    scalaVersion := "2.13.1",
    // Add the sources of the calculator project
    unmanagedSourceDirectories in Compile += baseDirectory.value / ".." / "src" / "main" / "scala" / "calculator",
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.7",
    scalaJSUseMainModuleInitializer := true
  )
