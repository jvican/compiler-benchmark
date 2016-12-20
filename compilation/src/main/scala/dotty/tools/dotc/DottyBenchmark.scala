package dotty.tools.dotc

import java.io.File
import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

import dotty.tools.dotc.core.Contexts.ContextBase
import org.openjdk.jmh.annotations.Mode._
import org.openjdk.jmh.annotations._

@State(Scope.Benchmark)
class DottyBenchmark {
  @Param(value = Array[String]())
  var _classpath: String = _
  @Param(value = Array[String]())
  var source: String = _
  @Param(value = Array(""))
  var extraArgs: String = _
  var driver: Driver = _

  protected def compile(): Unit = {
    val cp = _classpath
    val compilerArgs =
      if (source.startsWith("@")) Array(source)
      else {
        import scala.collection.JavaConverters._
        val path = Collectors.toList[Path]()
        val allFiles = Files.walk(findSourceDir).collect(path).asScala.toList
        allFiles
          .filter(_.getFileName.toString.endsWith(".scala"))
          .map(_.toAbsolutePath.toString)
          .toArray
      }

    implicit val ctx = (new ContextBase).initialCtx.fresh
    ctx.setSetting(ctx.settings.classpath, cp)
    ctx.setSetting(ctx.settings.usejavacp, true)
    ctx.setSetting(ctx.settings.d, tempOutDir.getAbsolutePath)
    ctx.setSetting(ctx.settings.nowarn, true)
    val reporter = Bench.doCompile(new Compiler, compilerArgs.toList)
    assert(!reporter.hasErrors)
  }
  private def tempOutDir: File = {
    val tempFile = java.io.File.createTempFile("output", "")
    tempFile.delete()
    tempFile.mkdir()
    tempFile
  }
  private def findSourceDir: Path = {
    val path = Paths.get("../corpus/" + source)
    if (Files.exists(path)) path
    else Paths.get(source)
  }
}

@State(Scope.Benchmark)
@BenchmarkMode(Array(SingleShotTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
// TODO -Xbatch reduces fork-to-fork variance, but incurs 5s -> 30s slowdown
@Fork(value = 16, jvmArgs = Array("-XX:CICompilerCount=2"))
class ColdDottyBenchmark extends DottyBenchmark {
  @Benchmark
  override def compile(): Unit = super.compile()
}

@BenchmarkMode(Array(SampleTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 0)
@Measurement(iterations = 1, time = 30, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3)
class WarmDottyBenchmark extends DottyBenchmark {
  @Benchmark
  override def compile(): Unit = super.compile()
}

@BenchmarkMode(Array(SampleTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 6, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 12, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3)
class HotDottyBenchmark extends DottyBenchmark {
  @Benchmark
  override def compile(): Unit = super.compile()
}
