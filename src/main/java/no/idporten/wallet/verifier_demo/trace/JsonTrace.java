package no.idporten.wallet.verifier_demo.trace;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.util.Map;

public record JsonTrace (String id, String description, Map<String, Object> json) implements ProtocolTrace {

    @SneakyThrows
    @Override
    public String formatted() {
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(json);
    }

}
