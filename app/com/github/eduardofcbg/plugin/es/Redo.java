package com.github.eduardofcbg.plugin.es;

@FunctionalInterface
public interface Redo<T extends Index> {

    public T redo(final T actual, T redo);

}
