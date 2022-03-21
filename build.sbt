organization := "edu.berkeley.cs"

version := "1.0"

name := "HPPS_Project"

scalaVersion := "2.12.4"


libraryDependencies ++=Seq(
  "edu.berkeley.cs" %% "chisel3" % "3.4.+",
  "edu.berkeley.cs" %% "rocketchip" % "1.2.+",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "edu.berkeley.cs" %% "chisel-iotesters" % "1.5.+",
)