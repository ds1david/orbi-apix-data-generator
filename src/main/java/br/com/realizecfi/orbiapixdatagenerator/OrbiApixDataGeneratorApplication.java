package br.com.realizecfi.orbiapixdatagenerator;

import br.com.realizecfi.orbiapixdatagenerator.domain.ApixDomain;
import br.com.realizecfi.orbiapixdatagenerator.domain.type.EntryType;
import br.com.realizecfi.orbiapixdatagenerator.domain.type.EventType;
import br.com.realizecfi.orbiapixdatagenerator.domain.type.TransactionType;
import br.com.realizecfi.orbiapixdatagenerator.repository.mongo.MongoDictTransactionRepository;
import br.com.realizecfi.orbiapixdatagenerator.repository.mongo.MongoTransactionRepository;
import br.com.realizecfi.orbiapixdatagenerator.repository.mongo.model.MongoDictTransactionModel;
import br.com.realizecfi.orbiapixdatagenerator.repository.mongo.model.MongoTransactionModel;
import br.com.realizecfi.orbiapixdatagenerator.repository.mysql.MysqlDictTransactionRepository;
import br.com.realizecfi.orbiapixdatagenerator.repository.mysql.MysqlTransactionRepository;
import br.com.realizecfi.orbiapixdatagenerator.repository.mysql.model.MysqlDictTransactionModel;
import br.com.realizecfi.orbiapixdatagenerator.repository.mysql.model.MysqlTransactionModel;
import com.microsoft.applicationinsights.core.dependencies.google.common.collect.Iterables;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Precision;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@Slf4j
@SpringBootApplication
@EnableMongoRepositories
@ComponentScan("br.com.realizecfi")
public class OrbiApixDataGeneratorApplication implements CommandLineRunner {

    private static final Integer DB_CHUNK_SIZE = 1000;
    private static final AtomicInteger NUMBER_OF_TRANSACTIONS = new AtomicInteger(0);
    private static final AtomicInteger NUMBER_OF_DICT_TRANSACTIONS = new AtomicInteger(0);

    private static final EnumMap<TransactionType, Double> PERCENT_OF_TRANSACTIONS_BY_TYPE =
        new EnumMap<>(TransactionType.class);

    private static final EnumMap<EventType, Double> PERCENT_OF_TRANSACTIONS_BY_EVENT =
        new EnumMap<>(EventType.class);

    private static final EnumMap<EventType, Duration> PROCESS_MINIMUM_DURATION_BY_EVENT =
        new EnumMap<>(EventType.class);

    private static final EnumMap<EventType, Duration> PROCESS_MAXIMUM_DURATION_BY_EVENT =
        new EnumMap<>(EventType.class);

    @Autowired
    private MongoTransactionRepository mongoTransactionRepository;

    @Autowired
    private MongoDictTransactionRepository mongoDictTransactionRepository;

    @Autowired
    private MysqlTransactionRepository mysqlTransactionRepository;

