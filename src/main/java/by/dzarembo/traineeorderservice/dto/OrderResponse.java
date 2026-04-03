package by.dzarembo.traineeorderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long id;
    private Long userId;
    private String status;
    private Long totalPrice;
    private boolean deleted;
    private Instant createdAt;
    private Instant updatedAt;
    private UserInfoResponse user;
    private List<OrderItemResponse> items;
}
