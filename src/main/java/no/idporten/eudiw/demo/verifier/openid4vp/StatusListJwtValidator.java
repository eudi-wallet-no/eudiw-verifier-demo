package no.idporten.eudiw.demo.verifier.openid4vp;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.demo.verifier.VerificationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.zip.InflaterInputStream;

/**
 * I henhold til Token Status List V20
 * https://datatracker.ietf.org/doc/draft-ietf-oauth-status-list/20/
 */
public class StatusListJwtValidator {

    private static final String STATUS_LIST_JWT_TYP = "statuslist+jwt";
    private static final Set<Integer> ALLOWED_BITS = Set.of(1, 2, 4, 8);
    private static final String ERROR_CODE = "invalid_request";
    private static final int MAX_DECOMPRESSED_BYTES = 1_000_000;

    private final Set<JWSAlgorithm> allowedAlgorithms;
    private final Duration clockSkew;

    public StatusListJwtValidator(Set<JWSAlgorithm> allowedAlgorithms, Duration clockSkew) {
        this.allowedAlgorithms = allowedAlgorithms;
        this.clockSkew = clockSkew;
    }

    public int validateAndResolveStatus(URI expectedUri, int idx, String statusListJwt, Instant now, JWSVerifier verifier) {
        if (expectedUri == null) {
            throw new VerificationException(ERROR_CODE, "Expected status list uri is required");
        }
        if (statusListJwt == null || statusListJwt.isBlank()) {
            throw new VerificationException(ERROR_CODE, "Status list JWT is required");
        }
        if (now == null) {
            throw new VerificationException(ERROR_CODE, "Current time is required");
        }
        if (verifier == null) {
            throw new VerificationException(ERROR_CODE, "JWS verifier is required");
        }

        SignedJWT jwt = parseJwt(statusListJwt);
        validateHeader(jwt.getHeader());
        verifySignature(jwt, verifier);
        JWTClaimsSet claims = parseClaims(jwt);

        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new VerificationException(ERROR_CODE, "Status list JWT is missing required sub claim");
        }
        if (!expectedUri.toString().equals(subject)) {
            throw new VerificationException(ERROR_CODE, "Status list JWT sub does not match expected status list uri");
        }

        if (claims.getIssueTime() == null) {
            throw new VerificationException(ERROR_CODE, "Status list JWT is missing required iat claim");
        }
        Instant issuedAt = claims.getIssueTime().toInstant();
        if (issuedAt.isAfter(now.plus(clockSkew))) {
            throw new VerificationException(ERROR_CODE, "Status list JWT iat is in the future");
        }

        Instant expirationTime = null;
        if (claims.getExpirationTime() != null) {
            expirationTime = claims.getExpirationTime().toInstant();
            if (!now.minus(clockSkew).isBefore(expirationTime)) {
                throw new VerificationException(ERROR_CODE, "Status list JWT is expired");
            }
        }

        Map<String, Object> statusList;
        try {
            statusList = claims.getJSONObjectClaim("status_list");
        } catch (ParseException e) {
            throw new VerificationException(ERROR_CODE, "Status list JWT status_list claim could not be parsed");
        }
        if (statusList == null) {
            throw new VerificationException(ERROR_CODE, "Status list JWT is missing required status_list claim");
        }

        int bits = extractBits(statusList.get("bits"));
        String lst = extractLst(statusList.get("lst"));
        byte[] statuses = decodeAndDecompress(lst, idx, bits);
        return statusAt(idx, bits, statuses);
    }

    private SignedJWT parseJwt(String statusListJwt) {
        try {
            return SignedJWT.parse(statusListJwt);
        } catch (ParseException e) {
            throw new VerificationException(ERROR_CODE, "Status list JWT is not a valid compact signed JWT");
        }
    }

    private void validateHeader(JWSHeader header) {
        JOSEObjectType typ = header.getType();
        if (typ == null || !STATUS_LIST_JWT_TYP.equals(typ.toString())) {
            throw new VerificationException(ERROR_CODE, "Status list JWT typ must be statuslist+jwt");
        }

        JWSAlgorithm algorithm = header.getAlgorithm();
        if (algorithm == null || JWSAlgorithm.NONE.equals(algorithm) || !allowedAlgorithms.contains(algorithm)) {
            throw new VerificationException(ERROR_CODE, "Status list JWT algorithm is not allowed");
        }
    }

    private void verifySignature(SignedJWT jwt, JWSVerifier verifier) {
        try {
            if (!jwt.verify(verifier)) {
                throw new VerificationException(ERROR_CODE, "Status list JWT signature validation failed");
            }
        } catch (JOSEException e) {
            throw new VerificationException(ERROR_CODE, "Status list JWT signature validation failed");
        }
    }

    private JWTClaimsSet parseClaims(SignedJWT jwt) {
        try {
            return jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new VerificationException(ERROR_CODE, "Status list JWT claims could not be parsed");
        }
    }

    private int extractBits(Object bitsClaim) {
        final int bits;
        if (bitsClaim instanceof Integer intBits) {
            bits = intBits;
        } else if (bitsClaim instanceof Long longBits && longBits <= Integer.MAX_VALUE && longBits >= Integer.MIN_VALUE) {
            bits = longBits.intValue();
        } else {
            throw new VerificationException(ERROR_CODE, "status_list.bits must be an integer");
        }
        if (!ALLOWED_BITS.contains(bits)) {
            throw new VerificationException(ERROR_CODE, "status_list.bits must be one of 1, 2, 4, 8");
        }
        return bits;
    }

    private String extractLst(Object lstClaim) {
        if (!(lstClaim instanceof String lst) || lst.isBlank()) {
            throw new VerificationException(ERROR_CODE, "status_list.lst must be a non-empty base64url string");
        }
        return lst;
    }

    private byte[] decodeAndDecompress(String lst, int idx, int bits) {
        final byte[] compressed;
        try {
            compressed = Base64.getUrlDecoder().decode(lst);
        } catch (IllegalArgumentException e) {
            throw new VerificationException(ERROR_CODE, "status_list.lst is not valid base64url");
        }

        long requiredBytesLong = ((long) Math.max(idx, 0) * bits) / 8L + 1L;
        if (requiredBytesLong > MAX_DECOMPRESSED_BYTES) {
            throw new VerificationException(ERROR_CODE, "status_list.idx exceeds maximum supported range");
        }
        int requiredBytes = (int) requiredBytesLong;

        try (InflaterInputStream input = new InflaterInputStream(new ByteArrayInputStream(compressed));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            while (output.size() < requiredBytes) {
                int maxRead = Math.min(buffer.length, requiredBytes - output.size());
                int read = input.read(buffer, 0, maxRead);
                if (read < 0) {
                    break;
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } catch (IOException e) {
            throw new VerificationException(ERROR_CODE, "status_list.lst is not a valid zlib stream");
        }
    }

    private int statusAt(int idx, int bits, byte[] statuses) {
        if (idx < 0) {
            throw new VerificationException(ERROR_CODE, "status_list.idx must be non-negative");
        }

        int maxEntries = statuses.length * 8 / bits;
        if (idx >= maxEntries) {
            throw new VerificationException(ERROR_CODE, "status_list.idx is out of bounds");
        }

        int bitOffset = idx * bits;
        int byteIndex = bitOffset / 8;
        int bitShift = bitOffset % 8;
        int mask = (1 << bits) - 1;
        return (statuses[byteIndex] >> bitShift) & mask;
    }
}
