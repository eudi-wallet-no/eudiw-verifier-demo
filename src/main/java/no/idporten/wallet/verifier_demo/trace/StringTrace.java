package no.idporten.wallet.verifier_demo.trace;

import lombok.SneakyThrows;

public record StringTrace(String id, String description, String value) implements ProtocolTrace {

    @SneakyThrows
    @Override
    public String formatted() {
        return value;
    }

}
