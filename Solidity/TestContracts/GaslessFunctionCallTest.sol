// SPDX-License-Identifier: MIT
pragma solidity ^0.8.18;
import {IERC20} from "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import "@openzeppelin/contracts/token/ERC20/extensions/IERC20Permit.sol";


contract GaslessFunctionCallModule{ 
    address[] public authorizedAddresses;
    address immutable BidAddress;
    address immutable ChannelAddress;
    IERC20 private immutable POLYGON_MANA;
    address private immutable POLYGON_MANA_Address;
    address private immutable CompanyWallet; 
    mapping(string contentID => mapping(string userID => Bid)) public findBidByContentIDAndUserID;

    struct Bid {
        address bidAddress;
        string userID;
        uint256 amount;
    }

    constructor(address _bidAddress,address _channelAddress, address tokenAddress, address[] memory _authorizedAddresses) {
        CompanyWallet = msg.sender;
        BidAddress = _bidAddress;
        ChannelAddress = _channelAddress;
        POLYGON_MANA = IERC20(tokenAddress);
        POLYGON_MANA_Address = tokenAddress;
        authorizedAddresses = _authorizedAddresses;
        POLYGON_MANA.approve(ChannelAddress, type(uint256).max);
        POLYGON_MANA.approve(BidAddress, type(uint256).max);
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

    function gaslessFundChannel(
        string calldata channelName,
        address sender,
        uint256 amount,
        uint256 fee,
        uint256 deadline,
        uint8 v,
        bytes32 r,
        bytes32 s
    ) external isAuthorizedAddress{
        IERC20Permit(POLYGON_MANA_Address).permit(
            sender, address(this), amount + fee, deadline, v, r, s
        );
        POLYGON_MANA.transferFrom(sender,  address(this), amount);
        bytes memory data = abi.encodeWithSignature("fundChannel(string,uint256)",
            channelName,
            amount
        );
        executeTransaction(ChannelAddress, 0, data);
        POLYGON_MANA.transferFrom(sender, CompanyWallet, fee);
    }

    function gaslessPlaceBid(
        string calldata _contentID,
        string calldata _userID,
        address sender,
        uint256 amount,
        uint256 fee,
        uint256 deadline,
        uint8 v,
        bytes32 r,
        bytes32 s
    ) external isAuthorizedAddress{
        IERC20Permit(POLYGON_MANA_Address).permit(
            sender, address(this), amount + fee, deadline, v, r, s
        );
        POLYGON_MANA.transferFrom(sender,  address(this), amount);
        bytes memory data = abi.encodeWithSignature("placeBid(string,string,uint256)",
            _contentID,
            _userID,
            amount
        );
        executeTransaction(ChannelAddress, 0, data);
        findBidByContentIDAndUserID[_contentID][_userID] = Bid(
                sender, 
                _userID,
                amount
            );
        POLYGON_MANA.transferFrom(sender, CompanyWallet, fee);
    }

    function gaslessRaiseBid(
         string calldata _contentID,
        string calldata _userID,
        address sender,
        uint256 amount,
        uint256 fee,
        uint256 deadline,
        uint8 v,
        bytes32 r,
        bytes32 s
    ) external isAuthorizedAddress{
        IERC20Permit(POLYGON_MANA_Address).permit(
            sender, address(this), amount + fee, deadline, v, r, s
        );
        Bid storage bid = findBidByContentIDAndUserID[_contentID][_userID];
        POLYGON_MANA.transferFrom(sender,  address(this), amount);
        bytes memory data = abi.encodeWithSignature("raiseBid(string,string,uint256)",
            _contentID,
            _userID,
            amount
        );
        executeTransaction(ChannelAddress, 0, data);
        bid.amount += amount;
        POLYGON_MANA.transferFrom(sender, CompanyWallet, fee);
    }

    function gaslessCancelBid(
        string calldata _contentID,
        string calldata _userID,
        address sender,
        uint256 fee,
        uint256 deadline,
        uint8 v,
        bytes32 r,
        bytes32 s
    ) external isAuthorizedAddress{
        IERC20Permit(POLYGON_MANA_Address).permit(
            sender, address(this), fee, deadline, v, r, s
        );
        Bid storage bid = findBidByContentIDAndUserID[_contentID][_userID];
        if(sender != bid.bidAddress){
            revert("Request must come from the address that placed the first bid");
        }  
        bytes memory data = abi.encodeWithSignature("cancelBid(string,string)",
            _contentID,
            _userID
        );
        executeTransaction(ChannelAddress, 0, data);
        uint256 amountToSend = bid.amount;
        bid.amount = 0;
        bool returnMana = POLYGON_MANA.transfer(bid.bidAddress, amountToSend);
        if(!returnMana){
            revert("Failed to return mana");
        }
        POLYGON_MANA.transferFrom(sender, CompanyWallet, fee);
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
}