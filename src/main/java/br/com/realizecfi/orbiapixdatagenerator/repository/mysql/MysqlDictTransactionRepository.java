package br.com.realizecfi.orbiapixdatagenerator.repository.mysql;

import br.com.realizecfi.orbiapixdatagenerator.repository.mysql.model.MysqlDictTransactionModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
@EnableTransactionManagement
public interface MysqlDictTransactionRepository extends JpaRepository<MysqlDictTransactionModel, UUID> {

    void deleteByTransactionDateBetween(LocalDateTime beginTransactionDate, LocalDateTime endTransactionDate);
}
