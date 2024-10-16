package no.idporten.wallet.verifier_demo.web;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import no.idporten.wallet.verifier_demo.config.ConfigProvider;
import no.idporten.wallet.verifier_demo.config.CredentialConfig;
import no.idporten.wallet.verifier_demo.crypto.KeyProvider;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;


@Controller
@RequiredArgsConstructor
@Slf4j
public class RequestController {

    private final KeyProvider keyProvider;
    private final ConfigProvider configProvider;

    @ResponseBody
    //@GetMapping(value = { "/req", "/request" }, produces = { "application/oauth-authz-req+jwt", "plain/text"})
    @GetMapping(value = {"/req/{type}/{state}", "/request/{state}"})
    public String request(HttpServletRequest request, HttpSession session, @PathVariable("type") String type, @PathVariable("state") String state) throws Exception {
        log.info("Authorization request was requested from {}", request.getRemoteHost());
        String authorizationRequest = makeRequestJwt(type, state);
        log.info("Authorization request; {}", authorizationRequest);
        return authorizationRequest;
    }


    public String makeRequestJwt(String type, String state) throws Exception {
        CredentialConfig credentialConfig = configProvider.getCredentialConfig(type);

        List<Base64> certChain = new ArrayList<>();
        certChain.add(com.nimbusds.jose.util.Base64.encode(keyProvider.certificate().getEncoded()));

        JWSHeader jwtHeader = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .x509CertChain(certChain)
                .build();

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .audience("https://self-issued.me/v2")
                .issuer("issuer")
                .claim("response_uri", configProvider.getExternalBaseUrl() + "/response")
                .claim("response_type", "vp_token")
                .claim("response_mode", "direct_post.jwt")
                .claim("nonce", "nonceval")
                .claim("state", state)
                .claim("client_id_scheme", "x509_san_dns")
                .claim("client_id", configProvider.getSiop2ClientId())
                .claim("presentation_definition", makePresentationDefinition(credentialConfig))
                .claim("client_metadata", makeClientMetadata())
                .jwtID(UUID.randomUUID().toString()) // Must be unique for each grant
                .issueTime(new Date(Clock.systemUTC().millis())) // Use UTC time!
                .expirationTime(new Date(Clock.systemUTC().millis() + 120000));

        JWTClaimsSet claims = builder.build();

        JWSSigner signer = new RSASSASigner(keyProvider.privateKey());
        SignedJWT signedJWT = new SignedJWT(jwtHeader, claims);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    public JSONObject makePresentationDefinition(CredentialConfig credentialConfig) {
        JSONObject pd = new JSONObject();
        pd.appendField("id", UUID.randomUUID().toString());
        pd.appendField("input_descriptors", makeInputDescriptors(credentialConfig));
        return pd;
    }

    public JSONArray makeInputDescriptors(CredentialConfig credentialConfig) {
        JSONArray ids = new JSONArray(1);
        ids.add(makeInputDescriptor(credentialConfig.getDocType(), credentialConfig.getFields()));
        return ids;
    }

    public JSONObject makeInputDescriptor(String docType, List<String> attributes) {
        JSONObject descriptor = new JSONObject();
        descriptor.appendField("id", docType);
        descriptor.appendField("name", "EUDI PID");
        descriptor.appendField("purpose", "We need to verify your identity");
        descriptor.appendField("format", makeFormat());
        descriptor.appendField("constraints", makeConstraints(docType, attributes));
        return descriptor;
    }


    public JSONObject makeConstraints(String docType, List<String> attributes) {
        JSONObject constraints = new JSONObject();
        JSONArray fields = new JSONArray();
        for (String attribute : attributes) {
            fields.add(makeField(docType, attribute, false));
            constraints.appendField("fields", fields);
        }
        return constraints;
    }

    public JSONObject makeField(String docType, String claim, boolean retain) {
        JSONObject field = new JSONObject();
        field.appendField("path", makePath(docType, claim));
        field.appendField("intent_to_retain", retain);
        return field;
    }

    public JSONArray makePath(String docType, String claim) {
        JSONArray path = new JSONArray(1);
        path.add("$['" + docType + "']['" + claim + "']");
        return path;
    }

    public JSONObject makeFormat() {
        JSONArray algs = new JSONArray(1);
        algs.add("RS256");

        JSONObject mdoc = new JSONObject();
        mdoc.appendField("alg", algs);

        JSONObject format = new JSONObject();
        format.appendField("mso_mdoc", mdoc);
        return format;
    }

    public JSONObject makeClientMetadata() {
        JSONObject metadata = new JSONObject();
        metadata.appendField("jwks_uri", configProvider.getExternalBaseUrl() + "/jwks");
        metadata.appendField("id_token_signed_response_alg", "RS256");
        //metadata.appendField("authorization_encrypted_response_alg", "none");
        metadata.appendField("authorization_encrypted_response_alg", "RSA-OAEP-256");
        //metadata.appendField("authorization_encrypted_response_enc", "none");
        metadata.appendField("authorization_encrypted_response_enc", "A128CBC-HS256");
        return metadata;
    }
}
