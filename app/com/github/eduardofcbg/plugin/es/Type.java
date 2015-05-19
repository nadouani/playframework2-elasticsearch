package com.github.eduardofcbg.plugin.es;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Type {

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Name {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Parent {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface NestedField {}


}
