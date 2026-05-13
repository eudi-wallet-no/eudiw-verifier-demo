package no.idporten.eudiw.demo.verifier.web;

import no.idporten.eudiw.demo.verifier.config.ConfigProvider;
import no.idporten.eudiw.demo.verifier.config.CredentialConfig;
import no.idporten.eudiw.demo.verifier.openid4vp.VerificationTransaction;
import no.idporten.eudiw.demo.verifier.openid4vp.VerificationTransactionService;
import no.idporten.eudiw.demo.verifier.openid4vp.VerifiedCredentials;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@DisplayName("When rendering verification results")
@ExtendWith(MockitoExtension.class)
class VerificationResultControllerTest {

    @Mock
    private VerificationTransactionService verificationTransactionService;

    @Mock
    private ConfigProvider configProvider;

    @InjectMocks
    private VerificationResultController controller;

    @Test
    @DisplayName("generic result gets verification status and verify-result view")
    void verificationResult_genericView_hasStatus() {
        VerificationTransaction transaction = transaction("pid");
        VerifiedCredentials verifiedCredentials = new VerifiedCredentials("vp-token", Map.of("personal_administrative_number", "12345678901"));
        CredentialConfig credentialConfig = credentialConfig("pid", List.of("personal_administrative_number"));

        when(verificationTransactionService.getVerificationTransaction("tx-1")).thenReturn(transaction);
        when(verificationTransactionService.retrieveVerifiedCredentials("tx-1")).thenReturn(verifiedCredentials);
        when(configProvider.getCredentialConfig("pid")).thenReturn(credentialConfig);

        Model model = new ExtendedModelMap();

        String view = controller.verificationResult("tx-1", model);

        assertEquals("verify-result", view);
        assertEquals(VerificationStatus.VALID, model.getAttribute("verificationStatus"));
        assertEquals("12345678901", ((MultiValueMap<String, Object>) model.getAttribute("claims")).getFirst("personal_administrative_number"));
        assertEquals(credentialConfig, model.asMap().get("credentialConfig"));
    }

    @Test
    @DisplayName("age result gets verification status before early return")
    void verificationResult_ageView_hasStatus() {
        VerificationTransaction transaction = transaction("alder");
        VerifiedCredentials verifiedCredentials = new VerifiedCredentials("vp-token", Map.of("age_over_18", true));

        when(verificationTransactionService.getVerificationTransaction("tx-2")).thenReturn(transaction);
        when(verificationTransactionService.retrieveVerifiedCredentials("tx-2")).thenReturn(verifiedCredentials);

        Model model = new ExtendedModelMap();

        String view = controller.verificationResult("tx-2", model);

        assertEquals("alder/over", view);
        assertEquals(VerificationStatus.VALID, model.getAttribute("verificationStatus"));
        assertEquals(18, model.asMap().get("age"));
    }

    private static VerificationTransaction transaction(String credentialId) {
        VerificationTransaction transaction = new VerificationTransaction();
        CredentialConfig credentialConfig = credentialConfig(credentialId, List.of("ignored"));
        transaction.setCredentialConfiguration(credentialConfig);
        return transaction;
    }

    private static CredentialConfig credentialConfig(String id, List<String> claims) {
        CredentialConfig config = new CredentialConfig();
        config.setId(id);
        config.setFormat("mso_mdoc");
        config.setClaims(claims);
        return config;
    }
}
