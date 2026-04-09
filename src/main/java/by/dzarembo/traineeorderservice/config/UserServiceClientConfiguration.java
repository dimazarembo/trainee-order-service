package by.dzarembo.traineeorderservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(UserServiceProperties.class)
public class UserServiceClientConfiguration {

    @Bean
    RestClient userServiceRestClient(UserServiceProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }
}
