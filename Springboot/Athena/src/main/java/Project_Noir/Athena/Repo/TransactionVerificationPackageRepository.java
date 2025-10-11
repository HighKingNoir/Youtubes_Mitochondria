package Project_Noir.Athena.Repo;


import Project_Noir.Athena.Model.BlockchainInteractionStatusEnum;
import Project_Noir.Athena.Model.ClientSideMultiCallPackage;
import Project_Noir.Athena.Model.TransactionVerificationPackage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionVerificationPackageRepository extends MongoRepository<TransactionVerificationPackage, String> {
    List<TransactionVerificationPackage> findAllByStatus(BlockchainInteractionStatusEnum status);
}
