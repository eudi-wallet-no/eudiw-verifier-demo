package no.idporten.eudiw.demo.verifier.trace;

import java.util.Arrays;
import java.util.Map;

@SuppressWarnings("rawtypes")
public record MapTrace(String id, String description, Map map) implements ProtocolTrace {

    @Override
    public String formatted() {
        return Arrays.toString(map.entrySet().toArray());
    }

}
