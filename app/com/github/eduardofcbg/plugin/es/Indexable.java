package com.github.eduardofcbg.plugin.es;

import java.util.Optional;

public interface Indexable {

    void setId(String id);
    void setVersion(long version);
    Optional<String> getId();
    Optional<Long> getVersion();

}
