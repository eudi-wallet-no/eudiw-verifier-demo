package no.idporten.wallet.verifier_demo.service;

import lombok.RequiredArgsConstructor;
import no.idporten.wallet.verifier_demo.config.ConfigProvider;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class OID4VPRequestService {

    private final ConfigProvider configProvider;

    public String getAuthorizationRequest(String type, String state) {
        return "eudi-openid4vp://"
                + configProvider.getSiop2ClientId()
                +"?client_id="+configProvider.getSiop2ClientId()
                +"&request_uri="+configProvider.getExternalBaseUrl()+"/req/" + type + "/" + state;
    }




}
