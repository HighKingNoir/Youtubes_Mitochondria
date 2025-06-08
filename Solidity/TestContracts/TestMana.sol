// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract TestManaToken is ERC20 {

    mapping (address => uint256) private _balances; 

    mapping (address => mapping (address => uint256)) private _allowances;

    constructor() ERC20("Polygon Mana", "Mana") {
        _mint(msg.sender, 100000000000000000 * 10 ** decimals());
    }

    function mint() external {
        _mint(msg.sender, 100000000000000000 * 10 ** decimals());
    }

    function increaseAllowance(address spender, uint256 addedValue) public virtual returns (bool) {
        _approve(_msgSender(), spender, _allowances[_msgSender()][spender] += addedValue);
        return true;
    }

    function decreaseAllowance(address spender, uint256 subtractedValue) public virtual returns (bool) {
        _approve(_msgSender(), spender, _allowances[_msgSender()][spender] -= subtractedValue);
        return true;
    }
}
