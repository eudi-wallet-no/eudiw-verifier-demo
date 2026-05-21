package no.idporten.eudiw.demo.verifier.tsl;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jwt.JWT;

import no.idporten.eudiw.demo.verifier.openid4vp.StatusListJwtValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 *  Responsible for checking revocation status. Calls Status list API with
 *  status list specified inside the proof, gets the list and checks the
 *  status on the index that is inside the proof.
 *
 */

@Service
public class TokenStatuslistService {

    private StatusListJwtValidator statusListJwtValidator;
    private final RestClient restClient;
    private static final Logger logger = LoggerFactory.getLogger(TokenStatuslistService.class);

    public TokenStatuslistService(RestClient restClient) {
        this.restClient = restClient;
    }

    public int checkStatus(URI uri, JWSAlgorithm jwsAlgorithm, int idx, String statusListJwt, Instant now, JWSVerifier jwsVerifier) {
        if(uri == null){
            return -1;
        }
        statusListJwtValidator = new StatusListJwtValidator(Set.of(jwsAlgorithm), Duration.ofSeconds(10000));
        return statusListJwtValidator.validateAndResolveStatus(uri, idx, statusListJwt, now, jwsVerifier);
    }

    public JWT requestStatusList(URI url) {
        JWT jwt;
        if (url != null) {
            try {
                jwt = restClient.get()
                        .uri(url)
                        .retrieve()
                        .body(JWT.class);
            } catch (Exception e) {
                logger.warn("Could not request status list for url {}", url, e);
                return null;
            }
        } else {
            return null;
        }
        return jwt;
    }
}

