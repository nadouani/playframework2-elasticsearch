package com.github.eduardofcbg.plugin.es.scala

import java.util
import java.util.function.Consumer

import com.github.eduardofcbg.plugin.es.{Finder => FinderJ}
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse}
import play.api.libs.json.Format
import play.libs.F.Promise

import me.enkode.j8.Java8Converters._
import scala.collection.JavaConverters._

class Finder[T <: Index](indexName: String, typeName: String, parentType: String = null, resultPerPage: Integer = null)
                        (implicit es: Format[T])
  extends FinderJ[T](indexName, typeName, parentType, resultPerPage) {

  //use the implicit for rendering the json

  override def parse(hits: SearchResponse): java.util.ArrayList[T] = {
    ???
  }

  override def parse(hit: GetResponse): java.util.ArrayList[T] = {
    ???
  }



  //wrappers for interfacing with the java api

  def search(consumer: (SearchRequestBuilder => Unit), page: Int): Promise[util.List[T]] = super.search(consumer.asJava, page)

  def searchWhere(key: String, value: String, page: Int): Promise[Seq[T]] = {
    super.searchWhere(key, value, page).map()
  }

  def searchWhere(key: String, value: String, consumer: Consumer[SearchRequestBuilder], page: Int): Promise[util.List[T]] = super.searchWhere(key, value, consumer, page)

}