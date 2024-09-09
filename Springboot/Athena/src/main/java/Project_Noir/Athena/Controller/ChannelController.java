package Project_Noir.Athena.Controller;

import Project_Noir.Athena.DTO.*;
import Project_Noir.Athena.Model.*;
import Project_Noir.Athena.Repo.ChannelRepository;
import Project_Noir.Athena.Repo.UserRepository;
import Project_Noir.Athena.Service.ChannelService;
import Project_Noir.Athena.Service.JwtService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.web3j.protocol.exceptions.TransactionException;

import java.io.IOException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/Channel")
@AllArgsConstructor
@Slf4j
public class ChannelController {

    private final ChannelService channelService;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @PostMapping("/NewChannel")
    public ResponseEntity<Channels> requestNewChannel(@RequestBody @Valid ChannelRequest channelRequest, @RequestHeader("Authorization") String jwt) throws IOException {
        return new ResponseEntity<>(channelService.requestChannel(channelRequest, jwtService.getJWTString(jwt)), HttpStatus.CREATED);
    }

    @PutMapping("/Resubmit")
    public ResponseEntity<String> resubmitNewChannel(@RequestBody ResubmitChannelRequest resubmitChannelRequest) {
        channelService.resubmitChannelRequest(resubmitChannelRequest);
        return new ResponseEntity<>("Channel Request Successful. Please allow 48 hours for status update", HttpStatus.CREATED);
    }

    @GetMapping("/Exist/Channel/{channelName}")
    // @dev Generates a new user entity
    public ResponseEntity<Boolean> channelNameExist(@PathVariable String channelName){
        return new ResponseEntity<>(channelRepository.findByChannelNameIgnoreCase(channelName).isPresent(),HttpStatus.OK);
    }

    @GetMapping("/Exist/Channel/{platform}/{streamerId}")
    // @dev Generates a new user entity
    public ResponseEntity<Boolean> channelExist(@PathVariable String platform, @PathVariable String streamerId){
        return new ResponseEntity<>(channelRepository.findByPlatformAndStreamerId(platform, streamerId).isPresent(),HttpStatus.OK);
    }

    @GetMapping("/Single/{channelName}")
    public ResponseEntity<Channels> getChannel(@PathVariable String channelName){
        return new ResponseEntity<>(channelRepository.findByChannelName(channelName).orElseThrow(), HttpStatus.OK);
    }

    @GetMapping("/Active")
    public ResponseEntity<List<Channels>> getAllActiveChannels(){
        return new ResponseEntity<>(channelService.getAllActiveChannels(), HttpStatus.OK);
    }

        @GetMapping("/Active/Random")
        public ResponseEntity<List<Channels>> getRandomActiveChannels(
                @RequestParam(value = "excludedChannelIds", required = false) List<String> excludedChannelIds) {
            List<Channels> randomChannels;
            if (excludedChannelIds == null) {
                randomChannels = channelService.getRandomApprovedChannels(List.of());
            }
            else if(excludedChannelIds.size() > 1000){
                return new ResponseEntity<>(List.of(), HttpStatus.OK);
            }
            else{
                randomChannels = channelService.getRandomApprovedChannels(excludedChannelIds);
            }
            return new ResponseEntity<>(randomChannels, HttpStatus.OK);
        }

    @GetMapping("/User")
    public ResponseEntity<List<Channels>> getAllUserChannels(@RequestHeader("Authorization") String jwt){
        return new ResponseEntity<>(channelRepository.findAllById(userRepository.findById(jwtService.extractUserId(jwtService.getJWTString(jwt))).orElseThrow().getChannels()), HttpStatus.OK);
    }

    @GetMapping ("/ChannelSubscriptions")
    public ResponseEntity<List<Channels>> getChannelSubscriptions(@RequestHeader("Authorization") String jwt){
        var channels = channelRepository.findAllById(userRepository.findById(jwtService.extractUserId(jwtService.getJWTString(jwt))).orElseThrow().getChannelSubscribedTo());
        channels.sort(Comparator.comparing(Channels::getChannelName));
        return new ResponseEntity<>(channels, HttpStatus.OK);
    }


    @GetMapping("/Request")
    public ResponseEntity<List<Channels>> getChannelRequest(@RequestParam(name = "resubmissionDate", required = false) String resubmissionDate){
        Sort sort = Sort.by("nextAvailableResubmitDate").ascending();
        List<Channels> channels;

        if (resubmissionDate != null) {
            long timestampMillis = (long) (Double.parseDouble(resubmissionDate) * 1000); // Convert to milliseconds
            channels = channelRepository.findByNextAvailableResubmitDateAfterAndChannelStatus(Instant.ofEpochMilli(timestampMillis), ChannelStatus.Pending ,PageRequest.of(0, 50, sort));
        } else {
            channels = channelRepository.findByChannelStatus(ChannelStatus.Pending, PageRequest.of(0, 50, sort));
        }

        return new ResponseEntity<>(channels, HttpStatus.OK);
    }

