package no.idporten.eudiw.demo.verifier.tsl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;


import java.net.URI;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TokenStatusListServiceTest {

private TokenStatuslistService service;
private MockRestServiceServer mockServer;
private static final String STATUSLIST = "https://status.eidas2sandkasse.dev/lists/1";

@BeforeEach
void setup() {
    RestClient.Builder builder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(builder).build();
    RestClient restClient = builder.build();
    service = new TokenStatuslistService(restClient);
}

@Test
void testRestClientCall() {
    final String vpToken = "eyJhbGciOiJFUzI1NiIsImtpZCI6IjEyIiwidHlwIjoic3RhdHVzbGlzdCtqd3QifQ.eyJleHAiOjIyOTE3MjAxNzAsImlhdCI6MTY4NjkyMDE3MCwiaXNzIjoiaHR0cHM6Ly9leGFtcGxlLmNvbSIsInN0YXR1c19saXN0Ijp7ImJpdHMiOjEsImxzdCI6ImVOcmJ1UmdBQWhjQlhRIn0sInN1YiI6Imh0dHBzOi8vZXhhbXBsZS5jb20vc3RhdHVzbGlzdHMvMSIsInR0bCI6NDMyMDB9.2lKUUNG503R9htu4aHAYi7vjmr3sgApbfoDvPrl65N3URUO1EYqqQl45Jfzd-Av4QzlKa3oVALpLwOEUOq-U_g";
    mockServer.expect(requestTo(STATUSLIST))
            .andRespond(withSuccess(vpToken, MediaType.parseMediaType("application/statuslist+jwt")));

    service.requestStatusList(URI.create(STATUSLIST));

    mockServer.verify();
}
}