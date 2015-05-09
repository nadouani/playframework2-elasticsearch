Plugin that integrates Elasticsearch clients into your PlayFramework app.

## Install

This plugin requres Java 1.8 and PlayFramework 2.

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

## Enable the plugin

Add the following line to `conf/play.plugins` file. If it doesn't exist you can create it.

```
9000:com.github.eduardofcbg.plugin.es.ESPlugin
```

You should also configure the plugin in `conf/application.conf`

```
## ElasticSearch Configuration

## Enable the plugin
es.status="enabled"
es.embed=false

## Coma-separated list of clients
es.client="127.0.0.1:9300"
# ex : es.client="192.168.0.46:9300,192.168.0.47:9300"

## Name of the index
es.index="mydb"
```

## How to use

Make your models extend the `com.github.eduardofcbg.plugin.es.Index` class and annotate it with the name of the [type](http://www.elastic.co/guide/en/elasticsearch/reference/current/glossary.html) that will be associated with the indexed objects of this class.
On each model add a find helper. This is the object that you will use query your ES cluster.

```
package models;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.index.query.FilterBuilders;

import com.github.eduardofcbg.plugin.es.Finder;
import com.github.eduardofcbg.plugin.es.Index;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

@Index.Entity(type="person")
public class Person extends Index {

	String name;
	int age;
	
	public static final Finder<Person> find = new Finder<Person>(Person.class);
	
	@JsonCreator
	public Person(@JsonProperty("name") String name, @JsonProperty("age") int age) {
		this.name = name;
		this.age = age;
	}
	
	public void index() throws JsonProcessingException, InterruptedException, ExecutionException {
		find.index(this);
	}
	
	public static Person getById(String id) throws NullPointerException, InterruptedException, ExecutionException {
		return find.getAndParse(id);
	}
			
	public static void delete(String id) throws InterruptedException, ExecutionException {
		find.delete(id);
	}
	
	public static long count() throws InterruptedException, ExecutionException {
		return find.countAndParse(null);
	}
		
	public static List<Person> getAll() throws InterruptedException, ExecutionException {
		return find.searchAndParse(null);
	}
	
	public static List<Person> getAdults() throws InterruptedException, ExecutionException {
		return find.searchAndParse(s -> s.setPostFilter(FilterBuilders.rangeFilter("age").from(18)));
	}
	
	
}
```

As you can see in the last method, you can easily construct any search query by passing a method that will change your `SearchRequestBuilder` object via side effects. This way you can use the ES java API directly in your models.

Of course you can also get the client directly by calling:

```
ESPlugin.getPlugin().getClient();
```

You also always get the type name of any model, just by accessing it's find helper object. 

```
Person.find.getTypeName()
```

There are some methods that will help you parse a `SearchResponse` and a `GetResponse`:

```
Person person = finder.utils().parse(response)
List<Person> persons = finder.utils().parse(response)
```

This plugin uses it's own Jackson ObjectMapper for indexing and parsing responses. You can customize it by simply passing a different one: `ESPlugin.getPlugin().setMapper(mymapper)`.



