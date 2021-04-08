package br.com.realizecfi.orbiapixdatagenerator.repository.mysql.model;

import br.com.realizecfi.orbiapixdatagenerator.domain.type.EntryType;
import br.com.realizecfi.orbiapixdatagenerator.domain.type.EventType;
import br.com.realizecfi.orbiapixdatagenerator.domain.type.TransactionType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.springframework.data.domain.Persistable;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity(name = "dict_transactions")
public class MysqlDictTransactionModel implements Persistable<UUID> {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Type(type = "uuid-char")
    private UUID id;
    @Type(type = "uuid-char")
    private UUID transactionId;
    @Type(type = "uuid-char")
    private UUID eventId;
    @Enumerated(EnumType.STRING)
    private EventType eventType;
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;
    private Long transactionExternalId;
    private LocalDateTime transactionDate;
    @Enumerated(EnumType.STRING)
    private EntryType entryType;

    @Override
    public boolean isNew() {
        return true;
    }
}
