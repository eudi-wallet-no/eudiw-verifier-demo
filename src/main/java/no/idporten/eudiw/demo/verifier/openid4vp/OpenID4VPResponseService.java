package no.idporten.eudiw.demo.verifier.openid4vp;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWEDecrypterFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.JSONArrayUtils;
import com.nimbusds.jose.util.X509CertUtils;
import id.walt.mdoc.dataelement.*;
import id.walt.mdoc.dataretrieval.DeviceResponse;
import id.walt.mdoc.doc.MDoc;
import id.walt.mdoc.issuersigned.IssuerSigned;
import id.walt.sdjwt.SDJwt;
import id.walt.sdjwt.SimpleJWTCryptoProvider;
import id.walt.sdjwt.VerificationResult;
import no.idporten.eudiw.demo.verifier.StatusCommunicationException;
import no.idporten.eudiw.demo.verifier.VerificationException;
import no.idporten.eudiw.demo.verifier.api.EncryptedAuthorizationResponse;
import no.idporten.eudiw.demo.verifier.config.ConfigProvider;
import no.idporten.eudiw.demo.verifier.trace.*;
import no.idporten.eudiw.demo.verifier.tsl.Status;
import no.idporten.eudiw.demo.verifier.tsl.TokenStatuslistService;
import no.idporten.eudiw.demo.verifier.web.VerificationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class OpenID4VPResponseService {

    private final static Logger log = LoggerFactory.getLogger(OpenID4VPResponseService.class);

    private final VerificationTransactionService verificationTransactionService;
    private final ConfigProvider configProvider;
    private final TokenStatuslistService tokenStatuslistService;
    private static final JsonMapper objectMapper = new JsonMapper();

    public OpenID4VPResponseService(VerificationTransactionService verificationTransactionService, ConfigProvider configProvider, TokenStatuslistService tokenStatuslistService) {
        this.verificationTransactionService = verificationTransactionService;
        this.configProvider = configProvider;
        this.tokenStatuslistService = tokenStatuslistService;
    }

    public String receiveResponse(String verifierTransactionId, EncryptedAuthorizationResponse encryptedAuthorizationResponse) throws Exception {
        VerificationTransaction verificationTransaction = verificationTransactionService.getVerificationTransaction(verifierTransactionId);
        if (verificationTransaction == null) {
            throw new VerificationException("invalid_request", "Unknown verification transaction id");
        }
        verificationTransaction.addProtocolTrace(new StringTrace("walletResponse", "Wallet response", encryptedAuthorizationResponse.response()));
        Map<String, Object> claimsFromJwePayload = decryptAndDeserializeJweResponse(encryptedAuthorizationResponse.response(), verificationTransaction.getEncryptionKey());
        log.info("Decrypted response claims: {}", claimsFromJwePayload);
        verificationTransaction.addProtocolTrace(new JsonTrace("jweClaims", "Decrypted JWE payload", claimsFromJwePayload));

        String nonce = (String) claimsFromJwePayload.get("nonce");
        String state = (String) claimsFromJwePayload.get("state");
        if (!Objects.equals(state, verificationTransaction.getState())) {
            throw new VerificationException("invalid_request", "Invalid state in authorization response");
        }
        final String vpToken = extractVpToken(verificationTransaction.getCredentialConfiguration().getId(), claimsFromJwePayload);
        final Map<String, Object> claims;
        final VerifiedCredentials verifiedCredentials;
        if ("dc+sd-jwt".equals(verificationTransaction.getCredentialConfiguration().getFormat())) {
            verificationTransaction.addProtocolTrace(new SDJwtTrace("vpTokenSDJwt", "SD JWT ", vpToken));
            verifiedCredentials = handleSDJwt(vpToken);
            claims = verifiedCredentials.credentials();

        } else {
            verificationTransaction.addProtocolTrace(new CBORTrace("vpTokenCbor", "mdoc CBOR", vpToken));
            verifiedCredentials = handleMDoc(vpToken);
            claims = verifiedCredentials.credentials();
        }
        verificationTransaction.addProtocolTrace(new MapTrace("credentialClaims", "Claims from credential", claims));
        verificationTransaction.setVerifiedCredentials(verifiedCredentials);
        String responseBody = "{}";
        if ("same-device".equals(verificationTransaction.getFlow())) {
            URI redirectURI = UriComponentsBuilder.fromUriString(configProvider.getExternalBaseUrl()).pathSegment("response-result", verifierTransactionId).build().toUri();
            responseBody = "{ \"redirect_uri\" : \"" + redirectURI + "\"}";
        }
        verificationTransaction.addProtocolTrace(new StringTrace("walletResponseResponse", "Response to wallet response", responseBody));
        verificationTransactionService.updateVerificationTransaction(verifierTransactionId, verificationTransaction);
        return responseBody;
    }

    @SuppressWarnings("unchecked")
    private static String extractVpToken(String credentialId, Map<String, Object> claimsFromJwePayload) {
        final String vpToken;
        Map<String, Object> credentialsMap = (Map<String, Object>) claimsFromJwePayload.get("vp_token");
        Object vpTokenObject = credentialsMap.get(credentialId);
        if (vpTokenObject instanceof String) {
            vpToken = (String) vpTokenObject;
        } else {
            vpToken = ((List<String>) vpTokenObject).getFirst();
        }
        return vpToken;
    }

    private Map<String, Object> decryptAndDeserializeJweResponse(String response, JWK encryptionKey) throws ParseException, JOSEException {
        JWEObject jwe = JWEObject.parse(response);
        JWEDecrypter decrypter = new DefaultJWEDecrypterFactory().createJWEDecrypter(jwe.getHeader(), encryptionKey.toECKey().toPrivateKey());
        jwe.decrypt(decrypter);
        return jwe.getPayload().toJSONObject();
    }

    protected VerifiedCredentials handleSDJwt(String vpToken) throws Exception {
        SDJwt unverifiedSDJwt = unverifiedSDJwt(vpToken);
        X509Certificate cert = certificate(unverifiedSDJwt);
        JWSVerifier jwsVerifier = jwsVerifier(cert);
        JWSAlgorithm jwsAlgorithm = algorithm(cert);
        SimpleJWTCryptoProvider cryptoProvider = new SimpleJWTCryptoProvider(jwsAlgorithm, null, jwsVerifier);
        VerificationResult<SDJwt> result = verificationResult(cryptoProvider, unverifiedSDJwt);
        final Map<String, Object> claims = retrieveClaimsFromSDJwtCredential(result);
        Status statusRecord = extractStatuslistUriAndIdx(result);
        VerificationStatus status;
        if (statusRecord != null) {
            final int idx;
            try {
                idx = Integer.parseInt(statusRecord.statuslist().idx().content());
            } catch (NumberFormatException e) {
                throw new VerificationException("invalid_request", "Invalid status list idx in vp_token");
            }
            try {
                status = tokenStatuslistService.checkStatus(
                        URI.create(statusRecord.statuslist().uri().content()),
                        idx,
                        tokenStatuslistService.requestStatusList(URI.create(statusRecord.statuslist().uri().content())).getParsedString(),
                        Instant.now());
            } catch (StatusCommunicationException e) {
                status = VerificationStatus.INCONCLUSIVE;
            }
        } else {
            status = VerificationStatus.VALID;
        }
        return new VerifiedCredentials(vpToken, claims, status);
    }


    protected Status extractStatuslistUriAndIdx(VerificationResult<SDJwt> sdjwt) {
        Object statusObj = sdjwt.getSdJwt().getFullPayload().get("status");
        if (Objects.isNull(statusObj) || !StringUtils.hasText(statusObj.toString())) {
            return null;
        }
        return objectMapper.convertValue(statusObj, Status.class);
    }

    protected VerificationResult<SDJwt> verificationResult(SimpleJWTCryptoProvider jwtCryptoProvider, SDJwt unverifiedSDJwt){
        VerificationResult<SDJwt> verificationResult = unverifiedSDJwt.verify(jwtCryptoProvider, null);
        if (!verificationResult.getVerified()) {
            throw new VerificationException("invalid_request", "Invalid vp_token. Signature verified: %s, disclosures verified: %s".formatted(verificationResult.getSignatureVerified(), verificationResult.getDisclosuresVerified()));
        }
        return verificationResult;
    }

    protected SDJwt unverifiedSDJwt(String vpToken) {
        return SDJwt.Companion.parse(vpToken);
    }

    protected X509Certificate certificate(SDJwt unverifiedSDJwt) throws Exception{
        JWSHeader jwsHeader = JWSHeader.parse(unverifiedSDJwt.getHeader().toString());
        return X509CertUtils.parse(jwsHeader.getX509CertChain().getFirst().decode());
    }

    protected JWSVerifier jwsVerifier(X509Certificate cert) throws Exception {
        return new ECDSAVerifier((ECPublicKey) cert.getPublicKey());
    }

    protected JWSAlgorithm algorithm(X509Certificate cert) {
        return ECUtils.jwsAlgorithmFromKey(cert.getPublicKey());
    }

    protected Map<String, Object> retrieveClaimsFromSDJwtCredential(VerificationResult<SDJwt> verificationResult) throws ParseException {
        Map<String, Object> claims = new HashMap<>();
        for (String disclosure : verificationResult.getSdJwt().getDisclosures()) {
            List<Object> parsedDisclosure = JSONArrayUtils.parse(new String(Base64.getUrlDecoder().decode(disclosure)));
            claims.put((String) parsedDisclosure.get(1), parsedDisclosure.get(2));
        }
        return claims;
    }

    protected VerifiedCredentials handleMDoc(String vpToken) {
        DeviceResponse deviceResponse = DeviceResponse.Companion.fromCBORBase64URL(vpToken);
        Map<String, Object> claims = new HashMap<>();
        VerificationStatus status = VerificationStatus.VALID;
        for(MDoc mdoc : deviceResponse.getDocuments()) {
            verifyMDoc(mdoc);
            mDocClaims(mdoc.getIssuerSigned(), claims);
            status = verificationStatusMdoc(mdoc);
        }
        return new VerifiedCredentials(vpToken, claims, status);
    }

    protected void verifyMDoc(MDoc mDoc) {
        mDoc.getMSO(); // MSO (Mobile Security Object) verification is not performed here because the issuer's public key or certificate is not available in this context.
        // Proper MSO verification is critical for mdoc validation and should be implemented as soon as the issuer's public key can be obtained.
        // Failing to verify the MSO means the authenticity and integrity of the credential cannot be guaranteed.
        // TODO: Implement MSO verification using the issuer's public key or certificate when it becomes available.
        mDoc.verifyDocType();
        mDoc.verifyIssuerSignedItems();
        mDoc.verifyValidity();
    }

    protected Map<String, Object> mDocClaims(IssuerSigned issuerSigned, Map<String, Object> claims) {
        for (String namespace : issuerSigned.getNameSpaces().keySet()) {
            List<EncodedCBORElement> elements = issuerSigned.getNameSpaces().get(namespace);
            for (EncodedCBORElement element : elements) {
                Map<MapKey, DataElement> elementMap = ((MapElement) element.decode()).getValue();
                String elementIdentifier = null;
                Object elementValue = null;
                for (MapKey mapKey : elementMap.keySet()) {
                    if (mapKey.getStr().equals("elementIdentifier")) {
                        elementIdentifier = String.valueOf(elementMap.get(mapKey).getInternalValue());
                    }
                    if (mapKey.getStr().equals("elementValue")) {
                        elementValue = extractValue(elementMap.get(mapKey));
                    }
                }
                claims.put(elementIdentifier, elementValue);
            }
        }
        return claims;
    }

    protected VerificationStatus verificationStatusMdoc(MDoc mDoc) {
        VerificationStatus verificationStatus;
        if(Objects.requireNonNull(Objects.requireNonNull(mDoc.getMSO()).getStatus()).getStatusList() != null) {

            Status statusObj = objectMapper.convertValue(mDoc.getMSO().getStatus(), Status.class);
            if (statusObj != null) {
                try {
                    verificationStatus = tokenStatuslistService.checkStatus(
                            URI.create(statusObj.statuslist().uri().content()),
                            Integer.parseInt(statusObj.statuslist().idx().content()),
                            tokenStatuslistService.requestStatusList(URI.create(statusObj.statuslist().uri().content())).getParsedString(),
                            Instant.now());
                } catch (StatusCommunicationException e) {
                    verificationStatus = VerificationStatus.INCONCLUSIVE;
                }
            } else {
                verificationStatus = VerificationStatus.VALID;
            }
        } else {
            verificationStatus = VerificationStatus.VALID;
        }
        return verificationStatus;
    }

    protected Object extractValue(DataElement dataElement) {
        if (dataElement == null) {
            return null;
        }
        if (dataElement instanceof BooleanElement) {
            return ((BooleanElement) dataElement).getValue();
        }
        return switch (dataElement.getType()) {
            case number -> ((NumberElement) dataElement).getValue();
            case textString -> ((StringElement) dataElement).getValue();
            case dateTime ->  ((DateTimeElement) dataElement).getValue().toString();
            case fullDate ->  ((FullDateElement) dataElement).getValue().toString();
            case nil -> null;
            case map ->  ((MapElement) dataElement).getValue().entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            e -> String.valueOf(e.getKey()),
                            e -> extractValue(e.getValue())));
            case list ->  ((ListElement) dataElement).getValue()
                    .stream()
                    .map(this::extractValue)
                    .toList();
            case byteString -> new String(Base64.getEncoder().encode(((ByteStringElement) dataElement).getValue()));
            default -> String.valueOf(dataElement.getInternalValue());
        };
    }
}
