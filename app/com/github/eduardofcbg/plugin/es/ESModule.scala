package com.github.eduardofcbg.plugin.es

import play.api._
import play.api.inject._

/**
 * Created by eduardo on 16-06-2015.
 */
class ESModule[T <: Index] extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[ESConfig].toProvider[ESConfigProvider].eagerly
    )
  }

}
