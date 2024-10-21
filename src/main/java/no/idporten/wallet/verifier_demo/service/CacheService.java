package no.idporten.wallet.verifier_demo.service;

import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
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

    public boolean containsState(String state) {
        return stateCache.containsKey(state);
    }

    public MultiValueMap<String, String> getState(String state) {
        // TODO get istedenfor å remove for å fikse 2xtab-problem, men dette vil fylle opp mine og gjøre data tilgjengelig mye lengre enn tiltenkt!
        return stateCache.get(state);
    }

    Map<String, String> resultURICache = new HashMap<>();

    public void addRUri(String state, String uri) {
        resultURICache.put(state, uri);
    }

    public boolean containsRUri(String state) {
        return resultURICache.containsKey(state);
    }

    public String getRUri(String state) {
        return resultURICache.remove(state);
    }
    

}
