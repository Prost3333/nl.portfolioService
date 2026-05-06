package nlgrandtaskmanager.financesevice.dto;

import nlgrandtaskmanager.financesevice.model.Transaction;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
    TransactionMapper INSTANCE = Mappers.getMapper(TransactionMapper.class);

    TransactionResponse toResponse(Transaction transaction);

    Transaction toEntity(CreateTransactionRequest transactionResponse);
    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(
            @MappingTarget Transaction entity,
            CreateTransactionRequest request
    );
}
