package by.dzarembo.traineeorderservice.repository;

import by.dzarembo.traineeorderservice.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {

    boolean existsByItemId(Long itemId);
}
