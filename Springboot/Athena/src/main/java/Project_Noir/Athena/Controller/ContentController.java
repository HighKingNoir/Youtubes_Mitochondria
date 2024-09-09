package Project_Noir.Athena.Controller;

import Project_Noir.Athena.DTO.*;
import Project_Noir.Athena.Exception.SivantisException;
import Project_Noir.Athena.Model.*;
import Project_Noir.Athena.Repo.*;
import Project_Noir.Athena.Service.ContentService;
import Project_Noir.Athena.Service.ContractServiceInterface;
import Project_Noir.Athena.Service.JwtService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.web3j.protocol.exceptions.TransactionException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;


@RestController
@RequestMapping("/Content")
@AllArgsConstructor
@Slf4j
public class ContentController {

    private final UserRepository userRepository;
    private final ContentService contentService;
    private final ContentRepository contentRepository;
    private final WatchNowPayLaterRepository watchNowPayLaterRepository;
    private final ContractServiceInterface contractServiceInterface;
    private final PaymentRepository paymentRepository;
    private final ChannelRepository channelRepository;
    private final JwtService jwtService;


    @PostMapping("/Complete")
    // @dev Generates a new content entity that is finished
    public ResponseEntity<Content> createCompleteContent(@RequestBody @Valid ContentRequest contentRequest, @RequestHeader("Authorization") String jwt) throws IOException, URISyntaxException, TransactionException, ExecutionException, InterruptedException {
        return new ResponseEntity<>(contentService.saveCompleteContent(contentRequest, jwtService.getJWTString(jwt)), HttpStatus.CREATED);
    }

    @PostMapping("/InDevelopment")
    // @dev Generates a new content entity that's under development
    public ResponseEntity<Content> createInDevelopmentBuiltContent(@RequestBody @Valid ContentRequest contentRequest, @RequestHeader("Authorization") String jwt) throws IOException, URISyntaxException, TransactionException, ExecutionException, InterruptedException {
        return new ResponseEntity<>(contentService.saveInDevelopmentContent(contentRequest, jwtService.getJWTString(jwt)), HttpStatus.CREATED);
    }

    @PutMapping("/Reactivate")
    public ResponseEntity<String> reactivateContent(@RequestBody ReactivateContentRequest reactivateContentRequest, @RequestHeader("Authorization") String jwt) throws TransactionException, IOException, ExecutionException, InterruptedException {
        contentService.reactivateContent(reactivateContentRequest, jwtService.getJWTString(jwt));
        return new ResponseEntity<String>("Content Reactivated", HttpStatus.ACCEPTED);
    }

   @GetMapping("/All")
   // @dev returns all content
    public ResponseEntity<List<Content>> getAllContent(){
        return ResponseEntity.status(HttpStatus.OK).body(contentRepository.findAll());
   }

    @GetMapping("/Single/{contentId}")
    public ResponseEntity<Content> getSingleContent(@PathVariable String contentId){
        return ResponseEntity.status(HttpStatus.OK).body(contentRepository.findById(contentId).orElseThrow());
    }

    @GetMapping("/All/Active")
    public ResponseEntity<List<Content>> getAllActiveContent(@RequestParam(name = "activeDate", required = false) String activeDate) {
        Sort sort = Sort.by("activeDate").descending();
        List<Content> videos;

        if (activeDate != null) {
            long timestampMillis = (long) (Double.parseDouble(activeDate) * 1000); // Convert to milliseconds
            videos = contentRepository.findByContentTypeAndContentEnum(Instant.ofEpochMilli(timestampMillis), ContentEnum.Active, PageRequest.of(0, 50, sort));
        } else {
            videos = contentRepository.findByContentEnum(ContentEnum.Active,PageRequest.of(0, 50, sort));
        }

        return  ResponseEntity.status(HttpStatus.OK).body(videos);
    }

    @GetMapping("/All/Active/Inventions")
    public ResponseEntity<List<Content>> getAllActiveInventiveContent(@RequestParam(name = "activeDate", required = false) String activeDate) {
        Sort sort = Sort.by("activeDate").descending();
        List<Content> videos;

        if (activeDate != null) {
            long timestampMillis = (long) (Double.parseDouble(activeDate) * 1000); // Convert to milliseconds
            videos = contentRepository.findByActiveDateBeforeAndContentEnumAndContentType(Instant.ofEpochMilli(timestampMillis), ContentEnum.Active,"Invention" ,PageRequest.of(0, 50, sort));
        } else {
            videos = contentRepository.findByContentTypeAndContentEnum("Invention", ContentEnum.Active, PageRequest.of(0, 50, sort));
        }

        return  ResponseEntity.status(HttpStatus.OK).body(videos);
    }

