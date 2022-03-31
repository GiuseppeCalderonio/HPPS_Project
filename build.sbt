organization := "edu.berkeley.cs"

version := "1.0"

name := "torus"

scalaVersion := "2.12.4"

scalacOptions ++= Seq("-deprecation","-unchecked","-Xsource:2.11")


libraryDependencies ++=Seq(
  "edu.berkeley.cs" %% "chisel3" % "3.4.+",
  "edu.berkeley.cs" %% "rocketchip" % "1.2.+",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "edu.berkeley.cs" %% "chisel-iotesters" % "1.5.+",
)