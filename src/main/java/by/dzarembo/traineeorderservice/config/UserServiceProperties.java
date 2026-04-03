package by.dzarembo.traineeorderservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "user-service")
public record UserServiceProperties(
        String baseUrl,
        Long authUserId,
        String authRole
) {
}
