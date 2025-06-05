package no.idporten.eudiw.demo.verifier.web;

import com.nimbusds.jose.jwk.JWK;
import no.idporten.eudiw.demo.verifier.crypto.JwksProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.stream.Collectors;

@Controller
@RequestMapping(value = {"/jwk", "/jwks", "/jwk/", "/jwks/", "/.well-known/jwks.json", "/jwk.json", "/jwks/.well-known/jwks.json", "/jwks-endpoint", "/jwks-endpoint/", "/wallet/public-keys.json"})
@CrossOrigin(origins = "*")
public class JwksController {

    private JwksProvider jwksProvider;

    @Autowired
    public JwksController(JwksProvider jwksProvider) {
        this.jwksProvider = jwksProvider;
    }

    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity getJwks() {
        return ResponseEntity.ok(
                JwkResponse
                        .builder()
                        .addJwks(jwksProvider.jwks()
                                .getKeys()
                                .stream()
                                .map(JWK::toPublicJWK)
                                .map(JWK::toJSONString)
                                .collect(Collectors.toList()))
                        .build());
    }

}
