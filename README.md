Plugin that integrates Elasticsearch clients into your PlayFramework app.

## Install

Clone this plugin and add it to the local repository

~~~ sh
$ git clone https://github.com/eduardofcbg/play-es.git
$ cd play-es
$ activator publish-local
~~~

On your project add the following dependency declaration:

```
  "com.github.eduardofcbg" %% "play-es" % "1.0-SNAPSHOT"
```

Your `build.sbt` should look something like this:

```
name := """myproject"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  cache,
  javaWs,
  "com.github.eduardofcbg" %% "play-es" % "1.0-SNAPSHOT"
)
```

## Enable Plugin

Add the following line to `conf/play.plugins` file. If it doesn't exist you can create it.

```
9000:com.github.eduardofcbg.plugin.es.ESPlugin
```

You should also configure the plugin in `conf/application.conf`

```
## ElasticSearch Configuration

## Enable the plugin
es.status="enabled"

## Coma-separated list of clients
es.client="127.0.0.1:9300"
# ex : es.client="192.168.0.46:9300,192.168.0.47:9300"

## Name of the index
es.index="mydb"

## Custom settings to apply when creating the index (optional)
es.index.settings="{ analysis: { analyzer: { my_analyzer: { type: \"custom\", tokenizer: \"standard\" } } } }"
```

## How to use



