package no.idporten.eudiw.demo.verifier.tsl;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

public record Status(
        @JsonProperty("status_list")
        Statuslist statuslist
) {
    public record Statuslist(
            @JsonProperty("idx")
            int idx,
            @JsonProperty("uri")
            URI uri
    ){
    }
}

