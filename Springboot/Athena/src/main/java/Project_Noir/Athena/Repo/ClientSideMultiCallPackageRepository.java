package Project_Noir.Athena.Repo;

import Project_Noir.Athena.Model.BlockchainInteractionStatusEnum;
import Project_Noir.Athena.Model.ClientSideMultiCallPackage;
import Project_Noir.Athena.Model.Content;
import Project_Noir.Athena.Model.ContentEnum;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientSideMultiCallPackageRepository extends MongoRepository<ClientSideMultiCallPackage, String> {
    List<ClientSideMultiCallPackage> findAllByStatus(BlockchainInteractionStatusEnum status);
}
