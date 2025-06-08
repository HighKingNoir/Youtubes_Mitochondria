
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.18;

import {IERC20} from "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import {AggregatorV3Interface} from "@chainlink/contracts/src/v0.8/interfaces/AggregatorV3Interface.sol";
error FunctionError(string errorMessage);

contract ChannelService{
    address private immutable CompanyWallet; 
    address public warChestAddress; 
    address[] public allowedAddresses;
    bool public contractWarChestAddressSet;
    mapping(string channelName => Channel) public findChannelByName;
    mapping(string channelName => mapping(string contentID => PaymentPlan)) public findPaymentPlans;
    AggregatorV3Interface private priceFeed = AggregatorV3Interface(0xA1CbF3Fe43BC3501e3Fc4b573e822c70e76A7512);
    IERC20 private immutable POLYGON_MANA;
    Channel[] private listOfChannels;
    uint256 constant private WEEK = 1 weeks;

    uint256 testManaPrice = 45887321;
  
    struct Channel{
		int256 balance;
		uint256 averageWeeklyViewers;
        string channelName;
	}

    struct PaymentPlan {
        string channelName;
        string contentID;
        uint256 paymentInWei;
        uint8 increments;
        uint256 lastActionTime;
    }

	receive() external payable{

    }

    fallback() external payable {
        revert FunctionError("");
    }
   
    constructor(address tokenAddress){
        CompanyWallet = msg.sender;
        POLYGON_MANA = IERC20(tokenAddress);
        contractWarChestAddressSet = false;
        allowedAddresses.push(CompanyWallet);
    }

    function setWarChestServiceAddress(address payable _warChestServiceAddress) isOverseer external {
        if(contractWarChestAddressSet){
            revert FunctionError("WarChestService address has already been set"); 
        }
        warChestAddress = _warChestServiceAddress;
        contractWarChestAddressSet = true;
        allowedAddresses.push(_warChestServiceAddress);
    }

    function addChannel(
        string calldata _channelName,
        uint256 _averageWeeklyViewers
    ) external isOverseer {
        if(keccak256(abi.encodePacked(findChannelByName[_channelName].channelName)) != keccak256("")){
            revert FunctionError("Channel Name already Taken");
        }     
        Channel memory newChannel = Channel(
            0,
            _averageWeeklyViewers,
            _channelName
        );
        listOfChannels.push(newChannel);
        findChannelByName[_channelName] = newChannel;
    }

    function fundChannel(
        string calldata _channelName,
        uint256 _manaAmountInWei
    ) external {
        if(keccak256(abi.encodePacked(findChannelByName[_channelName].channelName)) == keccak256("")){
            revert FunctionError("Must enter a valid Channel");
        }
        if(_manaAmountInWei == 0){
            revert FunctionError("Didn't send any Polygon MANA");
        }
        if(POLYGON_MANA.allowance(msg.sender, address(this)) < _manaAmountInWei){
            revert FunctionError("Not enough allowance");
        }
        if(POLYGON_MANA.balanceOf(msg.sender) < _manaAmountInWei){
            revert FunctionError("Insufficient MANA balance");
        }
        bool receiveMana = POLYGON_MANA.transferFrom(msg.sender, address(this), _manaAmountInWei);
        if(!receiveMana){
            revert FunctionError("Failed to receive Polygon Mana");
        }
        Channel storage channel = findChannelByName[_channelName];
        channel.balance += int256(_manaAmountInWei);
    }

    function payForContent(
        string calldata _channelName,
        string calldata _contentCreatorID,
        string calldata _contentID,
        uint256 _pricePerHundredAWV    
    ) external payable isOverseer {
        Channel storage channel = findChannelByName[_channelName];
        uint256 totalPrice = (channel.averageWeeklyViewers *_pricePerHundredAWV * 1e26) / (100 * testManaPrice);
        if(channel.balance < int256(totalPrice)){
            revert FunctionError("Insufficient funds");
        }
        channel.balance -= int256(totalPrice);
        uint256 manaToSivantis = (totalPrice * 10) / 100;
        uint256 manaToContentCreator = totalPrice - manaToSivantis;

        bool sendToWarChest = POLYGON_MANA.transfer(warChestAddress, manaToContentCreator);
        if(!sendToWarChest){
            revert FunctionError("Mana to WarChest Failed");
        }
        
        bool sendToCompanyWallet = POLYGON_MANA.transfer(CompanyWallet, manaToSivantis);
        if(!sendToCompanyWallet){
            revert FunctionError("Mana to Company Failed");
        }

        (bool callToWarChest,) = warChestAddress.call(abi.encodeWithSignature(
			"receivePayment(string,string,string,uint256)",_contentCreatorID,_contentID, channel.channelName, manaToContentCreator
		));
        if(!callToWarChest){
            revert FunctionError("Call to WarChest Failed");
        }
    }

    function watchNowPayLater(
        string calldata _channelName,
        string calldata _contentCreatorID,
        string calldata _contentID,
        uint256 _pricePerHundredAWV,
        uint8 paymentIncrements    
    ) external isOverseer {
        Channel storage channel = findChannelByName[_channelName];
        uint256 totalPrice = (channel.averageWeeklyViewers *_pricePerHundredAWV * 1e26) / (100 * testManaPrice);
        if(channel.balance < int256(totalPrice / paymentIncrements)){
            revert FunctionError("Insufficient funds");
        }
        uint256 initialDeposit = totalPrice / paymentIncrements;
        channel.balance -= int256(initialDeposit);
        findPaymentPlans[_channelName][_contentID] = PaymentPlan(
                _channelName,
                _contentID,
                initialDeposit,
                (paymentIncrements - 1),
                block.timestamp
            );
        uint256 manaToSivantis = (totalPrice * 10) / 100;
        uint256 manaToContentCreator = totalPrice - manaToSivantis;

        bool sendToWarChest = POLYGON_MANA.transfer(warChestAddress, manaToContentCreator);
        if(!sendToWarChest){
            revert FunctionError("Mana to WarChest Failed");
        }
        
        bool sendToCompanyWallet = POLYGON_MANA.transfer(CompanyWallet, manaToSivantis);
        if(!sendToCompanyWallet){
            revert FunctionError("Mana to Company Failed");
        }

        (bool callToWarChest,) = warChestAddress.call(abi.encodeWithSignature(
			"receivePayment(string,string,string,uint256)",_contentCreatorID,_contentID, channel.channelName, manaToContentCreator
		));
        if(!callToWarChest){
            revert FunctionError("call to WarChest Failed");
        }
    }

    function receiveRefundPayment(
        string calldata _channelName,
        uint256 refundAmount
    ) external isOverseer {
        findChannelByName[_channelName].balance += int256(refundAmount);
    }

    function receiveWatchNowPayLaterRefundPayment(
        string calldata _channelName,
        string calldata _contentID,
        uint256 refundAmount
    ) external isOverseer {
        findChannelByName[_channelName].balance += int256(refundAmount);
        PaymentPlan storage paymentPlan =  findPaymentPlans[_channelName][_contentID];
        uint256 deficit = paymentPlan.increments * paymentPlan.paymentInWei;
        paymentPlan.increments = 0;
        findChannelByName[_channelName].balance -= int256(deficit);
    }

    function updateAverageWeeklyViewers(
		string calldata _channelName,
        uint256 _newAverageWeeklyViewers
    ) external isOverseer {
            findChannelByName[_channelName].averageWeeklyViewers = _newAverageWeeklyViewers;
    }

    function watchNowPayLaterPayment(
        string calldata _channelName, 
        string calldata _contentID
    ) external isOverseer{
        PaymentPlan storage paymentPlan = findPaymentPlans[_channelName][_contentID];
        if(paymentPlan.increments == 0){
            revert FunctionError("Payment Plan has been completely paid"); 
        }
        if(block.timestamp < (paymentPlan.lastActionTime + WEEK)){
            revert FunctionError("A week hasn't passed since last Payment"); 
        }
        paymentPlan.increments -= 1;
        findChannelByName[_channelName].balance -= int256(paymentPlan.paymentInWei);
        paymentPlan.lastActionTime += WEEK;
    }

    function getChannelBalance(string calldata _channelName) public view returns (int256){
        return findChannelByName[_channelName].balance;
    }

    function getAllChannels() public view returns (Channel[] memory){
        Channel[] memory channelArray= new Channel[](listOfChannels.length);
        for(uint256 index = 0; index < listOfChannels.length; index++){
            channelArray[index] = findChannelByName[listOfChannels[index].channelName];    
        }
        return channelArray;
    }

    function getBalance() public view returns (uint256) {
        return POLYGON_MANA.balanceOf(address(this));
    }

	function getManaBalanceInUSD(
        int256 manaAmount
    ) internal view returns (int256) {
        int256 manaPrice = int256(testManaPrice) * 1e10; 
        int256 manaAmountInUsd = (manaPrice * manaAmount) / 1e18;
        return manaAmountInUsd;
    }

    function addOverseer(address _newContractAddress) external{
        if(msg.sender != CompanyWallet){
            revert FunctionError("This function can only be called by Company Wallet");
        }
        allowedAddresses.push(_newContractAddress);
    }

    function removeOverseer(address overseerAddress) external {
        if(msg.sender != CompanyWallet){
            revert FunctionError("This function can only be called by Company Wallet");
        }
        for (uint256 i = 2; i < allowedAddresses.length; i++) {
            if (allowedAddresses[i] == overseerAddress) {
                allowedAddresses[i] = allowedAddresses[allowedAddresses.length - 1];
                allowedAddresses.pop();
                return;
            }
        }
        revert("Address not an Overseer");
    }


    function isAddressAllowed() internal view returns (bool) {
        for (uint256 i = 0; i < allowedAddresses.length; i++) {
            if (allowedAddresses[i] == msg.sender) {
                return true;
            }
        }
        return false;
    }
    
    modifier isOverseer(){
        if(!isAddressAllowed()){
            revert FunctionError("This function can only be called by an Allowed Addresses");
        }
        _;
	}
}