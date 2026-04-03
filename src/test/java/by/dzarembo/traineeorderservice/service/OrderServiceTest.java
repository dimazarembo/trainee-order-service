package by.dzarembo.traineeorderservice.service;

import by.dzarembo.traineeorderservice.dto.OrderCreateRequest;
import by.dzarembo.traineeorderservice.dto.OrderItemRequest;
import by.dzarembo.traineeorderservice.dto.OrderResponse;
import by.dzarembo.traineeorderservice.dto.OrderUpdateRequest;
import by.dzarembo.traineeorderservice.dto.UserInfoResponse;
import by.dzarembo.traineeorderservice.entity.ItemEntity;
import by.dzarembo.traineeorderservice.entity.OrderEntity;
import by.dzarembo.traineeorderservice.entity.OrderItemEntity;
import by.dzarembo.traineeorderservice.exception.OrderNotFoundException;
import by.dzarembo.traineeorderservice.mapper.OrderMapper;
import by.dzarembo.traineeorderservice.repository.ItemRepository;
import by.dzarembo.traineeorderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private RestUserServiceClient userServiceClient;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    @Test
    void create_shouldReturnCreatedOrder_whenRequestValid() {
        OrderCreateRequest request = new OrderCreateRequest(
                7L,
                "created",
                List.of(
                        new OrderItemRequest(10L, 2),
                        new OrderItemRequest(11L, 1)
                )
        );
        UserInfoResponse user = buildUser(7L, true);
        ItemEntity firstItem = buildItem(10L, "Coffee", 500L);
        ItemEntity secondItem = buildItem(11L, "Milk", 400L);
        OrderEntity mappedOrder = buildOrderDraft(7L, "created", 10L, 2, 11L, 1);
        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .userId(7L)
                .status("CREATED")
                .totalPrice(1400L)
                .deleted(false)
                .user(user)
                .build();

        when(userServiceClient.getById(7L)).thenReturn(user);
        when(orderMapper.toEntity(request)).thenReturn(mappedOrder);
        when(itemRepository.findAllById(any())).thenReturn(List.of(firstItem, secondItem));
        when(orderRepository.save(mappedOrder)).thenReturn(mappedOrder);
        when(orderMapper.toResponse(mappedOrder, user)).thenReturn(response);

        OrderResponse result = orderService.create(request);

        assertThat(result).isEqualTo(response);
        assertThat(mappedOrder.getStatus()).isEqualTo("CREATED");
        assertThat(mappedOrder.isDeleted()).isFalse();
        assertThat(mappedOrder.getTotalPrice()).isEqualTo(1400L);
        assertThat(mappedOrder.getOrderItems()).hasSize(2);
        assertThat(mappedOrder.getOrderItems().get(0).getItem()).isEqualTo(firstItem);
        assertThat(mappedOrder.getOrderItems().get(1).getItem()).isEqualTo(secondItem);
        assertThat(mappedOrder.getOrderItems())
                .allSatisfy(orderItem -> assertThat(orderItem.getOrder()).isSameAs(mappedOrder));

        verify(userServiceClient).getById(7L);
        verify(itemRepository).findAllById(any());
        verify(orderRepository).save(mappedOrder);
        verify(orderMapper).toResponse(mappedOrder, user);
    }

    @Test
    void create_shouldThrowException_whenUserInactive() {
        OrderCreateRequest request = new OrderCreateRequest(
                7L,
                "created",
                List.of(new OrderItemRequest(10L, 1))
        );

        when(userServiceClient.getById(7L)).thenReturn(buildUser(7L, false));

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User with id 7 is inactive");

        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void getById_shouldReturnOrder_whenOrderExists() {
        Long orderId = 5L;
        OrderEntity order = OrderEntity.builder()
                .id(orderId)
                .userId(15L)
                .status("CREATED")
                .totalPrice(500L)
                .deleted(false)
                .build();
        UserInfoResponse user = buildUser(15L, true);
        OrderResponse response = OrderResponse.builder()
                .id(orderId)
                .userId(15L)
                .status("CREATED")
                .user(user)
                .build();

        when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
        when(userServiceClient.getById(15L)).thenReturn(user);
        when(orderMapper.toResponse(order, user)).thenReturn(response);

        OrderResponse result = orderService.getById(orderId);

        assertThat(result).isEqualTo(response);
        verify(orderRepository).findByIdAndDeletedFalse(orderId);
        verify(userServiceClient).getById(15L);
        verify(orderMapper).toResponse(order, user);
    }

    @Test
    void getById_shouldThrowException_whenOrderDoesNotExist() {
        when(orderRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getById(5L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("Order with id 5 not found");
    }

    @Test
    void getAll_shouldReturnMappedPageAndLoadUsersOncePerDistinctUserId() {
        Pageable pageable = PageRequest.of(0, 10);
        OrderEntity firstOrder = OrderEntity.builder()
                .id(1L)
                .userId(9L)
                .status("CREATED")
                .totalPrice(500L)
                .deleted(false)
                .build();
        OrderEntity secondOrder = OrderEntity.builder()
                .id(2L)
                .userId(9L)
                .status("PAID")
                .totalPrice(800L)
                .deleted(false)
                .build();
        Page<OrderEntity> orders = new PageImpl<>(List.of(firstOrder, secondOrder), pageable, 2);
        UserInfoResponse user = buildUser(9L, true);
        OrderResponse firstResponse = OrderResponse.builder().id(1L).userId(9L).user(user).build();
        OrderResponse secondResponse = OrderResponse.builder().id(2L).userId(9L).user(user).build();

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(orders);
        when(userServiceClient.getById(9L)).thenReturn(user);
        when(orderMapper.toResponse(firstOrder, user)).thenReturn(firstResponse);
        when(orderMapper.toResponse(secondOrder, user)).thenReturn(secondResponse);

        Page<OrderResponse> result = orderService.getAll(null, null, List.of("CREATED", "PAID"), pageable);

        assertThat(result.getContent()).containsExactly(firstResponse, secondResponse);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(orderRepository).findAll(any(Specification.class), eq(pageable));
        verify(userServiceClient).getById(9L);
    }

    @Test
    void getAll_shouldThrowException_whenDateRangeInvalid() {
        Instant createdFrom = Instant.parse("2026-03-20T00:00:00Z");
        Instant createdTo = Instant.parse("2026-03-10T00:00:00Z");

        assertThatThrownBy(() -> orderService.getAll(createdFrom, createdTo, List.of("CREATED"), PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("createdFrom must be before or equal to createdTo");
    }

    @Test
    void getAllByUserId_shouldReturnEmptyList_whenOrdersDoNotExist() {
        when(orderRepository.findAllByUserIdAndDeletedFalseOrderByCreatedAtDesc(42L)).thenReturn(List.of());

        List<OrderResponse> result = orderService.getAllByUserId(42L);

        assertThat(result).isEmpty();
        verify(orderRepository).findAllByUserIdAndDeletedFalseOrderByCreatedAtDesc(42L);
        verify(userServiceClient, never()).getById(any(Long.class));
    }

    @Test
    void update_shouldReplaceItemsAndRecalculateTotalPrice() {
        Long orderId = 12L;
        OrderUpdateRequest request = new OrderUpdateRequest(
                5L,
                "paid",
                List.of(new OrderItemRequest(20L, 3))
        );
        UserInfoResponse user = buildUser(5L, true);
        ItemEntity oldItem = buildItem(10L, "Burger", 800L);
        ItemEntity newItem = buildItem(20L, "Salad", 250L);
        OrderEntity existingOrder = OrderEntity.builder()
                .id(orderId)
                .userId(5L)
                .status("CREATED")
                .totalPrice(800L)
                .deleted(false)
                .build();
        existingOrder.addOrderItem(OrderItemEntity.builder()
                .item(oldItem)
                .quantity(1)
                .build());

        OrderEntity updatedOrder = buildOrderDraft(5L, "paid", 20L, 3);
        OrderResponse response = OrderResponse.builder()
                .id(orderId)
                .userId(5L)
                .status("PAID")
                .totalPrice(750L)
                .user(user)
                .build();

        when(userServiceClient.getById(5L)).thenReturn(user);
        when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(existingOrder));
        when(orderMapper.toEntity(request)).thenReturn(updatedOrder);
        when(itemRepository.findAllById(any())).thenReturn(List.of(newItem));
        when(orderRepository.save(existingOrder)).thenReturn(existingOrder);
        when(orderMapper.toResponse(existingOrder, user)).thenReturn(response);

        OrderResponse result = orderService.update(orderId, request);

        assertThat(result).isEqualTo(response);
        assertThat(existingOrder.getStatus()).isEqualTo("PAID");
        assertThat(existingOrder.getUserId()).isEqualTo(5L);
        assertThat(existingOrder.getTotalPrice()).isEqualTo(750L);
        assertThat(existingOrder.getOrderItems()).hasSize(1);
        assertThat(existingOrder.getOrderItems().get(0).getItem()).isEqualTo(newItem);
        assertThat(existingOrder.getOrderItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(existingOrder.getOrderItems().get(0).getOrder()).isSameAs(existingOrder);

        verify(orderRepository).findByIdAndDeletedFalse(orderId);
        verify(orderRepository).save(existingOrder);
    }

    @Test
    void delete_shouldSoftDeleteOrder_whenOrderExists() {
        Long orderId = 1L;
        OrderEntity order = OrderEntity.builder()
                .id(orderId)
                .userId(7L)
                .status("CREATED")
                .totalPrice(500L)
                .deleted(false)
                .build();

        when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.delete(orderId);

        assertThat(order.isDeleted()).isTrue();
        verify(orderRepository).findByIdAndDeletedFalse(orderId);
        verify(orderRepository).save(order);
    }

    private UserInfoResponse buildUser(Long userId, boolean active) {
        return UserInfoResponse.builder()
                .id(userId)
                .name("User" + userId)
                .surname("Test")
                .email("user" + userId + "@example.com")
                .active(active)
                .build();
    }

    private ItemEntity buildItem(Long id, String name, Long price) {
        return ItemEntity.builder()
                .id(id)
                .name(name)
                .price(price)
                .build();
    }

    private OrderEntity buildOrderDraft(Long userId, String status, long itemId, int quantity) {
        return buildOrderDraft(userId, status, new long[]{itemId}, new int[]{quantity});
    }

    private OrderEntity buildOrderDraft(Long userId, String status, long firstItemId, int firstQuantity, long secondItemId, int secondQuantity) {
        return buildOrderDraft(
                userId,
                status,
                new long[]{firstItemId, secondItemId},
                new int[]{firstQuantity, secondQuantity}
        );
    }

    private OrderEntity buildOrderDraft(Long userId, String status, long[] itemIds, int[] quantities) {
        OrderEntity order = OrderEntity.builder()
                .userId(userId)
                .status(status)
                .build();

        for (int i = 0; i < itemIds.length; i++) {
            order.addOrderItem(OrderItemEntity.builder()
                    .item(ItemEntity.builder().id(itemIds[i]).build())
                    .quantity(quantities[i])
                    .build());
        }

        return order;
    }
}