    @GetMapping("/All/Active/Innovation")
    public ResponseEntity<List<Content>> getAllActiveInnovativeContent(@RequestParam(name = "activeDate", required = false) String activeDate) {
        Sort sort = Sort.by("activeDate").descending();
        List<Content> videos;

        if (activeDate != null) {
            long timestampMillis = (long) (Double.parseDouble(activeDate) * 1000); // Convert to milliseconds
            videos = contentRepository.findByActiveDateBeforeAndContentEnumAndContentType(Instant.ofEpochMilli(timestampMillis), ContentEnum.Active,"Innovation" , PageRequest.of(0, 50, sort));
        } else {
            videos = contentRepository.findByContentTypeAndContentEnum("Innovation", ContentEnum.Active, PageRequest.of(0, 50, sort));
        }

        return  ResponseEntity.status(HttpStatus.OK).body(videos);
    }

    @GetMapping("/All/Active/ShortFilm")
    public ResponseEntity<List<Content>> getAllActiveShortFilmContent(@RequestParam(name = "activeDate", required = false) String activeDate) {
        Sort sort = Sort.by("activeDate").descending();
        List<Content> videos;

        if (activeDate != null) {
            long timestampMillis = (long) (Double.parseDouble(activeDate) * 1000); // Convert to milliseconds
            videos = contentRepository.findByActiveDateBeforeAndContentEnumAndContentType(Instant.ofEpochMilli(timestampMillis), ContentEnum.Active,"Short Film" , PageRequest.of(0, 50, sort));
        } else {
            videos = contentRepository.findByContentTypeAndContentEnum("Short Film", ContentEnum.Active, PageRequest.of(0, 50, sort));
        }

        return  ResponseEntity.status(HttpStatus.OK).body(videos);
    }

    @GetMapping("/All/Active/Movies")
    public ResponseEntity<List<Content>> getAllActiveMovieContent(@RequestParam(name = "activeDate", required = false) String activeDate) {
        Sort sort = Sort.by("activeDate").descending();
        List<Content> videos;

        if (activeDate != null) {
            long timestampMillis = (long) (Double.parseDouble(activeDate) * 1000); // Convert to milliseconds
            videos = contentRepository.findByActiveDateBeforeAndContentEnumAndContentType(Instant.ofEpochMilli(timestampMillis), ContentEnum.Active,"Movies" , PageRequest.of(0, 50, sort));
        } else {
            videos = contentRepository.findByContentTypeAndContentEnum("Movies", ContentEnum.Active, PageRequest.of(0, 50, sort));
        }

        return  ResponseEntity.status(HttpStatus.OK).body(videos);
    }

    @GetMapping("/All/Active/Sports")
    public ResponseEntity<List<Content>> getAllActiveSportsContent(@RequestParam(name = "activeDate", required = false) String activeDate) {
        Sort sort = Sort.by("activeDate").descending();
        List<Content> videos;

        if (activeDate != null) {
            long timestampMillis = (long) (Double.parseDouble(activeDate) * 1000); // Convert to milliseconds
            videos = contentRepository.findByActiveDateBeforeAndContentEnumAndContentType(Instant.ofEpochMilli(timestampMillis), ContentEnum.Active,"Sports" , PageRequest.of(0, 50, sort));
        } else {
            videos = contentRepository.findByContentTypeAndContentEnum("Sports", ContentEnum.Active, PageRequest.of(0, 50, sort));
        }

        return  ResponseEntity.status(HttpStatus.OK).body(videos);
    }

