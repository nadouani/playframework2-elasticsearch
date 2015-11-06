name := """play-es"""

organization := "com.github.eduardofcbg"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.6"

crossScalaVersions := Seq("2.11.6")

libraryDependencies ++= Seq(
  "org.elasticsearch" % "elasticsearch" % "2.0.0",
  "me.enkode" %% "java8-converters" % "1.1.0"
)

resolvers += "kender" at "http://dl.bintray.com/kender/maven"
