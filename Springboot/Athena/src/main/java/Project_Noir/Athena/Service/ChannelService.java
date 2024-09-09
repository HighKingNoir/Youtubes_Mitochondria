package Project_Noir.Athena.Service;

import Project_Noir.Athena.Controller.ServerSideEventController;
import Project_Noir.Athena.DTO.*;
import Project_Noir.Athena.Exception.SivantisException;
import Project_Noir.Athena.Model.*;
import Project_Noir.Athena.Repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.bson.types.ObjectId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChannelService {

    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final ChannelRepository channelRepository;
    private final ContractServiceInterface contractServiceInterface;
    private final ServerSideEventController serverSideEventController;
    private final WatchNowPayLaterRepository watchNowPayLaterRepository;
    private final JwtService jwtService;
    private final PaymentRepository paymentRepository;
    private final ContentRepository contentRepository;
    private final MessageService messageService;
    private final Integer minRequiredAWV = 1000;
    private final Integer maxNumberOfBuyers = 50;
    private final Integer maxLogoSize = 4096 * 1024;
    private final Integer maxNumberOfChannels = 1;

    public Channels requestChannel(ChannelRequest channelRequest, String JWT) {
        if(channelRepository.findByChannelNameIgnoreCase(channelRequest.getChannelName()).isPresent()){
            throw new SivantisException("A channel with this name already exist");
        }
        if(!hasImageExtension(channelRequest.getChannelLogo())){
            throw new SivantisException("Logo must be in .png, .jpeg, or jpg");
        }
        if(isImageSizeNotValid(channelRequest.getChannelLogo(), maxLogoSize)){
            throw new SivantisException("Logo too large");
        }
        isStreamerValid(channelRequest.getSteamerInfo());
        for(StreamerInfo streamerInfo: channelRequest.getSteamerInfo()){
            if(!validPlatform(streamerInfo.getPlatform())){
                throw new SivantisException("Invalid Platform " + streamerInfo.getPlatform());
            }
            if(streamerInfo.getUsername().length() > 50){
                throw new SivantisException("Invalid Username " + streamerInfo.getUsername());
            }
            if(channelRepository.findByPlatformAndStreamerId(streamerInfo.getPlatform(), streamerInfo.getStreamerId()).isPresent()){
                throw new SivantisException("Another channel already uses " + streamerInfo.getUsername() + " on " + streamerInfo.getPlatform());
            }
        }
        var userID = jwtService.extractUserId(JWT);
        var newChannel = Channels.builder()
                .channelId(ObjectId.get().toHexString())
                .channelStatus(ChannelStatus.Pending)
                .channelName(channelRequest.getChannelName())
                .channelLogo(channelRequest.getChannelLogo())
                .ownerID(userID)
                .channelSubscribers(new ArrayList<>())
                .timezone(channelRequest.getTimezone())
                .streamerInfo(channelRequest.getSteamerInfo())
                .purchasedContent(new HashMap<String, String>())
                .watchNowPayLaterIDs(new ArrayList<String>())
                .channelEvents(new ArrayList<>())
                .awvIsUpdated(true)
                .isBanned(false)
                .streamerChangeInfo(new ArrayList<>())
                .streamerChangeRequest(false)
                .nextAvailableResubmitDate(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();
        var user = userRepository.findById(userID).orElseThrow();
        if(user.getChannels().size() >= maxNumberOfChannels){
            throw new SivantisException("Sivantis is only allowing " + maxNumberOfChannels + " channel per User");
        }
        user.getChannels().add(newChannel.getChannelId());
        userRepository.save(user);
        channelRepository.save(newChannel);
        return newChannel;
    }

    private boolean validPlatform(String platform){
        return platform.equals("Youtube") || platform.equals("Twitch") || platform.equals("Kick");
    }


    public void approveChannelRequest(ApproveChannelRequest approveChannelRequest) {
        var channel = channelRepository.findById(approveChannelRequest.getChannelId()).orElseThrow();
        var highestAverageWeeklyViewers = paymentService.getMaxNumber(approveChannelRequest.getStreamerInfo().stream().map(StreamerInfo::getAverageWeeklyViewers).collect(Collectors.toList()));
        contractServiceInterface.createChannel( channel.getChannelName(), highestAverageWeeklyViewers);
        channel.setStreamerInfo(approveChannelRequest.getStreamerInfo());
        channel.setChannelStatus(ChannelStatus.Approved);
        channel.setAwvIsUpdated(true);
        channel.setApprovedDate(Instant.now());
        channelRepository.save(channel);
        messageService.approvedChannelMessage(channel);
    }

    public void disapproveChannelRequest(DisapproveChannelResponse disapproveChannelResponse){
        if(disapproveChannelResponse.getReason().length() > 5000){
            throw new SivantisException("Reason is too large");
        }
        var channel = channelRepository.findById(disapproveChannelResponse.getChannelID()).orElseThrow();
        channel.setChannelStatus(ChannelStatus.Disapproved);
        channel.setNextAvailableResubmitDate(Instant.now().plus(1, ChronoUnit.DAYS));
        channelRepository.save(channel);
        messageService.disapprovedChannelMessage(channel, disapproveChannelResponse.getReason());
    }

    public void resubmitChannelRequest(ResubmitChannelRequest resubmitChannelRequest){
        var channel = channelRepository.findByChannelName(resubmitChannelRequest.getChannelName()).orElseThrow();
        if(channel.getChannelStatus().equals(ChannelStatus.Pending)){
            throw new SivantisException("Request Already Submitted");
        }
        if(channel.getNextAvailableResubmitDate().isAfter(Instant.now())){
            throw new SivantisException("Resubmitting can only be done once a day");
        }
        if(resubmitChannelRequest.getSteamerInfo().isEmpty()){
            throw new SivantisException("Must include your live streaming info");
        }
        if(resubmitChannelRequest.getSteamerInfo().size() > 3){
            throw new SivantisException("Steamer info is limited to 3");
        }
        checkNewStreamerInfo(channel, resubmitChannelRequest.getSteamerInfo());
        channel.setChannelStatus(ChannelStatus.Pending);
        channel.setStreamerInfo(resubmitChannelRequest.getSteamerInfo());
        channelRepository.save(channel);
    }

    public List<Channels> getRandomApprovedChannels(List<String> excludedChannelIds) {
        List<Channels> approvedChannels = channelRepository.findByChannelStatusAndChannelIdNotIn("Approved", excludedChannelIds);
        Collections.shuffle(approvedChannels);
        return approvedChannels.size() > 50 ? approvedChannels.subList(0, 50) : approvedChannels;
    }

    public Channels editChannel(EditChannelRequest editChannelRequest, String JWT){
        var channel = channelRepository.findById(editChannelRequest.getChannelID()).orElseThrow();
        if(!channel.getOwnerID().equals(jwtService.extractUserId(JWT))){
            throw new SivantisException("You are not the owner of this channel");
        }
        if(editChannelRequest.getChannelDescription().length() > 1000){
            throw new SivantisException("Description is too large");
        }
        if(!editChannelRequest.getChannelBanner().isEmpty()){
            if(!hasImageExtension(editChannelRequest.getChannelBanner())){
                throw new SivantisException("Invalid Banner File");
            }
            var maxBannerSize = 6144 * 1024;
            if(isImageSizeNotValid(editChannelRequest.getChannelBanner(), maxBannerSize)){
                throw new SivantisException("Banner too large");
            }
        }
        if(!hasImageExtension(editChannelRequest.getChannelLogo())){
            throw new SivantisException("Invalid Logo File");
        }
        if(isImageSizeNotValid(editChannelRequest.getChannelLogo(), maxLogoSize)){
            throw new SivantisException("Logo too large");
        }
        if(editChannelRequest.getStreamerLinks().size() != channel.getStreamerInfo().size()){
            throw new SivantisException("Invalid number of StreamerLinks");
        }
        channel.setChannelLogo(editChannelRequest.getChannelLogo());
        channel.setChannelBanner(editChannelRequest.getChannelBanner());
        channel.setChannelDescription(editChannelRequest.getChannelDescription());
        for(int index = 0; index < editChannelRequest.getStreamerLinks().size(); index++){
            if(editChannelRequest.getStreamerLinks().get(index).getPlatform().equals("Youtube")){
                channel.getStreamerInfo().get(index).setYoutubeChannelId(editChannelRequest.getStreamerLinks().get(index).getYoutubeChannelId());
            }
        }
        channelRepository.save(channel);
        return channel;
    }


    public void addChannelEvents(String channelName, ChannelEvents channelEvents) {
        var channel = channelRepository.findByChannelName(channelName).orElseThrow();
        PriorityQueue<ChannelEvents> priorityQueue = new PriorityQueue<>(Comparator.comparing(ChannelEvents::getShowTime));
        priorityQueue.addAll(channel.getChannelEvents());
        priorityQueue.add(channelEvents);
        channel.setChannelEvents(priorityQueue.stream().toList());
        channelRepository.save(channel);
    }

    public Double payForContent(ChannelPaymentRequest channelPaymentRequest, String JWT) {
        var channel = channelRepository.findByChannelName(channelPaymentRequest.getChannelName()).orElseThrow();
        if(channel.getIsBanned()){
            throw new SivantisException("This channel is currently banned from buying Content");
        }
        if(!channel.getOwnerID().equals(jwtService.extractUserId(JWT))){
            throw new SivantisException("You are not the owner of this channel");
        }
        var highestAverageWeeklyViewers = paymentService.getMaxNumber(channel.getStreamerInfo().stream().map(StreamerInfo::getAverageWeeklyViewers).collect(Collectors.toList()));
        var threshold = (minRequiredAWV * .9);
        if(highestAverageWeeklyViewers < (threshold)){
            throw new SivantisException("This channel is below the minimum required Average weekly viewer threshold of " + threshold);
        }
        var content = contentRepository.findById(channelPaymentRequest.getContentID()).orElseThrow();
        if(content.getListOfBuyerIds().size() > maxNumberOfBuyers){
            throw new SivantisException("This video has exceed it's maximum number of Buyers of: " + maxNumberOfBuyers);
        }
        if(!content.getContentEnum().equals(ContentEnum.Active)){
            throw new SivantisException("This video is no longer active");
        }
        var purchasedContent = paymentService.findChannelPayment(channel.getChannelName(), content.getContentId());
        if(purchasedContent != null && !purchasedContent.getStatus().equals(PaymentEnum.RefundedPurchase)){
            throw new SivantisException("Content Already Purchased");
        }
        var manaPrice = serverSideEventController.latestValue;
        if(!contractServiceInterface.hasSufficientChannelBalance(channel.getChannelName(), manaPrice, highestAverageWeeklyViewers, content.getContentType(), 1)){
            throw new SivantisException("Insufficient Channel Balance");
        }
        this.contractServiceInterface.payForContent(
                channel,
                content.getCreatorID(),
                content.getContentId(),
                content.getContentType(),
                manaPrice,
                highestAverageWeeklyViewers
        );
        return manaAmountSpent(manaPrice, highestAverageWeeklyViewers, content.getContentType());
    }

    public Double watchNowPayLater(WatchNowPayLaterRequest watchNowPayLaterRequest, String JWT) {
        if(watchNowPayLaterRequest.getPaymentIncrements() < 1 || watchNowPayLaterRequest.getPaymentIncrements() > 8) {
            throw new SivantisException("Payment Increments entered is currently not Available");
        }
        var channel = channelRepository.findByChannelName(watchNowPayLaterRequest.getChannelName()).orElseThrow();
        if(channel.getIsBanned()){
            throw new SivantisException("This channel is currently banned from buying content");
        }
        if(!channel.getOwnerID().equals(jwtService.extractUserId(JWT))){
            throw new SivantisException("You are not the owner of this channel");
        }
        var highestAverageWeeklyViewers = paymentService.getMaxNumber(channel.getStreamerInfo().stream().map(StreamerInfo::getAverageWeeklyViewers).collect(Collectors.toList()));
        var threshold =  (minRequiredAWV * .9);
        if(highestAverageWeeklyViewers < threshold){
            throw new SivantisException("This channel is below the minimum required Average weekly viewer threshold of " + threshold);
        }
        var content = contentRepository.findById(watchNowPayLaterRequest.getContentID()).orElseThrow();
        if(content.getListOfBuyerIds().size() > maxNumberOfBuyers){
            throw new SivantisException("This video has exceed it's maximum number of Buyers of: " + maxNumberOfBuyers);
        }
        if(!content.getContentEnum().equals(ContentEnum.Active)){
            throw new SivantisException("This video is no longer active");
        }
        var purchasedContent = paymentService.findChannelPayment(channel.getChannelName(), content.getContentId());
        if(purchasedContent != null && !purchasedContent.getStatus().equals(PaymentEnum.RefundedPurchase)){
            throw new SivantisException("Content Already Purchased");
        }
        var manaPrice = serverSideEventController.latestValue;
        if(!contractServiceInterface.hasSufficientChannelBalance(channel.getChannelName(), manaPrice, highestAverageWeeklyViewers, content.getContentType(), watchNowPayLaterRequest.getPaymentIncrements())){
            throw new SivantisException("Insufficient Channel Balance");
        }
        this.contractServiceInterface.watchNowPayLater(
                channel,
                content.getCreatorID(),
                content.getContentId(),
                content.getContentType(),
                watchNowPayLaterRequest.getPaymentIncrements(),
                manaPrice,
                highestAverageWeeklyViewers
        );
        return manaAmountSpent(manaPrice, highestAverageWeeklyViewers, content.getContentType());
    }

    public void updateChannelAverageWeeklyViewers(ChannelStreamerInfoRequest channelStreamerInfoRequest) {
        var channel = channelRepository.findById(channelStreamerInfoRequest.getChannelId()).orElseThrow();
        if(channel.getStreamerInfo().size() != channelStreamerInfoRequest.getStreamerInfo().size()){
            throw new SivantisException("Streamer size imbalance");
        }
        for (int index= 0; index < channel.getStreamerInfo().size(); index++){
            channel.getStreamerInfo().get(index).setAverageWeeklyViewers(channelStreamerInfoRequest.getStreamerInfo().get(index).getAverageWeeklyViewers());
        }
        channel.setAwvIsUpdated(true);
        var highestAverageWeeklyViewers = paymentService.getMaxNumber(channel.getStreamerInfo().stream().map(StreamerInfo::getAverageWeeklyViewers).collect(Collectors.toList()));
        contractServiceInterface.updateAverageWeeklyViewers(channel.getChannelName(), highestAverageWeeklyViewers);
        channelRepository.save(channel);
    }

    @Scheduled(cron = "0 0 20 ? * SUN", zone = "America/New_York")
    @SchedulerLock(name = "averageWeeklyViewersUpdate", lockAtMostFor = "PT2M", lockAtLeastFor = "PT30S")
    public void averageWeeklyViewersUpdate() {
        var allApprovedChannels = channelRepository.findAllByChannelStatus(ChannelStatus.Approved);
        for (Channels channel: allApprovedChannels){
            channel.setAwvIsUpdated(false);
            channelRepository.save(channel);
        }
    }

    public void banChannel(String channelID, LocalDate unbanDate){
        var channel = channelRepository.findById(channelID).orElseThrow();
        channel.setIsBanned(true);
        channel.setUnbanDate(unbanDate);
        channelRepository.save(channel);
    }

    @Scheduled(cron = "0 0 6 * * *", zone = "America/New_York")
    @SchedulerLock(name = "checkBannedChannels", lockAtMostFor = "PT1M", lockAtLeastFor = "PT30S")
    public void checkBannedChannels(){
        LocalDate currentDate = LocalDate.now();
        var bannedChannels = getAllBannedChannels();
        for(Channels bannedChannel: bannedChannels){
            if(currentDate.isEqual(bannedChannel.getUnbanDate())){
               bannedChannel.setIsBanned(false);
               channelRepository.save(bannedChannel);
            }
        }
    }

    public void refundPayment(ChannelPaymentRequest channelRefundPaymentRequest, String JWT) {
            var channel = channelRepository.findByChannelName(channelRefundPaymentRequest.getChannelName()).orElseThrow();
            if(!channel.getOwnerID().equals(jwtService.extractUserId(JWT))){
                throw new SivantisException("You are not the owner of this channel");
            }
            var content = contentRepository.findById(channelRefundPaymentRequest.getContentID()).orElseThrow();
            if(!content.getContentEnum().equals(ContentEnum.Active)){
                throw new SivantisException("This video is no longer active");
            }
            var purchasedContent = paymentRepository.findById(channel.getPurchasedContent().get(content.getContentId())).orElseThrow();
            if(!purchasedContent.getStatus().equals(PaymentEnum.Purchased)){
                throw new SivantisException("This video was already refunded");
            }
            var manaAmount = content.getListOfBuyerIds().get(channelRefundPaymentRequest.getChannelName());
            var optionalWatchNowPayLater = watchNowPayLaterRepository.findByChannelNameAndContentID(channel.getChannelName(),channelRefundPaymentRequest.getContentID());
        if(optionalWatchNowPayLater.isPresent() && optionalWatchNowPayLater.get().getWatchNowPayLaterEnum().equals(WatchNowPayLaterEnum.Unpaid)){
            var watchNowPayLater = optionalWatchNowPayLater.get();
            contractServiceInterface.CancelWatchNowPayLaterPayment(watchNowPayLater,content.getCreatorID(), content.getContentId(), channel.getChannelName(), manaAmount);
        }
        else{
            contractServiceInterface.CancelPayment(content.getCreatorID(), content.getContentId(), channel.getChannelName(), manaAmount);
        }
    }

    public List<Channels> getAllActiveChannels(){
        return channelRepository.findAllByChannelStatus(ChannelStatus.Approved);
    }

    public List<WatchNowPayLater> getAllWatchNowPayLater(String channelName){
        return watchNowPayLaterRepository.findAllById(channelRepository.findByChannelName(channelName).orElseThrow().getWatchNowPayLaterIDs()).stream().toList();
    }

    private List<Channels> getAllBannedChannels(){
        return channelRepository.findAllByIsBanned(true);
    }

    private boolean hasImageExtension(String filename) {
        return filename.startsWith("data:image/jpg") || filename.startsWith("data:image/jpeg") || filename.startsWith("data:image/png");
    }

    public List<Channels> getChannelsWithEventsInQueue(int page) {
        Pageable pageable = PageRequest.of(page, 50, Sort.by(Sort.Order.desc("channelEvents.showTime")));
        return channelRepository.findByChannelEvents_ChannelEventStatusOrderByChannelEvents_ShowTimeDesc(
                ChannelEventStatus.InQueue, pageable);
    }

    public List<Channels> getChannelsWithEventsWatching(int page) {
        Pageable pageable = PageRequest.of(page, 50, Sort.by(Sort.Order.desc("channelEvents.showTime")));
        return channelRepository.findByChannelEvents_ChannelEventStatusOrderByChannelEvents_ShowTimeDesc(
                ChannelEventStatus.Watching, pageable);
    }

    public List<Channels> getChannelSubscriptionEvents(String userID) {
        return channelRepository.findAllById(userRepository.findById(userID).orElseThrow().getChannelSubscribedTo()).stream()
                .sorted(Comparator.comparing(
                        channel -> channel.getChannelEvents().stream()
                                .findFirst()
                                .map(ChannelEvents::getShowTime)
                                .orElse(Instant.MIN)
                ))
                .collect(Collectors.toList());
    }

    private void isStreamerValid(ArrayList<StreamerInfo> allStreamerInfo){
        if(allStreamerInfo == null || allStreamerInfo.isEmpty()){
            throw new SivantisException("Must include your live streaming info");
        }
        if(allStreamerInfo.size() > 3){
            throw new SivantisException("Steamer info is limited to 3");
        }
        for (StreamerInfo streamerInfo : allStreamerInfo) {
            if(streamerInfo.getStreamerId() == null || streamerInfo.getStreamerId().isEmpty()){
                throw new SivantisException(streamerInfo.getUsername() + " on " + streamerInfo.getPlatform() + " is Invalid");
            }
        }
    }

    private void checkNewStreamerInfo(Channels channel, ArrayList<StreamerInfo> allStreamerInfo){
        for (StreamerInfo streamerInfo : allStreamerInfo) {
            if (channel.getStreamerInfo().stream().noneMatch(info ->
                    info.getPlatform().equals(streamerInfo.getPlatform()) &&
                            info.getStreamerId().equals(streamerInfo.getStreamerId())))
            {
                if(channelRepository.findByPlatformAndStreamerId(streamerInfo.getPlatform(), streamerInfo.getStreamerId()).isPresent()){
                    throw new SivantisException("Another channel already uses " + streamerInfo.getUsername() + " on " + streamerInfo.getPlatform());
                }
            }
        }
    }

    private Double manaAmountSpent(Double manaPrice, Double highestAverageWeeklyViewers, String contentType){
        return highestAverageWeeklyViewers * contractServiceInterface.priceOfContent(contentType) / (manaPrice * 100);
    }

    private boolean isImageSizeNotValid(String imageBase64, int maxSizeInBytes) {
        try {
            // Decode the base64 image string to binary data
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64.split(",")[1].getBytes(StandardCharsets.UTF_8));

            // Calculate the size of the binary data
            int imageSizeInBytes = imageBytes.length;

            // Check if the size is within the limit
            return imageSizeInBytes > maxSizeInBytes;
        } catch (Exception e) {
            return true;
        }
    }

}
