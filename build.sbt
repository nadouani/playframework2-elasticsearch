name := """playframework2-elasticsearch"""

organization := "com.github.eduardofcbg"

version := "0.1"

val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.6"

crossScalaVersions := Seq("2.11.6")

libraryDependencies ++= Seq(
  "org.elasticsearch" % "elasticsearch" % "2.0.0",
  "me.enkode" %% "java8-converters" % "1.1.0"
)

resolvers += "kender" at "http://dl.bintray.com/kender/maven"

licenses += ("GPL-2.0", url("http://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html"))