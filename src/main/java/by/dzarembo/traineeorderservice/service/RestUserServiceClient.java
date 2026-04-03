package by.dzarembo.traineeorderservice.service;

import by.dzarembo.traineeorderservice.config.UserServiceProperties;
import by.dzarembo.traineeorderservice.dto.UserInfoResponse;
import by.dzarembo.traineeorderservice.exception.UserNotFoundException;
import by.dzarembo.traineeorderservice.exception.UserServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
@AllArgsConstructor
public class RestUserServiceClient {

    private final RestClient userServiceRestClient;

    private final UserServiceProperties properties;

    @CircuitBreaker(name = "userService", fallbackMethod = "getByIdFallback")
    public UserInfoResponse getById(Long userId) {
        try {
            UserInfoResponse response = userServiceRestClient.get()
                    .uri("/users/{id}", userId)
                    .header("X-User-Id", String.valueOf(properties.authUserId()))
                    .header("X-User-Role", properties.authRole())
                    .retrieve()
                    .body(UserInfoResponse.class);

            if (response == null) {
                throw new UserServiceUnavailableException("User service returned empty response");
            }

            return response;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new UserNotFoundException("User not found with id: " + userId);
        } catch (RestClientResponseException ex) {
            throw new UserServiceUnavailableException(
                    "User service returned status: " + ex.getStatusCode().value(),
                    ex
            );
        } catch (RestClientException ex) {
            throw new UserServiceUnavailableException("User service is unavailable", ex);
        }
    }

    private UserInfoResponse getByIdFallback(Long userId, UserNotFoundException ex) {
        throw ex;
    }

    private UserInfoResponse getByIdFallback(Long userId, UserServiceUnavailableException ex) {
        throw ex;
    }

    private UserInfoResponse getByIdFallback(Long userId, CallNotPermittedException ex) {
        throw new UserServiceUnavailableException("User service circuit breaker is open", ex);
    }

    private UserInfoResponse getByIdFallback(Long userId, Exception ex) {
        throw new UserServiceUnavailableException("Unexpected error during user-service call", ex);
    }
}
