package Project_Noir.Athena.Repo;

import Project_Noir.Athena.Model.ClientSideMultiCallPackage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientSideMultiCallPackageRepository extends MongoRepository<ClientSideMultiCallPackage, String> {
}
