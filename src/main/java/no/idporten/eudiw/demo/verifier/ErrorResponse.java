package no.idporten.eudiw.demo.verifier;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        @JsonProperty("error")
        String error,
        @JsonProperty("error_description")
        String errorDescription) {
}
