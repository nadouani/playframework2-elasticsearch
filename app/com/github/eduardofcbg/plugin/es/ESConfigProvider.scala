package com.github.eduardofcbg.plugin.es

import javax.inject.{Provider, Inject}

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
//@Singleton
class ESConfigProvider @Inject() (lifecycle: ApplicationLifecycle, config: Configuration) extends Provider[ESConfig] {

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

    //config getBoolean("es.sniff") foreach (s => settings.put("client.transport.sniff", s))
    //config getInt ("es.timeout") foreach (p => settings.put("client.transport.ping_timeout", p))
    //config getInt ("es.ping") foreach (p => settings.put("client.transport.nodes_sampler_interval", p))

    val remoteClient = new TransportClient(settings.build)
    config getStringSeq ("es.hosts") foreach (_.foreach(s => {
      remoteClient.addTransportAddress(
        new InetSocketTransportAddress
          (s.split(":")(0), s.split(":")(1) toInt))
    }))
    this.client = remoteClient

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

  override def get(): ESConfig = new ESConfigSet(getClient, log, indexName())

  private def getClient: Client = client

  private def log(): Boolean = config.getBoolean("es.log") getOrElse(false)

  private def indexName(): String = "play-es_test"

}

class ESConfigSet[T <: Index](client: Client, logFlag: Boolean, index: String) extends ESConfig {

  override def getClient: Client = client

  override def log: Boolean = logFlag

  override def indexName: String = index

}
