package by.dzarembo.traineeorderservice.mapper;

import by.dzarembo.traineeorderservice.dto.ItemCreateRequest;
import by.dzarembo.traineeorderservice.dto.ItemResponse;
import by.dzarembo.traineeorderservice.dto.ItemUpdateRequest;
import by.dzarembo.traineeorderservice.entity.ItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ItemEntity toEntity(ItemCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ItemEntity toEntity(ItemUpdateRequest request);

    ItemResponse toResponse(ItemEntity item);
}
