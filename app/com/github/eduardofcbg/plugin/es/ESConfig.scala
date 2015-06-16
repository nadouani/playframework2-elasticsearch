package com.github.eduardofcbg.plugin.es

import org.elasticsearch.client.Client

/**
 * Created by eduardo on 16-06-2015.
 */
trait ESConfig {

    def getClient: Client
    def log: Boolean
    def indexName: String

}
