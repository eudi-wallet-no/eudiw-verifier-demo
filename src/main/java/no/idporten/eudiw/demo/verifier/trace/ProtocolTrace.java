package no.idporten.eudiw.demo.verifier.trace;

import java.io.Serializable;

public interface ProtocolTrace extends Serializable {

    String id();
    String description();
    String formatted();

}
