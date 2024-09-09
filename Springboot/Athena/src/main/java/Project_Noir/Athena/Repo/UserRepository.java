package Project_Noir.Athena.Repo;

import Project_Noir.Athena.Model.Users;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends MongoRepository<Users, String> {

    // @dev returns user entity by username
    Optional<Users> findByUsername(String Username);

    // @dev returns user entity by email
    Optional<Users> findByEmail(String email);

    // @dev returns user entity by username
    Optional<Users> findByUsernameIgnoreCase(String Username);

    // @dev returns user entity by email
    Optional<Users> findByEmailIgnoreCase(String email);

    // @dev returns user entity by createdContentID
    Optional<Users> findByCreatedContent(String createdContentID);

    // @dev returns user entity by createdContentID
    Optional<Users> findBySecret(String mfaSecret);

    Long countByCreationDateBetween(Instant startDate, Instant endDate);

    List<Users> findAllByIsContentCreator(Boolean isContentCreator);

    List<Users> findUsersByDeleteRequestIsTrueOrEnabledIsFalse();

    List<Users> findAllByIsBanned(boolean isBanned);

    List<Users> findTop10ByUsernameContainingOrUserIdContaining(String channelName, String channelId);

    List<Users> findTop50ByOrderByTotalHypeDesc();

    List<Users> findByTotalHypeLessThan(double totalHype, PageRequest of);
}
