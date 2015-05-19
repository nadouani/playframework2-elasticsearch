Plugin that integrates Elasticsearch into your PlayFramework app.

## Install

This plugin requres Java 1.8 and PlayFramework 2.

Clone this plugin and add it to the local repository

~~~ sh
$ git clone https://github.com/eduardofcbg/playframework2-elasticsearch.git
$ cd playframework2-elasticsearch
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

## Name of the index (defaults to "play-es")
#es.index="play-es"

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

```java
package models;

@Type.Name("person")
public class Person extends Index {

	public String name;
	public int age;
	
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

All the finder's methods are asynchronous. They return `Promises` which can be easily turned into actual responses and even better, they can be turned into `Promises<Result>`, being the `Result` the type returned by play's controllers. This means you can easily construct asynchronous controllers like the one bellow.

```java
public class PersonController extends Controller {

    public static F.Promise<Result> getAdults() {
        return Person.getAdults().map(persons -> {
            return ok(listOfPerson.render(persons));
        });
    }

}
```

Of course you can also get the ES transport client by calling:

```java
Finder.getClient();
```

You also can get the index name and the type any model, just by accessing it's find helper object. 

```java
Person.find.getType();
```
```java
Finder.getIndex();
```

There are also some methods that will help you parse a `SearchResponse` and a `GetResponse`:

```java
Person person = find.parse(response);
List<Person> persons = find.parse(response);
```

This plugin uses it's own Jackson ObjectMapper for indexing and parsing responses. You can customize it by simply passing a different one: `Finder.setMapper(mymapper)`.

##Dealing with concurrency problems

ElasticSearch uses the [Optimistic concurrency control](https://www.elastic.co/guide/en/elasticsearch/guide/master/optimistic-concurrency-control.html#optimistic-concurrency-control) method. This plugin allows one to update a document without having to deal with the case when there are such problems.

```java
public static F.Promise<UpdateResponse> incrementAge(String id) {
        return find.update(id, original -> {
            original.age++;
            return original;
        });
}
```
An update can be done by specifying the document's Id and a Function that will be applied to the Object associated with it. This means that the Finder is aware of what transformation you are applying to the original object and can simply redo it if a problem related with concurrency occurs.

##Document relationships and mapping

You are able to set mappings in your index using the `application.conf`, but mappings that affect specific types should be specified in your actual models just by passing them when you create the Find helper.

```java
public static final Finder<Person> finder = new Finder<>(Person.class, m -> {
    try {
        m.startObject("_timestamp")
                .field("enabled", true)
                .field("path", "timestamp")
        .endObject();
    } catch (IOException e) {}
});
```

Additionally, if you want to set a parent-child relationship or a nested type, it is as easy as setting an annotation:

```java
package models;

@Type.Name("person")
public class Person extends Index {

	public String name;
	public int age;
	@Type.NestedField
	public List<Book> books;
	
	public static final Finder<Person> find = new Finder<Person>(Person.class);
	
	@JsonCreator
	public Person(@JsonProperty("name") String name, @JsonProperty("age") int age) {
		this.name = name;
		this.age = age;
	}
	
	public static F.Promise<List<Pet>> getPets(String personId) {
	    return Pet.find.getAsChildrenOf(Person.class, personId);
	}
		
}
```

```java
package models;

@Type.Name("pet")
@Type.Parent("person")
public class Pet extends Index {

	public String name;	
	
	public static final Finder<Pet> find = new Finder<Pet>(Pet.class);
	
	@JsonCreator
	public Person(@JsonProperty("name") String name) {
		this.name = name;
	}
	
	public F.Promise<IndexResponse> index(String personId) {
		return find.indexChild(this, personId);
	}
		
}
```

For more details check the unit tests and the [javadoc](http://play-es-doc.s3-website-eu-west-1.amazonaws.com/com/github/eduardofcbg/plugin/es/package-summary.html)

