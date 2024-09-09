package Project_Noir.Athena.Service;

import Project_Noir.Athena.Model.Content;
import Project_Noir.Athena.Model.ContentEnum;
import Project_Noir.Athena.Model.WatchNowPayLaterEnum;
import Project_Noir.Athena.Repo.ContentRepository;
import Project_Noir.Athena.Repo.WatchNowPayLaterRepository;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class YouTubeAPIService {
    private final MessageService messageService;
    private final ContentRepository contentRepository;
    private final WatchNowPayLaterRepository watchNowPayLaterRepository;
    private final ContractServiceInterface contractServiceInterface;
    @Value("${youtube.api.key}")
    private String API_KEY;
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private YouTube getService() throws GeneralSecurityException, IOException {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        return new YouTube.Builder(httpTransport, JSON_FACTORY, null)
                .setApplicationName("Sivantis")
                .build();
    }

    @Scheduled(fixedRate = 120000)
    @SchedulerLock(name = "videoChecks", lockAtMostFor = "PT1M", lockAtLeastFor = "PT30S")
    void videoChecks() throws IOException, GeneralSecurityException {
        List<Video> videos = new ArrayList<>();
        List<String> youtubeVideoIds = getAllYoutubeMainIds();
        if(!youtubeVideoIds.isEmpty()){
            int startIndex = 0;
            int batchSize = 50; // Number of video IDs to request in each batch

            while (startIndex < youtubeVideoIds.size()) {
                List<String> batchIds = youtubeVideoIds.subList(startIndex, Math.min(startIndex + batchSize, youtubeVideoIds.size()));

                YouTube.Videos.List listRequest = getService().videos().list(List.of("status"))
                        .setKey(API_KEY)
                        .setId(batchIds);

                VideoListResponse response = listRequest.execute();
                videos.addAll(response.getItems());

                // Update the start index for the next batch
                startIndex += batchSize;
            }
            var allFailedVideos = videos.stream().map(Video::getId).toList();

            for (String youtubeMainVideoID: allFailedVideos ){
                if(contentRepository.findByYoutubeMainVideoID(youtubeMainVideoID).isPresent()){
                    var content = contentRepository.findByYoutubeMainVideoID(youtubeMainVideoID).get();
                    messageService.failedVideoMessage(content);
                    returnAllMana(content);
                }
            }
        }

    }

    // @dev Returns all YoutubeMainIds labeled 'Active' that have Complete status
    public List<String> getAllYoutubeMainIds() {
        return contentRepository.findAllByIsCompleteAndContentEnumNot(true, ContentEnum.Inactive).stream().map(Content::getYoutubeMainVideoID).collect(Collectors.toList());
    }


    private void returnAllMana(Content content){
        if(isAuction(content.getContentType())){
            contractServiceInterface.failAuction(content.getContentId(), content.getListOfBuyerIds());
        }
        else{
            for (Map.Entry<String, String> buyer : content.getListOfBuyerIds().entrySet()) {
                var optionalWatchNowPayLater = watchNowPayLaterRepository.findByChannelNameAndContentID(buyer.getKey(), content.getContentId());
                if (optionalWatchNowPayLater.isPresent() && optionalWatchNowPayLater.get().getWatchNowPayLaterEnum().equals(WatchNowPayLaterEnum.Unpaid)) {
                    var watchNowPayLater = optionalWatchNowPayLater.get();
                    contractServiceInterface.sendWatchNowPayLaterRefundPayment(watchNowPayLater, content.getCreatorID(), content.getContentId(), buyer.getKey(), buyer.getValue());
                } else {
                    contractServiceInterface.sendRefundPayment(content.getCreatorID(), content.getContentId(), buyer.getKey(), buyer.getValue());
                }
            }
        }
    }

    private boolean isAuction(String contentType){
        return contentType.equals("Invention") || contentType.equals("Innovation");
    }

}
