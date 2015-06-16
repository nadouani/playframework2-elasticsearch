package com.github.eduardofcbg.plugin.es

import java.util.function.Consumer

import org.elasticsearch.common.xcontent.XContentBuilder

/**
 * Created by eduardo on 16-06-2015.
 */
trait ESType[T <: Index] {

  def consumer: Consumer[XContentBuilder]
  def typeClass: java.lang.Class[T]
  def setConsumer(consumer: Consumer[XContentBuilder]): Unit
  def setTypeClass(typeClass: java.lang.Class[T]): Unit

}
