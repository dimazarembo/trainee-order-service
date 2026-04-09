package by.dzarembo.traineeorderservice.service;

import by.dzarembo.traineeorderservice.dto.ItemCreateRequest;
import by.dzarembo.traineeorderservice.dto.ItemResponse;
import by.dzarembo.traineeorderservice.dto.ItemUpdateRequest;
import by.dzarembo.traineeorderservice.entity.ItemEntity;
import by.dzarembo.traineeorderservice.exception.ItemInUseException;
import by.dzarembo.traineeorderservice.exception.ItemNotFoundException;
import by.dzarembo.traineeorderservice.mapper.ItemMapper;
import by.dzarembo.traineeorderservice.repository.ItemRepository;
import by.dzarembo.traineeorderservice.repository.OrderItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemService itemService;

    @Test
    void create_shouldReturnCreatedItem_whenRequestValid() {
        ItemCreateRequest request = new ItemCreateRequest("Coffee", 500L);
        ItemEntity item = buildItem(1L, "Coffee", 500L);
        ItemResponse response = buildResponse(1L, "Coffee", 500L);

        when(itemMapper.toEntity(request)).thenReturn(item);
        when(itemRepository.save(item)).thenReturn(item);
        when(itemMapper.toResponse(item)).thenReturn(response);

        ItemResponse result = itemService.create(request);

        assertThat(result).isEqualTo(response);
        verify(itemRepository).save(item);
    }

    @Test
    void getById_shouldReturnItem_whenItemExists() {
        ItemEntity item = buildItem(1L, "Coffee", 500L);
        ItemResponse response = buildResponse(1L, "Coffee", 500L);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemMapper.toResponse(item)).thenReturn(response);

        ItemResponse result = itemService.getById(1L);

        assertThat(result).isEqualTo(response);
        verify(itemRepository).findById(1L);
    }

    @Test
    void getById_shouldThrowException_whenItemDoesNotExist() {
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.getById(1L))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessage("Item with id 1 not found");
    }

    @Test
    void getAll_shouldReturnItemsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        ItemEntity firstItem = buildItem(1L, "Coffee", 500L);
        ItemEntity secondItem = buildItem(2L, "Milk", 400L);
        Page<ItemEntity> items = new PageImpl<>(List.of(firstItem, secondItem), pageable, 2);
        ItemResponse firstResponse = buildResponse(1L, "Coffee", 500L);
        ItemResponse secondResponse = buildResponse(2L, "Milk", 400L);

        when(itemRepository.findAll(pageable)).thenReturn(items);
        when(itemMapper.toResponse(firstItem)).thenReturn(firstResponse);
        when(itemMapper.toResponse(secondItem)).thenReturn(secondResponse);

        Page<ItemResponse> result = itemService.getAll(pageable);

        assertThat(result.getContent()).containsExactly(firstResponse, secondResponse);
        verify(itemRepository).findAll(pageable);
    }

    @Test
    void update_shouldReturnUpdatedItem_whenItemExists() {
        ItemUpdateRequest request = new ItemUpdateRequest("Tea", 300L);
        ItemEntity existingItem = buildItem(1L, "Coffee", 500L);
        ItemEntity updatedItem = buildItem(null, "Tea", 300L);
        ItemResponse response = buildResponse(1L, "Tea", 300L);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(existingItem));
        when(itemMapper.toEntity(request)).thenReturn(updatedItem);
        when(itemRepository.save(existingItem)).thenReturn(existingItem);
        when(itemMapper.toResponse(existingItem)).thenReturn(response);

        ItemResponse result = itemService.update(1L, request);

        assertThat(result).isEqualTo(response);
        assertThat(existingItem.getName()).isEqualTo("Tea");
        assertThat(existingItem.getPrice()).isEqualTo(300L);
        verify(itemRepository).save(existingItem);
    }

    @Test
    void delete_shouldRemoveItem_whenItemNotUsedInOrders() {
        ItemEntity item = buildItem(1L, "Coffee", 500L);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(orderItemRepository.existsByItemId(1L)).thenReturn(false);

        itemService.delete(1L);

        verify(orderItemRepository).existsByItemId(1L);
        verify(itemRepository).delete(item);
    }

    @Test
    void delete_shouldThrowException_whenItemUsedInOrders() {
        ItemEntity item = buildItem(1L, "Coffee", 500L);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(orderItemRepository.existsByItemId(1L)).thenReturn(true);

        assertThatThrownBy(() -> itemService.delete(1L))
                .isInstanceOf(ItemInUseException.class)
                .hasMessage("Item with id 1 is used in orders and cannot be deleted");

        verify(itemRepository, never()).delete(item);
    }

    private ItemEntity buildItem(Long id, String name, Long price) {
        return ItemEntity.builder()
                .id(id)
                .name(name)
                .price(price)
                .build();
    }

    private ItemResponse buildResponse(Long id, String name, Long price) {
        return ItemResponse.builder()
                .id(id)
                .name(name)
                .price(price)
                .build();
    }
}
