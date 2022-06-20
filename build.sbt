organization := "com.vaddya"
name := "fpscala"
version := "1.0"
scalaVersion := "2.13.1"

lazy val global = project
  .in(file("."))
  .settings(settings)
  .disablePlugins(AssemblyPlugin)
  .aggregate(
    principles,
    design,
    parallel,
    spark,
    capstone,
    reactive
  )

lazy val principles = sparkProject("principles")
lazy val design = sparkProject("design")
lazy val parallel = sparkProject("parallel")
lazy val spark = sparkProject("spark")
lazy val capstone = sparkProject("capstone")
lazy val reactive = sparkProject("reactive")

def sparkProject(dir: String) = Project(dir, file(dir))
  .settings(
    name := dir,
    settings,
    assemblySettings,
    libraryDependencies ++= dependencies,
    Test / testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-v", "-s")
)

lazy val dependencies = Seq(
  "org.typelevel" %% "jawn-parser" % "1.3.2",
  "org.scalacheck" %% "scalacheck" % "1.16.0",
  "com.novocode" % "junit-interface" % "0.11" % Test
)

lazy val settings = Seq(
  scalaVersion := "3.1.2",
  scalacOptions ++= compilerOptions
)

lazy val compilerOptions = Seq(
  "-unchecked",
  "-feature",
  "-deprecation",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-encoding",
  "utf8"
)

lazy val assemblySettings = Seq(
  assembly / assemblyJarName := name.value + ".jar",
  assembly / assemblyMergeStrategy := {
    case PathList("META-INF", _*) => MergeStrategy.discard
    case "application.conf" => MergeStrategy.concat
    case x =>
      val oldStrategy = (assembly / assemblyMergeStrategy).value
      oldStrategy(x)
  }
)
