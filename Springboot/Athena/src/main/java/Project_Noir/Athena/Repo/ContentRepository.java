package Project_Noir.Athena.Repo;

import Project_Noir.Athena.Model.Content;
import Project_Noir.Athena.Model.ContentEnum;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContentRepository extends MongoRepository<Content, String>  {

    List<Content> findByContentNameContainingIgnoreCaseAndContentEnum(String contentName, ContentEnum contentEnum, PageRequest of);

    // @dev returns content entity by youtubeMainVideoID
    Optional<Content> findByYoutubeMainVideoID(String youtubeMainVideoID);

    List<Content> findByReleaseDateAfterAndContentIdIn(LocalDate date, List<String> contentIds);

    List<Content> findByReleaseDateBetweenAndContentIdIn(LocalDate startDate, LocalDate endDate, List<String> contentIds);

    Long countByReleaseDateBetweenAndContentIdIn(LocalDate startDate, LocalDate endDate, List<String> contentIds);

    Long countByReleaseDateAfterAndContentIdIn(LocalDate date, List<String> contentIds);

    List<Content> findByReleaseDateGreaterThanEqualOrderByReleaseDate(LocalDate today);

    Long countByCreatedDateBetween(Instant startDate, Instant endDate);

    List<Content> findByContentEnum(ContentEnum contentEnum,PageRequest of);

    List<Content> findAllByContentEnum(ContentEnum contentEnum);

    List<Content> findAllByIsCompleteAndContentEnumNot(Boolean isComplete, ContentEnum contentEnum);

    List<Content> findByContentTypeAndContentEnum(Instant instant, ContentEnum contentEnum,PageRequest of);

    List<Content> findByActiveDateBeforeAndContentEnumAndContentType(Instant instant, ContentEnum contentEnum, String contentType, PageRequest of);

    List<Content> findByContentTypeAndContentEnum(String contentType, ContentEnum contentEnum, PageRequest of);

    List<Content> findByHypeLessThanAndContentEnum(Double hypeValue, ContentEnum contentEnum, PageRequest of);

    List<Content> findByContentIdIn(ArrayList<String> contentIds, PageRequest of);

    List<Content> findByActiveDateBeforeAndContentIdIn(Instant activeDate, ArrayList<String> contentIds, PageRequest of);

    List<Content> findContentsByContentEnumIn(List<ContentEnum> contentEnums);

    List<Content> findByReportRateLessThanAndCreatedDateAfterAndContentEnum(Double reportRate,Instant createdDate,ContentEnum contentEnum, PageRequest of);

    List<Content> findAllByContentEnumAndReportRateIsNotNull(ContentEnum contentEnum, PageRequest of);

    @Query("{ 'contentReports' : { $exists : true, $not: { $size: 0 } } }")
    List<Content> findAllWithNonEmptyContentReports();


}
