// run webUI/fastOptJS to build js
lazy val webUI = project.in(file("web-ui"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaVersion := "2.13.1",
    Compile / unmanagedSources ++= {
      val root = baseDirectory.value / ".." / "src" / "main" / "scala" / "com" / "vaddya" / "fpscala" / "design" / "calculator"
      Seq(
        root / "Calculator.scala",
        root / "Interfaces.scala",
        root / "Polynomial.scala",
        root / "Signal.scala",
        root / "TweetLength.scala"
      )
    },
    libraryDependencies ++= Seq(
      "org.scala-js" %% "scalajs-dom_sjs0.6" % "0.9.7",
      "com.lihaoyi" %% "scalatags" % "0.7.0"
    ),
    scalaJSUseMainModuleInitializer := true,
  )
