package by.dzarembo.traineeorderservice.consumer;

import by.dzarembo.traineeorderservice.event.PaymentEvent;
import by.dzarembo.traineeorderservice.event.PaymentEventType;
import by.dzarembo.traineeorderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaPaymentEventConsumer {
    private final OrderService orderService;

    @KafkaListener(topics = "payment-events", groupId = "order-service")
    public void consume(PaymentEvent event) {
        if (event.getEventType() != PaymentEventType.CREATE_PAYMENT) {
            return;
        }

        orderService.handlePaymentEvent(event);
    }
}
