// SPDX-License-Identifier: MIT
pragma solidity ^0.8.18;

import {IERC20} from "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import {AggregatorV3Interface} from "@chainlink/contracts/src/v0.8/interfaces/AggregatorV3Interface.sol";
error FunctionError(string errorMessage);

contract PayToRankUpServiceTest {
    address private immutable CompanyWallet; 
    IERC20 private immutable POLYGON_MANA;
    AggregatorV3Interface private priceFeed = AggregatorV3Interface(0xA1CbF3Fe43BC3501e3Fc4b573e822c70e76A7512);
    mapping(string userID => bool) public hasPaidArchonPassList;
    mapping(string userID => bool) public hasPaidMasterPassList;
    uint256 testManaPrice = 45887321;

    constructor(address tokenAddress) {
        POLYGON_MANA = IERC20(tokenAddress); 
        CompanyWallet = msg.sender;
    }


    function ArchonPass(
        string calldata _userID
    ) external {
        uint256 manaPrice = getManaPrice(); 
        uint256 usdScaled = 250000 * 1e8;
        uint256 manaAmountWei = (usdScaled * 1e18) / manaPrice;
        if(hasPaidArchonPassList[_userID]){
            revert FunctionError("Already Paid");
        }
        if(checkAllowance() < manaAmountWei){
            revert FunctionError("Not enough allowance");
        }
        if(POLYGON_MANA.balanceOf(msg.sender) < manaAmountWei){
            revert FunctionError("Insufficient MANA balance");
        }

        bool sendToCompanyWallet = POLYGON_MANA.transferFrom(msg.sender, CompanyWallet, manaAmountWei);
        if(!sendToCompanyWallet){
            revert FunctionError("Mana to Company failed");
        }
        hasPaidArchonPassList[_userID] = true;

    }

    function MasterPass(
        string calldata _userID
    ) external{
        uint256 manaPrice = getManaPrice(); 
        uint256 usdScaled = 75000 * 1e8;
        uint256 manaAmountWei = (usdScaled * 1e18) / manaPrice;
        if(hasPaidMasterPassList[_userID]){
            revert FunctionError("Already Paid");
        }
        if(checkAllowance() < manaAmountWei){
            revert FunctionError("Not enough allowance");
        }
        if(POLYGON_MANA.balanceOf(msg.sender) < manaAmountWei){
            revert FunctionError("Insufficient MANA balance");
        }

        bool sendToCompanyWallet = POLYGON_MANA.transferFrom(msg.sender, CompanyWallet, manaAmountWei);
        if(!sendToCompanyWallet){
            revert FunctionError("Mana to Company failed");
        }
        hasPaidMasterPassList[_userID] = true;
    }

    function checkAllowance() internal view returns (uint256) {
        return POLYGON_MANA.allowance(msg.sender, address(this));
    }

    function getManaPrice() internal view returns (uint256) {
        return testManaPrice;
    }

}