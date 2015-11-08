package com.github.eduardofcbg.plugin.es.scala

import org.elasticsearch.action.search.SearchRequestBuilder
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

case class Person(name: String, age: Int) extends Index[Person]
object Person extends Index[Person] {

  implicit val es: Format[Person] = (
    (__ \ "name").format[String] and
    (__ \ "age").format[Int]
  )(Person.apply, unlift(Person.unapply))

  val add = (s: SearchRequestBuilder) => {}

  def getAdults = Finder.search(add, 0)

}
