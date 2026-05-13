package no.idporten.eudiw.demo.verifier.web;

import lombok.Getter;

@Getter
public enum VerificationStatus {
    VALID("success", "result.status.heading.VALID"),
    INVALID("error", "result.status.heading.INVALID");

    private final String notificationVariant;
    private final String headingKey;

    VerificationStatus(String notificationVariant, String headingKey) {
        this.notificationVariant = notificationVariant;
        this.headingKey = headingKey;
    }

}
