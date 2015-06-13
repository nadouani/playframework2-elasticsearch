package com.github.eduardofcbg.plugin.es

import com.fasterxml.jackson.databind.ObjectMapper
import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.node.Node
import org.elasticsearch.node.NodeBuilder._
import play.api.Configuration
import play.libs.Json

/**
 * Created by eduardo on 13-06-2015.
 */
class ElasticSearchPlugin extends ESComponent {

  val config: Configuration = Config.myConfig

  val local = config getBoolean("es.embed") getOrElse(false)
  if (local) createNode
  else setTransportClient
  setMappings

  /*lifecycle.addStopHook(() => {
    if (local) node.close()
    Future.successful(client.close())
  })*/

  private var client: Client = null
  private var node: Node = null

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

  override def getClient: Client = client

  override def getMapper: ObjectMapper = Json.mapper()

  override def log(): Boolean = config.getBoolean("es.log") getOrElse(false)

  override def indexName(): String = "play-es_test"

}

object Config {
  var myConfig: Configuration = null
  def setConfig(configuration: Configuration): Unit = {
    myConfig = configuration;
  }
}
