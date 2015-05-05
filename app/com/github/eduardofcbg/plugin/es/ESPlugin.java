package com.github.eduardofcbg.plugin.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import play.Application;
import play.Play;
import play.Plugin;

public class ESPlugin extends Plugin {

    private final Application application;
    private ObjectMapper mapper;

    private Client client;

    public ESPlugin(Application application) {
        this.application = application;
        if (enabled()) {
            this.mapper = new ObjectMapper();
            this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
    }

    private boolean isPluginDisabled() {
        String status =  application.configuration().getString("es.status");
        String host = application.configuration().getString("es.host");
        return status == null || !status.equals("enabled") || host == null;
    }

    public static ESPlugin getPlugin() {
        return Play.application().plugin(ESPlugin.class);
    }

    public Client getClient() {
        return client;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    @Override
    public boolean enabled() {
        return !isPluginDisabled();
    }

    @Override
    public void onStart() {
        int defaultPort = 9300;
        String host = application.configuration().getString("es.host");
        String  port = application.configuration().getString("es.port");
        if (port != null) defaultPort = Integer.parseInt(port);
        if (host == null) play.Logger.error("ES host not specified");
        else
            client = new TransportClient().addTransportAddress(new InetSocketTransportAddress(host, defaultPort));

    }

    @Override
    public void onStop() {
        client.close();
    }
}
