package com.github.eduardofcbg.plugin.es;

import java.util.Map;

@FunctionalInterface
public interface ManualRedo<T> {

    public void redo(final T actual, Map<String, Object> redo);

}
