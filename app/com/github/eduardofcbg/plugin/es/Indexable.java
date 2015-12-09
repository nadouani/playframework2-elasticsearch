package com.github.eduardofcbg.plugin.es;

import java.util.Optional;

public interface Indexable {

    void setId(String id);
    void setVersion(long version);
    void setScore(Float score);
    Optional<String> getId();
    Optional<Long> getVersion();
    Optional<Float> getScore();

}
