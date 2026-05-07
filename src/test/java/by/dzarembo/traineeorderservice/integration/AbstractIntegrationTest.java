package by.dzarembo.traineeorderservice.integration;

import by.dzarembo.traineeorderservice.repository.ItemRepository;
import by.dzarembo.traineeorderservice.repository.OrderItemRepository;
import by.dzarembo.traineeorderservice.repository.OrderRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractIntegrationTest {

    protected static final Long INACTIVE_USER_ID = 66L;
    protected static final WireMockServer wireMockServer = new WireMockServer(options().dynamicPort());

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected ItemRepository itemRepository;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected OrderItemRepository orderItemRepository;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        ensureWireMockStarted();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.listener.auto-startup", () -> false);
        registry.add("user-service.base-url", wireMockServer::baseUrl);
    }

    @BeforeEach
    void clearDatabase() {
        ensureWireMockStarted();
        wireMockServer.resetAll();
        orderItemRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        itemRepository.deleteAllInBatch();
    }

    private static void ensureWireMockStarted() {
        if (!wireMockServer.isRunning()) {
            wireMockServer.start();
        }
    }
}
