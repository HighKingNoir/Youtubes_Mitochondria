package Project_Noir.Athena.Repo;

import Project_Noir.Athena.Model.RankUpPaymentLock;
import Project_Noir.Athena.Model.TransactionVerificationFunctionEnum;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RankUpPaymentLockRepository extends MongoRepository<RankUpPaymentLock, String> {
    Optional<RankUpPaymentLock> findByUserIdAndTransactionVerificationFunctionEnum(
            String userId,
            TransactionVerificationFunctionEnum transactionVerificationFunctionEnum
    );
}
