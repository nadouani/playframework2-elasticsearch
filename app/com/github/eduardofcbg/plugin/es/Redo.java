package com.github.eduardofcbg.plugin.es;

@FunctionalInterface
public interface Redo<T extends Index> {

    public void redo(final T actual, T redo);

}
