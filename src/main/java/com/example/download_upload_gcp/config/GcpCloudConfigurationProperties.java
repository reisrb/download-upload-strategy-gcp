package com.example.download_upload_gcp.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Data
@Configuration
@ConfigurationProperties(prefix = "gcp")
public class GcpCloudConfigurationProperties {
    private String bucket;
    private GcpCredentials credentials;

    @Data
    public static class GcpCredentials {
        private String type;
        private String projectId;
        private String clientEmail;
        private String clientId;
        private String privateKey;
        private String privateKeyId;
        private String tokenUri;
        private String authUri;
        private String authProviderX509CertUrl;
        private String clientCertUrl;
    }

    public String credentialsJson() {
        return "{\n" +
                "  \"type\": \"" + credentials.getType() + "\",\n" +
                "  \"project_id\": \"" + credentials.getProjectId() + "\",\n" +
                "  \"private_key_id\": \"" + credentials.getPrivateKeyId() + "\",\n" +
                "  \"private_key\": \"" + credentials.getPrivateKey() + "\",\n" +
                "  \"client_email\": \"" + credentials.getClientEmail() + "\",\n" +
                "  \"client_id\": \"" + credentials.getClientId() + "\",\n" +
                "  \"auth_uri\": \"" + credentials.getAuthUri() + "\",\n" +
                "  \"token_uri\": \"" + credentials.getTokenUri() + "\",\n" +
                "  \"auth_provider_x509_cert_url\": \"" + credentials.getAuthProviderX509CertUrl() + "\",\n" +
                "  \"client_x509_cert_url\": \"" + credentials.getClientCertUrl() + "\"\n" +
                "}";
    }

    public GoogleCredentials getGoogleCredentials() throws IOException {
        try (ByteArrayInputStream credentialsStream = new ByteArrayInputStream(credentialsJson().getBytes(StandardCharsets.UTF_8))) {
            return ServiceAccountCredentials.fromStream(credentialsStream);
        }
    }
}
