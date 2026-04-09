package by.dzarembo.traineeorderservice.mapper;

import by.dzarembo.traineeorderservice.dto.OrderCreateRequest;
import by.dzarembo.traineeorderservice.dto.OrderItemRequest;
import by.dzarembo.traineeorderservice.dto.OrderItemResponse;
import by.dzarembo.traineeorderservice.dto.OrderResponse;
import by.dzarembo.traineeorderservice.dto.OrderUpdateRequest;
import by.dzarembo.traineeorderservice.dto.UserInfoResponse;
import by.dzarembo.traineeorderservice.entity.OrderEntity;
import by.dzarembo.traineeorderservice.entity.OrderItemEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "orderItems", source = "items")
    OrderEntity toEntity(OrderCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "orderItems", source = "items")
    OrderEntity toEntity(OrderUpdateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "item.id", source = "itemId")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    OrderItemEntity toEntity(OrderItemRequest request);

    @Mapping(target = "id", source = "order.id")
    @Mapping(target = "userId", source = "order.userId")
    @Mapping(target = "status", source = "order.status")
    @Mapping(target = "totalPrice", source = "order.totalPrice")
    @Mapping(target = "deleted", source = "order.deleted")
    @Mapping(target = "createdAt", source = "order.createdAt")
    @Mapping(target = "updatedAt", source = "order.updatedAt")
    @Mapping(target = "user", source = "user")
    @Mapping(target = "items", source = "order.orderItems")
    OrderResponse toResponse(OrderEntity order, UserInfoResponse user);

    @Mapping(target = "itemId", source = "item.id")
    @Mapping(target = "itemName", source = "item.name")
    @Mapping(target = "itemPrice", source = "item.price")
    OrderItemResponse toItemResponse(OrderItemEntity orderItem);

    @AfterMapping
    default void linkOrderItems(@MappingTarget OrderEntity order) {
        if (order.getOrderItems() == null) {
            return;
        }

        for (OrderItemEntity orderItem : order.getOrderItems()) {
            orderItem.setOrder(order);
        }
    }
}
