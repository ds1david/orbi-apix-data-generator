package br.com.realizecfi.orbiapixdatagenerator.domain;

import br.com.realizecfi.orbiapixdatagenerator.domain.type.EntryType;
import br.com.realizecfi.orbiapixdatagenerator.domain.type.EventType;
import br.com.realizecfi.orbiapixdatagenerator.domain.type.TransactionType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ApixDomain {

    private LocalDate firstDayOfMonth;
    private LocalDate lastDayOfMonth;
    private LocalTime firstHourOfDay;
    private LocalTime lastHourOfDay;
    private List<NumberOfTransaction> numberOfTransactions;
    private List<Transaction> transactions;
    private List<TransactionEvent> transactionEvents;
    private List<NumberOfTransaction> numberOfDictTransactions;
    private List<DictTransaction> dictTransactions;
    private List<DictTransactionEvent> dictTransactionEvents;

    @Data
    @Builder
    public static class Transaction {

        private UUID transactionId;
        private TransactionType transactionType;
        private LocalDateTime transactionDate;
        private Double amount;
        private Boolean hadReceived;
        private Boolean hadFraudSuspected;
        private Boolean hadRejected;
        private Boolean hadCompleted;
        private Boolean hadClientNotified;
    }

    @Data
    @Builder
    public static class DictTransaction {

        private UUID transactionId;
        private TransactionType transactionType;
        private LocalDateTime transactionDate;
        private EntryType entryType;
        private Boolean hadReceived;
        private Boolean hadOwnerShipRequested;
        private Boolean hadCrkNotified;
        private Boolean hadCompleted;
        private Boolean hadClientNotified;
    }

    @Data
    @Builder
    public static class TransactionEvent {
        private UUID transactionId;
        private UUID eventId;
        private EventType eventType;
        private TransactionType transactionType;
        private LocalDateTime transactionDate;
        private BigDecimal amount;
    }

    @Data
    @Builder
    public static class DictTransactionEvent {
        private UUID transactionId;
        private UUID eventId;
        private EventType eventType;
        private TransactionType transactionType;
        private LocalDateTime transactionDate;
        private EntryType entryType;
    }

    @Data
    @Builder
    public static class NumberOfTransaction {

        private TransactionType transactionType;
        private EnumMap<EventType, Integer> numberByEventTypes;
        private Integer number;
    }
}
