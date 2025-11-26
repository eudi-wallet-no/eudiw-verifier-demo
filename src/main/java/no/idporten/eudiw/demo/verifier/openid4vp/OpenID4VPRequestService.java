package no.idporten.eudiw.demo.verifier.openid4vp;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import no.idporten.eudiw.demo.verifier.VerificationException;
import no.idporten.eudiw.demo.verifier.config.ConfigProvider;
import no.idporten.eudiw.demo.verifier.config.CredentialConfig;
import no.idporten.eudiw.demo.verifier.cache.CacheService;
import no.idporten.eudiw.demo.verifier.trace.JsonTrace;
import no.idporten.lib.keystore.KeyProvider;
import no.idporten.lib.keystore.KeystoreManager;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.MessageDigest;
import java.security.interfaces.ECPrivateKey;
import java.time.Clock;
import java.util.*;

@Service
public class OpenID4VPRequestService {

    private final ConfigProvider configProvider;
    private final KeystoreManager keystoreManager;
    private final VerificationTransactionService verificationTransactionService;
    private final CacheService cacheService;

    public OpenID4VPRequestService(ConfigProvider configProvider, KeystoreManager keystoreManager, VerificationTransactionService verificationTransactionService, CacheService cacheService) {
        this.configProvider = configProvider;
        this.keystoreManager = keystoreManager;
        this.verificationTransactionService = verificationTransactionService;
        this.cacheService = cacheService;
    }

    protected URI createRequestUri(String requestId, String flow) {
        return UriComponentsBuilder
                .fromUriString(configProvider.getExternalBaseUrl())
                .pathSegment("openid4vp", "authz-request", flow, requestId)
                .build()
                .toUri();
    }

    protected URI createResponseUri(String verifierTransactionId) {
        return UriComponentsBuilder
                .fromUriString(configProvider.getExternalBaseUrl())
                .pathSegment("openid4vp", "authz-response", verifierTransactionId)
                .build()
                .toUri();
    }