    @GetMapping("/All/Active/Concerts")
    public ResponseEntity<List<Content>> getAllActiveConcertsContent(@RequestParam(name = "activeDate", required = false) String activeDate) {
        Sort sort = Sort.by("activeDate").descending();
        List<Content> videos;

        if (activeDate != null) {
            long timestampMillis = (long) (Double.parseDouble(activeDate) * 1000); // Convert to milliseconds
            videos = contentRepository.findByActiveDateBeforeAndContentEnumAndContentType(Instant.ofEpochMilli(timestampMillis), ContentEnum.Active,"Concerts" , PageRequest.of(0, 50, sort));
        } else {
            videos = contentRepository.findByContentTypeAndContentEnum("Concerts", ContentEnum.Active, PageRequest.of(0, 50, sort));
        }

        return  ResponseEntity.status(HttpStatus.OK).body(videos);
    }

    @GetMapping("/All/Active/Hype")
    public ResponseEntity<List<Content>> getAllActiveMostHypedContent(@RequestParam(name = "hypeValue", required = false) Double hypeValue) {
        Sort sort = Sort.by("hype").descending();
        List<Content> videos;

        if (hypeValue != null && hypeValue > .1) {
            videos = contentRepository.findByHypeLessThanAndContentEnum(hypeValue, ContentEnum.Active,PageRequest.of(0, 50, sort));
        } else {
            videos = contentRepository.findByContentEnum(ContentEnum.Active, PageRequest.of(0, 50, sort));
        }

        return  ResponseEntity.status(HttpStatus.OK).body(videos);
    }

    @GetMapping("/PayLater")
    public ResponseEntity<List<Content>> getAllPayLater(@RequestParam(name = "JWT") String JWT, @RequestParam(name = "start") int start) {

        List<Content> videos = contentRepository.findByContentIdIn(userRepository.findById(jwtService.extractUserId(JWT)).orElseThrow().getPayLater(), PageRequest.of(start, 50));

        return ResponseEntity.status(HttpStatus.OK).body(videos);
    }

    @GetMapping("/All/Active/Upcoming")
    public ResponseEntity<List<Content>> getAllActiveUpcomingReleaseContent(@RequestParam(name = "start") int start) {
        Sort sort = Sort.by(Sort.Order.asc("releaseDate"), Sort.Order.desc("activeDate"));

        List<Content> videos = contentRepository.findByContentEnum(
                ContentEnum.Active,
                PageRequest.of(start, 50, sort)
        );

        return ResponseEntity.status(HttpStatus.OK).body(videos);
    }

    @GetMapping("/Created/{JWT}")
    public ResponseEntity<List<Content>> getAllUserCreatedContent(@PathVariable String JWT, @RequestParam(name = "activeDate", required = false) String activeDate){
        Sort sort = Sort.by("activeDate").descending();
        var userCreatedContentIds = userRepository.findById(jwtService.extractUserId(JWT)).orElseThrow().getCreatedContent();
        List<Content> videos;

        if (activeDate != null) {
            long timestampMillis = (long) (Double.parseDouble(activeDate) * 1000); // Convert to milliseconds
            videos = contentRepository.findByActiveDateBeforeAndContentIdIn(Instant.ofEpochMilli(timestampMillis), userCreatedContentIds, PageRequest.of(0, 50, sort));
        } else {
            videos = contentRepository.findByContentIdIn(userCreatedContentIds, PageRequest.of(0, 50, sort));
        }
        return ResponseEntity.status(HttpStatus.OK).body(videos);
    }

    @GetMapping("/All/User")
    public ResponseEntity<List<Content>> getAllCreatedContentFromUser(@RequestParam(name = "username") String username, @RequestParam(name = "activeDate", required = false) String activeDate){
        Sort sort = Sort.by("activeDate").descending();
        var userCreatedContentIds = userRepository.findByUsername(username).orElseThrow().getCreatedContent();
        List<Content> videos;

        if (activeDate != null) {
            long timestampMillis = (long) (Double.parseDouble(activeDate) * 1000); // Convert to milliseconds
            videos = contentRepository.findByActiveDateBeforeAndContentIdIn(Instant.ofEpochMilli(timestampMillis), userCreatedContentIds, PageRequest.of(0, 50, sort));
        } else {
            videos = contentRepository.findByContentIdIn(userCreatedContentIds, PageRequest.of(0, 50, sort));
        }
        return ResponseEntity.status(HttpStatus.OK).body(videos);
    }


