package br.com.realizecfi.orbiapixdatagenerator.repository.mongo.model;

import br.com.realizecfi.orbiapixdatagenerator.domain.type.EntryType;
import br.com.realizecfi.orbiapixdatagenerator.domain.type.EventType;
import br.com.realizecfi.orbiapixdatagenerator.domain.type.TransactionType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Document(collection = "DictTransactions")
public class MongoDictTransactionModel {

    @MongoId
    @Field(targetType = FieldType.STRING)
    private String id;
    @Field(targetType = FieldType.STRING)
    private UUID transactionId;
    @Field(targetType = FieldType.STRING)
    private UUID eventId;
    private EventType eventType;
    private Long transactionExternalId;
    private TransactionType transactionType;
    private LocalDateTime transactionDate;
    private EntryType entryType;
}
