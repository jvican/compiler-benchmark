name := """compiler-benchmark"""

version := "1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.11.8"
val dottyVersion = settingKey[String]("Dotty version to be benchmarked.")

val compilation = project
  .enablePlugins(JmhPlugin)
  .settings(
    // We should be able to switch this project to a broad range of Scala versions for comparative
    // benchmarking. As such, this project should only depend on the high level `MainClass` compiler API.
    description := "Black box benchmark of the compiler",
    scalaVersion := "2.11.5",
    dottyVersion := "0.1-20161216-ee5dd32-NIGHTLY",
    libraryDependencies += "ch.epfl.lamp" % "dotty-compiler_2.11" % dottyVersion.value,
    libraryDependencies += "ch.epfl.lamp" % "dotty-library_2.11" % dottyVersion.value,
    libraryDependencies +=
      ScalaArtifacts.Organization % ScalaArtifacts.LibraryID % "2.11.5",
    // Convenient access to builds from PR validation
    resolvers ++= (
      if (dottyVersion.value.endsWith("-SNAPSHOT"))
        List(
          Resolver.mavenLocal,
          Resolver.sonatypeRepo("snapshots")
        )
      else List(Resolver.mavenLocal)
    )
  )

val micro = project
  .enablePlugins(JmhPlugin)
  .settings(
    description := "Finer grained benchmarks of compiler internals",
    libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value
  )

val jvm = project
  .enablePlugins(JmhPlugin)
  .settings(
    description := "Pure Java benchmarks for demonstrating performance anomalies independent from the Scala language/library",
    autoScalaLibrary := false
  )

val ui = project.settings(
  scalaVersion := "2.11.8",
  libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.3",
  libraryDependencies += "com.h2database" % "h2" % "1.4.192"
)

val runBatch = taskKey[Unit]("Run a batch of benchmark suites")
val runBatchVersions = settingKey[Seq[String]]("Scala versions")
val runBatchBenches = settingKey[Seq[(sbt.Project, String)]]("Benchmarks")
val runBatchSources = settingKey[Seq[String]]("Sources")

runBatchVersions := List(
  "0.1-20161216-ee5dd32-NIGHTLY",
  "0.1-SNAPSHOT"
)

runBatchBenches := List(
  (compilation, "ColdDottyBenchmark"),
  (compilation, "HotDottyBenchmark")
)

runBatchSources := List(
  //"scalap",
  "better-files",
  "squants"
)

def setVersion(s: State, proj: sbt.Project, newVersion: String): State = {
  val extracted = Project.extract(s)
  import extracted._
  if (get(dottyVersion in proj) == newVersion) s
  else {
    val append = Load.transformSettings(
      Load.projectScope(currentRef),
      currentRef.build,
      rootProject,
      (dottyVersion in proj := newVersion) :: Nil)
    val newSession = session.appendSettings(append map (a => (a, Nil)))
    s.log.info(s"Switching to Scala version $newVersion")
    BuiltinCommands.reapply(newSession, structure, s)
  }
}

commands += Command.args("runBatch", "") { (s: State, args: Seq[String]) =>
  val targetDir = target.value
  val outFile = targetDir / "combined.csv"

  def filenameify(s: String) = s.replaceAll("""[@/:]""", "-")
  val tasks: Seq[State => State] = for {
    p <- runBatchSources.value.map(x => (filenameify(x), s"-p source=$x"))
    (sub, b) <- runBatchBenches.value
    v <- runBatchVersions.value
  } yield {
    import ScalaArtifacts._
    val scalaLibrary = Organization % LibraryID % "2.11.5"
    val dottyLibrary = "ch.epfl.lamp" % "dotty-library_2.11" % v
    val ioArgs = s"-rf csv -rff $targetDir/${p._1}-$b-$v.csv"
    val argLine = s"$b ${p._2} $ioArgs"

    (s1: State) =>
      {
        val s2 = setVersion(s1, sub, v)
        val extracted = Project.extract(s2)
        val (s3, cp) = extracted.runTask(fullClasspath in sub in Jmh, s2)
        val cpFiles = cp.files
        val dottyArt = cpFiles.filter(_.getName.contains(dottyLibrary.name))
        val scalaArt = cpFiles.filter(_.getName.contains(scalaLibrary.name))
        val classpath = (dottyArt ++ scalaArt).mkString(":")
        val cargs = s" -p _classpath=$classpath $argLine -p _dottyVersion=$v"
        val (s4, _) = extracted.runInputTask(run in sub in Jmh, cargs, s3)
        s4
      }
  }
  tasks.foldLeft(s)((state: State, fun: (State => State)) => {
    val newState = fun(state)
    Project
      .extract(newState)
      .runInputTask(runMain in ui in Compile,
                    " compilerbenchmark.PlotData",
                    newState)
      ._1
  })
}
