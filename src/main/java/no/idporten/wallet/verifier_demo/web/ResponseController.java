package no.idporten.wallet.verifier_demo.web;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.factories.DefaultJWEDecrypterFactory;
import id.walt.mdoc.dataelement.DataElement;
import id.walt.mdoc.dataelement.EncodedCBORElement;
import id.walt.mdoc.dataelement.MapElement;
import id.walt.mdoc.dataelement.MapKey;
import id.walt.mdoc.dataretrieval.DeviceResponse;
import id.walt.mdoc.doc.MDoc;
import id.walt.mdoc.issuersigned.IssuerSigned;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.wallet.verifier_demo.crypto.KeyProvider;
import no.idporten.wallet.verifier_demo.service.CacheService;
import no.idporten.wallet.verifier_demo.trace.*;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
class ResponseController {

    private final KeyProvider keyProvider;
    private final CacheService cacheService;


    @ResponseBody
    @PostMapping("/response")
    public String handleResponse(@ModelAttribute(name = "response") String response, @ModelAttribute(name = "state") String state) throws ParseException, JOSEException, IOException {

        List<ProtocolTrace> traces = new ArrayList<>();
        traces.add(new StringTrace("walletResponse", "Wallet response", response));


        // TODO samle opp her, legge i cache?
        Map<String, Object> claimsFromJwePayload = decryptAndDeserializeJweResponse(response);
        traces.add(new JsonTrace("jweClaims", "Decrypted JWE payload", claimsFromJwePayload));
        String nonce = (String) claimsFromJwePayload.get("nonce");
        String vpToken = (String) claimsFromJwePayload.get("vp_token");
        traces.add(new CBORTrace("vpTokenCbort", "vp_token CBOR pretty", vpToken));

        MultiValueMap<String, String> elementsFromPidDocumentInMDoc = retrieveElementsFromPidDocumentInMDoc(vpToken);
        traces.add(new MapTrace("pidDocumentElements", "Elements from PID-document in MDoc", elementsFromPidDocumentInMDoc));

        log.info("Received authorization response from wallet");
        log.info("Got following elements from PID-document:");
        elementsFromPidDocumentInMDoc.keySet().stream()
                .forEach(k -> log.info(k + ": "+elementsFromPidDocumentInMDoc.get(k)));
        String cacheState = state.startsWith("CD:") ? state.substring(3) : state;
        cacheService.addCrossDevice(cacheState, !cacheState.equals(state));
        cacheService.addState(cacheState, elementsFromPidDocumentInMDoc);

        String responseBody = "{}";
        if (cacheState.equals(state)){
            String redirectUri = cacheService.getRUri(cacheState);
            responseBody = "{ \"redirect_uri\" : \"" + redirectUri + "\"}";
        }
        traces.add(new StringTrace("walletResponseResponse", "Response to wallet response", responseBody));
        cacheService.addTrace(cacheState, traces);
        return responseBody;
    }

    private Map<String, Object> decryptAndDeserializeJweResponse(String response) throws ParseException, JOSEException {
        JWEObject jwe = JWEObject.parse(response);
        JWEDecrypter decrypter = new DefaultJWEDecrypterFactory().createJWEDecrypter(jwe.getHeader(), keyProvider.privateKey());
        jwe.decrypt(decrypter);
        return jwe.getPayload().toJSONObject();
    }

    private MultiValueMap<String, String> retrieveElementsFromPidDocumentInMDoc(String vpToken) {
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
