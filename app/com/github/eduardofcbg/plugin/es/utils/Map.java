package com.github.eduardofcbg.plugin.es.utils;

import play.api.libs.concurrent.Promise;
import play.libs.F;

import javax.xml.transform.Result;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by eduardo on 10-07-2015.
 */
public class Map {

    List<Promise> promises;

    public Map(Promise promise) {
        promises = new ArrayList<>();
        promises.add(promise);
    }

    public static Map Mapping(Promise promise) {
       return new Map(promise);
    }

    public Map with(Promise promise) {
        this.promises.add(promise);
        return this;
    }

    public F.Promise<Result> to() {
        return null;
    }

}
