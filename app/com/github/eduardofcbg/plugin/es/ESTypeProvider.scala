package com.github.eduardofcbg.plugin.es

import java.util.function.Consumer
import javax.inject.Provider

import org.elasticsearch.common.xcontent.XContentBuilder

/**
 * Created by eduardo on 16-06-2015.
 */
class ESTypeProvider[T <: Index] extends Provider[ESType[T]] {

  override def get(): ESType = ???

}

class ESTypeSet[T <: Index] extends ESType[T] {

  var cons: Consumer[XContentBuilder] = null
  var typeCl: Class[T] = null

  override def consumer: Consumer[XContentBuilder] = cons

  override def typeClass: Class[T] = typeCl

  override def setConsumer(consumer: Consumer[XContentBuilder]): Unit = {
    cons = consumer;
  }

  override def setTypeClass(typeClas: Class[T]): Unit = {
    typeCl = typeClas;
  }
}