    @GetMapping("/Purchased/{JWT}")
    public ResponseEntity<PurchasedContentResponse> getAllUserPurchasedContent(@PathVariable String JWT, @RequestParam(name = "paymentDate", required = false) String paymentDate){
        return ResponseEntity.status(HttpStatus.OK).body(contentService.getUserPurchasedContent(jwtService.extractUserId(JWT), paymentDate));
    }

    @GetMapping("Channel/Purchased/{channelName}")
    public ResponseEntity<PurchasedContentResponse> getAllChannelPurchasedContent(@PathVariable String channelName, @RequestParam(name = "paymentDate", required = false) String paymentDate){
        return ResponseEntity.status(HttpStatus.OK).body(contentService.getChannelPurchasedContent(channelName, paymentDate));
    }

    @GetMapping("/sortBuyers/{contentID}")
    public ResponseEntity<Map<String, Double>> getSortedBuyersByUsername(@PathVariable String contentID){
        return ResponseEntity.status(HttpStatus.OK).body(contentService.sortedBuyersByUsername(contentID));
    }

    @GetMapping("/sortBuyers/Channel/{contentID}")
    public ResponseEntity<Map<String, Double>> getSortedBuyersChannelName(@PathVariable String contentID){
        return ResponseEntity.status(HttpStatus.OK).body(contentService.sortedBuyersChannelName(contentID));
    }

    @GetMapping("/Active/Hype/{JWT}")
    public ResponseEntity<Double> getAllUserActiveHype(@PathVariable String JWT){
        return new ResponseEntity<>(contentService.getAllUserActiveHype(jwtService.extractUserId(JWT)), HttpStatus.OK);
    }

    @GetMapping("/Search/Videos")
    public ResponseEntity<List<Content>> searchVideos(@RequestParam(name = "query") String query, @RequestParam(name = "start") int start){
        return ResponseEntity.status(HttpStatus.OK).body(contentRepository.findByContentNameContainingIgnoreCaseAndContentEnum(query, ContentEnum.Active, PageRequest.of(start, 50)));
    }


    @PutMapping("/Edit")
    public ResponseEntity<String> editVideo(@RequestBody @Valid EditVideoRequest editVideoRequest, @RequestHeader("Authorization") String jwt){
        contentService.editVideo(editVideoRequest, jwtService.getJWTString(jwt));
        return new ResponseEntity<>("Video Changes Successful", HttpStatus.OK);
    }

    @PatchMapping("/Videos")
    public ResponseEntity<String> sentEmails(@RequestBody SentVideoRequest sentVideoRequest, @RequestHeader("Authorization") String jwt){
        contentService.sentVideos(sentVideoRequest, jwtService.getJWTString(jwt));
        return new ResponseEntity<>("Emails Sent", HttpStatus.OK);
    }

    @PutMapping("/Complete/InDevelopment")
    public ResponseEntity<String> completeVideo(@RequestBody @Valid CompleteVideoRequest completeVideoRequest, @RequestHeader("Authorization") String jwt){
        contentService.completeVideo(completeVideoRequest, jwtService.getJWTString(jwt));
        return new ResponseEntity<>("Video Successfully Completed", HttpStatus.OK);
    }

    @PatchMapping("/Report")
    public ResponseEntity<String> reportVideo(@RequestBody ReportVideoRequest reportVideoRequest, @RequestHeader("Authorization") String jwt){
        contentService.reportVideo(reportVideoRequest, jwtService.extractUserId(jwtService.getJWTString(jwt)));
        return new ResponseEntity<>("Video Successfully Reported", HttpStatus.OK);
    }

    @GetMapping("/Report/View")
    public ResponseEntity<List<Content>> viewReports(@RequestParam(name = "createdDate", required = false) String createdDate, @RequestParam(name = "reportRate", required = false) Double reportRate){
        Sort sort = Sort.by(Sort.Order.desc("reportRate"), Sort.Order.asc("createdDate"));
        List<Content> videos;

        if (reportRate != null) {
            long timestampMillis = (long) (Double.parseDouble(createdDate) * 1000); // Convert to milliseconds
            videos = contentRepository.findByReportRateLessThanAndCreatedDateAfterAndContentEnum(reportRate, Instant.ofEpochMilli(timestampMillis),ContentEnum.InProgress, PageRequest.of(0, 50, sort));
        } else {
            videos = contentRepository.findAllByContentEnumAndReportRateIsNotNull(ContentEnum.InProgress,PageRequest.of(0, 50, sort));
        }
        return ResponseEntity.status(HttpStatus.OK).body(videos);
    }