    @PutMapping("/Request/Approve")
    public ResponseEntity<String> approveChannel(@RequestBody ApproveChannelRequest approveChannelRequest) throws TransactionException, IOException, ExecutionException, InterruptedException {
        channelService.approveChannelRequest(approveChannelRequest);
        return new ResponseEntity<>("Channel has been Approved", HttpStatus.ACCEPTED);
    }

    @PutMapping("/Request/Disapprove")
    public ResponseEntity<String> disapproveChannel(@RequestBody DisapproveChannelResponse disapproveChannelResponse){
        channelService.disapproveChannelRequest(disapproveChannelResponse);
        return new ResponseEntity<>("Channel has been Disapproved", HttpStatus.OK);
    }

    @GetMapping("/Change/View")
    public ResponseEntity<List<Channels>> getChannelStreamerInfo(@RequestParam(name = "start") int start){
        return new ResponseEntity<>(channelRepository.findByStreamerChangeRequest(true, PageRequest.of(start, 50)), HttpStatus.OK);
    }

//    @PutMapping("/Change")
//    public ResponseEntity<Channels> changeChannelStreamerInfo(){
//        return new ResponseEntity<>(channelService.editChannel(editChannelRequest, jwtService.getJWTString(jwt)), HttpStatus.OK);
//    }
//
//    @PutMapping("/Change/Request")
//    public ResponseEntity<Channels> changeChannelStreamerInfo(){
//        return new ResponseEntity<>(channelService.editChannel(editChannelRequest, jwtService.getJWTString(jwt)), HttpStatus.OK);
//    }

    @PutMapping("/Edit")
    public ResponseEntity<Channels> editChannel(@RequestBody EditChannelRequest editChannelRequest, @RequestHeader("Authorization") String jwt){
        return new ResponseEntity<>(channelService.editChannel(editChannelRequest, jwtService.getJWTString(jwt)), HttpStatus.OK);
    }

    @PutMapping("/Ban")
    public String banChannel(@RequestBody Integer duration){
        return "";
    }

    @GetMapping("/AWV/Update/Count")
    public ResponseEntity<Long> getAllChannelUpdateCount(){
        return new ResponseEntity<>(channelRepository.countByChannelStatusAndAwvIsUpdated(ChannelStatus.Approved, false), HttpStatus.OK);
    }

    //When you remove an element from the array, you also have to subtract 1 from start
    @GetMapping("/AWV/Update")
    public ResponseEntity<List<Channels>> getAllChannelsNeedingUpdate(@RequestParam(name = "start") int start){
        return new ResponseEntity<>(channelRepository.findByChannelStatusAndAwvIsUpdated(ChannelStatus.Approved, false, PageRequest.of(start, 50)), HttpStatus.OK);
    }

    @GetMapping("/Search/Channels")
    public ResponseEntity<List<Channels>> searchVideos(@RequestParam(name = "query") String query, @RequestParam(name = "start") int start){
        return ResponseEntity.status(HttpStatus.OK).body(channelRepository.findByChannelNameContainingIgnoreCaseAndChannelStatus(query, ChannelStatus.Approved, PageRequest.of(start, 50)));
    }

    @PutMapping("/AWV/Update/Channel")
    public ResponseEntity<String> updateAverageWeeklyViewers(@RequestBody ChannelStreamerInfoRequest channelStreamerInfoRequest) throws TransactionException, IOException, ExecutionException, InterruptedException {
        channelService.updateChannelAverageWeeklyViewers(channelStreamerInfoRequest);
        return new ResponseEntity<>("Channel has been Updated", HttpStatus.OK);
    }

    @PostMapping("/Pay")
    public ResponseEntity<Double> payForContent(@RequestBody ChannelPaymentRequest channelPaymentRequest, @RequestHeader("Authorization") String jwt) throws TransactionException, IOException, ExecutionException, InterruptedException {
        return new ResponseEntity<>(channelService.payForContent(channelPaymentRequest, jwtService.getJWTString(jwt)), HttpStatus.OK);
    }

    @PostMapping("/WatchNowPayLater")
    public ResponseEntity<Double> watchNowPayLater(@RequestBody WatchNowPayLaterRequest watchNowPayLaterRequest, @RequestHeader("Authorization") String jwt) throws TransactionException, IOException, ExecutionException, InterruptedException {
        return new ResponseEntity<>(channelService.watchNowPayLater(watchNowPayLaterRequest, jwtService.getJWTString(jwt)), HttpStatus.OK);
    }

    @PostMapping("/Refund")
    public ResponseEntity<String> refundContent(@RequestBody ChannelPaymentRequest channelRefundPaymentRequest, @RequestHeader("Authorization") String jwt) throws TransactionException, IOException, ExecutionException, InterruptedException {
        channelService.refundPayment(channelRefundPaymentRequest, jwtService.getJWTString(jwt));
        return new ResponseEntity<>("Refund Successful", HttpStatus.OK);
    }
}
