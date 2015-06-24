package com.github.eduardofcbg.plugin.es

import javax.inject.{Singleton, Inject}

import com.google.inject.ImplementedBy
import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.node.Node
import org.elasticsearch.node.NodeBuilder._
import play.api.Configuration
import play.api.inject.ApplicationLifecycle

import scala.concurrent._

/**
 * Created by eduardo on 13-06-2015.
 */
@Singleton
class ESConfig @Inject() (config: Configuration, lifecycle: ApplicationLifecycle) extends ES {

  private var client: Client = null
  private var node: Node = null

  lifecycle.addStopHook(() => {
    if (local) node.close()
    Future.successful(client.close())
  })

  val local = config getBoolean("es.embed") getOrElse(false)

  if (local) createNode
  else setTransportClient

  setMappings

  private def setTransportClient = {
    println("creating client node!!")
    val settings: ImmutableSettings.Builder = ImmutableSettings.settingsBuilder

    config getBoolean("es.sniff") foreach (s => settings.put("client.transport.sniff", s))
    config getInt ("es.timeout") foreach (p => settings.put("client.transport.ping_timeout", p))
    config getInt ("es.ping") foreach (p => settings.put("client.transport.nodes_sampler_interval", p))

    val remoteClient = new TransportClient(settings.build)
    config getStringSeq ("es.hosts") foreach (_.foreach(s => {
      remoteClient.addTransportAddress(
        new InetSocketTransportAddress
          (s.split(":")(0), s.split(":")(1) toInt))
    }))
    client = remoteClient

  }

  private def createNode = {
    node = nodeBuilder.local(true).node
    client = node.client()
  }

  private def setMappings {
    config getString("es.mappings") foreach (
      getClient.admin.indices.preparePutMapping(indexName).setSource().execute.actionGet
    )
  }

  //override def find(typeClass: Class[T]): Finder[T] = new Finder[T](typeClass, getClient, indexName)

  override def log = config.getBoolean("es.log") getOrElse(false)

  override def indexName = "play-es_test"

  override def getClient = client

}

@ImplementedBy(classOf[ESConfig])
trait ES {

  def getClient: Client
  def indexName: String
  def log: Boolean

}