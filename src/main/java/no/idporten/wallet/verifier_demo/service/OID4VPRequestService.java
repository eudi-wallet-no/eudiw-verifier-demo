package no.idporten.wallet.verifier_demo.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import no.idporten.wallet.verifier_demo.config.ConfigProvider;
import no.idporten.wallet.verifier_demo.config.CredentialConfig;
import no.idporten.wallet.verifier_demo.crypto.KeyProvider;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class OID4VPRequestService {

    private final ConfigProvider configProvider;
    private final KeyProvider keyProvider;

    public String getAuthorizationRequest(String type, String state) {
        return "eudi-openid4vp://"
                + configProvider.getSiop2ClientId()
                +"?client_id="+configProvider.getSiop2ClientId()
                +"&request_uri="+configProvider.getExternalBaseUrl()+"/req/" + type + "/" + state;
    }

    public JWT makeRequestJwt(String type, String state) throws Exception {
        CredentialConfig credentialConfig = configProvider.getCredentialConfig(type);

        List<Base64> certChain = new ArrayList<>();
        certChain.add(com.nimbusds.jose.util.Base64.encode(keyProvider.certificate().getEncoded()));

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .audience("https://self-issued.me/v2")
                .issuer("issuer")
                .claim("response_uri", configProvider.getExternalBaseUrl() + "/response")
                .claim("response_type", "vp_token")
                .claim("response_mode", "direct_post.jwt")
                .claim("nonce", "nonceval") // TODO: Generate nonce
                .claim("state", state)
                .claim("client_id_scheme", "x509_san_dns")
                .claim("client_id", configProvider.getSiop2ClientId())
                .claim("presentation_definition", makePresentationDefinition(credentialConfig))
                .claim("client_metadata", makeClientMetadata())
                .jwtID(UUID.randomUUID().toString()) // Must be unique for each grant
                .issueTime(new Date(Clock.systemUTC().millis())) // Use UTC time!
                .expirationTime(new Date(Clock.systemUTC().millis() + 120000));

        JWTClaimsSet claims = builder.build();

        final JWSHeader jwtHeader;
        final JWSSigner signer;
        if (keyProvider.isRsa()) {
            jwtHeader = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .x509CertChain(certChain)
                    .build();
            signer = new RSASSASigner(keyProvider.privateKey());
        } else {
            jwtHeader = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .x509CertChain(certChain)
                    .build();
            signer = new ECDSASigner(keyProvider.privateKey(), Curve.P_256);
        }
        SignedJWT signedJWT = new SignedJWT(jwtHeader, claims);
        signedJWT.sign(signer);
        return signedJWT;
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
        JSONArray algs = new JSONArray(2);
        algs.add(JWSAlgorithm.RS256.getName());
        algs.add(JWSAlgorithm.ES256.getName());

        JSONObject mdoc = new JSONObject();
        mdoc.appendField("alg", algs);

        JSONObject format = new JSONObject();
        format.appendField("mso_mdoc", mdoc);
        return format;
    }

    public JSONObject makeClientMetadata() {
        JSONObject metadata = new JSONObject();
        metadata.appendField("jwks_uri", configProvider.getExternalBaseUrl() + "/jwks");
        metadata.appendField("id_token_signed_response_alg", JWSAlgorithm.RS256.getName());
        metadata.appendField("authorization_encrypted_response_alg", JWEAlgorithm.ECDH_ES.getName());
        metadata.appendField("authorization_encrypted_response_enc", EncryptionMethod.A128CBC_HS256.getName());
        return metadata;
    }



}
