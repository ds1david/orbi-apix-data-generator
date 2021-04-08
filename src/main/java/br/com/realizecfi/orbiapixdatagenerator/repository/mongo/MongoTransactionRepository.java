package br.com.realizecfi.orbiapixdatagenerator.repository.mongo;

import br.com.realizecfi.orbiapixdatagenerator.repository.mongo.model.MongoTransactionModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface MongoTransactionRepository extends MongoRepository<MongoTransactionModel, String> {

    void deleteByTransactionDateBetween(LocalDateTime beginTransactionDate, LocalDateTime endTransactionDate);
}
