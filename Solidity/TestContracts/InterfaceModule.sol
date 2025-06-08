/**
 *Submitted for verification at polygonscan.com on 2024-05-19
*/

// SPDX-License-Identifier: MIT
pragma solidity ^0.8.18;

contract InterfaceModuleTest{    
    address[] public authorizedAddresses;
    address immutable BidAddress;
    address immutable ChannelAddress;
    address immutable WarChestAddress;
    address private immutable CompanyWallet; 
     mapping(string contentID => bool) public bidServiceAllowList;
    mapping(string channelName => bool) public channelServiceAllowList;
    mapping(string contentCreatorID => bool) public warChestAllowList;


    constructor(address _bidAddress, address _channelAddress, address _warChestAddress, address[] memory _authorizedAddresses) {
        CompanyWallet = msg.sender;
        BidAddress = _bidAddress;
        ChannelAddress = _channelAddress;
        WarChestAddress = _warChestAddress;
        authorizedAddresses = _authorizedAddresses;
    }
    
    function executeTransaction(
        address to,
        uint256 value,
        bytes memory data
    ) internal isAuthorizedAddress{
        (bool success, ) = to.call{value: value}(data);
        if(!success){
            revert("Transaction Failed");
        }
    }

    function createNewAuction(
        string calldata _contentID,
        uint256 _maxWinners,
        uint256 _minEntryCost,
        address _creatorAddress
    ) external {
        bytes memory data = abi.encodeWithSignature("createNewAuction(string,uint256,uint256,address)",
            _contentID,
            _maxWinners,
            _minEntryCost,
            _creatorAddress
        );
        executeTransaction(BidAddress, 0, data);
        bidServiceAllowList[_contentID] = true;
    }

    function returnBid(
        string calldata _contentID,
        string calldata _userID
    ) external onBidServiceAllowList(_contentID){
        bytes memory data = abi.encodeWithSignature("returnBid(string,string)",
            _contentID,
            _userID
        );
        executeTransaction(BidAddress, 0, data);
    }

    function setAuctionToInactive(
        string calldata _contentID
    ) external onBidServiceAllowList(_contentID){
        bytes memory data = abi.encodeWithSignature("setAuctionToInactive(string)",
            _contentID
        );
        executeTransaction(BidAddress, 0, data);
    }

    function setAuctionToActive(
        string calldata _contentID,
        string[] calldata previousWinners
    ) external onBidServiceAllowList(_contentID){
        bytes memory data = abi.encodeWithSignature("setAuctionToActive(string,string[])",
            _contentID,
            previousWinners
        );
        executeTransaction(BidAddress, 0, data);
    }

    function sendMana(
        string calldata _contentID
    ) external onBidServiceAllowList(_contentID){
        bytes memory data = abi.encodeWithSignature("sendMana(string)",
            _contentID
        );
        executeTransaction(BidAddress, 0, data);
    }

    function addChannel(
        string calldata _channelName,
        uint256 _averageWeeklyViewers
    ) external {
        bytes memory data = abi.encodeWithSignature("addChannel(string,uint256)",
            _channelName,
            _averageWeeklyViewers
        );
        executeTransaction(ChannelAddress, 0, data);
        channelServiceAllowList[_channelName] = true;
    }

    function payForContent(
        string calldata _channelName,
        string calldata _contentCreatorID,
        string calldata _contentID,
        uint256 _pricePerHundredAWV    
    ) external onChannelServiceAllowList(_channelName){
        bytes memory data = abi.encodeWithSignature("payForContent(string,string,string,uint256)",
            _channelName,
            _contentCreatorID,
            _contentID,
            _pricePerHundredAWV
        );
        executeTransaction(ChannelAddress, 0, data);
    }

    function watchNowPayLater(
        string calldata _channelName,
        string calldata _contentCreatorID,
        string calldata _contentID,
        uint256 _pricePerHundredAWV,
        uint8 paymentIncrements    
    ) external onChannelServiceAllowList(_channelName){
        bytes memory data = abi.encodeWithSignature("watchNowPayLater(string,string,string,uint256,uint8)",
            _channelName,
            _contentCreatorID,
            _contentID,
            _pricePerHundredAWV,
            paymentIncrements
        );
        executeTransaction(ChannelAddress, 0, data);
    }

    function updateAverageWeeklyViewers(
		string calldata _channelName,
        uint256 _newAverageWeeklyViewers
    ) external onChannelServiceAllowList(_channelName){
        bytes memory data = abi.encodeWithSignature("updateAverageWeeklyViewers(string,uint256)",
            _channelName,
            _newAverageWeeklyViewers
        );
        executeTransaction(ChannelAddress, 0, data);
    }

    function watchNowPayLaterPayment(
        string calldata _channelName, 
        string calldata _contentID
    ) external onChannelServiceAllowList(_channelName){
        bytes memory data = abi.encodeWithSignature("watchNowPayLaterPayment(string,string)",
            _channelName,
            _contentID
        );
        executeTransaction(ChannelAddress, 0, data);
    }

    function addContentCreator(
		string calldata _userID, 
		address _personalWallet, 
		uint8 _rank
	) external {
        bytes memory data = abi.encodeWithSignature("addContentCreator(string,address,uint8)",
            _userID,
            _personalWallet,
            _rank
        );
        executeTransaction(WarChestAddress, 0, data);
        warChestAllowList[_userID] = true;
    }

    function sendRefundPayment(
		string calldata _contentCreatorID, 
		string calldata _contentID, 
		string calldata _channelName
	) external onChannelServiceAllowList(_channelName){
        bytes memory data = abi.encodeWithSignature("sendRefundPayment(string,string,string)",
            _contentCreatorID,
            _contentID,
            _channelName
        );
        executeTransaction(WarChestAddress, 0, data);
    }

    function sendWatchNowPayLaterRefundPayment(
		string calldata _contentCreatorID, 
		string calldata _contentID, 
		string calldata _channelName
	) external onChannelServiceAllowList(_channelName){
        bytes memory data = abi.encodeWithSignature("sendWatchNowPayLaterRefundPayment(string,string,string)",
            _contentCreatorID,
            _contentID,
            _channelName
        );
        executeTransaction(WarChestAddress, 0, data);
    }

    function updatePersonalWallet(
		string calldata _userID, 
		address _newPersonalWallet
	) external onWarChestServiceAllowList(_userID){
        bytes memory data = abi.encodeWithSignature("updatePersonalWallet(string,address)",
            _userID,
            _newPersonalWallet
        );
        executeTransaction(WarChestAddress, 0, data);
    }

    function increaseCreatorRank(
		string calldata _userID
	) external onWarChestServiceAllowList(_userID){
        bytes memory data = abi.encodeWithSignature("increaseCreatorRank(string)",
            _userID
        );
        executeTransaction(WarChestAddress, 0, data);
    }

    function reactivateContent(
        string calldata _contentID,
        string[] calldata previousWinners
    ) external{
        bytes memory data = abi.encodeWithSignature("reactivateContent(string,string[])",
            _contentID,
            previousWinners
        );
        executeTransaction(WarChestAddress, 0, data);
    }

    function sendWeeklyMana(
        string calldata creatorID
    ) external onWarChestServiceAllowList(creatorID){
        bytes memory data = abi.encodeWithSignature("sendWeeklyMana(string)",
            creatorID
        );
        executeTransaction(WarChestAddress, 0, data);
    }

    function addAuthorizedAddress(
        address newOwner
    ) external {
        if(msg.sender != CompanyWallet){
            revert("Caller must be from Company Wallet address");
        }
        authorizedAddresses.push(newOwner);
    }

    function removeAuthorizedAddress(address _authorizedAddresses) external {
        if(msg.sender != CompanyWallet){
            revert("Caller must be from Company Wallet address");
        }
        for (uint256 i = 0; i < authorizedAddresses.length; i++) {
            if (authorizedAddresses[i] == _authorizedAddresses) {
                authorizedAddresses[i] = authorizedAddresses[authorizedAddresses.length - 1];
                authorizedAddresses.pop();
                return;
            }
        }
        revert("Address entered is not an Authorized Address");
    }

    modifier isAuthorizedAddress() {
        bool found = false;
        for (uint256 i = 0; i < authorizedAddresses.length; i++) {
            if (authorizedAddresses[i] == msg.sender) {
                _;
                found = true;
                break;
            }
        }
        require(found, "Unauthorized Access");
    }

    modifier onBidServiceAllowList(string calldata contentID) {
        bool found = false;
        if (bidServiceAllowList[contentID]) {
            _;
            found = true;
        }
        require(found, "Not on Bid Service Allow List");
    }


    modifier onChannelServiceAllowList(string calldata channelName) {
        bool found = false;
        if (channelServiceAllowList[channelName]) {
            _;
            found = true;
        }
        require(found, "Not on Channel Service Allow List");
    }


    modifier onWarChestServiceAllowList(string calldata contentCreatorID) {
        bool found = false;
        if (warChestAllowList[contentCreatorID]) {
            _;
            found = true;
        }
        require(found, "Not on WarChest Service Allow List");
    }

}