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
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemMapper itemMapper;

    @Transactional
    public ItemResponse create(ItemCreateRequest request) {
        ItemEntity item = itemMapper.toEntity(request);
        ItemEntity savedItem = itemRepository.save(item);
        return itemMapper.toResponse(savedItem);
    }

    @Transactional(readOnly = true)
    public ItemResponse getById(Long itemId) {
        return itemMapper.toResponse(findById(itemId));
    }

    @Transactional(readOnly = true)
    public Page<ItemResponse> getAll(Pageable pageable) {
        return itemRepository.findAll(pageable).map(itemMapper::toResponse);
    }

    @Transactional
    public ItemResponse update(Long itemId, ItemUpdateRequest request) {
        ItemEntity existingItem = findById(itemId);
        ItemEntity updatedItem = itemMapper.toEntity(request);

        existingItem.setName(updatedItem.getName());
        existingItem.setPrice(updatedItem.getPrice());

        ItemEntity savedItem = itemRepository.save(existingItem);
        return itemMapper.toResponse(savedItem);
    }

    @Transactional
    public void delete(Long itemId) {
        ItemEntity existingItem = findById(itemId);

        if (orderItemRepository.existsByItemId(itemId)) {
            throw new ItemInUseException("Item with id " + itemId + " is used in orders and cannot be deleted");
        }

        itemRepository.delete(existingItem);
    }

    private ItemEntity findById(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Item with id " + itemId + " not found"));
    }
}
