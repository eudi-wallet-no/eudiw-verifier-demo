package no.idporten.eudiw.demo.verifier.tsl;

import com.fasterxml.jackson.annotation.JsonProperty;


public record Status(
        @JsonProperty("status_list")
        Statuslist statuslist
) {
    public record Statuslist(
            @JsonProperty("idx")
            ContentString idx,
            @JsonProperty("uri")
            ContentString uri
    ) {}

    // Because of kotlin serialisation, everything is packed inside an object with
    // content. therefore, this wrapper
    public record ContentString(
            @JsonProperty("content")
            String content
    ) {}
}


