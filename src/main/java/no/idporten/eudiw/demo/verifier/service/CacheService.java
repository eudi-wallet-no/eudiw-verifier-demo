package no.idporten.eudiw.demo.verifier.service;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.demo.verifier.trace.ProtocolTrace;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Caching av å starte autentisering og lagre claims når ferdig.
 */
@RequiredArgsConstructor
@Service
public class CacheService {

    @Value("${spring.application.name}")
    private String applicationName;

    private final Cache cache;

    protected String stateCacheKey(String state) {
        return applicationName + ":state:" + state;
    }

    public void addState(String state, MultiValueMap<String, Object> claims) {
        cache.set(stateCacheKey(state), claims, Duration.of(30, ChronoUnit.MINUTES));
    }

    public Boolean containsState(String state) {
        return cache.get(stateCacheKey(state)) != null;
    }

    public MultiValueMap<String, Object> getState(String state) {
        return (MultiValueMap<String, Object>) cache.remove(stateCacheKey(state));
    }

    protected String resultUriCacheKey(String state) {
        return applicationName + ":result-uri:" + state;
    }

    public void addRUri(String state, String uri) {
        cache.set(resultUriCacheKey(state), uri, Duration.of(30, ChronoUnit.MINUTES));
    }

    public Boolean containsRUri(String state) {
        return cache.get(resultUriCacheKey(state)) != null;
    }

    public String getRUri(String state) {
        return (String) cache.remove(resultUriCacheKey(state));
    }


    protected String crossDeviceCacheKey(String state) {
        return applicationName + ":cross-device:" + state;
    }

    public void addCrossDevice(String state, Boolean isCrossDevice) {
        cache.set(crossDeviceCacheKey(state), isCrossDevice, Duration.of(30, ChronoUnit.MINUTES));
    }

    public Boolean containsCrossDevice(String state) {
        return cache.get(crossDeviceCacheKey(state)) != null;
    }

    public Boolean getCrossDevice(String state) {
        return (Boolean) cache.remove(crossDeviceCacheKey(state));
    }

    protected String traceCacheKey(String state) {
        return applicationName + ":trace:" + state;
    }

    public void addTrace(String state, List<ProtocolTrace> traces) {
        cache.set(traceCacheKey(state), traces, Duration.of(30, ChronoUnit.MINUTES));
    }

    public List<ProtocolTrace> getTrace(String state) {
        return (List<ProtocolTrace>) cache.remove(traceCacheKey(state));
    }

}
