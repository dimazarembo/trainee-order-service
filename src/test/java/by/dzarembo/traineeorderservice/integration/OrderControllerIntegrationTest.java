package by.dzarembo.traineeorderservice.integration;

import by.dzarembo.traineeorderservice.dto.OrderCreateRequest;
import by.dzarembo.traineeorderservice.dto.OrderItemRequest;
import by.dzarembo.traineeorderservice.dto.OrderUpdateRequest;
import by.dzarembo.traineeorderservice.dto.UserInfoResponse;
import by.dzarembo.traineeorderservice.entity.ItemEntity;
import by.dzarembo.traineeorderservice.entity.OrderEntity;
import by.dzarembo.traineeorderservice.entity.OrderItemEntity;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createOrder_shouldCreateOrderAndCalculateTotalPrice() throws Exception {
        ItemEntity itemOne = saveItem("Coffee", 500L);
        ItemEntity itemTwo = saveItem("Milk", 400L);
        stubUser(7L, true);

        OrderCreateRequest request = new OrderCreateRequest(
                7L,
                "created",
                List.of(
                        new OrderItemRequest(itemOne.getId(), 2),
                        new OrderItemRequest(itemTwo.getId(), 1)
                )
        );

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalPrice").value(1400))
                .andExpect(jsonPath("$.deleted").value(false))
                .andExpect(jsonPath("$.user.id").value(7))
                .andExpect(jsonPath("$.user.email").value("user7@example.com"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].itemId").value(itemOne.getId()))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[1].itemId").value(itemTwo.getId()))
                .andExpect(jsonPath("$.items[1].quantity").value(1));
    }

    @Test
    void createOrder_shouldReturnBadRequestWhenUserIsInactive() throws Exception {
        ItemEntity item = saveItem("Coffee", 500L);
        stubUser(INACTIVE_USER_ID, false);

        OrderCreateRequest request = new OrderCreateRequest(
                INACTIVE_USER_ID,
                "created",
                List.of(new OrderItemRequest(item.getId(), 1))
        );

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User with id " + INACTIVE_USER_ID + " is inactive"));
    }

    @Test
    void getOrderById_shouldReturnOrderWithUserInfo() throws Exception {
        ItemEntity item = saveItem("Cookie", 250L);
        OrderEntity order = saveOrder(15L, "CREATED", item, 2);
        stubUser(15L, true);

        mockMvc.perform(get("/orders/{id}", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()))
                .andExpect(jsonPath("$.userId").value(15))
                .andExpect(jsonPath("$.user.id").value(15))
                .andExpect(jsonPath("$.user.email").value("user15@example.com"))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void getOrderById_shouldReturnServiceUnavailableWhenUserServiceFails() throws Exception {
        ItemEntity item = saveItem("Cookie", 250L);
        OrderEntity order = saveOrder(17L, "CREATED", item, 2);

        wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/users/17"))
                .withHeader("X-User-Id", equalTo("0"))
                .withHeader("X-User-Role", equalTo("ADMIN"))
                .willReturn(aResponse().withStatus(500)));

        mockMvc.perform(get("/orders/{id}", order.getId()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("User service returned status: 500"));
    }

    @Test
    void getOrders_shouldFilterByCreatedAtRangeAndStatuses() throws Exception {
        ItemEntity item = saveItem("Tea", 300L);

        OrderEntity orderOne = saveOrder(1L, "CREATED", item, 1);
        pauseBetweenSaves();
        OrderEntity orderTwo = saveOrder(2L, "PAID", item, 2);
        pauseBetweenSaves();
        OrderEntity orderThree = saveOrder(3L, "CANCELLED", item, 3);
        stubUser(2L, true);

        Instant createdFrom = midpoint(orderOne.getCreatedAt(), orderTwo.getCreatedAt());
        Instant createdTo = midpoint(orderTwo.getCreatedAt(), orderThree.getCreatedAt());

        mockMvc.perform(get("/orders")
                        .param("createdFrom", createdFrom.toString())
                        .param("createdTo", createdTo.toString())
                        .param("statuses", "CREATED", "PAID")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(orderTwo.getId()))
                .andExpect(jsonPath("$.content[0].status").value("PAID"))
                .andExpect(jsonPath("$.content[0].user.id").value(2))
                .andExpect(jsonPath("$.content[0].user.email").value("user2@example.com"));
    }

    @Test
    void getOrdersByUserId_shouldReturnOnlyActiveOrdersForUser() throws Exception {
        ItemEntity item = saveItem("Cake", 900L);

        OrderEntity activeOrder = saveOrder(42L, "CREATED", item, 1);
        OrderEntity deletedOrder = saveOrder(42L, "PAID", item, 2);
        deletedOrder.setDeleted(true);
        orderRepository.save(deletedOrder);
        saveOrder(99L, "CREATED", item, 1);
        stubUser(42L, true);

        mockMvc.perform(get("/orders/user/{userId}", 42L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(activeOrder.getId()))
                .andExpect(jsonPath("$[0].userId").value(42))
                .andExpect(jsonPath("$[0].user.id").value(42))
                .andExpect(jsonPath("$[0].user.email").value("user42@example.com"));
    }

    @Test
    void updateOrder_shouldReplaceItemsAndRecalculateTotalPrice() throws Exception {
        ItemEntity oldItem = saveItem("Burger", 800L);
        ItemEntity newItem = saveItem("Salad", 250L);
        OrderEntity order = saveOrder(5L, "CREATED", oldItem, 1);
        stubUser(5L, true);

        OrderUpdateRequest request = new OrderUpdateRequest(
                5L,
                "paid",
                List.of(new OrderItemRequest(newItem.getId(), 3))
        );

        mockMvc.perform(put("/orders/{id}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.totalPrice").value(750))
                .andExpect(jsonPath("$.user.id").value(5))
                .andExpect(jsonPath("$.user.email").value("user5@example.com"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].itemId").value(newItem.getId()))
                .andExpect(jsonPath("$.items[0].quantity").value(3));

        OrderEntity updatedOrder = orderRepository.findByIdAndDeletedFalse(order.getId()).orElseThrow();
        assertThat(updatedOrder.getOrderItems()).hasSize(1);
        assertThat(updatedOrder.getOrderItems().get(0).getItem().getId()).isEqualTo(newItem.getId());
    }

    @Test
    void updateOrder_shouldReturnBadRequestWhenUserIsInactive() throws Exception {
        ItemEntity oldItem = saveItem("Burger", 800L);
        ItemEntity newItem = saveItem("Salad", 250L);
        OrderEntity order = saveOrder(5L, "CREATED", oldItem, 1);
        stubUser(INACTIVE_USER_ID, false);

        OrderUpdateRequest request = new OrderUpdateRequest(
                INACTIVE_USER_ID,
                "paid",
                List.of(new OrderItemRequest(newItem.getId(), 1))
        );

        mockMvc.perform(put("/orders/{id}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User with id " + INACTIVE_USER_ID + " is inactive"));
    }

    @Test
    void deleteOrder_shouldSoftDeleteOrder() throws Exception {
        ItemEntity item = saveItem("Pizza", 1200L);
        OrderEntity order = saveOrder(8L, "CREATED", item, 1);

        mockMvc.perform(delete("/orders/{id}", order.getId()))
                .andExpect(status().isNoContent());

        OrderEntity deletedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(deletedOrder.isDeleted()).isTrue();

        mockMvc.perform(get("/orders/{id}", order.getId()))
                .andExpect(status().isNotFound());
    }

    private ItemEntity saveItem(String name, Long price) {
        return itemRepository.save(ItemEntity.builder()
                .name(name)
                .price(price)
                .build());
    }

    private OrderEntity saveOrder(Long userId, String status, ItemEntity item, int quantity) {
        OrderEntity order = OrderEntity.builder()
                .userId(userId)
                .status(status)
                .totalPrice(item.getPrice() * quantity)
                .deleted(false)
                .build();

        order.addOrderItem(OrderItemEntity.builder()
                .item(item)
                .quantity(quantity)
                .build());

        return orderRepository.saveAndFlush(order);
    }

    private Instant midpoint(Instant left, Instant right) {
        return Instant.ofEpochMilli((left.toEpochMilli() + right.toEpochMilli()) / 2);
    }

    private void pauseBetweenSaves() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while preparing test data", ex);
        }
    }

    private void stubUser(Long userId, boolean active) throws Exception {
        UserInfoResponse response = UserInfoResponse.builder()
                .id(userId)
                .name("User" + userId)
                .surname("Test")
                .email("user" + userId + "@example.com")
                .active(active)
                .build();

        wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/users/" + userId))
                .withHeader("X-User-Id", equalTo("0"))
                .withHeader("X-User-Role", equalTo("ADMIN"))
                .willReturn(okJson(objectMapper.writeValueAsString(response))));
    }
}
