package Project_Noir.Athena.Repo;

import Project_Noir.Athena.Model.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelRepository extends MongoRepository<Channels, String> {

    List<Channels> findByChannelNameContainingIgnoreCaseAndChannelStatus(String channelName, ChannelStatus channelStatus,PageRequest of);

    // @dev returns content entity by youtubeMainVideoID
    Optional<Channels> findByChannelName(String channelName);

    // @dev returns user entity by username
    Optional<Users> findByChannelNameIgnoreCase(String ChannelName);

    List<Channels> findByChannelEvents_ChannelEventStatusOrderByChannelEvents_ShowTimeDesc(
            ChannelEventStatus channelEventStatus, Pageable pageable);

    @Query("{'streamerAWVInfo.platform': ?0, 'streamerAWVInfo.streamerId': ?1}")
    Optional<Channels> findByPlatformAndStreamerId(String platform, String streamerId);

    Long countByChannelStatus(ChannelStatus status);

    List<Channels> findAllByChannelStatus(ChannelStatus channelStatus);

    List<Channels> findByChannelStatus(ChannelStatus channelStatus, PageRequest of);

    List<Channels> findByStreamerChangeRequest(boolean streamerChangeRequest, PageRequest of);
    
    List<Channels> findAllByIsBanned(boolean isBanned);

    List<Channels> findTop10ByChannelNameContainingOrChannelIdContaining(String channelName, String channelId);

    Long countByApprovedDateBetween(Instant startDate, Instant endDate);

    Long countByChannelStatusAndAwvIsUpdated(ChannelStatus channelStatus, boolean awvIsUpdated);

    List<Channels> findByChannelStatusAndAwvIsUpdated(ChannelStatus channelStatus, boolean awvIsUpdated, PageRequest of);

    List<Channels> findByChannelStatusAndChannelIdNotIn(String channelStatus, List<String> excludedChannelIds);

    List<Channels> findByNextAvailableResubmitDateAfterAndChannelStatus(Instant instant, ChannelStatus channelStatus, PageRequest of);
}
