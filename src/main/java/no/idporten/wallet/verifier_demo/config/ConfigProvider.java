package no.idporten.wallet.verifier_demo.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@Configuration
@ConfigurationProperties(prefix = "vcverifier")
@Validated
public class ConfigProvider implements InitializingBean {

    @NotNull
    private String siop2ClientId;
    @NotNull
    private String externalBaseUrl;

    @NotEmpty
    private List<Keystore> keystores = new ArrayList<>();

    public Keystore activeKeystore() {
        return keystores.get(0);
    }

    @Data
    public static class Keystore {
        private String type;
        private String location;
        private String password;
        private String keyAlias;
        private String keyPassword;

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        int i = 1;

    }
}
