package no.idporten.eudiw.demo.verifier.cache;

import java.time.Duration;

public interface Cache {

    void set(String cacheKey, Object object, Duration duration);

    Object get(String cacheKey);

    Object remove(String cacheKey);

}
