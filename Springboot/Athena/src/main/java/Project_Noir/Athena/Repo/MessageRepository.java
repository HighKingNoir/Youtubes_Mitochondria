package Project_Noir.Athena.Repo;

import Project_Noir.Athena.Model.Messages;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Messages, String> {

    List<Messages> findByMessageIdIn(ArrayList<String> messages, PageRequest of);
}
