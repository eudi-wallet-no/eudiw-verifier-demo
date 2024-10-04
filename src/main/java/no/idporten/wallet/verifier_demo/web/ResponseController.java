package no.idporten.wallet.verifier_demo.web;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.RSADecrypter;
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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.text.ParseException;
import java.util.HashMap;
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
    public void handleResponse(@ModelAttribute(name = "response") String response, @ModelAttribute(name = "state") String state, @ModelAttribute(name = "vp_token") String vpToken) throws ParseException, JOSEException, IOException {

        Map<String, Object> claimsFromJwePayload = decryptAndDeserializeJweResponse(response);
        String nonce = (String) claimsFromJwePayload.get("nonce");

        Map<String, String> elementsFromPidDocumentInMDoc = retrieveElementsFromPidDocumentInMDoc((String) claimsFromJwePayload.get("vp_token"));

        log.info("Received authorization response from wallet");
        log.info("Got following elements from PID-document:");
        elementsFromPidDocumentInMDoc.keySet().stream()
                .forEach(k -> log.info(k + ": "+elementsFromPidDocumentInMDoc.get(k)));
        cacheService.addState(state, elementsFromPidDocumentInMDoc);
    }

    private Map<String, Object> decryptAndDeserializeJweResponse(String response) throws IOException, ParseException, JOSEException {

        RSAPrivateKey privateKey = (RSAPrivateKey) keyProvider.privateKey();
        RSADecrypter decrypter = new RSADecrypter(privateKey);

        JWEObject jwe = JWEObject.parse(response);
        jwe.decrypt(decrypter);

        return jwe.getPayload().toJSONObject();
    }

    private Map<String, String> retrieveElementsFromPidDocumentInMDoc(String vpToken) {
        DeviceResponse deviceResponse = DeviceResponse.Companion.fromCBORBase64URL(vpToken);
        Map<String, String> claims = new HashMap<>();
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
                    claims.put(elementIdentifier, elementValue);
                }
            }
        }
        return claims;
    }

}