    @Autowired
    private MysqlDictTransactionRepository mysqlDictTransactionRepository;

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(OrbiApixDataGeneratorApplication.class, args)));
    }

    @Transactional
    @Override
    public void run(String... args) throws Exception {

        NUMBER_OF_TRANSACTIONS.set(1_000_000);
        NUMBER_OF_DICT_TRANSACTIONS.set(2_000_000);

        // transações pagamento
        PERCENT_OF_TRANSACTIONS_BY_TYPE.put(TransactionType.INTERNAL_SEND, .55);
        PERCENT_OF_TRANSACTIONS_BY_TYPE.put(TransactionType.EXTERNAL_SEND, .35);
        PERCENT_OF_TRANSACTIONS_BY_TYPE.put(TransactionType.INTERNAL_SCHEDULE, .03);
        PERCENT_OF_TRANSACTIONS_BY_TYPE.put(TransactionType.EXTERNAL_SCHEDULE, .02);
        PERCENT_OF_TRANSACTIONS_BY_TYPE.put(TransactionType.INTERNAL_REFUND, .03);
        PERCENT_OF_TRANSACTIONS_BY_TYPE.put(TransactionType.EXTERNAL_REFUND, .02);

        // transações dict
        PERCENT_OF_TRANSACTIONS_BY_TYPE.put(TransactionType.SEARCH, .9);
        PERCENT_OF_TRANSACTIONS_BY_TYPE.put(TransactionType.REGISTER, .09);
        PERCENT_OF_TRANSACTIONS_BY_TYPE.put(TransactionType.EXCLUSION, .01);

        // transações pagamento
        PERCENT_OF_TRANSACTIONS_BY_EVENT.put(EventType.RECEIVED, 1.0);
        PERCENT_OF_TRANSACTIONS_BY_EVENT.put(EventType.FRAUD_SUSPECTED, .07);
        PERCENT_OF_TRANSACTIONS_BY_EVENT.put(EventType.REJECTED, .2);
        PERCENT_OF_TRANSACTIONS_BY_EVENT.put(EventType.COMPLETED, .98);

        // transações dict
        PERCENT_OF_TRANSACTIONS_BY_EVENT.put(EventType.OWNERSHIP_REQUESTED, 1.0);
        PERCENT_OF_TRANSACTIONS_BY_EVENT.put(EventType.CRK_NOTIFIED, .99);

        PROCESS_MINIMUM_DURATION_BY_EVENT.put(EventType.FRAUD_SUSPECTED, Duration.ofMillis(100));
        PROCESS_MINIMUM_DURATION_BY_EVENT.put(EventType.REJECTED, Duration.ofMinutes(1));
        PROCESS_MINIMUM_DURATION_BY_EVENT.put(EventType.CRK_NOTIFIED, Duration.ofMillis(100));
        PROCESS_MINIMUM_DURATION_BY_EVENT.put(EventType.COMPLETED, Duration.ofSeconds(1));
        PROCESS_MINIMUM_DURATION_BY_EVENT.put(EventType.CLIENT_NOTIFIED, Duration.ofMillis(100));
        PROCESS_MINIMUM_DURATION_BY_EVENT.put(EventType.OWNERSHIP_REQUESTED, Duration.ofMillis(100));

        PROCESS_MAXIMUM_DURATION_BY_EVENT.put(EventType.FRAUD_SUSPECTED, Duration.ofSeconds(1));
        PROCESS_MAXIMUM_DURATION_BY_EVENT.put(EventType.REJECTED, Duration.ofMinutes(30));
        PROCESS_MAXIMUM_DURATION_BY_EVENT.put(EventType.CRK_NOTIFIED, Duration.ofSeconds(1));
        PROCESS_MAXIMUM_DURATION_BY_EVENT.put(EventType.COMPLETED, Duration.ofSeconds(3));
        PROCESS_MAXIMUM_DURATION_BY_EVENT.put(EventType.CLIENT_NOTIFIED, Duration.ofSeconds(2));
        PROCESS_MAXIMUM_DURATION_BY_EVENT.put(EventType.OWNERSHIP_REQUESTED, Duration.ofSeconds(1));

        LocalDate referenceDate = LocalDate.of(2019, 2, 1);

        ApixDomain apixDomain = generateApixDomain()
            .andThen(deletePreviousTransactionEvents())
//            .andThen(deletePreviousDictTransactionEvents())
            .apply(referenceDate);

        Thread threadTransactions = new Thread(() -> generateNumberOfTransactionsDomain()
            .andThen(generateTransactionsDomain())
            .andThen(generateTransactionEventsDomain())
            .andThen(saveTransactionEvents())
            .apply(apixDomain));

        threadTransactions.start();

//        Thread threadDictTransactions = new Thread(() -> generateNumberOfDictTransactionsDomain()
//            .andThen(generateDictTransactionsDomain())
//            .andThen(generateDictTransactionEventsDomain())
//            .andThen(saveDictTransactionEvents())
//            .apply(apixDomain));
//
//        threadDictTransactions.start();

        threadTransactions.join();
//        threadDictTransactions.join();
    }

    @Transactional
    private UnaryOperator<ApixDomain> deletePreviousTransactionEvents() {
        return apixDomain -> {
            LocalDateTime beginTransactionDate =
                apixDomain.getFirstDayOfMonth().atTime(LocalTime.MIN);
            LocalDateTime endTransactionDate =
                apixDomain.getFirstDayOfMonth().withDayOfMonth(apixDomain.getFirstDayOfMonth().lengthOfMonth())
                    .atTime(LocalTime.MAX);

            log.info("Apagando TRANSACTIONS events");
            mongoTransactionRepository.deleteByTransactionDateBetween(beginTransactionDate, endTransactionDate);
            mysqlTransactionRepository.deleteByTransactionDateBetween(beginTransactionDate, endTransactionDate);
            log.info("TRANSACTIONS events apagados!");

            return apixDomain;
        };
    }

    @Transactional
    private UnaryOperator<ApixDomain> deletePreviousDictTransactionEvents() {
        return apixDomain -> {
            LocalDateTime beginTransactionDate =
                apixDomain.getFirstDayOfMonth().atTime(LocalTime.MIN);
            LocalDateTime endTransactionDate =
                apixDomain.getFirstDayOfMonth().withDayOfMonth(apixDomain.getFirstDayOfMonth().lengthOfMonth())
                    .atTime(LocalTime.MAX);

            log.info("Apagando DICT_TRANSACTIONS events");
            mongoDictTransactionRepository.deleteByTransactionDateBetween(beginTransactionDate, endTransactionDate);
            mysqlDictTransactionRepository.deleteByTransactionDateBetween(beginTransactionDate, endTransactionDate);
            log.info("DICT TRANSACTIONS events apagados!");

            return apixDomain;
        };
    }

    @Transactional
    private UnaryOperator<ApixDomain> saveTransactionEvents() {
        return apixDomain -> {
            log.info("Gravando TRANSACTIONS events");
            Iterables.partition(apixDomain.getTransactionEvents(), DB_CHUNK_SIZE)
                .forEach(transactionsEvents -> {
                    mongoTransactionRepository.insert(transactionsEvents.stream()
                        .map(transactionEvent -> {
                            MongoTransactionModel mongoTransactionModel = new MongoTransactionModel();
                            BeanUtils.copyProperties(transactionEvent, mongoTransactionModel);

                            return mongoTransactionModel;
                        })
                        .collect(Collectors.toList()));

                    mysqlTransactionRepository.saveAll(transactionsEvents.stream()
                        .map(transactionEvent -> {
                            MysqlTransactionModel mysqlTransactionModel = new MysqlTransactionModel();
                            BeanUtils.copyProperties(transactionEvent, mysqlTransactionModel);

                            return mysqlTransactionModel;
                        })
                        .collect(Collectors.toList()));
                });
            log.info("TRANSACTIONS events gravados");

            return apixDomain;
        };
    }

    @Transactional
    private UnaryOperator<ApixDomain> saveDictTransactionEvents() {
        return apixDomain -> {
            log.info("Gravando DICT_TRANSACTIONS events");
            Iterables.partition(apixDomain.getDictTransactionEvents(), DB_CHUNK_SIZE)
                .forEach(dictTransactionEvents -> {
                    mongoDictTransactionRepository.saveAll(dictTransactionEvents.stream()
                        .map(dictTransactionEvent -> {
                            MongoDictTransactionModel mongoDictTransactionModel = new MongoDictTransactionModel();
                            BeanUtils.copyProperties(dictTransactionEvent, mongoDictTransactionModel);

                            return mongoDictTransactionModel;
                        })
                        .collect(Collectors.toList()));

                    mysqlDictTransactionRepository.saveAll(dictTransactionEvents.stream()
                        .map(dictTransactionEvent -> {
                            MysqlDictTransactionModel mysqlDictTransactionModel = new MysqlDictTransactionModel();
                            BeanUtils.copyProperties(dictTransactionEvent, mysqlDictTransactionModel);

                            return mysqlDictTransactionModel;
                        })
                        .collect(Collectors.toList()));
                });
            log.info("DICT TRANSACTIONS events gravados");

            return apixDomain;
        };
    }

    private UnaryOperator<ApixDomain> generateTransactionEventsDomain() {
        return apixDomain -> {
            apixDomain.setTransactionEvents(
                createTransactionEventsDomain(apixDomain, EventType.RECEIVED,
                    ApixDomain.Transaction::getHadReceived)
                    .andThen(createTransactionEventsDomain(apixDomain, EventType.FRAUD_SUSPECTED,
                        ApixDomain.Transaction::getHadFraudSuspected))
                    .andThen(createTransactionEventsDomain(apixDomain, EventType.REJECTED,
                        ApixDomain.Transaction::getHadRejected))
                    .andThen(createTransactionEventsDomain(apixDomain, EventType.COMPLETED,
                        ApixDomain.Transaction::getHadCompleted))
                    .andThen(createTransactionEventsDomain(apixDomain, EventType.CLIENT_NOTIFIED,
                        ApixDomain.Transaction::getHadClientNotified))
                    .apply(new ArrayList<>()));

            return apixDomain;
        };
    }

    private UnaryOperator<List<ApixDomain.TransactionEvent>> createTransactionEventsDomain(
        ApixDomain apixDomain, EventType eventType,
        Predicate<? super ApixDomain.Transaction> filter) {
        return transactionsData -> {
            apixDomain.getTransactions().stream()
                .filter(filter)
                .forEach(transaction -> transaction
                    .setTransactionDate(randomDateTimeBetween(eventType, transaction.getTransactionDate())));

            transactionsData.addAll(apixDomain.getTransactions().stream()
                .filter(filter)
                .map(transaction -> ApixDomain.TransactionEvent.builder()
                    .transactionId(transaction.getTransactionId())
                    .transactionType(transaction.getTransactionType())
                    .eventType(eventType)
                    .eventId(UUID.randomUUID())
                    .transactionDate(transaction.getTransactionDate())
                    .amount(BigDecimal.valueOf(transaction.getAmount()))
                    .build())
                .collect(Collectors.toList()));

            return transactionsData;
        };
    }

    private UnaryOperator<ApixDomain> generateDictTransactionEventsDomain() {
        return apixDomain -> {
            apixDomain.setDictTransactionEvents(
                createDictTransactionEventsDomain(apixDomain, EventType.RECEIVED,
                    ApixDomain.DictTransaction::getHadReceived)
                    .andThen(createDictTransactionEventsDomain(apixDomain, EventType.OWNERSHIP_REQUESTED,
                        ApixDomain.DictTransaction::getHadOwnerShipRequested))
                    .andThen(createDictTransactionEventsDomain(apixDomain, EventType.CRK_NOTIFIED,
                        ApixDomain.DictTransaction::getHadCrkNotified))
                    .andThen(createDictTransactionEventsDomain(apixDomain, EventType.COMPLETED,
                        ApixDomain.DictTransaction::getHadCompleted))
                    .andThen(createDictTransactionEventsDomain(apixDomain, EventType.CLIENT_NOTIFIED,
                        ApixDomain.DictTransaction::getHadClientNotified))
                    .apply(new ArrayList<>()));

            return apixDomain;
        };
    }

    private UnaryOperator<List<ApixDomain.DictTransactionEvent>> createDictTransactionEventsDomain(
        ApixDomain apixDomain, EventType eventType,
        Predicate<? super ApixDomain.DictTransaction> filter) {
        return dictTransactionsData -> {
            apixDomain.getDictTransactions().stream()
                .filter(filter)
                .forEach(dictTransaction -> dictTransaction
                    .setTransactionDate(randomDateTimeBetween(eventType, dictTransaction.getTransactionDate())));

            dictTransactionsData.addAll(apixDomain.getDictTransactions().stream()
                .filter(filter)
                .map(dictTransaction -> ApixDomain.DictTransactionEvent.builder()
                    .transactionId(dictTransaction.getTransactionId())
                    .transactionType(dictTransaction.getTransactionType())
                    .eventType(eventType)
                    .eventId(UUID.randomUUID())
                    .transactionDate(dictTransaction.getTransactionDate())
                    .entryType(dictTransaction.getEntryType())
                    .build())
                .collect(Collectors.toList()));

            return dictTransactionsData;
        };
    }

    private Function<LocalDate, ApixDomain> generateApixDomain() {
        return referenceDate -> ApixDomain.builder()
            .firstDayOfMonth(referenceDate.withDayOfMonth(1))
            .lastDayOfMonth(referenceDate.withDayOfMonth(referenceDate.lengthOfMonth()))
            .firstHourOfDay(LocalTime.MIN)
            .lastHourOfDay(LocalTime.MAX)
            .numberOfTransactions(new ArrayList<>())
            .transactions(new ArrayList<>())
            .transactionEvents(new ArrayList<>())
            .numberOfDictTransactions(new ArrayList<>())
            .dictTransactions(new ArrayList<>())
            .dictTransactionEvents(new ArrayList<>())
            .build();
    }

    private UnaryOperator<ApixDomain> generateTransactionsDomain() {
        return apixDomain -> {
            apixDomain.getNumberOfTransactions().forEach(numberOfTransactionsDomain -> {
                for (int i = 0; i < numberOfTransactionsDomain.getNumber(); i++) {
                    Integer numberToTest;

                    ApixDomain.Transaction transaction = ApixDomain.Transaction.builder()
                        .transactionId(UUID.randomUUID())
                        .transactionType(numberOfTransactionsDomain.getTransactionType())
                        .transactionDate(randomDateBetween(apixDomain.getFirstDayOfMonth(),
                            apixDomain.getLastDayOfMonth())
                            .atTime(randomTimeBetween(apixDomain.getFirstHourOfDay(),
                                apixDomain.getLastHourOfDay())))
                        .amount(randomAmount())
                        .hadReceived(true)
                        .hadFraudSuspected(false)
                        .hadRejected(false)
                        .hadCompleted(false)
                        .hadClientNotified(true)
                        .build();

                    // marca supeita de fraude conforme proporção
                    numberToTest = numberOfTransactionsDomain.getNumberByEventTypes().get(EventType.FRAUD_SUSPECTED);
                    if (numberToTest > 0) {
                        transaction.setHadFraudSuspected(true);
                        numberOfTransactionsDomain.getNumberByEventTypes()
                            .put(EventType.FRAUD_SUSPECTED, numberToTest - 1);
                    }

                    // marca rejeitada por suspeita de fraude conforme proporção
                    numberToTest = numberOfTransactionsDomain.getNumberByEventTypes().get(EventType.REJECTED);
                    if (numberToTest > 0) {
                        transaction.setHadRejected(true);
                        numberOfTransactionsDomain.getNumberByEventTypes().put(EventType.REJECTED, numberToTest - 1);
                    }

                    // marca completa apenas se não for rejeitada por suspeita de fraude
                    if (!transaction.getHadRejected()) {
                        // marca como completa e cliente nofificado conforme proporção
                        numberToTest = numberOfTransactionsDomain.getNumberByEventTypes().get(EventType.COMPLETED);
                        if (numberToTest > 0) {
                            transaction.setHadCompleted(true);
                            numberOfTransactionsDomain.getNumberByEventTypes()
                                .put(EventType.COMPLETED, numberToTest - 1);
                        }
                    }

                    apixDomain.getTransactions().add(transaction);
                }
            });

            return apixDomain;
        };
    }

    private UnaryOperator<ApixDomain> generateDictTransactionsDomain() {
        return apixDomain -> {
            apixDomain.getNumberOfDictTransactions().forEach(numberOfDictTransactionsDomain -> {
                for (int i = 0; i < numberOfDictTransactionsDomain.getNumber(); i++) {
                    Integer numberToTest;

                    ApixDomain.DictTransaction dictTransaction = ApixDomain.DictTransaction.builder()
                        .transactionId(UUID.randomUUID())
                        .transactionType(numberOfDictTransactionsDomain.getTransactionType())
                        .transactionDate(randomDateBetween(apixDomain.getFirstDayOfMonth(),
                            apixDomain.getLastDayOfMonth())
                            .atTime(randomTimeBetween(apixDomain.getFirstHourOfDay(),
                                apixDomain.getLastHourOfDay())))
                        .hadReceived(true)
                        .hadOwnerShipRequested(false)
                        .hadCrkNotified(false)
                        .hadCompleted(false)
                        .hadClientNotified(true)
                        .build();

                    // marca confirmação de posse conforme somente para eventos de registro
                    if (numberOfDictTransactionsDomain.getTransactionType().equals(TransactionType.REGISTER)) {
                        dictTransaction.setEntryType(randomEntryType());
                        dictTransaction.setHadOwnerShipRequested(true);
                    }

                    // marca crk notificada conforme proporção
                    numberToTest = numberOfDictTransactionsDomain.getNumberByEventTypes().get(EventType.CRK_NOTIFIED);
                    if (numberToTest > 0) {
                        dictTransaction.setHadCrkNotified(true);
                        numberOfDictTransactionsDomain.getNumberByEventTypes()
                            .put(EventType.CRK_NOTIFIED, numberToTest - 1);
                    }

                    // marca como completa e cliente nofificado conforme proporção
                    numberToTest = numberOfDictTransactionsDomain.getNumberByEventTypes().get(EventType.COMPLETED);
                    if (numberToTest > 0) {
                        dictTransaction.setHadCompleted(true);
                        numberOfDictTransactionsDomain.getNumberByEventTypes()
                            .put(EventType.COMPLETED, numberToTest - 1);
                    }

                    apixDomain.getDictTransactions().add(dictTransaction);
                }
            });

            return apixDomain;
        };
    }

    private UnaryOperator<ApixDomain> generateNumberOfTransactionsDomain() {
        return apixDomain -> {
            apixDomain.getNumberOfTransactions()
                .add(createNumberOfTransactionsDomain(TransactionType.EXTERNAL_REFUND));
            apixDomain.getNumberOfTransactions()
                .add(createNumberOfTransactionsDomain(TransactionType.INTERNAL_REFUND));
            apixDomain.getNumberOfTransactions()
                .add(createNumberOfTransactionsDomain(TransactionType.EXTERNAL_SCHEDULE));
            apixDomain.getNumberOfTransactions()
                .add(createNumberOfTransactionsDomain(TransactionType.INTERNAL_SCHEDULE));
            apixDomain.getNumberOfTransactions()
                .add(createNumberOfTransactionsDomain(TransactionType.EXTERNAL_SEND));
            apixDomain.getNumberOfTransactions()
                .add(createNumberOfTransactionsDomain(TransactionType.INTERNAL_SEND));

            return apixDomain;
        };
    }

    private UnaryOperator<ApixDomain> generateNumberOfDictTransactionsDomain() {
        return apixDomain -> {
            apixDomain.getNumberOfDictTransactions()
                .add(createNumberOfDictTransactionsDomain(TransactionType.SEARCH));
            apixDomain.getNumberOfDictTransactions()
                .add(createNumberOfDictTransactionsDomain(TransactionType.REGISTER));
            apixDomain.getNumberOfDictTransactions()
                .add(createNumberOfDictTransactionsDomain(TransactionType.EXCLUSION));

            return apixDomain;
        };
    }

    private static ApixDomain.NumberOfTransaction createNumberOfTransactionsDomain(TransactionType transactionType) {
        ApixDomain.NumberOfTransaction numberOfTransactionsDomain =
            ApixDomain.NumberOfTransaction.builder()
                .transactionType(transactionType)
                .number((int) Math
                    .ceil(NUMBER_OF_TRANSACTIONS.doubleValue() * PERCENT_OF_TRANSACTIONS_BY_TYPE.get(transactionType)))
                .numberByEventTypes(new EnumMap<>(EventType.class))
                .build();

        // proporção do total
        numberOfTransactionsDomain.getNumberByEventTypes().put(EventType.FRAUD_SUSPECTED,
            calculateProportionByEventType(numberOfTransactionsDomain.getNumber(), EventType.FRAUD_SUSPECTED));

        // proporção das suspeitas de fraude
        numberOfTransactionsDomain.getNumberByEventTypes().put(EventType.REJECTED,
            calculateProportionByEventType(
                numberOfTransactionsDomain.getNumberByEventTypes().get(EventType.FRAUD_SUSPECTED),
                EventType.REJECTED));

        // proporção do total sem as rejeitadas
        numberOfTransactionsDomain.getNumberByEventTypes().put(EventType.COMPLETED,
            calculateProportionByEventType(
                numberOfTransactionsDomain.getNumber() -
                    numberOfTransactionsDomain.getNumberByEventTypes().get(EventType.REJECTED),
                EventType.COMPLETED));

        return numberOfTransactionsDomain;
    }

    private static ApixDomain.NumberOfTransaction createNumberOfDictTransactionsDomain(
        TransactionType transactionType) {
        ApixDomain.NumberOfTransaction numberOfDictTransactionsDomain =
            ApixDomain.NumberOfTransaction.builder()
                .transactionType(transactionType)
                .number((int) Math
                    .ceil(NUMBER_OF_DICT_TRANSACTIONS.doubleValue() *
                        PERCENT_OF_TRANSACTIONS_BY_TYPE.get(transactionType)))
                .numberByEventTypes(new EnumMap<>(EventType.class))
                .build();

        // somente register tem ownership request
        if (transactionType.equals(TransactionType.REGISTER)) {
            // proporção do total
            numberOfDictTransactionsDomain.getNumberByEventTypes().put(EventType.OWNERSHIP_REQUESTED,
                calculateProportionByEventType(numberOfDictTransactionsDomain.getNumber(),
                    EventType.OWNERSHIP_REQUESTED));
        }

        // proporção do total
        numberOfDictTransactionsDomain.getNumberByEventTypes().put(EventType.CRK_NOTIFIED,
            calculateProportionByEventType(
                numberOfDictTransactionsDomain.getNumber(), EventType.CRK_NOTIFIED));

        // proporção de crk notificada
        numberOfDictTransactionsDomain.getNumberByEventTypes().put(EventType.COMPLETED,
            calculateProportionByEventType(
                numberOfDictTransactionsDomain.getNumberByEventTypes().get(EventType.CRK_NOTIFIED),
                EventType.COMPLETED));

        return numberOfDictTransactionsDomain;
    }

    private static Integer calculateProportionByEventType(Integer numberOfTransactions, EventType eventType) {
        return (int) Math.ceil(numberOfTransactions.doubleValue() * PERCENT_OF_TRANSACTIONS_BY_EVENT.get(eventType));
    }

    private static Double randomAmount() {
        return Precision.round(ThreadLocalRandom
            .current()
            .nextDouble(0.01, 5000), 2);
    }

    private static EntryType randomEntryType() {
        return EntryType.values()[ThreadLocalRandom
            .current()
            .nextInt(EntryType.values().length)];
    }

    private static LocalDateTime randomDateTimeBetween(EventType eventType, LocalDateTime referenceDateTime) {
        if (eventType.equals(EventType.RECEIVED)) {
            return referenceDateTime;
        }

        long startSeconds = referenceDateTime
            .plusNanos(PROCESS_MINIMUM_DURATION_BY_EVENT.get(eventType).toNanos())
            .toInstant(ZoneOffset.UTC).toEpochMilli();
        long endSeconds = referenceDateTime
            .plusNanos(PROCESS_MAXIMUM_DURATION_BY_EVENT.get(eventType).toNanos())
            .toInstant(ZoneOffset.UTC).toEpochMilli();

        long randomTime = ThreadLocalRandom
            .current()
            .nextLong(startSeconds, endSeconds);

        return LocalDateTime.ofInstant(Instant.ofEpochMilli(randomTime), ZoneOffset.UTC);
    }

    private static LocalTime randomTimeBetween(LocalTime startTime, LocalTime endTime) {
        long startSeconds = startTime.toNanoOfDay();
        long endSeconds = endTime.toNanoOfDay();
        long randomTime = ThreadLocalRandom
            .current()
            .nextLong(startSeconds, endSeconds);

        return LocalTime.ofNanoOfDay(randomTime);
    }

    private static LocalDate randomDateBetween(LocalDate startInclusive, LocalDate endExclusive) {
        long startEpochDay = startInclusive.toEpochDay();
        long endEpochDay = endExclusive.toEpochDay();
        long randomDay = ThreadLocalRandom
            .current()
            .nextLong(startEpochDay, endEpochDay);

        return LocalDate.ofEpochDay(randomDay);
    }
}
