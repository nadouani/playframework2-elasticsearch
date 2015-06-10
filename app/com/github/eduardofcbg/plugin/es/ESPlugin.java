package com.github.eduardofcbg.plugin.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import play.api.Configuration;
import play.libs.Json;
import play.libs.Scala;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

/**
 * Created by eduardo on 09-06-2015.
 */

@Singleton
public class ESPlugin implements ESComponent {

    private Client client;
    private Node node;

    private static Configuration configuration;

    public ESPlugin() {
        final boolean local = configuration.getBoolean("es.embed").getOrElse(Scala.asScala(() -> false));
        if (local)
            createNode();
        else
            setTransportClient();
        setMappings();

        //lifecycle.addStopHook(() -> {
        //    if (node != null) node.close();
        //    client.close();
        //    return F.Promise.pure(null);
       // });
    }

    public Client getClient() {
        return client;
    }

    public ObjectMapper getMapper() {
        return Json.mapper();
    }

    @Override
    public boolean log() {
        return (boolean) configuration.getBoolean("es.log").get();
    }

    public String indexName() {
        return configuration.getString("es.index", null).get();
    }

    private void setMappings() {
        String mappings = configuration.getString("es.mappings", null).get();
        if (mappings != null)
            getClient().admin().indices().preparePutMapping(indexName())
                    .setSource(mappings).execute().actionGet();
    }

    private void createNode() {
        node = nodeBuilder().local(true).node();
        client = node.client();
    }

    private void setTransportClient() {
        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();

        //TODO:
        String host = configuration.getDeprecatedString("es.client", "es.client");
        //List<String> hosts = Scala.asJava(list);
        final boolean sniff = configuration.getBoolean("es.sniff").getOrElse(Scala.asScala(() -> false));
        final String pingTimeout = configuration.getString("es.timeout", null).get();
        final String pingFrequency = configuration.getString("es.ping", null).get();
        final String clusterName = configuration.getString("es.cluster", null).get();

        if (sniff) settings.put("client.transport.sniff", true);
        if (pingTimeout != null) settings.put("client.transport.ping_timeout", pingTimeout);
        if (pingFrequency != null) settings.put("client.transport.nodes_sampler_interval", pingTimeout);
        if (clusterName != null) settings.put("cluster.name", clusterName);

        TransportClient client = new TransportClient(settings.build());
        //for(int i = 0; i < hosts.size(); i++)
            client.addTransportAddress(
                    //new InetSocketTransportAddress(hosts.get(i).split(":")[0], Integer.valueOf(hosts.get(i).split(":")[1]))
                    new InetSocketTransportAddress(host.split(":")[0], Integer.valueOf(host.split(":")[1]))
            );

        this.client = client;
    }

    public static void setConfiguration(Configuration conf) {
        configuration = conf;
    }

}
