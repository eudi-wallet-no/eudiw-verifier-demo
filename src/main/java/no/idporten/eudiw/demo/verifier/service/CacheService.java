package no.idporten.eudiw.demo.verifier.service;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.demo.verifier.openid4vp.VerificationTransaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@RequiredArgsConstructor
@Service
public class CacheService {

    @Value("${spring.application.name}")
    private String applicationName;

    private final Cache cache;

    protected String verificationTransactionKey(String verificationTransactionId) {
        return applicationName + ":verification-transactions:" + verificationTransactionId;
    }

    public void putVerificationTransaction(String verificationTransactionId, VerificationTransaction verificationTransaction) {
        cache.set(verificationTransactionKey(verificationTransactionId), verificationTransaction, Duration.of(30, ChronoUnit.MINUTES));
    }

    public VerificationTransaction removeVerificationTransaction(String verificationTransactionId) {
        return (VerificationTransaction) cache.remove(verificationTransactionKey(verificationTransactionId));
    }

    public VerificationTransaction getVerificationTransaction(String verificationTransactionId) {
        return (VerificationTransaction) cache.get(verificationTransactionKey(verificationTransactionId));
    }

}
