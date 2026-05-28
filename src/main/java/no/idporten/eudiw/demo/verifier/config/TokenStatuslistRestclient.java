package no.idporten.eudiw.demo.verifier.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class TokenStatuslistRestclient {

    private final TokenStatuslistConfig tokenStatuslistConfig;

    private final String STATUSLISTJWT = "application/statuslist+jwt";

    public TokenStatuslistRestclient(TokenStatuslistConfig tokenStatuslistConfig) {
        this.tokenStatuslistConfig = tokenStatuslistConfig;
    }

    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(tokenStatuslistConfig.connectTimeout());
        clientHttpRequestFactory.setReadTimeout(tokenStatuslistConfig.readTimeout());
        return RestClient.builder()
                .requestFactory(clientHttpRequestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, STATUSLISTJWT)
                .build();
    }
}
