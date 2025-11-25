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
import no.idporten.eudiw.demo.verifier.VerificationException;
import no.idporten.eudiw.demo.verifier.api.EncryptedAuthorizationResponse;
import no.idporten.eudiw.demo.verifier.config.ConfigProvider;
import no.idporten.eudiw.demo.verifier.crypto.ECUtils;
import no.idporten.eudiw.demo.verifier.trace.*;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class OpenID4VPResponseService {

    private final VerificationTransactionService verificationTransactionService;
    private final ConfigProvider configProvider;

    public OpenID4VPResponseService(VerificationTransactionService verificationTransactionService, ConfigProvider configProvider) {
        this.verificationTransactionService = verificationTransactionService;
        this.configProvider = configProvider;
    }

    public String receiveResponse(String verifierTransactionId, EncryptedAuthorizationResponse encryptedAuthorizationResponse) throws Exception {
        VerificationTransaction verificationTransaction = verificationTransactionService.getVerificationTransaction(verifierTransactionId);
        if (verificationTransaction == null) {
            throw new VerificationException("invalid_request", "Unknown verification transaction id");
        }
        verificationTransaction.addProtocolTrace(new StringTrace("walletResponse", "Wallet response", encryptedAuthorizationResponse.getResponse()));
        Map<String, Object> claimsFromJwePayload = decryptAndDeserializeJweResponse(encryptedAuthorizationResponse.getResponse(), verificationTransaction.getEncryptionKey());
        verificationTransaction.addProtocolTrace(new JsonTrace("jweClaims", "Decrypted JWE payload", claimsFromJwePayload));

        String nonce = (String) claimsFromJwePayload.get("nonce");
        String state = (String) claimsFromJwePayload.get("state");
        if (!Objects.equals(state, verificationTransaction.getState())) {
            throw new VerificationException("invalid_request", "Invalid state in authorization response");
        }
        final String vpToken = extractVpToken(verificationTransaction.getCredentialConfiguration().getId(), claimsFromJwePayload);
        final Map<String, Object> claims;
        if ("dc+sd-jwt".equals(verificationTransaction.getCredentialConfiguration().getFormat())) {
            verificationTransaction.addProtocolTrace(new SDJwtTrace("vpTokenSDJwt", "SD JWT ", vpToken));
            claims = retrieveClaimsFromSDJwtCredential(vpToken);
        } else {
            verificationTransaction.addProtocolTrace(new CBORTrace("vpTokenCbor", "mdoc CBOR", vpToken));
            claims = retrieveClaimsFromMDocCredential(vpToken);
        }
        verificationTransaction.addProtocolTrace(new MapTrace("credentialClaims", "Claims from credential", claims));

        VerifiedCredentials verifiedCredentials = new VerifiedCredentials(vpToken, claims);
        verificationTransactionService.addVerifiedCredentials(verifierTransactionId, verifiedCredentials);
        String responseBody = "{}";
        if ("same-device".equals(verificationTransaction.getFlow())) {
            URI redirectURI = UriComponentsBuilder.fromUriString(configProvider.getExternalBaseUrl()).pathSegment("response-result", verificationTransaction.getCredentialConfiguration().getId(), verifierTransactionId).build().toUri();
            responseBody = "{ \"redirect_uri\" : \"" + redirectURI.toString() + "\"}";
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
        vpToken = ((List<String>) vpTokenObject).getFirst();
        return vpToken;
    }

    private Map<String, Object> decryptAndDeserializeJweResponse(String response, JWK encryptionKey) throws ParseException, JOSEException {
        JWEObject jwe = JWEObject.parse(response);
        JWEDecrypter decrypter = new DefaultJWEDecrypterFactory().createJWEDecrypter(jwe.getHeader(), encryptionKey.toECKey().toPrivateKey());
        jwe.decrypt(decrypter);
        return jwe.getPayload().toJSONObject();
    }

    protected Map<String, Object> retrieveClaimsFromSDJwtCredential(String vpToken) throws Exception{
        SDJwt unverifiedSDJwt = SDJwt.Companion.parse(vpToken);
        JWSHeader jwsHeader = JWSHeader.parse(unverifiedSDJwt.getHeader().toString());
        X509Certificate cert = X509CertUtils.parse(jwsHeader.getX509CertChain().getFirst().decode());
        JWSVerifier jwsVerifier = new ECDSAVerifier((ECPublicKey) cert.getPublicKey());
        JWSAlgorithm jwsAlgorithm = ECUtils.jwsAlgorithmFromKey(cert.getPublicKey());
        SimpleJWTCryptoProvider cryptoProvider = new SimpleJWTCryptoProvider(jwsAlgorithm, null, jwsVerifier);
        VerificationResult<SDJwt> verificationResult = unverifiedSDJwt.verify(cryptoProvider, null);
        if (!verificationResult.getVerified()) {
            throw new VerificationException("invalid_request", "Invalid vp_token signature or unverified disclosures");
        }
        Map<String, Object> claims = new HashMap<>();
        for (String disclosure : verificationResult.getSdJwt().getDisclosures()) {
            List<Object> parsedDisclosure = JSONArrayUtils.parse(new String(Base64.getUrlDecoder().decode(disclosure)));
            claims.put((String) parsedDisclosure.get(1), parsedDisclosure.get(2));
        }
        return claims;
    }

    protected Map<String, Object> retrieveClaimsFromMDocCredential(String vpToken) throws Exception {
        DeviceResponse deviceResponse = DeviceResponse.Companion.fromCBORBase64URL(vpToken);
        Map<String, Object> claims = new HashMap<>();
        for (MDoc mDoc : deviceResponse.getDocuments()) {
            mDoc.getMSO(); // MSO (Mobile Security Object) verification is not performed here because the issuer's public key or certificate is not available in this context.
            // Proper MSO verification is critical for mdoc validation and should be implemented as soon as the issuer's public key can be obtained.
            // Failing to verify the MSO means the authenticity and integrity of the credential cannot be guaranteed.
            // TODO: Implement MSO verification using the issuer's public key or certificate when it becomes available.
            mDoc.verifyDocType();
            mDoc.verifyIssuerSignedItems();
            mDoc.verifyValidity();
            IssuerSigned issuerSigned = mDoc.getIssuerSigned();
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
        }
        return claims;
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
