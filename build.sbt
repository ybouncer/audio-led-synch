name := "AudioLEDSync"

version := "1.0"

scalaVersion := "2.13.12"

// Allow version conflicts (Monix uses older cats-effect)
ThisBuild / libraryDependencySchemes += "org.typelevel" %% "cats-effect" % VersionScheme.Always

// Reactive programming libraries
libraryDependencies ++= Seq(
  // Monix for Observables (reactive streams)
  "io.monix" %% "monix" % "3.4.1",
  
  // Akka for Actor model
  "com.typesafe.akka" %% "akka-actor-typed" % "2.8.5",
  "com.typesafe.akka" %% "akka-stream" % "2.8.5",
  
  // Audio processing
  "com.github.wendykierp" % "JTransforms" % "3.1", // FFT
  // Note: javax.sound.sampled is part of Java SDK, no dependency needed
  
  // Visualization
  "org.scala-lang.modules" %% "scala-swing" % "3.0.0",
  
  // Utility
  "org.typelevel" %% "cats-core" % "2.10.0",
  "org.typelevel" %% "cats-effect" % "2.5.4",  // Downgraded to match Monix
  
  // Configuration
  "com.typesafe" % "config" % "1.4.3",
  
  // Logging
  "ch.qos.logback" % "logback-classic" % "1.4.11",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5"
)

// Compiler options
scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard"
)

// Fork JVM for running to avoid issues with audio
fork := true