    @PatchMapping("/Report/Resolve")
    public ResponseEntity<Content> resolveReport(@RequestBody ResolveReportRequest resolveReportRequest){
        var content = contentRepository.findById(resolveReportRequest.getContentID()).orElseThrow();
        var reporterId = resolveReportRequest.getReporterIDs().get(0);
        var index = findContentReportsIndexByReporterID(content.getContentReports(), reporterId);
        if(index != -1){
            if(isAuction(content.getContentType())){
                var isSuccessful = contractServiceInterface.returnBidReport(content.getContentId(), reporterId);
                if(isSuccessful){
                    content.getContentReports().get(index).setResolved(true);
                    contentRepository.save(content);
                }
                else {
                    throw new SivantisException("Failed to Resolve All Reports");
                }
            }
            else{
                var isSuccessful = returnChannelPayment(content, reporterId);
                if(isSuccessful){
                    content.getContentReports().get(index).setResolved(true);
                    contentRepository.save(content);
                }
                else {
                    throw new SivantisException("Failed to Resolve All Reports");
                }
            }

        }
        double unresolvedReports = 0.0;
        for(ContentReports report: content.getContentReports()){
            if (!report.isResolved()){
                unresolvedReports++;
            }
        }
        if(unresolvedReports == 0.0){
            content.setReportRate(null);
        }
        else{
            double allBuyers = content.getListOfBuyerIds().size();
            var newReportRate = unresolvedReports / allBuyers * 100;
            content.setReportRate(newReportRate);
        }
        contentRepository.save(content);
        return new ResponseEntity<>(content, HttpStatus.OK);
    }

    @PatchMapping("/Report/Resolve/All")
    public ResponseEntity<Content> resolveAllReports(@RequestBody ResolveReportRequest resolveReportRequest){
        var content = contentRepository.findById(resolveReportRequest.getContentID()).orElseThrow();
        for(String reporterId: resolveReportRequest.getReporterIDs()){
            var index = findContentReportsIndexByReporterID(content.getContentReports(), reporterId);
            if(index != -1 && !content.getContentReports().get(index).isResolved()){
                if(isAuction(content.getContentType())){
                    var isSuccessful = contractServiceInterface.returnBidReport(content.getContentId(), reporterId);
                    if(isSuccessful){
                        content.getContentReports().get(index).setResolved(true);
                        contentRepository.save(content);
                    }
                    else {
                        throw new SivantisException("Failed to Resolve All Reports");
                    }
                }
                else{
                    var isSuccessful = returnChannelPayment(content, reporterId);
                    if(isSuccessful){
                        content.getContentReports().get(index).setResolved(true);
                        contentRepository.save(content);
                    }
                    else {
                        throw new SivantisException("Failed to Resolve All Reports");
                    }
                }

            }
        }
        double unresolvedReports = 0.0;
        for(ContentReports report: content.getContentReports()){
            if (!report.isResolved()){
                unresolvedReports++;
            }
        }
        if(unresolvedReports == 0.0){
            content.setReportRate(null);
        }
        else{
            double allBuyers = content.getListOfBuyerIds().size();
            var newReportRate = unresolvedReports / allBuyers * 100;
            content.setReportRate(newReportRate);
        }
        contentRepository.save(content);
        return new ResponseEntity<>(content, HttpStatus.OK);
    }

    @PatchMapping("/Report/Ignore")
    public ResponseEntity<Content> ignoreReport(@RequestBody ResolveReportRequest resolveReportRequest){
        var content = contentRepository.findById(resolveReportRequest.getContentID()).orElseThrow();
        var index = findContentReportsIndexByReporterID(content.getContentReports(), resolveReportRequest.getReporterIDs().get(0));
        if(index != -1){
            content.getContentReports().get(index).setResolved(true);
        }
        double unresolvedReports = 0.0;
        for(ContentReports report: content.getContentReports()){
            if (!report.isResolved()){
                unresolvedReports++;
            }
        }
        if(unresolvedReports == 0.0){
            content.setReportRate(null);
        }
        else{
            double allBuyers = content.getListOfBuyerIds().size();
            var newReportRate = unresolvedReports / allBuyers * 100;
            content.setReportRate(newReportRate);
        }
        contentRepository.save(content);
        return new ResponseEntity<>(content, HttpStatus.OK);
    }

