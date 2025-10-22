package no.idporten.eudiw.demo.verifier.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.util.Base64;
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
import no.idporten.lib.keystore.KeyProvider;
import no.idporten.lib.keystore.KeystoreManager;
import org.springframework.stereotype.Service;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class OID4VPRequestService {

    private final ConfigProvider configProvider;
    private final KeystoreManager keystoreManager;

    public String getAuthorizationRequest(String type, String state) {
        return "eudi-openid4vp://"
                + configProvider.getSiop2ClientId()
                + "?client_id=" + configProvider.getClientIdentifier()
                + "&request_uri=" + configProvider.getExternalBaseUrl() + "/req/" + type + "/" + state;
    }


    public JWT makeRequestJwt(String type, String state) throws Exception {
        CredentialConfig credentialConfig = configProvider.getCredentialConfig(type);
        KeyProvider keyProvider = keystoreManager.getKeyProvider("access");
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
                .claim("dcql_query", "mso_mdoc".equals(credentialConfig.getFormat()) ? makeDCQLmDoc(credentialConfig) : makeDCQLSDJwt(credentialConfig))
                .claim("client_metadata", makeClientMetadata())
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

    @Deprecated
    private JSONObject makeVpFormats() {
        JSONArray algs = new JSONArray();
        algs.add(JWSAlgorithm.RS256.getName());
        algs.add(JWSAlgorithm.ES256.getName());
        algs.add(JWSAlgorithm.ES384.getName());
        JSONObject mdoc = new JSONObject();
        mdoc.appendField("alg", algs);
        JSONObject format = new JSONObject();
        format.appendField("mso_mdoc", mdoc);
        return format;
    }

    private JSONObject makeVpFormatsSupported() {
        JSONObject mdoc = new JSONObject();
        JSONObject format = new JSONObject();
        format.appendField("mso_mdoc", mdoc);
        return format;
    }

    private JSONObject makeClientMetadata() {
        JSONObject metadata = new JSONObject();
        metadata.appendField("jwks", makeJwks().toPublicJWKSet().toJSONObject());
        JSONArray encryptedResponseAlgs = new JSONArray();
        encryptedResponseAlgs.add(EncryptionMethod.A128GCM.getName());
        metadata.appendField("encrypted_response_enc_values_supported", encryptedResponseAlgs);
        metadata.appendField("vp_formats_supported", makeVpFormatsSupported());
        // JARM Android v24
        metadata.appendField("id_token_signed_response_alg", JWSAlgorithm.RS256.getName());
        metadata.appendField("authorization_encrypted_response_alg", JWEAlgorithm.ECDH_ES.getName());
        metadata.appendField("authorization_encrypted_response_enc", EncryptionMethod.A128CBC_HS256.getName());
        // VP formats Android v24
        metadata.appendField("vp_formats", makeVpFormats());
        return metadata;
    }

    @SneakyThrows
    private JWKSet makeJwks() {
        List<JWK> jwkList = new ArrayList<>();
        ECPublicKey publicKey = (ECPublicKey) keystoreManager.getKeyProvider("access").publicKey();
        jwkList.add(new ECKey.Builder(ECUtils.curveFromKey(publicKey), publicKey)
                .keyUse(KeyUse.ENCRYPTION)
                .keyIDFromThumbprint()
                .algorithm(JWEAlgorithm.ECDH_ES)
                .build());
        return new JWKSet(jwkList);
    }

}
