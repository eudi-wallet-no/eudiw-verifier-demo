package no.idporten.eudiw.demo.verifier.openid4vp;

import no.idporten.eudiw.demo.verifier.VerificationException;
import no.idporten.eudiw.demo.verifier.config.CredentialConfig;
import no.idporten.eudiw.demo.verifier.service.CacheService;
import org.springframework.stereotype.Service;

@Service
public class VerificationTransactionService {

    public static String STATUS_UNKNOWN = "UNKNOWN";
    public static String STATUS_WAIT = "WAIT";
    public static String STATUS_AVAILABLE = "AVAILABLE";

    private final CacheService cacheService;

    public VerificationTransactionService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public void initTransaction(String verifierTransactionId, CredentialConfig credentialConfiguration) {
        VerificationTransaction verificationTransaction = new VerificationTransaction();
        verificationTransaction.setStatus(STATUS_WAIT);
        verificationTransaction.setCredentialConfiguration(credentialConfiguration);
        cacheService.putVerificationTransaction(verifierTransactionId, verificationTransaction);
    }

    public VerificationTransaction getVerificationTransaction(String verifierTransactionId) {
        return cacheService.getVerificationTransaction(verifierTransactionId);
    }

    public void addVerifiedCredentials(String verifierTransactionId, VerifiedCredentials verifiedCredentials) {
        VerificationTransaction verificationTransaction = cacheService.getVerificationTransaction(verifierTransactionId);
        if (verificationTransaction == null) {
            throw new VerificationException("invalid_request", "Unknown verifier transaction");
        }
        verificationTransaction.setStatus(STATUS_AVAILABLE);
        verificationTransaction.setVerifiedCredentials(verifiedCredentials);
        updateVerificationTransaction(verifierTransactionId, verificationTransaction);
    }

    public void updateVerificationTransaction(String verifierTransactionId, VerificationTransaction verificationTransaction) {
        cacheService.putVerificationTransaction(verifierTransactionId, verificationTransaction);
    }

    public VerifiedCredentials retrieveVerifiedCredentials(String verifierTransactionId) {
        VerificationTransaction verificationTransaction = cacheService.getVerificationTransaction(verifierTransactionId);
        if (verificationTransaction == null) {
            throw new VerificationException("invalid_request", "Unknown verifier transaction");
        }
        if (verificationTransaction.getVerifiedCredentials() == null) {
            throw new VerificationException("invalid_request", "Verifier transaction data not available");
        }
        VerifiedCredentials verifiedCredentials = verificationTransaction.getVerifiedCredentials();
        cacheService.removeVerificationTransaction(verifierTransactionId);
        return verifiedCredentials;
    }

}
