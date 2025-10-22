package no.idporten.eudiw.demo.verifier.trace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import id.walt.sdjwt.SDJwt;
import lombok.SneakyThrows;

import java.util.Map;

public record SDJwtTrace(String id, String description, String sdJwt) implements ProtocolTrace {

    @SneakyThrows
    @Override
    public String formatted() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectWriter writer =objectMapper.writerWithDefaultPrettyPrinter();
        ObjectReader reader = objectMapper.readerFor(Map.class);
        SDJwt parsedSDJwt = SDJwt.Companion.parse(sdJwt);
        return """
                %s
                .
                %s
                .
                <signature>
                .
                %s~
                """.formatted(writer.writeValueAsString(reader.readValue(parsedSDJwt.getHeader().toString())),
                writer.writeValueAsString(reader.readValue(parsedSDJwt.getSdPayload().getUndisclosedPayload().toString())),
                String.join("~", parsedSDJwt.getDisclosures()));
    }

}
