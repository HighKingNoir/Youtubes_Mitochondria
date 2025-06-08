// SPDX-License-Identifier: MIT
pragma solidity ^0.8.18;

import {IERC20} from "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import {AggregatorV3Interface} from "@chainlink/contracts/src/v0.8/interfaces/AggregatorV3Interface.sol";
error FunctionError(string errorMessage);

contract BidService {
    address private immutable CompanyWallet; 
    address[] public allowedAddresses;
    mapping(string contentID => Auction) public findAuctionByContentID;
    mapping(string contentID => mapping(string userID => Bid)) public findBidByContentIDAndUserID;
    IERC20 private immutable POLYGON_MANA;
    AggregatorV3Interface private priceFeed = AggregatorV3Interface(0xA1CbF3Fe43BC3501e3Fc4b573e822c70e76A7512);
    Auction[] public listOfAuctions; 

    uint256 testManaPrice = 45887321;

    struct Bid {
        address bidAddress;
        string userID;
        uint256 amount;
    }

    struct Auction {
        uint256 maxWinners;
        uint256 minEntryCost;
        bool isActive;
        address creatorAddress;
        uint256 totalBalance;
    }
    
    constructor(address tokenAddress) {
        POLYGON_MANA = IERC20(tokenAddress); 
        CompanyWallet = msg.sender;
        allowedAddresses.push(CompanyWallet);
    }

    receive() external payable {

    }

    fallback() external payable{
        revert FunctionError("");
    }

    function createNewAuction(
        string calldata _contentID,
        uint256 _maxWinners,
        uint256 _minEntryCost,
        address _creatorAddress
    ) external isOverseer(){
        if(findAuctionByContentID[_contentID].creatorAddress != address(0)){
            revert FunctionError("Auction Already exist");
        }
        Auction memory newAuction = Auction(
            _maxWinners,
            _minEntryCost,
            true,
            _creatorAddress,
            0
        );
        listOfAuctions.push(newAuction); 
        findAuctionByContentID[_contentID] = newAuction;
    }

    function placeBid(
        string calldata _contentID,
        string calldata _userID,
        uint256 _manaAmountInWei
    ) external {
        Auction storage auction = findAuctionByContentID[_contentID];
        if(!auction.isActive){
            revert FunctionError("Cannot place bid because this auction is no longer active");
        }
        if(getManaAmountInUSD(_manaAmountInWei) < (auction.minEntryCost * 1e18)){
            revert FunctionError("Bid must exceed minimum entry cost");
        }  
        if(findBidByContentIDAndUserID[_contentID][_userID].amount != 0){
            revert FunctionError("Bid has already been placed and must be raised");
        }  
        if(checkAllowance() < _manaAmountInWei){
            revert FunctionError("Not enough allowance");
        }
        if(POLYGON_MANA.balanceOf(msg.sender) < _manaAmountInWei){
            revert FunctionError("Insufficient MANA balance");
        }
        bool receiveMana = POLYGON_MANA.transferFrom(msg.sender, address(this), _manaAmountInWei);
        if(!receiveMana){
            revert FunctionError("Failed to receive mana");
        }
        auction.totalBalance += _manaAmountInWei;
        Bid memory newBid = Bid(
                msg.sender, 
                _userID,
                _manaAmountInWei
            );
        findBidByContentIDAndUserID[_contentID][_userID] = newBid;
    }

    function raiseBid(
        string calldata _contentID,
        string calldata _userID,
        uint256 _manaAmountInWei
    ) external {
        Bid storage bid = findBidByContentIDAndUserID[_contentID][_userID];
        Auction storage auction = findAuctionByContentID[_contentID];
        if(bid.amount == 0){
            revert FunctionError("Bid must be placed before you can raise it");
        }
        if(!auction.isActive){
            revert FunctionError("This auction is no longer active");
        }  
        if(getManaAmountInUSD(_manaAmountInWei) < (5 * 1e18)){
            revert FunctionError("Bid raise must be at least $5");
        }  
        if(checkAllowance() < _manaAmountInWei){
            revert FunctionError("Not enough allowance");
        }
        if(POLYGON_MANA.balanceOf(msg.sender) < _manaAmountInWei){
            revert FunctionError("Insufficient MANA balance");
        }
        bool receiveMana = POLYGON_MANA.transferFrom(msg.sender, address(this), _manaAmountInWei);
        if(!receiveMana){
            revert FunctionError("Failed to receive mana");
        }
        auction.totalBalance += _manaAmountInWei;
        bid.amount += _manaAmountInWei;
    }

    function cancelBid(
        string calldata _contentID,
        string calldata _userID
    ) external {
        Auction storage auction = findAuctionByContentID[_contentID];
        Bid storage bid = findBidByContentIDAndUserID[_contentID][_userID];
        if(bid.amount == 0){
            revert FunctionError("Bid was already canceled");
        }
        if(!findAuctionByContentID[_contentID].isActive){
            revert FunctionError("Cannot cancel because this auction is no longer active");
        }  
        if(msg.sender != bid.bidAddress){
            revert FunctionError("Request must come from the address that placed the first bid");
        }  
        uint256 amountToSend = bid.amount;
        auction.totalBalance -= amountToSend;
        bid.amount = 0;
        bool returnMana = POLYGON_MANA.transfer(bid.bidAddress, amountToSend);
        if(!returnMana){
            revert FunctionError("Failed to return mana");
        }
    }

    function getAllAuctionBids(
        string calldata _contentID,
        string[] calldata _userIDs
    ) external view returns (Bid[] memory){
        Bid[] memory bidArray = new Bid[](_userIDs.length);
        for(uint256 index = 0; index < _userIDs.length; index++){
            bidArray[index] = findBidByContentIDAndUserID[_contentID][_userIDs[index]];    
        }
        return bidArray;
    }

    function returnBid(
        string calldata _contentID,
        string calldata _userID
    ) external isOverseer() {
        Auction storage auction = findAuctionByContentID[_contentID];
        Bid storage bid = findBidByContentIDAndUserID[_contentID][_userID];
        uint256 amountToReturn = bid.amount;
        bid.amount = 0;
        auction.totalBalance -= amountToReturn;
        bool returnMana = POLYGON_MANA.transfer(bid.bidAddress, amountToReturn);
        if(!returnMana){
            revert FunctionError("Failed to return mana");
        }
    }

    function setAuctionToInactive(
        string calldata _contentID
    ) external isOverseer() {
        findAuctionByContentID[_contentID].isActive = false;
    }

    function setAuctionToActive(
        string calldata _contentID,
        string[] calldata previousWinners
    ) external isOverseer() {
        Auction storage auction = findAuctionByContentID[_contentID];
        auction.isActive = true;
        for (uint256 index = 0; index < previousWinners.length; index++) 
        {
            findBidByContentIDAndUserID[_contentID][previousWinners[index]].amount = 0;
        }
    }

    function sendMana(
        string calldata _contentID
    ) external isOverseer(){
        Auction storage auction = findAuctionByContentID[_contentID];
        if(auction.isActive){
            revert FunctionError("Auction must be inactive");
        }
        uint256 manaSentToCompanyWallet = (auction.totalBalance * 10) / 100;
        uint256 manaSentToCreator = auction.totalBalance - manaSentToCompanyWallet;
        auction.totalBalance = 0;

        bool sendToCompanyWallet = POLYGON_MANA.transfer(CompanyWallet, manaSentToCompanyWallet);
        if(!sendToCompanyWallet){
            revert FunctionError("Mana to Company failed");
        }

        bool sendToCreator = POLYGON_MANA.transfer(findAuctionByContentID[_contentID].creatorAddress, manaSentToCreator);
        if(!sendToCreator){
            revert FunctionError("Mana to Creator failed");
        }
    }

    function getBalance() public view returns (uint256) {
        return POLYGON_MANA.balanceOf(address(this));
    }

    function getManaAmountInUSD(
        uint256 manaAmount
    ) internal view returns (uint256) {
        uint256 manaPrice = testManaPrice * 1e10; 
        uint256 manaAmountInUsd = (manaPrice * manaAmount) / 1e18;
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
        for (uint256 i = 1; i < allowedAddresses.length; i++) {
            if (allowedAddresses[i] == overseerAddress) {
                allowedAddresses[i] = allowedAddresses[allowedAddresses.length - 1];
                allowedAddresses.pop();
                return;
            }
        }
        revert("Address not an Overseer");
    }

    function checkAllowance() internal view returns (uint256) {
        return POLYGON_MANA.allowance(msg.sender, address(this));
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