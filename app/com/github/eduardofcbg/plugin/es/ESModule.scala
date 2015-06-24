package com.github.eduardofcbg.plugin.es

import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}

/**
 * Created by eduardo on 22-06-2015.
 */
class ESModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[ES].to[ESConfig].eagerly()
    )
  }

}
