scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.8", // for visualization
  // You don’t *have to* use Spark, but in case you want to, we have added the dependency
  "org.apache.spark" %% "spark-sql" % "2.4.3",
  // You don’t *have to* use akka-stream, but in case you want to, we have added the dependency
  "com.typesafe.akka" %% "akka-stream" % "2.6.0",
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.6.0" % Test,
  // You don’t *have to* use Monix, but in case you want to, we have added the dependency
  "io.monix" %% "monix" % "2.3.3",
  // You don’t *have to* use fs2, but in case you want to, we have added the dependency
  "co.fs2" %% "fs2-io" % "1.0.5",
)

parallelExecution in Test := false // So that tests are executed for each milestone, one after the other

val capstoneUI =
  project.in(file("capstone-ui"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      scalaVersion := "2.13.1",
      // Add the sources of the main project
      unmanagedSources in Compile ++= {
        val rootSourceDirectory = baseDirectory.value / ".." / "src" / "main" / "scala" / "com" / "vaddya" / "fpscala" / "observatory"
        Seq(
          rootSourceDirectory / "Interaction2.scala",
          rootSourceDirectory / "Signal.scala",
          rootSourceDirectory / "models.scala",
          rootSourceDirectory / "package.scala"
        )
      },
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "0.9.7",
        "com.lihaoyi" %%% "scalatags" % "0.7.0"
      ),
      scalaJSUseMainModuleInitializer := true
    )
