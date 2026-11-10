// microrv32 - fork dla projektow newhope (FPGA-snake).
// Wersje MUSZA byc te same co w fpga-hw-lib/sbt/HwBuild.scala: konsument laduje
// ten build w jednej sesji sbt przez ProjectRef(file("cpu/microrv32/microrv32"), "microrv32").
ThisBuild / version      := "1.0-newhope"
ThisBuild / scalaVersion := "2.13.14"
ThisBuild / organization := "core.microrv32"

val spinalVersion = "1.15.0"
val spinalCore = "com.github.spinalhdl" %% "spinalhdl-core" % spinalVersion
val spinalLib = "com.github.spinalhdl" %% "spinalhdl-lib" % spinalVersion
val spinalIdslPlugin = compilerPlugin("com.github.spinalhdl" %% "spinalhdl-idsl-plugin" % spinalVersion)

// id "microrv32" (bylo: projectname) - pod tym id widzi go konsument
lazy val microrv32 = (project in file("."))
  .settings(
    name := "microrv32",
    Compile / scalaSource := baseDirectory.value / "src",
    Test    / scalaSource := baseDirectory.value / "test",
    libraryDependencies ++= Seq(spinalCore, spinalLib, spinalIdslPlugin,
                                "org.scalatest" %% "scalatest" % "3.2.19" % Test),
    Test / parallelExecution := false,
    fork := true,
    publish / skip := true
  )
