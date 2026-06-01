package no.idporten.eudiw.demo.verifier.tsl;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.demo.verifier.VerificationException;
import no.idporten.eudiw.demo.verifier.config.TokenStatuslistConfig;
import no.idporten.eudiw.demo.verifier.openid4vp.StatusListJwtValidator;
import no.idporten.eudiw.demo.verifier.web.VerificationStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.text.ParseException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 *  Responsible for checking revocation status. Calls Status list API with
 *  status list specified inside the proof, gets the list and checks the
 *  status on the index that is inside the proof.
 *
 */

@Service
public class TokenStatuslistService {
    private final RestClient restClient;
    private final TokenStatuslistConfig tokenStatuslistConfig;

    public TokenStatuslistService(RestClient restClient, TokenStatuslistConfig tokenStatuslistConfig) {
        this.restClient = restClient;
        this.tokenStatuslistConfig = tokenStatuslistConfig;
    }

    public VerificationStatus checkStatus(URI uri, int idx, String statusListJwt, Instant now) {
        if(uri == null || !StringUtils.hasText(uri.toString())){
            return VerificationStatus.INVALID;
        }
        StatusListJwtValidator statusListJwtValidator = new StatusListJwtValidator(Set.of(JWSAlgorithm.RS256), tokenStatuslistConfig.clockSkew());
        JWSVerifier jwsVerifier = statusListJwsVerifier(statusListJwt);
        return convertIntStatusToEnum(statusListJwtValidator.validateAndResolveStatus(uri, idx, statusListJwt, now, jwsVerifier));
    }

    private JWSVerifier statusListJwsVerifier(String statusListJwt) {
        final SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(statusListJwt);
        } catch (ParseException e) {
            throw new VerificationException("invalid_request", "Status list JWT is not a valid compact signed JWT");
        }

        List<Base64> x5c = jwt.getHeader().getX509CertChain();
        if (x5c == null || x5c.isEmpty()) {
            throw new VerificationException("invalid_request", "Status list JWT must include x5c certificate chain");
        }

        final byte[] encodedCert;
        try {
            encodedCert = x5c.getFirst().decode();
        } catch (Exception e) {
            throw new VerificationException("invalid_request", "Status list JWT x5c certificate could not be parsed");
        }

        X509Certificate cert = X509CertUtils.parse(encodedCert);
        if (cert == null) {
            throw new VerificationException("invalid_request", "Status list JWT x5c certificate could not be parsed");
        }
        if (!(cert.getPublicKey() instanceof RSAPublicKey rsaPublicKey)) {
            throw new VerificationException("invalid_request", "Status list JWT x5c certificate must contain RSA public key");
        }
        return new RSASSAVerifier(rsaPublicKey);
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
            } catch (HttpServerErrorException.GatewayTimeout e) {
                throw new VerificationException("Could not verify status " , "Error in communication with status api "+ e.getMessage());
            }

            catch (Exception e) {
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
