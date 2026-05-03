package by.dzarembo.traineeorderservice.event;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEvent {
    private PaymentEventType eventType;
    private String paymentId;
    private Long orderId;
    private Long userId;
    private PaymentStatus paymentStatus;
    private Instant occurredAt;
}
