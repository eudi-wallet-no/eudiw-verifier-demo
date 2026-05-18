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
        // 1. prepare request with header specified in protocol
        JWT jwt;
        try {
            jwt = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(JWT.class);
        } catch (Exception e) {
            logger.warn("Could not request status list for url {}", url, e);
            return null;
        }
        // 2.  Receive response in form of jwt
        logger.info("JWT requested for url {}", url);
        logger.info("JWT {}", jwt);
        return jwt;
    }

    protected boolean validateStatusList(JWT statusList) {
        // if valid, return true, else false
        return false;
    }

    protected boolean checkRevocationStatus(JWT statusList, int index) {
        // find the status on the given index

        // if revoked, return 1, if not revoked return 0
        return false;
    }

}
