Plugin that integrates Elasticsearch into your PlayFramework app.

## Install

This plugin requres Java 1.8 and PlayFramework 2.

Clone this plugin and add it to the local repository

~~~ sh
$ git clone https://github.com/eduardofcbg/play-es.git
$ cd play-es
$ sbt publish-local
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

## Name of the index (if empty defaults to "play-es")
es.index="mydb"

## Adicional options
#es.log=true
#es.sniff=true
#es.timeout=5
#es.ping=5
#es.cluster="mycluster"
#es.mappings="{ analysis: { analyzer: { my_analyzer: { type: \"custom\", tokenizer: \"standard\" } } } }"

```

## How to use

Make your models extend the `com.github.eduardofcbg.plugin.es.Index` class and annotate it with the name of the [type](http://www.elastic.co/guide/en/elasticsearch/reference/current/glossary.html) that will be associated with the indexed objects of this class.
On each model add a find helper. This is the object that you will use query your ES cluster.

```
package models;

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
	
	public F.Promise<IndexResponse> index() {
		return find.index(this);
	}
	
	public static F.Promise<Person> get(String id) {
		return find.get(id);
	}
					
    public static F.Promise<List<Person>> getAdults() {
        return find.search(s -> s.setPostFilter(FilterBuilders.rangeFilter("age").from(18)));
    }
	
	
}
```
As you can see in the last method, you can easily construct any search query by passing a method that will change your `SearchRequestBuilder` object via side effects. This way you can use the ES java API directly in your models.

All the finder's methods asynchronous. They return Promises which can be easily turned into actual responses, and even better, they can be turned into Promises<Result>, being the Result the object returned by play's controllers. This means you can easily construct asynchrounous controllers like the one bellow.

```
public class PersonController extends Controller {

    public static F.Promise<Result> getAdults() {
        return Person.getAdults().map(persons -> {
            return ok(listOfPerson.render(persons));
        });
    }

}
```

Of course you can also get the client directly by calling:

```
Finder.getClient();
```

You also always get the type name of any model, just by accessing it's find helper object. 

```
Person.find.getTypeName()
```

There are some methods that will help you parse a `SearchResponse` and a `GetResponse`:

```
Person person = find.parse(response)
List<Person> persons = find.parse(response)
```

This plugin uses it's own Jackson ObjectMapper for indexing and parsing responses. You can customize it by simply passing a different one: `ESPlugin.getPlugin().setMapper(mymapper)`.

##Dealing with concurrency problems

ElasticSearch uses the [Optimistic concurrency control](https://www.elastic.co/guide/en/elasticsearch/guide/master/optimistic-concurrency-control.html#optimistic-concurrency-control) method. This plugin allows one to update a document without having to deal with the case when there are such problems.

```
public static F.Promise<UpdateResponse> incrementAge(String id) {
        return find.update(id, original -> {
            original.age++;
            return original;
        });
    }
```
An update can be done by specifying the document's Id and a Function that will be applied to the Object associated with it. This means that the Finder is aware of what tranformation with are applying to the original object and can simply redo it if a problem related with concurrency occurs.

For more details check the unit tests and the [javadoc][http://play-es-doc.s3-website-eu-west-1.amazonaws.com/com/github/eduardofcbg/plugin/es/package-summary.html]

