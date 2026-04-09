package by.dzarembo.traineeorderservice.repository;

import by.dzarembo.traineeorderservice.entity.OrderEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long>, JpaSpecificationExecutor<OrderEntity> {

    @EntityGraph(attributePaths = {"orderItems", "orderItems.item"})
    Optional<OrderEntity> findByIdAndDeletedFalse(Long id);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.item"})
    List<OrderEntity> findAllByUserIdAndDeletedFalseOrderByCreatedAtDesc(Long userId);
}
