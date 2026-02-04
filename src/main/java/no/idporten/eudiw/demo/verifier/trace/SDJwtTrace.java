package no.idporten.eudiw.demo.verifier.trace;

import id.walt.sdjwt.SDJwt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ObjectWriter;

import java.util.Map;

public record SDJwtTrace(String id, String description, String sdJwt) implements ProtocolTrace {

    private static final Logger logger = LoggerFactory.getLogger(SDJwtTrace.class);

    @Override
    public String formatted() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
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
        } catch (Exception e) {
            logger.error("Failed to format SD-JWT", e);
            return sdJwt;
        }
    }

}
