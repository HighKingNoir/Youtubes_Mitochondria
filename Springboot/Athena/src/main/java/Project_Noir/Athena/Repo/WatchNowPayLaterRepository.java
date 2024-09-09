package Project_Noir.Athena.Repo;

import Project_Noir.Athena.Model.WatchNowPayLater;
import Project_Noir.Athena.Model.WatchNowPayLaterEnum;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface WatchNowPayLaterRepository extends MongoRepository<WatchNowPayLater, String> {

    Optional<WatchNowPayLater> findByChannelNameAndContentID(String channelName, String contentID);

    List<WatchNowPayLater> findAllByWatchNowPayLaterEnumAndNextPaymentDateBefore(WatchNowPayLaterEnum watchNowPayLaterEnum, Instant now);

}
