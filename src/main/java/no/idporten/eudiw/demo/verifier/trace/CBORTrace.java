package no.idporten.eudiw.demo.verifier.trace;

import com.authlete.cbor.CBORDecoder;
import com.authlete.cbor.CBORItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

public record CBORTrace(String id, String description, String value) implements ProtocolTrace {

    private final static Logger log = LoggerFactory.getLogger(CBORTrace.class);

    @Override
    public String formatted() {
        try {
            byte[] input = Base64.getUrlDecoder().decode(value);
            CBORItem item = new CBORDecoder(input).next();
            return item.prettify();
        } catch (Exception e) {
            log.error("Failed to format CBOR", e);
            return value;
        }
    }

}
