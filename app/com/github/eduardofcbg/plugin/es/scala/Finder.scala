package com.github.eduardofcbg.plugin.es.scala

import java.util

import com.github.eduardofcbg.plugin.es.{ES, Finder => FinderJ, Indexable}
import me.enkode.j8.Java8Converters._
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse}
import play.api.libs.json.{Format, Json}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class Finder[T <: Indexable](typeName: String, parentType: String = "", resultPerPage: Int = 5, indexName: String = "play-es")
                        (implicit val es: Format[T], implicit var esClient: ES)
  extends FinderJ[T](indexName, typeName, parentType, resultPerPage) {

  FinderJ.setEsClient(esClient)

  override def parse(hits: SearchResponse): util.List[T] = {
    val r: Seq[T] = hits.getHits.getHits.map { hit =>
      val parsed: T = Json.parse(hit.getSourceAsString).as[T]
      parsed.setVersion(hit.getVersion)
      parsed.setId(hit.getId)
      parsed
    }
    r asJava
  }

  override def parse(result: GetResponse): T = Json.parse(result.getSourceAsBytes).as[T]

  private val all = (s: SearchRequestBuilder) => {}

  def search(page: Int = 0, consumer: (SearchRequestBuilder => Unit) = all)(implicit ec: ExecutionContext = ExecutionContext.global): Future[Seq[T]] = {
    super.search(consumer.asJava, page).wrapped().map { l => l.asScala }
  }

  def searchWhere(page: Int = 0, key: String, value: String)(implicit ec: ExecutionContext = ExecutionContext.global): Future[Seq[T]] = {
    super.searchWhere(key, value, page).wrapped().map { l => l.asScala }
  }

  def index(toIndex: T)(implicit ec: ExecutionContext = ExecutionContext.global): Future[IndexResponse] = super.index(toIndex).wrapped()

  def get(id: String)(implicit ec: ExecutionContext = ExecutionContext.global): Future[Option[T]] = super.get(id).wrapped().map( l => l.asScala)

  override def getObjectAsBytes(toIndex: T): Array[Byte] = Json.toJson(toIndex).toString().getBytes

}