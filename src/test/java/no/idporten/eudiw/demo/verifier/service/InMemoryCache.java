package no.idporten.eudiw.demo.verifier.service;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * In-memory cache for bruk i enhetstester.
 */
@Primary
@Service
public class InMemoryCache implements Cache {

    private final Map<String, Object> cache = new HashMap<>();

    @Override
    public void set(String cacheKey, Object object, Duration duration) {
        cache.put(cacheKey, object);
    }

    @Override
    public Object get(String cacheKey) {
        return cache.get(cacheKey);
    }

    @Override
    public Object remove(String cacheKey) {
        return cache.remove(cacheKey);
    }

}
