package no.idporten.wallet.verifier_demo.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;

import java.util.ArrayList;
import java.util.List;

public class JwkResponse {

    @JsonProperty(value = "keys")
    @JsonRawValue
    private List<String> jwkList = new ArrayList<>();

    private JwkResponse() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private JwkResponse instance;

        private Builder() {
            instance = new JwkResponse();
        }

        public JwkResponse build() {
            return instance;
        }

        public Builder addJwk(String jwk) {
            instance.jwkList.add(jwk);
            return this;
        }

        public Builder addJwks(List<String> jwks) {
            for (String jwk : jwks) {
                instance.jwkList.add(jwk);
            }
            return this;
        }
    }

}
