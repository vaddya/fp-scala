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
    reactive
  )

lazy val principles = sparkProject("principles")
lazy val design = sparkProject("design")
lazy val parallel = sparkProject("parallel")
lazy val spark = sparkProject("spark")
lazy val reactive = sparkProject("reactive")

def sparkProject(dir: String) = Project(dir, file(dir))
  .settings(
    name := dir,
    settings,
    assemblySettings,
    libraryDependencies ++= dependencies,
    testOptions in Test += Tests.Argument(TestFrameworks.JUnit, "-a", "-v", "-s")
)

lazy val dependencies = Seq(
  "com.novocode" % "junit-interface" % "0.11" % Test
)

lazy val settings = Seq(
  scalaVersion := "2.13.1",
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
  assemblyJarName in assembly := name.value + ".jar",
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", _*) => MergeStrategy.discard
    case "application.conf" => MergeStrategy.concat
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)
