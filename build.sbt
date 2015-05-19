name := """play-es"""

organization := "com.github.eduardofcbg"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "org.elasticsearch" % "elasticsearch" % "1.5.2"
)
