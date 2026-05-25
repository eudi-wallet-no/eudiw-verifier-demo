package no.idporten.eudiw.demo.verifier.tsl;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jwt.JWT;

import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.demo.verifier.VerificationException;
import no.idporten.eudiw.demo.verifier.openid4vp.StatusListJwtValidator;
import no.idporten.eudiw.demo.verifier.web.VerificationStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.text.ParseException;
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

    public TokenStatuslistService(RestClient restClient) {
        this.restClient = restClient;
    }

    public VerificationStatus checkStatus(URI uri, JWSAlgorithm jwsAlgorithm, int idx, String statusListJwt, Instant now, JWSVerifier jwsVerifier) {
        if(uri == null || !StringUtils.hasText(uri.toString())){
            return VerificationStatus.INVALID;
        }
        statusListJwtValidator = new StatusListJwtValidator(Set.of(jwsAlgorithm), Duration.ofSeconds(10000));
        return convertIntStatusToEnum(statusListJwtValidator.validateAndResolveStatus(uri, idx, statusListJwt, now, jwsVerifier));
    }

    protected VerificationStatus convertIntStatusToEnum(int status) {
        if(status == 0){
            return VerificationStatus.VALID;
        } else {
            return VerificationStatus.INVALID;
        }
    }

    public JWT requestStatusList(URI url) {
        String jwt;
        if (url != null) {
            try {
                jwt = restClient.get()
                        .uri(url)
                        .retrieve()
                        .body(String.class);
            } catch (Exception e) {
                throw new VerificationException("Invalid response " , "Error in communication with status api "+ e.getMessage());
            }
        } else {
            throw new VerificationException("Invalid response ", "Statuslist url is null for url "+ url);
        }
        if(jwt != null) {
            try {
                return SignedJWT.parse(jwt);
            } catch (ParseException e) {
                throw new VerificationException("Invalid response ", "Signed statuslist JWT cannot be parsed " +url + " "+ e.getMessage());
            }
        }
        else {
            throw new VerificationException("Invalid response ","Response body from statusAPI is null for url  " + url  + " JWT is null");
        }
    }
}

