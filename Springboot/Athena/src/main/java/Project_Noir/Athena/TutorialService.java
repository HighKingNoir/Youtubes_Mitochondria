package Project_Noir.Athena;

import Project_Noir.Athena.Controller.ServerSideEventController;
import Project_Noir.Athena.DTO.ApproveChannelRequest;
import Project_Noir.Athena.Exception.SivantisException;
import Project_Noir.Athena.Model.*;
import Project_Noir.Athena.Repo.*;
import Project_Noir.Athena.Service.*;
import Project_Noir.Athena.SmartContracts.BidService.BidService;
import Project_Noir.Athena.SmartContracts.TestManaContract.TestManaContract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.web3j.abi.datatypes.Function;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TutorialService {
    @Value("${private.key.company}")
    private String privateKeyCompany;

    @Value("${contract.multiSend.address}")
    private String multiSendAddress;

    @Value("${private.key.one}")
    private String privateKeyOne;

    @Value("${private.key.two}")
    private String privateKeyTwo;

    @Value("${private.key.three}")
    private String privateKeyThree;

    @Value("${private.key.four}")
    private String privateKeyFour;

    @Value("${private.key.five}")
    private String privateKeyFive;

    @Value("${private.key.six}")
    private String privateKeySix;

    @Value("${contract.channel.address}")
    private String ChannelAddress;
    @Value("${contract.bid.address}")
    private String BidAddress;
    private Integer selectedPrivateKey = 1;
    private final Web3j web3j = Web3j.build(new HttpService());
    private final UserRepository userRepository;
    private final ChannelService channelService;
    private final ChannelRepository channelRepository;
    private final ContentService contentService;
    private final MongoTemplate mongoTemplate;
    private final ContentRepository contentRepository;
    private final ContractServiceInterface contractServiceInterface;
    private final ServerSideEventController serverSideEventController;
    private final PaymentRepository paymentRepository;
    private final TransactionVerificationPackageRepository transactionVerificationPackageRepository;
    private final ClientSideMultiSendContractService clientSideMultiSendContractService;
    private final MessageRepository messageRepository;
    private final MultiSendHelperService multiSendHelperService;
    private final MessageService messageService;
    private final Double auctionModifier = .02;
    private final PasswordEncoder passwordEncoder;
    private final Double buyModifier = .1;
    @Value("${contract.testMana.address}")
    private String TestManaContractAddress;
    private static final Scanner scanner = new Scanner(System.in);

    public void tutorial() {
        final String[] adminId = new String[1];
        final List<String>[] userIds = new List[1];
        final List<String>[] channelIds = new List[1];
        final List<String>[] videoIds = new List[1];

        step(
                "Setting Admin Account",
                "This step creates the admin account, which will have full permissions to manage the system. Username: admin, Password: Admin#123.",
                "Setting Allowance",
                () -> adminId[0] = createAdminAccount()
        );

        step(
                "Setting Allowance",
                "This step allows the company wallet the ability to interact with the Bid and Channel contracts using Test Mana.",
                "Creating 20 users",
                this::allowance
        );

        step(
                "Creating 20 Users",
                "You can log into any of these accounts using Username: user'N', Password: User#123, where 'N' is any number between 0-19.",
                "Creating 20 Channels",
                () -> userIds[0] = create20Users()
        );

        step(
                "Creating 20 Channels",
                "Channels are used to purchase Short Films, Movies, Sports, and Concerts.",
                "Approving 20 Channels",
                () -> channelIds[0] = create20Channels(userIds[0], adminId[0])
        );

        step(
                "Approving 20 Channels",
                "This step adds those channels to the blockchain.",
                "Creating 20 Videos",
                () -> approveChannels(channelIds[0])
        );

        step(
                "Creating 20 Videos",
                "Attribution - Joshua Rawson-Harris",
                "Funding Channels",
                () -> videoIds[0] = create20Videos(adminId[0])
        );

        step(
                "Funding Channels",
                "This step adds Test Mana to all the channels.",
                "Purchasing Videos",
                () -> fundChannels(channelIds[0])
        );

        step(
                "Purchasing Videos",
                "This step populates each video with buyers.",
                "Daily Check Up",
                () -> purchaseContent(channelIds[0], videoIds[0], userIds[0])
        );

        step(
                "Daily Check Up",
                "This step simulates the day before your release date. This is where you share access to your video with the buyers.",
                "Daily Check Up 2",
                contentService::dailyCheckup
        );

        step(
                "Daily Check Up 2",
                "This step simulates two days after your release date. This is where you'll get paid for Auctions and the ability to rank up.",
                "End of Tutorial",
                () -> {
                    var allContent = contentRepository.findAllById(videoIds[0]);
                    var allMessages = messageRepository.findByMessageEnum(MessageEnum.YoutubeEmails);
                    for (Messages message: allMessages){
                        message.setHasRead(true);
                    }
                    for (Content content : allContent) {
                        content.setSentEmails(true);
                        content.setReleaseDate(LocalDate.now().minusDays(2));
                        messageService.sentVideo(content);

                    }
                    messageRepository.saveAll(allMessages);
                    contentRepository.saveAll(allContent);
                    contentService.dailyCheckup();
                }
        );

        System.out.println("\n End of Tutorial");
    }

    private void waitForEnter() {
        System.out.print("\nPress ENTER to continue...");
        System.out.flush(); // Ensure text prints before pausing
        scanner.nextLine();
    }

    private void step(String title, String description, String next, Runnable action) {
        System.out.println("\n=== " + title + " ===");
        System.out.println("Description: " + description);
        System.out.println("Next Step: " + next);
        action.run();
        waitForEnter();
    }


    private void createAuctions(List<String> videoIds) {
        var admin = userRepository.findByUsername("admin").orElseThrow();
        var allContent = contentRepository.findAllById(videoIds);
        for (Content content: allContent){
            if(isAuction(content.getContentType())){
                contractServiceInterface.createNewAuction(content.getContentId(), content.getNumbBidders(), content.getStartingCost(), admin.getPersonalWallet());
            }
        }
    }

    private void purchaseContent(List<String> channelIds, List<String> videoIds, List<String> userIds) {
        BigInteger gasPrice;
        BigInteger gasLimit = BigInteger.valueOf(5000000L);
        try {
            gasPrice = web3j.ethGasPrice().send().getGasPrice();
        } catch (IOException e) {
            gasPrice = BigInteger.valueOf(40000000000L);
        }
        var swap = true;
        var bidServiceContract = BidService.load(BidAddress, web3j, org.web3j.crypto.Credentials.create(privateKeyCompany), new StaticGasProvider(gasPrice, gasLimit));
        var allContent = contentRepository.findAllById(videoIds);
        var allChannels = channelRepository.findAllById(channelIds);
        for (Content content: allContent){
            if (isAuction(content.getContentType())){
                for (String userId: userIds){
                    int randomCost = ThreadLocalRandom.current().nextInt(60, 100);
                    var manaPrice = serverSideEventController.latestValue;
                    var manaAmount = randomCost / manaPrice;
                    BigDecimal dollarAmount = BigDecimal.valueOf(randomCost);
                    BigDecimal manaAmountInWei = dollarAmount.multiply(BigDecimal.TEN.pow(18)).divide(BigDecimal.valueOf(manaPrice), 0, RoundingMode.DOWN);
                    var user = userRepository.findById(userId).orElseThrow();
                    String transactionHash = null;
                    try {
                        transactionHash = bidServiceContract.placeBid(content.getContentId(), user.getUserId(), manaAmountInWei.toBigInteger()).send().getTransactionHash();
                    } catch (Exception ignored) {
                    }
                    payment(user,content, String.valueOf(manaAmount), randomCost, transactionHash);
                }
            }
            else {
                if (swap){
                    for (Channels channel: allChannels) {
                        payForContent(channel, content);
                    }
                    swap = false;
                }
                else {
                    for (Channels channel: allChannels) {
                        watchNowPayLater(channel, content);
                    }
                    swap = true;
                }
            }
        }
    }

    private void watchNowPayLater(Channels channel, Content content) {
        if(channel.getIsBanned()){
            throw new SivantisException("This channel is currently banned from buying content");
        }
        var highestAverageWeeklyViewers = getMaxNumber(channel.getStreamerInfo().stream().map(StreamerInfo::getAverageWeeklyViewers).collect(Collectors.toList()));
        if(content.getListOfBuyerIds().size() == 50){
            throw new SivantisException("This video has exceed it's maximum number of Buyers of: " + 50);
        }
        if(!content.getContentEnum().equals(ContentEnum.Active)){
            throw new SivantisException("This video is no longer active");
        }
        var purchasedContent = findChannelPayment(channel.getChannelName(), content.getContentId());
        if(purchasedContent == null){
            boolean lockAcquired = tryCreatePaymentLock(channel.getChannelName(), content.getContentId());
            if (!lockAcquired) {
                throw new SivantisException("Another payment is already being processed for this content");
            }
        }
        else {
            if(purchasedContent.getStatus().equals(PaymentEnum.PendingRefund)){
                throw new SivantisException("Refund is Currently Pending");
            }
            if(purchasedContent.getStatus().equals(PaymentEnum.Purchased)){
                throw new SivantisException("Content Already Purchased");
            }
            if (!tryMarkingPaymentAsPendingPurchase(purchasedContent.getPaymentId())) {
                throw new SivantisException("Purchase already processed or is pending");
            }
        }
        var manaPrice = serverSideEventController.latestValue;
        if(!clientSideMultiSendContractService.hasSufficientChannelBalance(channel.getChannelName(), manaPrice, highestAverageWeeklyViewers, content.getContentType(), 4, channelService.pendingChannelPurchasesManaAmount(channel, manaPrice, highestAverageWeeklyViewers))){
            throw new SivantisException("Insufficient Channel Balance");
        }
        this.contractServiceInterface.watchNowPayLater(
                channel,
                content,
                4,
                manaPrice,
                highestAverageWeeklyViewers
        );
    }

    private boolean tryMarkingPaymentAsPendingPurchase(String paymentId) {
        Query query = new Query(Criteria
                .where("_id").is(paymentId)
                .and("status").is(PaymentEnum.RefundedPurchase.name()));

        Update update = new Update()
                .set("status", PaymentEnum.PendingPurchase.name());

        Payment updatedPayment = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Payment.class
        );

        return updatedPayment != null;
    }


    private boolean tryCreatePaymentLock(String channelName, String contentId) {
        String lockId = channelName + "::" + contentId;

        Query query = new Query(Criteria.where("_id").is(lockId));
        Update update = new Update().setOnInsert("createdAt", Instant.now());

        PaymentLock lock = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().upsert(true).returnNew(false),
                PaymentLock.class
        );

        return lock == null; // If null, we just inserted it → this request "won" the lock
    }

    private void payForContent(Channels channel, Content content){
        var highestAverageWeeklyViewers = getMaxNumber(channel.getStreamerInfo().stream().map(StreamerInfo::getAverageWeeklyViewers).collect(Collectors.toList()));
        if(content.getListOfBuyerIds().size() == 50){
            throw new SivantisException("This video has exceed it's maximum number of Buyers of: " + 50);
        }
        if(!content.getContentEnum().equals(ContentEnum.Active)){
            throw new SivantisException("This video is no longer active");
        }
        var purchasedContent = findChannelPayment(channel.getChannelName(), content.getContentId());
        if(purchasedContent == null){
            boolean lockAcquired = tryCreatePaymentLock(channel.getChannelName(), content.getContentId());
            if (!lockAcquired) {
                throw new SivantisException("Another payment is already being processed for this content");
            }
        }
        else {
            if(purchasedContent.getStatus().equals(PaymentEnum.PendingRefund)){
                throw new SivantisException("Refund is Currently Pending");
            }
            if(purchasedContent.getStatus().equals(PaymentEnum.Purchased)){
                throw new SivantisException("Content Already Purchased");
            }
            if (!tryMarkingPaymentAsPendingPurchase(purchasedContent.getPaymentId())) {
                throw new SivantisException("Purchase already processed or is pending");
            }
        }
        var manaPrice = serverSideEventController.latestValue;
        if(!clientSideMultiSendContractService.hasSufficientChannelBalance(channel.getChannelName(), manaPrice, highestAverageWeeklyViewers, content.getContentType(), 1, channelService.pendingChannelPurchasesManaAmount(channel, manaPrice, highestAverageWeeklyViewers))){
            throw new SivantisException("Insufficient Channel Balance");
        }
        this.contractServiceInterface.payForContent(
                channel,
                content,
                manaPrice,
                highestAverageWeeklyViewers
        );
    }

    public Double getMaxNumber(Collection<Number> numbers){
        var maxNumber = 0.0;
        for (Number number : numbers) {
            double currentNumber = number.doubleValue();
            if (currentNumber > maxNumber) {
                maxNumber = currentNumber;
            }
        }
        return maxNumber;
    }

    public Payment findChannelPayment(String channelName, String contentID){
        if(channelRepository.findByChannelName(channelName).orElseThrow().getPurchasedContent().get(contentID) == null){
            return null;
        }
        return paymentRepository.findById(channelRepository.findByChannelName(channelName).orElseThrow().getPurchasedContent().get(contentID)).orElseThrow();
    }

    private void payment(Users user, Content content, String manaAmount, int dollarAmount, String transactionHash){
        var Payment = findPayment(user.getUserId(), content.getContentId());
        double totalManaAmount = Double.parseDouble(manaAmount);
        var userID = user.getUserId();
        if(Payment != null){
            if(!Payment.getStatus().equals(PaymentEnum.RefundedPurchase)){
                throw new SivantisException("Content was already purchased");
            }
            Payment.setManaAmount(manaAmount);
            Payment.setDollarAmount(BigDecimal.valueOf(dollarAmount));
            Payment.setStatus(PaymentEnum.VerifyingTransaction);
            Payment.setTransactionHash(transactionHash);
            Payment.setPaymentDate(Instant.now());
            Payment.setManaToCreator(totalManaAmount * .9);
            addToListOfBuyersAndHype(content.getContentId(), userID, Payment.getManaAmount(), Payment.getDollarAmount(), auctionModifier);
            paymentRepository.save(Payment);
            messageService.verifyingTransactionMessage(userID, TransactionVerificationFunctionEnum.PlaceBid, transactionHash, content);
            addTransactionVerificationToQueue(
                    content.getContentId(),
                    userID,
                    transactionHash,
                    TransactionVerificationFunctionEnum.PlaceBid,
                    Convert.toWei(new BigDecimal(manaAmount), Convert.Unit.ETHER)
                            .toBigIntegerExact()
            );
        }
        else {
            var newPayment = Project_Noir.Athena.Model.Payment.builder()
                    .paymentId(ObjectId.get().toHexString())
                    .contentId(content.getContentId())
                    .userId(userID)
                    .manaAmount(manaAmount)
                    .dollarAmount(BigDecimal.valueOf(dollarAmount))
                    .refundDate(null)
                    .paymentDate(Instant.now())
                    .status(PaymentEnum.VerifyingTransaction)
                    .manaToCreator(totalManaAmount * .9)
                    .paymentRevertInfo(new HashMap<>())
                    .transactionHash(transactionHash)
                    .build();
            addPurchasedContent(userID, newPayment.getContentId(), newPayment.getPaymentId());
            addToListOfBuyersAndHype(content.getContentId(), userID, newPayment.getManaAmount(), newPayment.getDollarAmount(), auctionModifier);
            paymentRepository.save(newPayment);
            messageService.verifyingTransactionMessage(userID, TransactionVerificationFunctionEnum.PlaceBid, transactionHash, content);
            addTransactionVerificationToQueue(
                    content.getContentId(),
                    userID,
                    transactionHash,
                    TransactionVerificationFunctionEnum.PlaceBid,
                    Convert.toWei(new BigDecimal(manaAmount), Convert.Unit.ETHER)
                            .toBigIntegerExact()
            );
        }
    }

    public void addToListOfBuyersAndHype(String contentId, String userIdOrChannelName, String manaAmount, BigDecimal dollarAmount, Double modifier){
        Query query = new Query(Criteria.where("_id").is(contentId));

        Update update = new Update()
                .set("listOfBuyerIds." + userIdOrChannelName, manaAmount) // Add/overwrite buyer
                .inc("hype", dollarAmount.multiply(BigDecimal.valueOf(modifier))); // Increment hype

        mongoTemplate.updateFirst(query, update, Content.class);
    }

    public void addPurchasedContent(String userId, String contentId, String paymentId) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update().set("purchasedContent." + contentId, paymentId);

        mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Users.class
        );
    }

    public Payment findPayment(String userID, String contentID){
        if(userRepository.findById(userID).orElseThrow().getPurchasedContent().get(contentID) == null){
            return null;
        }
        return paymentRepository.findById(userRepository.findById(userID).orElseThrow().getPurchasedContent().get(contentID)).orElseThrow();
    }

    private void fundChannels(List<String> channelIds) {
        BigInteger gasPrice;
        BigInteger gasLimit = BigInteger.valueOf(5000000L);
        try {
            gasPrice = web3j.ethGasPrice().send().getGasPrice();
        } catch (IOException e) {
            gasPrice = BigInteger.valueOf(40000000000L);
        }
        var channelServiceContract = Project_Noir.Athena.SmartContracts.ChannelService.ChannelService.load(ChannelAddress, web3j, org.web3j.crypto.Credentials.create(privateKeyCompany), new StaticGasProvider(gasPrice, gasLimit));
        for (String channelId: channelIds){
            var channelName = channelRepository.findById(channelId).orElseThrow().getChannelName();
            try {
                channelServiceContract.fundChannel(channelName,  BigInteger.valueOf(999999999999999999L).multiply(BigInteger.valueOf(10000000000L))).send();
            } catch (Exception ignored) {

            }
        }
    }

    private org.web3j.crypto.Credentials getCredentials(){
        switch (selectedPrivateKey){
            case 1 -> {
                selectedPrivateKey++;
                return org.web3j.crypto.Credentials.create(privateKeyOne);
            }
            case 2 -> {
                selectedPrivateKey++;
                return org.web3j.crypto.Credentials.create(privateKeyTwo);
            }
            case 3 -> {
                selectedPrivateKey++;
                return org.web3j.crypto.Credentials.create(privateKeyThree);
            }
            case 4 -> {
                selectedPrivateKey++;
                return org.web3j.crypto.Credentials.create(privateKeyFour);

            }
            case 5 -> {
                selectedPrivateKey++;
                return org.web3j.crypto.Credentials.create(privateKeyFive);
            }
            default -> {
                selectedPrivateKey = 1;
                return org.web3j.crypto.Credentials.create(privateKeySix);
            }
        }
    }

    private void allowance(){
        BigInteger gasPrice;
        BigInteger gasLimit = BigInteger.valueOf(5000000L);
        try {
            gasPrice = web3j.ethGasPrice().send().getGasPrice();
        } catch (IOException e) {
            gasPrice = BigInteger.valueOf(40000000000L);
        }
        BigInteger MAX_UINT256 = new BigInteger(
                "115792089237316195423570985008687907853269984665640564039457584007913129639935"
        );
        var testManaContract = TestManaContract.load(TestManaContractAddress, web3j, org.web3j.crypto.Credentials.create(privateKeyCompany), new StaticGasProvider(gasPrice, gasLimit));
        try {
            testManaContract.approve(ChannelAddress, MAX_UINT256).send();
            testManaContract.approve(BidAddress, MAX_UINT256).send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ArrayList<String> create20Videos(String adminId) {
        var contentIds = new ArrayList<String>();
        var User = userRepository.findById(adminId).orElseThrow();
        var contentTypeList = new ArrayList<String>();
        contentTypeList.add("Short Film");
        contentTypeList.add("Movies");
        contentTypeList.add("Sports");
        contentTypeList.add("Invention");
        contentTypeList.add("Innovation");

        for (int i = 0; i < 20; i++) {
            int random = ThreadLocalRandom.current().nextInt(0, 5);
            var contentType = contentTypeList.get(random);
            String imagePath = "http://localhost:8080/images/image" + i + ".jpg";
            var Content = mapContentRequest(contentType, User.getUserId(),User.getIsViolator());
            Content.setYoutubeTrailerVideoID(("uuEWOAlgUVI"));
            Content.setThumbnail(imagePath);
            Content.setYoutubeProfilePicture("http://localhost:8080/images/Logo.png");
            Content.setContentName("video" + i);
            Content.setYoutubeMainVideoID("youtubeMainVideoID" + i);
            Content.setContentEnum(ContentEnum.PendingConfirmation);
            if(isAuction(Content.getContentType())){
                int randomBid = ThreadLocalRandom.current().nextInt(1, 26);
                int randomCost = ThreadLocalRandom.current().nextInt(10, 50);
                Content.setNumbBidders(randomBid);
                Content.setStartingCost(randomCost);
                contractServiceInterface.createNewAuction(Content.getContentId(), Content.getNumbBidders(), Content.getStartingCost(), User.getPersonalWallet());
            }
            else {
                if (isNotYetCreatorOrPending(User)) {
                    // Mark content as pending until on-chain addContentCreator finishes
                    Content.setPendingCreatorApproval(true);

                    // Only queue blockchain tx once
                    if (isNotContentCreator(User.getUserId())) {
                        contractServiceInterface.addContentCreator(User.getUserId(), User.getPersonalWallet(), User.getRank());
                    }
                } else if (!User.isContentCreator()) {
                    // User is pending — still mark content as PendingConfirmation
                    Content.setPendingCreatorApproval(true);
                } else {
                    // User is already a content creator
                    Content.setContentEnum(ContentEnum.Active);
                    Content.setActiveDate(Instant.now());
                }
            }
            Content.setIsComplete(true);
            User.setVideosPosted(User.getVideosPosted() + 1);
            User.getCreatedContent().add(Content.getContentId());
            contentRepository.save(Content);
            contentIds.add(Content.getContentId());
        }
        userRepository.save(User);
        return contentIds;
    }

    private boolean isNotContentCreator(String userId) {
        Query query = new Query(Criteria
                .where("_id").is(userId)
                .and("isContentCreator").is(false)
                .and("contentCreatorPending").is(false));

        Update update = new Update()
                .set("contentCreatorPending", true);

        Users updatedUser = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Users.class
        );

        return updatedUser != null;
    }

    private boolean isNotYetCreatorOrPending(Users user) {
        return !user.isContentCreator() && !user.isContentCreatorPending();
    }

    private Content mapContentRequest(String contentType, String userID, Boolean isViolator) {
        return Content.builder()
                .contentId(ObjectId.get().toHexString())
                .description("This is description")
                .numbBidders(0)
                .startingCost(0)
                .createdDate(Instant.now())
                .contentType(contentType)
                .releaseDate(LocalDate.now().plusDays(1))
                .creatorID(userID)
                .listOfBuyerIds(new HashMap<>())
                .hype(BigDecimal.ZERO)
                .googleSubject("")
                .youtubeUsername("admin")
                .isViolator(isViolator)
                .sentEmails(false)
                .contentReports(new ArrayList<>())
                .build();
    }

    private boolean isAuction(String contentType){
        return contentType.equals("Invention") || contentType.equals("Innovation");
    }

    private void approveChannels(List<String> channelIds) {
        for (String channelId: channelIds){
            var channel = channelRepository.findById(channelId).orElseThrow();
            channelService.approveChannelRequest(new ApproveChannelRequest(channelId, channel.getStreamerInfo()));
        }

    }

    private byte[] buildCall(String functionName, List parameters){
        Function function = new Function(
                functionName,
                parameters,
                Collections.emptyList()
        );
        return multiSendHelperService.buildCall(BigInteger.ZERO, function);
    }


    private ArrayList<String> create20Channels(List<String> userIds, String adminId){
        ArrayList<String> channelIds = new ArrayList<>();
        var channelSubscribers = new ArrayList<String>();
        channelSubscribers.add(adminId);
        var admin = userRepository.findById(adminId).orElseThrow();
        for (int i = 0; i < 20; i++) {
            int randomNumber = ThreadLocalRandom.current().nextInt(1000, 10000);
            var streamerInfo = StreamerInfo.builder()
                    .username("testname" + i)
                    .averageWeeklyViewers(randomNumber)
                    .platform("Youtube")
                    .streamerId("streamer" + i)
                    .build();
            var streamInfoArray = new ArrayList<StreamerInfo>();
            streamInfoArray.add(streamerInfo);
            var newChannel = Channels.builder()
                    .channelId(ObjectId.get().toHexString())
                    .channelStatus(ChannelStatus.Pending)
                    .channelName("testChannel" + i)
                    .ownerID(userIds.get(i))
                    .channelSubscribers(channelSubscribers)
                    .streamerInfo(streamInfoArray)
                    .purchasedContent(new HashMap<String, String>())
                    .watchNowPayLaterIDs(new ArrayList<String>())
                    .channelEvents(new ArrayList<>())
                    .awvIsUpdated(true)
                    .isBanned(false)
                    .nextAvailableResubmitDate(Instant.now().plus(1, ChronoUnit.DAYS))
                    .build();
            channelRepository.save(newChannel);
            channelIds.add(newChannel.getChannelId());
            var user = userRepository.findById(userIds.get(i)).orElseThrow();
            user.getChannels().add(newChannel.getChannelId());
            userRepository.save(user);
        }
        admin.setChannelSubscribedTo(channelIds);
        userRepository.save(admin);
        return channelIds;
    }

    private ArrayList<String> create20Users(){
        ArrayList<String> userIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            var user = Users.builder()
                    .userId(ObjectId.get().toHexString())
                    .email("user" + i + "@fakemail.com")
                    .username("user" + i)
                    .messages(new ArrayList<>())
                    .googleSubject(null)
                    //UserChannelDetails
                    .personalWallet(null)
                    .isViolator(false)
                    .channelSubscribedTo(new ArrayList<>())
                    .channels(new ArrayList<>())
                    //UserContentDetails
                    .payLater(new ArrayList<>())
                    .videosPosted(0)
                    .totalHype(0.0)
                    .rank(1)
                    //UserDetails not saved to the client
                    .contentCreatorPending(false)
                    .createdContent(new ArrayList<>())
                    .purchasedContent(new HashMap<>())
                    .password(passwordEncoder.encode("User#123"))
                    .allowedDevelopingVideos(0)
                    .role(Role.USER)
                    .enabled(true)
                    .creationDate(Instant.now())
                    .isContentCreator(false)
                    .nextWalletChangeTime(Instant.now())
                    .mfaEnabled(false)
                    .secret("")
                    .secretExpiration(Instant.now().plus(2, ChronoUnit.HOURS))
                    .deleteRequest(false)
                    .isBanned(false)
                    .build();
            userRepository.save(user);
            userIds.add(user.getUserId());
        }
        return userIds;
    }

    private String createAdminAccount(){
        var user = Users.builder()
                .userId(ObjectId.get().toHexString())
                .email("admin@fakemail.com")
                .username("admin")
                .messages(new ArrayList<>())
                .googleSubject(null)
                //UserChannelDetails
                .personalWallet(null)
                .isViolator(false)
                .channelSubscribedTo(new ArrayList<>())
                .channels(new ArrayList<>())
                //UserContentDetails
                .payLater(new ArrayList<>())
                .videosPosted(0)
                .totalHype(0.0)
                .rank(1)
                //UserDetails not saved to the client
                .contentCreatorPending(false)
                .createdContent(new ArrayList<>())
                .purchasedContent(new HashMap<>())
                .password(passwordEncoder.encode("Admin#123"))
                .allowedDevelopingVideos(0)
                .role(Role.ADMIN)
                .enabled(true)
                .creationDate(Instant.now())
                .personalWallet(getCredentials().getAddress())
                .isContentCreator(false)
                .nextWalletChangeTime(Instant.now())
                .mfaEnabled(false)
                .secret("")
                .secretExpiration(Instant.now().plus(2, ChronoUnit.HOURS))
                .deleteRequest(false)
                .isBanned(false)
                .build();
        userRepository.save(user);
        return user.getUserId();
    }

    public String addTransactionVerificationToQueue(
            String contentId,
            String userId,
            String transactionHash,
            TransactionVerificationFunctionEnum transactionVerificationFunctionEnum,
            BigInteger expectedManaAmount
    ){
        TransactionVerificationPackage transactionVerificationPackage = TransactionVerificationPackage.builder()
                .transactionVerificationId(ObjectId.get().toHexString())
                .transactionVerificationFunctionEnum(transactionVerificationFunctionEnum)
                .contentID(contentId)
                .transactionHash(transactionHash)
                .userId(userId)
                .status(BlockchainInteractionStatusEnum.Awaiting)
                .expectedManaAmount(expectedManaAmount)
                .build();
        transactionVerificationPackageRepository.save(transactionVerificationPackage);
        return transactionVerificationPackage.getTransactionVerificationId();
    }
}
