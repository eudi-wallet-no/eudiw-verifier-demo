package no.idporten.eudiw.demo.verifier.trace;

import java.net.URI;

public record UriTrace (String id, String description, URI uri) implements ProtocolTrace {

    @Override
    public String formatted() {
        return uri.toString()
                .replaceAll("\\?", "?\n")
                .replaceAll("&", "&\n");
    }

}
