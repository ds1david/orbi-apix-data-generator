package br.com.realizecfi.orbiapixdatagenerator.repository.mongo;

import br.com.realizecfi.orbiapixdatagenerator.repository.mongo.model.MongoDictTransactionModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface MongoDictTransactionRepository extends MongoRepository<MongoDictTransactionModel, String> {

    void deleteByTransactionDateBetween(LocalDateTime beginTransactionDate, LocalDateTime endTransactionDate);
}
