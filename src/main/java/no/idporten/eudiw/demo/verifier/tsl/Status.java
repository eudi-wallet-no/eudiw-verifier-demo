package no.idporten.eudiw.demo.verifier.tsl;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record Status(
        @JsonProperty("status_list")
        Statuslist statuslist
) {
    public record Statuslist(
            @NotNull
            @JsonProperty("idx")
            ContentString idx,
            @NotNull
            @JsonProperty("uri")
            ContentString uri
    ) {}

    // Because of kotlin serialisation, everything is packed inside an object with
    // content. therefore, this wrapper
    public record ContentString(
            @NotBlank
            @JsonProperty("content")
            String content
    ) {}
}


