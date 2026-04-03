package by.dzarembo.traineeorderservice.repository;

import by.dzarembo.traineeorderservice.entity.ItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<ItemEntity, Long> {
}
