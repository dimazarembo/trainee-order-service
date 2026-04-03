package by.dzarembo.traineeorderservice.specification;

import by.dzarembo.traineeorderservice.entity.OrderEntity;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

public final class OrderSpecification {

    private OrderSpecification() {
    }

    public static Specification<OrderEntity> notDeleted() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isFalse(root.get("deleted"));
    }

    public static Specification<OrderEntity> createdAtFrom(Instant createdFrom) {
        return (root, query, criteriaBuilder) -> {
            if (createdFrom == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
        };
    }

    public static Specification<OrderEntity> createdAtTo(Instant createdTo) {
        return (root, query, criteriaBuilder) -> {
            if (createdTo == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdTo);
        };
    }

    public static Specification<OrderEntity> hasStatuses(List<String> statuses) {
        return (root, query, criteriaBuilder) -> {
            if (statuses == null || statuses.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            List<String> normalizedStatuses = statuses.stream()
                    .filter(status -> status != null && !status.isBlank())
                    .map(status -> status.trim().toUpperCase())
                    .toList();

            if (normalizedStatuses.isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.upper(root.get("status")).in(normalizedStatuses);
        };
    }
}
