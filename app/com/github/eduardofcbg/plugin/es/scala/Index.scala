package com.github.eduardofcbg.plugin.es.scala

import play.api.libs.json.Format

abstract class Index[T <: Index] {

  def id: Option[String] = Option.empty
  def version: Option[Long] = Option.empty
  val timestamp = System.currentTimeMillis

  import com.github.eduardofcbg.plugin.es.{Index => IndexJ}

  def Finder(implicit es: Format[T]): Finder[T]
    = new Finder[T](IndexJ.getIndexName, this.getClass.getSimpleName.toLowerCase)[T]

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


}

