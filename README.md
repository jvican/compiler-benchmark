# JMH benchmarks for the Dotty Compiler

This is a fork of the compiler benchmarks for
[Scalac](https://github.com/scala/compiler-benchmark).

Goal: define a set of JMH benchmarks for the compiler to help drive performance
tuning and catch performance regressions in the Dotty compiler.

Based on:
 - [OpenJDK JMH](http://openjdk.java.net/projects/code-tools/jmh/), the definitive Java benchmarking tool.
 - via [sbt-jmh](https://github.com/ktoso/sbt-jmh).

## Structure

| Project | Bencmark of|
| ------------- | ------------- |
| compilation  | The equivalent of `scalac ...`  |
| micro  | Finer grained parts of the compiler  |
| jvm | Pure Java benchmarks to demonstrate JVM quirks |

## Recipes

### Learning about JMH options

```
sbt> compilation/jmh:run -help
```

### Benchmarking compiler performance
  - (optional) add new Scala sources (say `aardvark`) to a new directory in src/main/corpus

```
compilation/jmh:run (Cold|Warm|Hot)CompilationBenchmark 
   -p source=(<subdir of corpus>|/path/to/src/dir|@/path/to/argsfile)
   -p extraArgs=-nowarn
```

### Changing Scala Version

This section will be updated for Dotty.

```
sbt> set scalaVersion in ThisBuild := "2.12.0-ab61fed-SNAPSHOT"
sbt> set scalaHome in ThisBuild := Some(file("/code/scala/build/pack"))
sbt> set scalaHome in compilation := "2.11.1" // if micro project isn't compatible with "2.11.1"
```

### Collecting profiling data

```
sbt> .../jmh:run Benchmark -prof jmh.extras.JFR // Java Flight Recorder
```

### Using Graal

[Install](http://www.oracle.com/technetwork/oracle-labs/program-languages/downloads/index.html) Graal VM and JDK8 with [JVMCI](http://openjdk.java.net/jeps/243).

```
compilation/jmh:run CompileSourcesBenchmark 
    -jvm      /path/to/labsjdk1.8.0_92-jvmci-0.20/bin/java
    -jvmArgs -XX:+UnlockExperimentalVMOptions
    -jvmArgs -XX:+EnableJVMCI
    -jvmArgs -Djvmci.class.path.append=/path/to/graalvm-0.15-re/lib/jvmci/graal.jar
    -jvmArgs -Xbootclasspath/a:/path/to/graalvm-0.15-re/lib/truffle-api.jar
    -jvmArgs -Djvmci.Compiler=graal 
    -jvmArgs -XX:+UseJVMCICompiler 
```
