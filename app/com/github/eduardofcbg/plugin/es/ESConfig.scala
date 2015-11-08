package com.github.eduardofcbg.plugin.es

import java.net.InetSocketAddress
import javax.inject.{Inject, Singleton}

import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.node.Node
import org.elasticsearch.node.NodeBuilder._
import play.api.Configuration
import play.api.inject.ApplicationLifecycle

import _root_.scala.concurrent.Future

/**
 * Created by eduardo on 13-06-2015.
 */
@Singleton
class ESConfig @Inject() (config: Configuration, lifecycle: ApplicationLifecycle) extends ES {

  private var client: Client = null
  private var node: Node = null

  private val index: String = config getString("es.index") getOrElse("play-es")
  private val log: Boolean = config getBoolean("es.log") getOrElse(false)
  private val local = config getBoolean("es.embed") getOrElse(false)

  lifecycle.addStopHook(() => {
    if (local) node.close()
    Future.successful(client.close())
  })

  if (local) createNode
  else setTransportClient

  setMappings

  private def setTransportClient = {
    val settings = Settings.settingsBuilder();

    config getBoolean("es.sniff") foreach (s => settings.put("client.transport.sniff", s))
    config getInt ("es.timeout") foreach (p => settings.put("client.transport.ping_timeout", p))
    config getInt ("es.ping") foreach (p => settings.put("client.transport.nodes_sampler_interval", p))

    val remoteClient = TransportClient.builder().settings(settings).build();
    config getStringSeq ("es.hosts") foreach (_.foreach(s => {
      remoteClient.addTransportAddress(
        new InetSocketTransportAddress(
          new InetSocketAddress(s.split(":")(0), s.split(":")(1) toInt)))
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

  override def toLog = log

  override def indexName = index

  override def getClient = client

}

trait ES {

  def getClient: Client
  def indexName: String
  def toLog: Boolean

}