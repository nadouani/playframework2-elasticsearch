package com.github.eduardofcbg.plugin.es;

import play.api.Configuration;
import play.api.Environment;
import play.api.inject.Binding;
import play.api.inject.Module;
import scala.collection.Seq;

/**
 * Created by eduardo on 09-06-2015.
 */
public class ESModule extends Module {

    @Override
    public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {
        System.out.println("loading module");
        Config.setConfig(configuration);
        return seq(bind(ESComponent.class).to(ElasticSearchPlugin.class).eagerly());
    }

}