    @SneakyThrows
    private String makeClientId() {
        if ("x509_san_dns".equals(configProvider.getClientIdentifierScheme())) {
            return "x509_san_dns:" + configProvider.getSiop2ClientId();
        }
        if ("x509_hash".equals(configProvider.getClientIdentifierScheme())) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(keystoreManager.getKeyProvider("access").certificate().getEncoded());
            String clientId = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(md.digest());
            return "x509_hash:" + clientId;
        }
        throw new IllegalStateException("Unknown client identifier scheme: " + configProvider.getClientIdentifierScheme());
    }

    @SneakyThrows
    public URI createAuthorizationRequest(String verifierTransactionId, String flow) {
        String requestId = UUID.randomUUID().toString();
        cacheService.putAuthorizationRequest(requestId, verifierTransactionId);
        return UriComponentsBuilder.newInstance()
                .scheme("eudi-openid4vp")
                .host(configProvider.getSiop2ClientId())
                .queryParam("client_id", makeClientId())
                .queryParam("request_uri", createRequestUri(requestId, flow).toString())
                .build()
                .toUri();
    }

    @SneakyThrows
    public String retrieveAuthorizationRequest(String requestId, String flow) {
        String verificationTransactionId = cacheService.retrieveAuthorizationRequest(requestId);
        if (verificationTransactionId == null) {
            throw new VerificationException("invalid_request", "Unknown authorization request");
        }
        VerificationTransaction verificationTransaction = verificationTransactionService.getVerificationTransaction(verificationTransactionId);
        if (verificationTransaction == null) {
            throw new VerificationException("invalid_request", "Unknown verification transaction");
        }
        verificationTransaction.setFlow(flow);
        String state = UUID.randomUUID().toString();
        JWK encryptionKey = new ECKeyGenerator(Curve.P_256).algorithm(JWEAlgorithm.ECDH_ES).keyUse(KeyUse.ENCRYPTION).keyIDFromThumbprint(true).generate();
        verificationTransaction.setState(state);
        verificationTransaction.setEncryptionKey(encryptionKey);
        JWT authorizationRequest = makeRequestJwt(verificationTransaction.getCredentialConfiguration(), verificationTransactionId, encryptionKey, state);
        verificationTransaction.addProtocolTrace(new JsonTrace("jwtAuthzRequest", "JWT-Secured Authorization Request Body", authorizationRequest.getJWTClaimsSet().toJSONObject()));
        verificationTransactionService.updateVerificationTransaction(verificationTransactionId, verificationTransaction);
        return authorizationRequest.serialize();
    }

    public JWT makeRequestJwt(CredentialConfig credentialConfiguration, String verifierTransactionId, JWK encryptionKey, String state) throws Exception {
        KeyProvider keyProvider = keystoreManager.getKeyProvider("access");
        List<Base64> certChain = new ArrayList<>();
        certChain.add(Base64.encode(keyProvider.certificate().getEncoded()));
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .audience("https://self-issued.me/v2")
                .issuer(configProvider.getExternalBaseUrl())
                .claim("response_uri", createResponseUri(verifierTransactionId).toString())
                .claim("response_type", "vp_token")
                .claim("response_mode", "direct_post.jwt")
                .claim("nonce", UUID.randomUUID().toString())
                .claim("state", state)
                .claim("client_id", makeClientId())
                .claim("dcql_query", makeDCQLQuery(credentialConfiguration))
                .claim("client_metadata", makeClientMetadata(encryptionKey))
                .jwtID(UUID.randomUUID().toString()) // Must be unique for each grant
                .issueTime(new Date(Clock.systemUTC().millis())) // Use UTC time!
                .expirationTime(new Date(Clock.systemUTC().millis() + 120000));
        JWTClaimsSet claims = builder.build();
        final JWSHeader jwtHeader;
        final JWSSigner signer;
        jwtHeader = new JWSHeader.Builder(ECUtils.jwsAlgorithmFromKey(keyProvider.publicKey()))
                .x509CertChain(certChain)
                .type(new JOSEObjectType("oauth-authz-req+jwt"))
                .build();
        signer = new ECDSASigner((ECPrivateKey) keyProvider.privateKey());
        SignedJWT signedJWT = new SignedJWT(jwtHeader, claims);
        signedJWT.sign(signer);
        return signedJWT;
    }

    private JSONObject makeVpFormatsSupported() {
        JSONObject mdoc = new JSONObject();
        JSONObject format = new JSONObject();
        format.appendField("mso_mdoc", mdoc);
        JSONObject sdJwt = new JSONObject();
        sdJwt.appendField("sd-jwt_alg_values", List.of("ES256", "ES384"));
        sdJwt.appendField("kb-jwt_alg_values", List.of("ES256", "ES384"));
        format.appendField("dc+sd-jwt", sdJwt);
        return format;
    }

    private JSONObject makeClientMetadata(JWK encryptionKey) throws Exception {
        JSONObject metadata = new JSONObject();
        metadata.appendField("jwks", new JWKSet(encryptionKey).toPublicJWKSet().toJSONObject());
        JSONArray encryptedResponseAlgs = new JSONArray();
        encryptedResponseAlgs.add(EncryptionMethod.A128GCM.getName());
        encryptedResponseAlgs.add(EncryptionMethod.A256GCM.getName());
        metadata.appendField("encrypted_response_enc_values_supported", encryptedResponseAlgs);
        metadata.appendField("vp_formats_supported", makeVpFormatsSupported());
        return metadata;
    }


    public JSONObject makeDCQLQuery(CredentialConfig credentialConfig) {
        return "mso_mdoc".equals(credentialConfig.getFormat()) ? makeDCQLmDoc(credentialConfig) : makeDCQLSDJwt(credentialConfig);
    }

    @SneakyThrows
    public JSONObject makeDCQLSDJwt(CredentialConfig credentialConfig) {
        JSONObject credential = new JSONObject()
                .appendField("id", credentialConfig.getId())
                .appendField("format", credentialConfig.getFormat())
                .appendField("meta", new JSONObject().appendField(
                        "vct_values", List.of(credentialConfig.getDocType())));
        JSONArray claims = new JSONArray();
        for (String claim : credentialConfig.getClaims()) {
            claims.appendElement(new JSONObject()
                    .appendField("path",
                            new JSONArray()
                                    .appendElement(claim)));
        }
        credential.put("claims", claims);
        JSONObject dcql = new JSONObject().appendField("credentials", new JSONArray().appendElement(credential));
        return dcql;
    }


    public JSONObject makeDCQLmDoc(CredentialConfig credentialConfig) {
        JSONObject credential = new JSONObject()
                .appendField("id", credentialConfig.getId())
                .appendField("format", "mso_mdoc")
                .appendField("meta", new JSONObject().appendField("doctype_value", credentialConfig.getDocType()));
        JSONArray claims = new JSONArray();
        for (String claim : credentialConfig.getClaims()) {
            claims.appendElement(new JSONObject()
                    .appendField("path",
                            new JSONArray()
                                    .appendElement(credentialConfig.getDocType())
                                    .appendElement(claim)));
        }
        credential.put("claims", claims);
        JSONObject dcql = new JSONObject().appendField("credentials", new JSONArray().appendElement(credential));
        return dcql;
    }

}
