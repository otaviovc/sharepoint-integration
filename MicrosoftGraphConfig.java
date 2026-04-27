package com.example.sharepoint.config;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientCertificateCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GraphProperties.class)
public class MicrosoftGraphConfig {

    @Bean
    public TokenCredential tokenCredential(GraphProperties properties) {
        return new ClientCertificateCredentialBuilder()
                .tenantId(properties.getTenantId())
                .clientId(properties.getClientId())
                .pfxCertificate(properties.getCertificatePath()) // .p12 costuma funcionar aqui
                .clientCertificatePassword(properties.getCertificatePassword())
                .build();
    }

    @Bean
    public GraphServiceClient graphServiceClient(TokenCredential tokenCredential) {
        String[] scopes = new String[] { "https://graph.microsoft.com/.default" };
        return new GraphServiceClient(tokenCredential, scopes);
    }

    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }
}