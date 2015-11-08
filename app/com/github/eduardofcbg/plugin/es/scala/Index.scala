package com.github.eduardofcbg.plugin.es.scala

import java.lang
import java.util.Optional

import com.github.eduardofcbg.plugin.es.Indexable
import me.enkode.j8.Java8Converters._
import play.api.libs.json.Format

class Index[T <: Index[T]](var id: Option[String] = Option.empty, var version: Option[Long] = Option.empty) extends Indexable {

  val timestamp = System.currentTimeMillis

  import com.github.eduardofcbg.plugin.es.{Index => IndexJ}

  def Finder(implicit es: Format[T]): Finder[T]
    = new Finder[T](IndexJ.getIndexName, this.getClass.getSimpleName.toLowerCase)

  def Finder(typeName: String)(implicit es: Format[T]): Finder[T]
    = new Finder[T](IndexJ.getIndexName, typeName)

  def Finder(indexName: String, typeName: String)(implicit es: Format[T]): Finder[T]
    = new Finder[T](indexName, typeName)

  def Finder(indexName: String, typeName: String, parentType: String, resultPerPage: Integer)(implicit es: Format[T]): Finder[T]
    = new Finder[T](indexName: String, typeName: String, parentType: String, resultPerPage: Integer)

  def Finder(typeName: String, parentType: String, resultPerPage: Integer)(implicit es: Format[T]): Finder[T]
    = new Finder[T](IndexJ.getIndexName, typeName: String, parentType: String, resultPerPage: Integer)

  def Finder(typeName: String, resultPerPage: Integer)(implicit es: Format[T]): Finder[T]
    = new Finder[T](IndexJ.getIndexName, typeName: String, null, resultPerPage: Integer)

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

}

