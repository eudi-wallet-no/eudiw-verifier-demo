package no.idporten.eudiw.demo.verifier.trace;

public record StringTrace(String id, String description, String value) implements ProtocolTrace {

    @Override
    public String formatted() {
        return value;
    }

}
