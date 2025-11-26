package no.idporten.eudiw.demo.verifier.trace;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public record JsonTrace (String id, String description, Map<String, Object> json) implements ProtocolTrace {

    @Override
    public String formatted() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (Exception e) {
            return json.toString();
        }
    }

}
