organization := "edu.berkeley.cs"

version := "1.0"

name := "HPPS_Project"

scalaVersion := "2.12.4"

lazy val yourproject = (project in file("generators/HPPS_Project")).settings(commonSettings).dependsOn(rocketchip)



//to use it as a dependendy for other projects (here chipyard for ex since "." is chipyard):
//lazy val chipyard = (project in file(".")).settings(commonSettings).dependsOn(testchipip, HPPS_Project)