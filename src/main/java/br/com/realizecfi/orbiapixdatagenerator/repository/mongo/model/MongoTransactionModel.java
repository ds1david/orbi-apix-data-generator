package br.com.realizecfi.orbiapixdatagenerator.repository.mongo.model;

import br.com.realizecfi.orbiapixdatagenerator.domain.type.EventType;
import br.com.realizecfi.orbiapixdatagenerator.domain.type.TransactionType;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Document(collection = "Transactions")
public class MongoTransactionModel {

    @MongoId
    @Field(targetType = FieldType.STRING)
    private String id;
    @Field(targetType = FieldType.STRING)
    private UUID transactionId;
    @Field(targetType = FieldType.STRING)
    private UUID eventId;
    private EventType eventType;
    private TransactionType transactionType;
    private LocalDateTime transactionDate;
    private BigDecimal amount;
}
