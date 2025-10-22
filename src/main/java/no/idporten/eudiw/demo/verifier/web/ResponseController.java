package no.idporten.eudiw.demo.verifier.web;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWEDecrypterFactory;
import com.nimbusds.jose.util.X509CertUtils;
import id.walt.mdoc.dataelement.DataElement;
import id.walt.mdoc.dataelement.EncodedCBORElement;
import id.walt.mdoc.dataelement.MapElement;
import id.walt.mdoc.dataelement.MapKey;
import id.walt.mdoc.dataretrieval.DeviceResponse;
import id.walt.mdoc.doc.MDoc;
import id.walt.mdoc.issuersigned.IssuerSigned;
import id.walt.sdjwt.SDJwt;
import id.walt.sdjwt.SDisclosure;
import id.walt.sdjwt.SimpleJWTCryptoProvider;
import id.walt.sdjwt.VerificationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.demo.verifier.config.ConfigProvider;
import no.idporten.eudiw.demo.verifier.config.CredentialConfig;
import no.idporten.eudiw.demo.verifier.service.CacheService;
import no.idporten.eudiw.demo.verifier.trace.*;
import no.idporten.lib.keystore.KeystoreManager;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
class ResponseController {

    private final KeystoreManager keystoreManager;
    private final CacheService cacheService;
    private final ConfigProvider configProvider;

    @ResponseBody
    @PostMapping("/response/{id}")
    public String handleResponse(@PathVariable(name = "id") String id, @ModelAttribute(name = "response") String response) throws Exception {
        CredentialConfig credentialConfig = configProvider.getCredentialConfig(id);
        List<ProtocolTrace> traces = new ArrayList<>();
        traces.add(new StringTrace("walletResponse", "Wallet response", response));
        Map<String, Object> claimsFromJwePayload = decryptAndDeserializeJweResponse(response);
        traces.add(new JsonTrace("jweClaims", "Decrypted JWE payload", claimsFromJwePayload));
        String nonce = (String) claimsFromJwePayload.get("nonce");
        String state = (String) claimsFromJwePayload.get("state");
        final String vpToken;
        if (claimsFromJwePayload.get("vp_token") instanceof String) {
            vpToken = (String) claimsFromJwePayload.get("vp_token");
        } else {
            Map<String, Object> credentialsMap = (Map<String, Object>) claimsFromJwePayload.get("vp_token");
            Object vpTokenObject = credentialsMap.get(credentialConfig.getId());
            if (vpTokenObject instanceof String) {
                vpToken = (String) vpTokenObject;
            } else {
                vpToken = ((List<String>) vpTokenObject).getFirst();
            }
        }
        log.info("Received authorization response from wallet with nonce [%s] and state [%s]".formatted(nonce, state));
        final MultiValueMap<String, String> claimsFromCredential;
        if ("mso_mdoc".equals(credentialConfig.getFormat())) {
            traces.add(new CBORTrace("vpTokenCbor", "mdoc CBOR", vpToken));
            claimsFromCredential = retrieveClaimsFromMDocCredential(vpToken);
        } else {
            traces.add(new SDJwtTrace("vpTokenSDJwt", "SD JWT ", vpToken));
            claimsFromCredential = retrieveClaimsFromSDJwtCredential(vpToken);
        }
        traces.add(new MapTrace("credentialClaims", "Claims from credential", claimsFromCredential));

        log.info("Got following elements from mdoc:");
        claimsFromCredential.keySet().forEach(k -> log.info(k + ": "+claimsFromCredential.get(k)));
        String cacheState = state.startsWith("CD:") ? state.substring(3) : state;
        cacheService.addCrossDevice(cacheState, !cacheState.equals(state));
        cacheService.addState(cacheState, claimsFromCredential);

        String responseBody = "{}";
        String redirectUri = cacheService.getRUri(cacheState);
        if (cacheState.equals(state) && StringUtils.hasText(redirectUri)) {
            responseBody = "{ \"redirect_uri\" : \"" + redirectUri + "\"}";
        }
        traces.add(new StringTrace("walletResponseResponse", "Response to wallet response", responseBody));
        cacheService.addTrace(cacheState, traces);
        return responseBody;
    }

    private Map<String, Object> decryptAndDeserializeJweResponse(String response) throws ParseException, JOSEException {
        JWEObject jwe = JWEObject.parse(response);
        JWEDecrypter decrypter = new DefaultJWEDecrypterFactory().createJWEDecrypter(jwe.getHeader(), keystoreManager.getKeyProvider("access").privateKey());
        jwe.decrypt(decrypter);
        return jwe.getPayload().toJSONObject();
    }

    protected MultiValueMap<String, String> retrieveClaimsFromSDJwtCredential(String vpToken) throws Exception{
        SDJwt unverifiedSDJwt = SDJwt.Companion.parse(vpToken);
        JWSHeader jwsHeader = JWSHeader.parse(unverifiedSDJwt.getHeader().toString());
        X509Certificate cert = X509CertUtils.parse(jwsHeader.getX509CertChain().getFirst().decode());
        JWSVerifier jwsVerifier = new ECDSAVerifier((ECPublicKey) cert.getPublicKey());
        SimpleJWTCryptoProvider cryptoProvider = new SimpleJWTCryptoProvider(JWSAlgorithm.ES256, null, jwsVerifier);
        VerificationResult<SDJwt> verificationResult = unverifiedSDJwt.verify(cryptoProvider, null);
        SDJwt verifiedSDJwt = verificationResult.getSdJwt();
        Map<String, List<String>> extractedClaims = verifiedSDJwt
                .getDisclosureObjects()
                .stream()
                .collect(Collectors.toMap(SDisclosure::getKey, sDisclosure -> List.of(sDisclosure.getValue().toString())));

        return new LinkedMultiValueMap<>(extractedClaims);
    }

    protected MultiValueMap<String, String> retrieveClaimsFromMDocCredential(String vpToken) {
        DeviceResponse deviceResponse = DeviceResponse.Companion.fromCBORBase64URL(vpToken);
        MultiValueMap<String, String> claims = new LinkedMultiValueMap<>();
        for (MDoc mDoc : deviceResponse.getDocuments()) {
            mDoc.getMSO(); // TODO verify med hvilke nøkler?
            mDoc.verifyDocType();
            mDoc.verifyIssuerSignedItems();
            mDoc.verifyValidity();
            IssuerSigned issuerSigned = mDoc.getIssuerSigned();
            for (String namespace : issuerSigned.getNameSpaces().keySet()) {
                List<EncodedCBORElement> elements = issuerSigned.getNameSpaces().get(namespace);
                for (EncodedCBORElement element : elements) {
                    Map<MapKey, DataElement> elementMap = ((MapElement) element.decode()).getValue();
                    String elementIdentifier = null;
                    String elementValue = null;
                    for (MapKey mapKey : elementMap.keySet()) {
                        if (mapKey.getStr().equals("elementIdentifier")) {
                            elementIdentifier = String.valueOf(elementMap.get(mapKey).getInternalValue());
                        }
                        if (mapKey.getStr().equals("elementValue")) {
                            elementValue = String.valueOf(elementMap.get(mapKey).getInternalValue());
                        }
                        log.info(mapKey.toString() + "=" + elementMap.get(mapKey).getInternalValue());
                    }
                    claims.add(elementIdentifier, elementValue);
                }
            }
        }
        return claims;
    }

}