    @PatchMapping("/Report/Ignore/All")
    public ResponseEntity<Content> ignoreAllReports(@RequestBody ResolveReportRequest resolveReportRequest){
        var content = contentRepository.findById(resolveReportRequest.getContentID()).orElseThrow();
        for(String reporterId: resolveReportRequest.getReporterIDs()){
            var index = findContentReportsIndexByReporterID(content.getContentReports(), reporterId);
            if(index != -1 && !content.getContentReports().get(index).isResolved()){
                content.getContentReports().get(index).setResolved(true);
            }
        }
        double unresolvedReports = 0.0;
        for(ContentReports report: content.getContentReports()){
            if (!report.isResolved()){
                unresolvedReports++;
            }
        }
        if(unresolvedReports == 0.0){
            content.setReportRate(null);
        }
        else{
            double allBuyers = content.getListOfBuyerIds().size();
            var newReportRate = unresolvedReports / allBuyers * 100;
            content.setReportRate(newReportRate);
        }
        contentRepository.save(content);
        return new ResponseEntity<>(content, HttpStatus.OK);
    }

    private int findContentReportsIndexByReporterID(ArrayList<ContentReports> contentReports, String reporterID) {
        if (contentReports != null) {
            for (int i = 0; i < contentReports.size(); i++) {
                ContentReports contentReport = contentReports.get(i);
                if (contentReport.getReporterID().equals(reporterID)) {
                    return i; // Found the index
                }
            }
        }
        return -1; // Not found
    }

    private boolean isAuction(String contentType){
        return contentType.equals("Invention") || contentType.equals("Innovation");
    }

    private boolean returnChannelPayment(Content content, String reportID){
        var channel = channelRepository.findById(userRepository.findById(reportID).orElseThrow().getChannels().get(0)).orElseThrow();
        var purchasedContent = paymentRepository.findById(channel.getPurchasedContent().get(content.getContentId())).orElseThrow();
        if(!purchasedContent.getStatus().equals(PaymentEnum.Purchased)){
            return true;
        }
        var optionalWatchNowPayLater = watchNowPayLaterRepository.findByChannelNameAndContentID(channel.getChannelName(),content.getContentId());
        var manaAmount = content.getListOfBuyerIds().get(channel.getChannelName());
        if(optionalWatchNowPayLater.isPresent() && optionalWatchNowPayLater.get().getWatchNowPayLaterEnum().equals(WatchNowPayLaterEnum.Unpaid)){
            var watchNowPayLater = optionalWatchNowPayLater.get();
            return contractServiceInterface.CancelWatchNowPayLaterPaymentReport(watchNowPayLater,reportID, content.getContentId(), channel.getChannelName(), manaAmount);
        }
        else{
            return contractServiceInterface.CancelPaymentReport(content.getCreatorID(), content.getContentId(), channel.getChannelName(), manaAmount);
        }
    }

    @DeleteMapping("/Delete/{JWT}/{contentID}")
    public ResponseEntity<String> deleteVideo(@PathVariable String JWT, @PathVariable String contentID){
        contentService.deleteCreatedContent(jwtService.extractUserId(JWT), contentID);
        return new ResponseEntity<>("Video Successfully Deleted", HttpStatus.OK);
    }

    @DeleteMapping("/Delete/Channel/Purchased/{JWT}/{contentID}/{channelID}")
    public ResponseEntity<String> deleteChannelPurchasedVideo(@PathVariable String JWT, @PathVariable String contentID, @PathVariable String channelID){
        contentService.deleteChannelPurchasedVideo(jwtService.extractUserId(JWT), contentID, channelID);
        return new ResponseEntity<>("Video Successfully Deleted", HttpStatus.OK);
    }

    @DeleteMapping("/Delete/Purchased/{JWT}/{contentID}")
    public ResponseEntity<String> deletePurchasedVideo(@PathVariable String JWT, @PathVariable String contentID){
        contentService.deletePurchasedVideo(jwtService.extractUserId(JWT), contentID);
        return new ResponseEntity<>("Video Successfully Deleted", HttpStatus.OK);
    }



}
