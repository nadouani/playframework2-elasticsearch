package com.github.eduardofcbg.plugin.es;

import org.elasticsearch.client.Client;

/**
 * Created by eduardo on 09-06-2015.
 */
public interface ESComponent {

    Client getClient();
    com.fasterxml.jackson.databind.ObjectMapper getMapper();
    boolean log();
    String indexName();

}
