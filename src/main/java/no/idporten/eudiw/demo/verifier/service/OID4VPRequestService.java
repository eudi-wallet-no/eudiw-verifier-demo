package no.idporten.eudiw.demo.verifier.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import no.idporten.eudiw.demo.verifier.config.ConfigProvider;
import no.idporten.eudiw.demo.verifier.config.CredentialConfig;
import no.idporten.eudiw.demo.verifier.crypto.ECUtils;
import no.idporten.eudiw.demo.verifier.crypto.KeyProvider;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.*;

@RequiredArgsConstructor
@Service
public class OID4VPRequestService {

    private final ConfigProvider configProvider;
    private final KeyProvider keyProvider;

    public String getAuthorizationRequest(String type, String state) {
        return "eudi-openid4vp://"
                + configProvider.getSiop2ClientId()
                + "?client_id=" + configProvider.getClientIdentifier()
                + "&request_uri=" + configProvider.getExternalBaseUrl() + "/req/" + type + "/" + state;
    }


    public JWT makeRequestJwt(String type, String state) throws Exception {
        CredentialConfig credentialConfig = configProvider.getCredentialConfig(type);
        List<Base64> certChain = new ArrayList<>();
        certChain.add(com.nimbusds.jose.util.Base64.encode(keyProvider.certificate().getEncoded()));
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .audience("https://self-issued.me/v2")
                .issuer(configProvider.getExternalBaseUrl())
                .claim("response_uri", configProvider.getExternalBaseUrl() + "/response/" + credentialConfig.getId())
                .claim("response_type", "vp_token")
                .claim("response_mode", "direct_post.jwt")
                .claim("nonce", UUID.randomUUID().toString())
                .claim("state", state)
                .claim("client_id", configProvider.getClientIdentifier())
//                .claim("client_id_scheme", configProvider.getClientIdentifierScheme())
//                .claim("presentation_definition", makePresentationDefinition(credentialConfig))
                .claim("dcql_query", makeDCQL(credentialConfig))
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
                    .type(new JOSEObjectType("oauth-authz-req+jwt"))
                    .build();
            signer = new RSASSASigner(keyProvider.privateKey());
        } else {
            jwtHeader = new JWSHeader.Builder(ECUtils.jwsAlgorithmFromKey(keyProvider))
                    .x509CertChain(certChain)
                    .type(new JOSEObjectType("oauth-authz-req+jwt"))
                    .build();
            signer = new ECDSASigner(keyProvider.ecPrivateKey());
        }
        SignedJWT signedJWT = new SignedJWT(jwtHeader, claims);
        signedJWT.sign(signer);
        return signedJWT;
    }

    @SneakyThrows
    public JSONObject makeDCQL(CredentialConfig credentialConfig) {
        JSONObject pd = new JSONObject();
        pd.appendField("id", UUID.randomUUID().toString());
//        pd.appendField("input_descriptors", makeInputDescriptors(credentialConfig));
        // TODO dette skal gjøres ordentlig
        String dcqlQuery = """
                {
                  "credentials": [
                    {
                      "id": "%s",
                      "format": "mso_mdoc",
                      "meta": {
                        "doctype_value": "%s"
                      },
                      "claims": [
                        {
                          "path": [
                            "%s", "%s"
                          ]
                        }
                      ]
                    }
                  ]
                }""".formatted(credentialConfig.getId(), credentialConfig.getDocType(), credentialConfig.getDocType(), credentialConfig.getFields().getFirst());
        return new JSONObject(JSONObjectUtils.parse(dcqlQuery));
    }

    private JSONObject makePresentationDefinition(CredentialConfig credentialConfig) {
        JSONObject pd = new JSONObject();
        pd.appendField("id", UUID.randomUUID().toString());
        pd.appendField("input_descriptors", makeInputDescriptors(credentialConfig));
        return pd;
    }

    private JSONArray makeInputDescriptors(CredentialConfig credentialConfig) {
        JSONArray ids = new JSONArray(1);
        ids.add(makeInputDescriptor(credentialConfig.getDocType(), credentialConfig.getFields()));
        return ids;
    }

    private JSONObject makeInputDescriptor(String docType, List<String> attributes) {
        JSONObject descriptor = new JSONObject();
        descriptor.appendField("id", docType);
        descriptor.appendField("name", "EUDI PID");
        descriptor.appendField("purpose", "We need to verify your identity");
        descriptor.appendField("format", makeVpFormatsSupported());
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

    public JSONObject makeVpFormatsSupported() {
//        JSONArray algs = new JSONArray(2);
//
//
//        algs.add(JWSAlgorithm.RS256.getName());
//        algs.add(JWSAlgorithm.ES256.getName());
//        algs.add(JWSAlgorithm.ES384.getName());

        JSONObject mdoc = new JSONObject();
//        mdoc.appendField("alg", algs);

        JSONObject format = new JSONObject();
        format.appendField("mso_mdoc", mdoc);
        return format;
    }

    public JSONObject makeClientMetadata() {
        JSONObject metadata = new JSONObject();
        metadata.appendField("jwks", makeJwks().toPublicJWKSet().toJSONObject());
        // JARM Android v24
        metadata.appendField("id_token_signed_response_alg", JWSAlgorithm.RS256.getName());
        metadata.appendField("authorization_encrypted_response_alg", JWEAlgorithm.ECDH_ES.getName());
        metadata.appendField("authorization_encrypted_response_enc", EncryptionMethod.A128GCM.getName());
        JSONArray encryptedResponseAlgs = new JSONArray();
        encryptedResponseAlgs.add(EncryptionMethod.A128GCM.getName());
        metadata.appendField("encrypted_response_enc_values_supported", encryptedResponseAlgs);
        metadata.appendField("vp_formats_supported", makeVpFormatsSupported());
        // VP formats Android v24
        metadata.appendField("vp_formats", makeVpFormatsSupported());
        return metadata;
    }

    @SneakyThrows
    private JWKSet makeJwks() {
        List<JWK> jwkList = new ArrayList<>();
        if (keyProvider.isRsa()) {
            jwkList.add(new RSAKey.Builder(keyProvider.rsaPublicKey())
                    .keyUse(KeyUse.ENCRYPTION)
                    .keyIDFromThumbprint()

                    .build());
        } else {
            jwkList.add(new ECKey.Builder(ECUtils.curveFromKey(keyProvider), keyProvider.ecPublicKey())
                    .keyUse(KeyUse.ENCRYPTION)
                    .keyIDFromThumbprint()
                    .algorithm(JWEAlgorithm.ECDH_ES)
                    .build());
        }
        return new JWKSet(jwkList);
    }

}
