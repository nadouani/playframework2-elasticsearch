name := """play-es"""

organization := "com.github.eduardofcbg"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.6"

crossScalaVersions := Seq("2.11.6")

libraryDependencies ++= Seq(
  "org.elasticsearch" % "elasticsearch" % "1.6.0",
  "org.easytesting" % "fest-assert-core" % "2.0M10"
)
