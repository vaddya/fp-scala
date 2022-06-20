scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.8", // for visualization
  "org.apache.spark" %% "spark-sql" % "2.4.3",
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.6.19" % Test
)

Test / parallelExecution := false // So that tests are executed for each milestone, one after the other

