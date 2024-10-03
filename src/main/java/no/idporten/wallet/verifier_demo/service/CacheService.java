package no.idporten.wallet.verifier_demo.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Veldig enkel cache for å starte autentisering og lagre claims når ferdig.  Denne vil fylle opp minne!
 */
@Service
public class CacheService {

    Map<String, Map<String, String>> stateCache = new HashMap<>();

    public void addState(String state, Map<String, String> claims) {
        stateCache.put(state, claims);
    }

    public boolean containsState(String state) {
        return stateCache.containsKey(state);
    }

    public Map<String, String> getState(String state) {
        return stateCache.remove(state);
    }

}
