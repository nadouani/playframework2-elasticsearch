package com.github.eduardofcbg.plugin.es

import java.util.function.Consumer

import com.fasterxml.jackson.databind.ObjectMapper
import org.elasticsearch.client.Client
import org.elasticsearch.common.xcontent.XContentBuilder

/**
 * Created by eduardo on 16-06-2015.
 */
trait ESConfig {

    def getClient: Client
    def log: Boolean
    def indexName: String

}
