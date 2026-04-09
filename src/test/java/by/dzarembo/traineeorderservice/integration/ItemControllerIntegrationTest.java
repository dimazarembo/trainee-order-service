package by.dzarembo.traineeorderservice.integration;

import by.dzarembo.traineeorderservice.dto.ItemCreateRequest;
import by.dzarembo.traineeorderservice.dto.ItemUpdateRequest;
import by.dzarembo.traineeorderservice.entity.ItemEntity;
import by.dzarembo.traineeorderservice.entity.OrderEntity;
import by.dzarembo.traineeorderservice.entity.OrderItemEntity;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ItemControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createItem_shouldReturnCreatedItem() throws Exception {
        ItemCreateRequest request = new ItemCreateRequest("Coffee", 500L);

        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Coffee"))
                .andExpect(jsonPath("$.price").value(500));

        assertThat(itemRepository.findAll()).hasSize(1);
    }

    @Test
    void getItemById_shouldReturnItem() throws Exception {
        ItemEntity item = itemRepository.save(ItemEntity.builder()
                .name("Coffee")
                .price(500L)
                .build());

        mockMvc.perform(get("/items/{id}", item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(item.getId()))
                .andExpect(jsonPath("$.name").value("Coffee"))
                .andExpect(jsonPath("$.price").value(500));
    }

    @Test
    void getItems_shouldReturnPage() throws Exception {
        itemRepository.save(ItemEntity.builder().name("Coffee").price(500L).build());
        itemRepository.save(ItemEntity.builder().name("Milk").price(400L).build());

        mockMvc.perform(get("/items")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void updateItem_shouldReturnUpdatedItem() throws Exception {
        ItemEntity item = itemRepository.save(ItemEntity.builder()
                .name("Coffee")
                .price(500L)
                .build());
        ItemUpdateRequest request = new ItemUpdateRequest("Tea", 300L);

        mockMvc.perform(put("/items/{id}", item.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(item.getId()))
                .andExpect(jsonPath("$.name").value("Tea"))
                .andExpect(jsonPath("$.price").value(300));

        ItemEntity updatedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(updatedItem.getName()).isEqualTo("Tea");
        assertThat(updatedItem.getPrice()).isEqualTo(300L);
    }

    @Test
    void deleteItem_shouldReturnNoContent() throws Exception {
        ItemEntity item = itemRepository.save(ItemEntity.builder()
                .name("Coffee")
                .price(500L)
                .build());

        mockMvc.perform(delete("/items/{id}", item.getId()))
                .andExpect(status().isNoContent());

        assertThat(itemRepository.findById(item.getId())).isEmpty();
    }

    @Test
    void deleteItem_shouldReturnConflict_whenItemUsedInOrders() throws Exception {
        ItemEntity item = itemRepository.save(ItemEntity.builder()
                .name("Coffee")
                .price(500L)
                .build());
        OrderEntity order = orderRepository.save(OrderEntity.builder()
                .userId(1L)
                .status("CREATED")
                .totalPrice(500L)
                .deleted(false)
                .build());
        orderRepository.saveAndFlush(order);
        orderItemRepository.saveAndFlush(OrderItemEntity.builder()
                .order(order)
                .item(item)
                .quantity(1)
                .build());

        mockMvc.perform(delete("/items/{id}", item.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Item with id " + item.getId() + " is used in orders and cannot be deleted"));
    }
}
