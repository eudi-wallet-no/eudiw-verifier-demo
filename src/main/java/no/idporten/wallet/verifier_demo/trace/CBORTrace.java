package no.idporten.wallet.verifier_demo.trace;

import com.authlete.cbor.CBORDecoder;
import com.authlete.cbor.CBORItem;
import lombok.SneakyThrows;

import java.util.Base64;

public record CBORTrace(String id, String description, String value) implements ProtocolTrace {

    @SneakyThrows
    @Override
    public String formatted() {
        byte[] input = Base64.getUrlDecoder().decode(value);
        CBORItem item = new CBORDecoder(input).next();
        return item.prettify();
    }

}
