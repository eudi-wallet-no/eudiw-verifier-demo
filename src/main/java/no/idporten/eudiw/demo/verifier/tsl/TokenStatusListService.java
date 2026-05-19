package no.idporten.eudiw.demo.verifier.tsl;

import com.nimbusds.jwt.JWT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;

/**
 *  Responsible for checking revocation status. Calls Status list API with
 *  status list specified inside the proof, gets the list and checks the
 *  status on the index that is inside the proof.
 *
 */

@Service
public class TokenStatusListService {

    private final RestClient restClient;
    private static final Logger logger = LoggerFactory.getLogger(TokenStatusListService.class);

    public TokenStatusListService(RestClient restClient) {
        this.restClient = restClient;
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
