package com.github.eduardofcbg.plugin.es.scala

import java.util

import com.github.eduardofcbg.plugin.es.{Finder => FinderJ}
import me.enkode.j8.Java8Converters._
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse}
import play.api.libs.json.{Format, Json}
import play.libs.F.Promise

import scala.collection.JavaConverters._

class Finder[T <: Index](indexName: String, typeName: String, parentType: String = null, resultPerPage: Integer = null)
                        (implicit es: Format[T])
  extends FinderJ[T](indexName, typeName, parentType, resultPerPage) {

  override def parse(hits: SearchResponse): util.List[T] = {
    val r: Seq[T] = hits.getHits.getHits.map { hit =>
      val parsed: T = Json.parse(hit.getSourceAsString).as[T]
      parsed.version = Some(hit getVersion)
      parsed.id = Some(hit getId)
      parsed
    }
    r asJava
  }

  override def parse(result: GetResponse): T = Json.parse(result.getSourceAsBytes).as[T]

  def search(consumer: (SearchRequestBuilder => Unit), page: Int): Promise[Seq[T]] = {
    Promise.wrap( super.search(consumer.asJava, page).wrapped().map { l => l.asScala } )
  }

  override def searchWhere(key: String, value: String, page: Int): Promise[Seq[T]] = {
    Promise.wrap( super.searchWhere(key, value, page).wrapped().map { l => l.asScala } )
  }

  def searchWhere(key: String, value: String, consumer: (SearchRequestBuilder => Unit), page: Int): Promise[Seq[T]] = {
    Promise.wrap( super.searchWhere(key, value, consumer.asJava, page).wrapped().map { l => l.asScala } )
  }


}