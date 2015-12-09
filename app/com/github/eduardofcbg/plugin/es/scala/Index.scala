package com.github.eduardofcbg.plugin.es.scala

import java.lang
import java.lang.Float
import java.util.Optional

import com.github.eduardofcbg.plugin.es.{ES, Indexable}
import me.enkode.j8.Java8Converters._
import play.api.libs.json.Format

class Index[T <: Index[T]](var id: Option[String] = Option.empty, var version: Option[Long] = Option.empty, var score: Option[Float] = Option.empty) extends Indexable {

  val timestamp = System.currentTimeMillis

  def Finder(implicit es: Format[T]): Finder[T]
    = new Finder[T](this.getClass.getSimpleName.toLowerCase.dropRight(1))(es, registerClient.client)

  def Finder(typeName: String)(implicit es: Format[T]): Finder[T]
    = new Finder[T](typeName)(es, registerClient.client)

  override def getId = id.asJava

  override def getVersion: Optional[java.lang.Long] = {
    if (version.isEmpty) Optional.empty()
    Optional.of(new lang.Long(version.get))
  }

  override def setId(id: String) = {
    this.id = Some(id)
  }

  override def setVersion(version: Long) = {
    this.version = Some(version)
  }

  override def setScore(score: Float) = {
    this.score = Some(score)
  }

  override def getScore: Optional[Float] = {
    if (score.isEmpty) Optional.empty()
    Optional.of(new lang.Float(score.get))
  }

  object registerClient {
    var client: ES = null
    def apply(client: ES) = {
      if(client != null) this.client = client
    }
  }

}