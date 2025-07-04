package Project_Noir.Athena.Repo;

import Project_Noir.Athena.Model.NonceRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NonceRepository extends MongoRepository<NonceRecord, String> {
}
