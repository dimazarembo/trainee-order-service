package by.dzarembo.traineeorderservice.service;

import by.dzarembo.traineeorderservice.dto.OrderCreateRequest;
import by.dzarembo.traineeorderservice.dto.OrderResponse;
import by.dzarembo.traineeorderservice.dto.OrderUpdateRequest;
import by.dzarembo.traineeorderservice.dto.UserInfoResponse;
import by.dzarembo.traineeorderservice.entity.ItemEntity;
import by.dzarembo.traineeorderservice.entity.OrderEntity;
import by.dzarembo.traineeorderservice.entity.OrderItemEntity;
import by.dzarembo.traineeorderservice.exception.ItemNotFoundException;
import by.dzarembo.traineeorderservice.exception.OrderNotFoundException;
import by.dzarembo.traineeorderservice.mapper.OrderMapper;
import by.dzarembo.traineeorderservice.repository.ItemRepository;
import by.dzarembo.traineeorderservice.repository.OrderRepository;
import by.dzarembo.traineeorderservice.specification.OrderSpecification;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final RestUserServiceClient userServiceClient;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderResponse create(OrderCreateRequest request) {
        UserInfoResponse user = userServiceClient.getById(request.getUserId());
        validateUserIsActive(user, request.getUserId());

        OrderEntity order = orderMapper.toEntity(request);
        prepareNewOrder(order);

        OrderEntity savedOrder = orderRepository.save(order);
        return orderMapper.toResponse(savedOrder, user);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        OrderEntity order = findActiveOrderById(id);
        return orderMapper.toResponse(order, userServiceClient.getById(order.getUserId()));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAll(Instant createdFrom, Instant createdTo, List<String> statuses, Pageable pageable) {
        validateDateRange(createdFrom, createdTo);

        Specification<OrderEntity> specification = OrderSpecification.notDeleted()
                .and(OrderSpecification.createdAtFrom(createdFrom))
                .and(OrderSpecification.createdAtTo(createdTo))
                .and(OrderSpecification.hasStatuses(statuses));

        Page<OrderEntity> orders = orderRepository.findAll(specification, pageable);
        Map<Long, UserInfoResponse> usersById = loadUsersById(orders.getContent());

        List<OrderResponse> content = orders.getContent().stream()
                .map(order -> orderMapper.toResponse(order, usersById.get(order.getUserId())))
                .toList();

        return new PageImpl<>(content, pageable, orders.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllByUserId(Long userId) {
        List<OrderEntity> orders = orderRepository.findAllByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId);
        if (orders.isEmpty()) {
            return List.of();
        }

        UserInfoResponse user = userServiceClient.getById(userId);
        return orders.stream()
                .map(order -> orderMapper.toResponse(order, user))
                .toList();
    }

    @Transactional
    public OrderResponse update(Long orderId, OrderUpdateRequest request) {
        UserInfoResponse user = userServiceClient.getById(request.getUserId());
        validateUserIsActive(user, request.getUserId());

        OrderEntity existingOrder = findActiveOrderById(orderId);
        OrderEntity updatedOrder = orderMapper.toEntity(request);
        applyUpdates(existingOrder, updatedOrder);

        OrderEntity savedOrder = orderRepository.save(existingOrder);
        return orderMapper.toResponse(savedOrder, user);
    }

    @Transactional
    public void delete(Long orderId) {
        OrderEntity existingOrder = findActiveOrderById(orderId);
        existingOrder.setDeleted(true);
        orderRepository.save(existingOrder);
    }

    private void prepareNewOrder(OrderEntity order) {
        resolveOrderItems(order.getOrderItems());
        order.setStatus(normalizeStatus(order.getStatus()));
        order.setDeleted(false);
        order.setTotalPrice(calculateTotalPrice(order.getOrderItems()));
    }

    private void applyUpdates(OrderEntity existingOrder, OrderEntity updatedOrder) {
        resolveOrderItems(updatedOrder.getOrderItems());

        existingOrder.setUserId(updatedOrder.getUserId());
        existingOrder.setStatus(normalizeStatus(updatedOrder.getStatus()));
        existingOrder.getOrderItems().clear();
        updatedOrder.getOrderItems().forEach(existingOrder::addOrderItem);
        existingOrder.setTotalPrice(calculateTotalPrice(existingOrder.getOrderItems()));
    }

    private void resolveOrderItems(List<OrderItemEntity> orderItems) {
        Map<Long, ItemEntity> itemsById = loadItemsById(orderItems);

        for (OrderItemEntity orderItem : orderItems) {
            orderItem.setItem(itemsById.get(orderItem.getItem().getId()));
        }
    }

    private Map<Long, UserInfoResponse> loadUsersById(List<OrderEntity> orders) {
        return orders.stream()
                .map(OrderEntity::getUserId)
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        userServiceClient::getById
                ));
    }

    private OrderEntity findActiveOrderById(Long orderId) {
        return orderRepository.findByIdAndDeletedFalse(orderId)
                .orElseThrow(() -> new OrderNotFoundException(String.format("Order with id %d not found", orderId)));
    }

    private void validateDateRange(Instant createdFrom, Instant createdTo) {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new IllegalArgumentException("createdFrom must be before or equal to createdTo");
        }
    }

    private String normalizeStatus(String status) {
        return status.trim().toUpperCase();
    }

    private void validateUserIsActive(UserInfoResponse user, Long userId) {
        if (!user.isActive()) {
            throw new IllegalArgumentException("User with id " + userId + " is inactive");
        }
    }

    private Map<Long, ItemEntity> loadItemsById(List<OrderItemEntity> orderItems) {
        Set<Long> itemIds = orderItems.stream()
                .map(orderItem -> orderItem.getItem().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, ItemEntity> itemsById = itemRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(ItemEntity::getId, Function.identity()));

        if (itemsById.size() != itemIds.size()) {
            Set<Long> missingItemIds = new LinkedHashSet<>(itemIds);
            missingItemIds.removeAll(itemsById.keySet());
            throw new ItemNotFoundException("Items not found with ids: " + missingItemIds);
        }

        return itemsById;
    }

    private long calculateTotalPrice(List<OrderItemEntity> orderItems) {
        try {
            long totalPrice = 0L;
            for (OrderItemEntity orderItem : orderItems) {
                long positionTotal = Math.multiplyExact(orderItem.getItem().getPrice(), orderItem.getQuantity().longValue());
                totalPrice = Math.addExact(totalPrice, positionTotal);
            }
            return totalPrice;
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Order total exceeds supported range");
        }
    }
}
