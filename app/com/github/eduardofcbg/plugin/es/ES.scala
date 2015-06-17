package com.github.eduardofcbg.plugin.es

import org.elasticsearch.action.index.IndexResponse
import play.libs.F.Promise

/**
 * Created by eduardo on 17-06-2015.
 */
trait ES[T <: Index] {

  def ola(): Integer

}
