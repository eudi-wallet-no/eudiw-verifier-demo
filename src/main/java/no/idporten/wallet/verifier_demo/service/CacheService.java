package no.idporten.wallet.verifier_demo.service;

import no.idporten.wallet.verifier_demo.trace.ProtocolTrace;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Veldig enkel cache for å starte autentisering og lagre claims når ferdig.  Denne vil fylle opp minne!
 */
@Service
public class CacheService {

    Map<String, MultiValueMap<String, String>> stateCache = new HashMap<>();

    public void addState(String state, MultiValueMap<String, String> claims) {
        stateCache.put(state, claims);
    }

    public Boolean containsState(String state) {
        return stateCache.containsKey(state);
    }

    public MultiValueMap<String, String> getState(String state) {
        return stateCache.remove(state);
    }

    Map<String, String> resultURICache = new HashMap<>();

    public void addRUri(String state, String uri) {
        resultURICache.put(state, uri);
    }

    public Boolean containsRUri(String state) {
        return resultURICache.containsKey(state);
    }

    public String getRUri(String state) {
        return resultURICache.remove(state);
    }

    Map<String, Boolean> crossDeviceCache = new HashMap<>();

    public void addCrossDevice(String state, Boolean isCrossDevice) {
        crossDeviceCache.put(state, isCrossDevice);
    }

    public Boolean containsCrossDevice(String state) {
        return crossDeviceCache.containsKey(state);
    }

    public Boolean getCrossDevice(String state) {
        return crossDeviceCache.remove(state);
    }

    Map<String, List<ProtocolTrace>> traceCache = new HashMap<>();

    public void addTrace(String state, List<ProtocolTrace> traces) {
        traceCache.put(state, traces);
    }

    public List<ProtocolTrace> getTrace(String state) {
        return traceCache.remove(state);
    }

}
