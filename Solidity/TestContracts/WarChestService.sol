// SPDX-License-Identifier: MIT
pragma solidity ^0.8.18;

import {IERC20} from "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import {AggregatorV3Interface} from "@chainlink/contracts/src/v0.8/interfaces/AggregatorV3Interface.sol";
error FunctionError(string errorMessage);

contract WarChestService {
    address private immutable CompanyWallet;
	address public immutable channelServiceAddress; 
	address[] public allowedAddresses;
	mapping(string userID=> ContentCreator) public findContentCreatorByID;
	mapping(string contentID => mapping(string channelName => uint256 balance)) public findBalance;
	IERC20 private immutable POLYGON_MANA;
	AggregatorV3Interface private priceFeed = AggregatorV3Interface(0xA1CbF3Fe43BC3501e3Fc4b573e822c70e76A7512);
    ContentCreator[] private listOfContentCreators;
	uint256 public basePay;

    uint256 testManaPrice = 45887321;

	struct ContentCreator {
		string userID;
		address personalWallet;
		int256 balance;
		uint8 rank;
	}
	
	constructor(address payable _channelServiceAddress, address tokenAddress){
        CompanyWallet = msg.sender;
        POLYGON_MANA = IERC20(tokenAddress);
		channelServiceAddress = _channelServiceAddress;
		basePay = 750;
		allowedAddresses.push(CompanyWallet);
		allowedAddresses.push(channelServiceAddress);
	}

	receive() external payable {

	}

	fallback() external payable{
        revert FunctionError("");
	}
	
	function addContentCreator(
		string calldata _userID, 
		address _personalWallet, 
		uint8 _rank
	) external isOverseer{
		if(keccak256(abi.encodePacked(findContentCreatorByID[_userID].userID)) != keccak256("")){
            revert FunctionError("Content Creator Already Exist");
        }
		if(_rank > 11){
			revert FunctionError("Rank cannot be greater than 11");
		}     
		ContentCreator memory newContentCreator = ContentCreator(_userID, _personalWallet, 0, _rank);
		listOfContentCreators.push(newContentCreator);
		findContentCreatorByID[_userID] = newContentCreator;
	}

    function receivePayment(
		string calldata _contentCreatorID,
		string calldata _contentID,
		string calldata _channelName,
		uint256 amount
	) external isOverseer{
		if(findBalance[_contentID][_channelName] != 0){
			revert FunctionError("Already paid for content");
		}
		findContentCreatorByID[_contentCreatorID].balance += int256(amount);
		findBalance[_contentID][_channelName] = amount;
    }
	
	function sendRefundPayment(
		string calldata _contentCreatorID, 
		string calldata _contentID, 
		string calldata _channelName
	) external isOverseer{
		uint256 refundAmount = findBalance[_contentID][_channelName];
		if(refundAmount == 0){
			revert FunctionError("Content was already refunded");
		}
		findBalance[_contentID][_channelName] = 0;
		findContentCreatorByID[_contentCreatorID].balance -= int256(refundAmount);
		
		bool sendToChannel = POLYGON_MANA.transfer(channelServiceAddress, refundAmount);
		if(!sendToChannel){
			revert FunctionError("Mana to Channel Failed");
		}

		(bool callToChannelAddress,) = channelServiceAddress.call(abi.encodeWithSignature(
			"receiveRefundPayment(string,uint256)",_channelName, refundAmount
		));
		if(!callToChannelAddress){
			revert FunctionError("Call to channel Failed");
		}
    }

	function sendWatchNowPayLaterRefundPayment(
		string calldata _contentCreatorID, 
		string calldata _contentID, 
		string calldata _channelName
	) external isOverseer{
		uint256 refundAmount = findBalance[_contentID][_channelName];
		if(refundAmount == 0){
			revert FunctionError("Content was already refunded");
		}
		findBalance[_contentID][_channelName] = 0;
		findContentCreatorByID[_contentCreatorID].balance -= int256(refundAmount);
		
		bool sendToChannel = POLYGON_MANA.transfer(channelServiceAddress, refundAmount);
		if(!sendToChannel){
			revert FunctionError("Mana to Channel failed");
		}

		(bool callToChannelAddress,) = channelServiceAddress.call(abi.encodeWithSignature(
			"receiveWatchNowPayLaterRefundPayment(string,string,uint256)",_channelName, _contentID, refundAmount
		));
		if(!callToChannelAddress){
			revert FunctionError("call to Channel failed");
		}

    }
	
	function updatePersonalWallet(
		string calldata _userID, 
		address _newPersonalWallet
	) external isOverseer{
        findContentCreatorByID[_userID].personalWallet = _newPersonalWallet;
    }
	
	function increaseCreatorRank(
		string calldata _userID
	) external isOverseer{
		if(findContentCreatorByID[_userID].rank == 11){
            revert FunctionError("Rank cannot be higher than 11");
        }
		findContentCreatorByID[_userID].rank += 1;
    }

	function reactivateContent(
        string calldata _contentID,
        string[] calldata previousWinners
    ) external isOverseer() {
        for (uint256 index = 0; index < previousWinners.length; index++){
            findBalance[_contentID][previousWinners[index]] = 0;
        }
    }
	
	function userWithdraw(
		string calldata _userID,
		uint256 _dollarAmountInWei
	) external {
		ContentCreator storage contentCreator = findContentCreatorByID[_userID];
		if(getManaBalanceInUSD(contentCreator.balance) < int256(_dollarAmountInWei)){
            revert FunctionError("The amount entered exceeds your balance");
        }
		if(msg.sender != contentCreator.personalWallet){
			revert FunctionError("Must call from your address on file");
		}
		if(contentCreator.rank == 1){
			revert FunctionError("Must be at least rank 2");	
		}
		uint256 manaToSend = (_dollarAmountInWei * 1e8) / testManaPrice ;
		contentCreator.balance -= int256(manaToSend);
		uint256 manaSentToCompanyWallet = (manaToSend * 10) / 100; 
        uint256 manaSentToCreator = manaToSend - manaSentToCompanyWallet; 

		bool sendToCompanyWallet = POLYGON_MANA.transfer(CompanyWallet, manaSentToCompanyWallet);
        if(!sendToCompanyWallet){
			revert FunctionError("Mana To Company Failed");
		}

		bool sendToCreator = POLYGON_MANA.transfer(contentCreator.personalWallet, manaSentToCreator);
        if(!sendToCreator){
			revert FunctionError("Mana To Creator Failed");
		}
    }

	function getCreatorsBalance(string calldata _userID) external view returns (int256){
        return findContentCreatorByID[_userID].balance;
    }

	function getAllContentCreators() external view returns (ContentCreator[] memory){
        ContentCreator[] memory contentCreatorsArray= new ContentCreator[](listOfContentCreators.length);
        for(uint256 index = 0; index < listOfContentCreators.length; index++){
            contentCreatorsArray[index] = findContentCreatorByID[listOfContentCreators[index].userID];    
        }
        return contentCreatorsArray;
    }
	
    function sendWeeklyMana(string calldata creatorID) external isOverseer{
		ContentCreator storage contentCreator = findContentCreatorByID[creatorID];
		if(contentCreator.balance > 0){
			int256 userBalanceInDollars = getManaBalanceInUSD(contentCreator.balance);
			uint256 dollarAmountToSend = uint256(2 ** (contentCreator.rank - 1) * (basePay * 1e18));
			if(userBalanceInDollars > int256(dollarAmountToSend)){
				uint256 manaAmountToSend = (dollarAmountToSend * 1e8) / testManaPrice;
				contentCreator.balance -= int256(manaAmountToSend);
				bool manaToCreator = POLYGON_MANA.transfer(contentCreator.personalWallet, manaAmountToSend);
				if(!manaToCreator){
					revert FunctionError("Mana to Creator failed");
				}
			}
			else{
				uint256 remainingBalance = uint256(contentCreator.balance);
				contentCreator.balance = 0;
				bool manaToCreator = POLYGON_MANA.transfer(contentCreator.personalWallet, remainingBalance);
				if(!manaToCreator){
					revert FunctionError("Mana to Creator failed");
				}
			}
		}
    }

	function raiseBasePay(uint256 _newBasePay) external{
		if(msg.sender != CompanyWallet){
            revert FunctionError("This function can only be called by Company Wallet");
        }
		if(basePay >= _newBasePay){
				revert FunctionError("New base pay must be greater than previous base pay");
        }
        basePay = _newBasePay;
	}

	
	function getBalance() public view returns (uint) {
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