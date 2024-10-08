import { environment } from "src/Environment/environment";

export const bidServiceContract = {
    contractAddress: environment.Contract_Bid_Address,
    abi:[
        {
            "inputs": [],
            "stateMutability": "nonpayable",
            "type": "constructor"
        },
        {
            "inputs": [
                {
                    "internalType": "string",
                    "name": "errorMessage",
                    "type": "string"
                }
            ],
            "name": "FunctionError",
            "type": "error"
        },
        {
            "stateMutability": "payable",
            "type": "fallback"
        },
        {
            "inputs": [
                {
                    "internalType": "address",
                    "name": "_newContractAddress",
                    "type": "address"
                }
            ],
            "name": "addOverseer",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "uint256",
                    "name": "",
                    "type": "uint256"
                }
            ],
            "name": "allowedAddresses",
            "outputs": [
                {
                    "internalType": "address",
                    "name": "",
                    "type": "address"
                }
            ],
            "stateMutability": "view",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "string",
                    "name": "_contentID",
                    "type": "string"
                },
                {
                    "internalType": "string",
                    "name": "_userID",
                    "type": "string"
                }
            ],
            "name": "cancelBid",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "string",
                    "name": "_contentID",
                    "type": "string"
                },
                {
                    "internalType": "uint256",
                    "name": "_maxWinners",
                    "type": "uint256"
                },
                {
                    "internalType": "uint256",
                    "name": "_minEntryCost",
                    "type": "uint256"
                },
                {
                    "internalType": "address",
                    "name": "_creatorAddress",
                    "type": "address"
                }
            ],
            "name": "createNewAuction",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "string",
                    "name": "contentID",
                    "type": "string"
                }
            ],
            "name": "findAuctionByContentID",
            "outputs": [
                {
                    "internalType": "uint256",
                    "name": "maxWinners",
                    "type": "uint256"
                },
                {
                    "internalType": "uint256",
                    "name": "minEntryCost",
                    "type": "uint256"
                },
                {
                    "internalType": "bool",
                    "name": "isActive",
                    "type": "bool"
                },
                {
                    "internalType": "address",
                    "name": "creatorAddress",
                    "type": "address"
                },
                {
                    "internalType": "uint256",
                    "name": "totalBalance",
                    "type": "uint256"
                }
            ],
            "stateMutability": "view",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "string",
                    "name": "contentID",
                    "type": "string"
                },
                {
                    "internalType": "string",
                    "name": "userID",
                    "type": "string"
                }
            ],
            "name": "findBidByContentIDAndUserID",
            "outputs": [
                {
                    "internalType": "address",
                    "name": "bidAddress",
                    "type": "address"
                },
                {
                    "internalType": "string",
                    "name": "userID",
                    "type": "string"
                },
                {
                    "internalType": "uint256",
                    "name": "amount",
                    "type": "uint256"
                }
            ],
            "stateMutability": "view",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "string",
                    "name": "_contentID",
                    "type": "string"
                },
                {
                    "internalType": "string[]",
                    "name": "_userIDs",
                    "type": "string[]"
                }
            ],
            "name": "getAllAuctionBids",
            "outputs": [
                {
                    "components": [
                        {
                            "internalType": "address",
                            "name": "bidAddress",
                            "type": "address"
                        },
                        {
                            "internalType": "string",
                            "name": "userID",
                            "type": "string"
                        },
                        {
                            "internalType": "uint256",
                            "name": "amount",
                            "type": "uint256"
                        }
                    ],
                    "internalType": "struct BidService.Bid[]",
                    "name": "",
                    "type": "tuple[]"
                }
            ],
            "stateMutability": "view",
            "type": "function"
        },
        {
            "inputs": [],
            "name": "getBalance",
            "outputs": [
                {
                    "internalType": "uint256",
                    "name": "",
                    "type": "uint256"
                }
            ],
            "stateMutability": "view",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "uint256",
                    "name": "",
                    "type": "uint256"
                }
            ],
            "name": "listOfAuctions",
            "outputs": [
                {
                    "internalType": "uint256",
                    "name": "maxWinners",
                    "type": "uint256"
                },
                {
                    "internalType": "uint256",
                    "name": "minEntryCost",
                    "type": "uint256"
                },
                {
                    "internalType": "bool",
                    "name": "isActive",
                    "type": "bool"
                },
                {
                    "internalType": "address",
                    "name": "creatorAddress",
                    "type": "address"
                },
                {
                    "internalType": "uint256",
                    "name": "totalBalance",
                    "type": "uint256"
                }
            ],
            "stateMutability": "view",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "string",
                    "name": "_contentID",
                    "type": "string"
                },
                {
                    "internalType": "string",
                    "name": "_userID",
                    "type": "string"
                },
                {
                    "internalType": "uint256",
                    "name": "_manaAmountInWei",
                    "type": "uint256"
                }
            ],
            "name": "placeBid",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "string",
                    "name": "_contentID",
                    "type": "string"
                },
                {
                    "internalType": "string",
                    "name": "_userID",
                    "type": "string"
                },
                {
                    "internalType": "uint256",
                    "name": "_manaAmountInWei",
                    "type": "uint256"
                }
            ],
            "name": "raiseBid",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "address",
                    "name": "overseerAddress",
                    "type": "address"
                }
            ],
            "name": "removeOverseer",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "string",
                    "name": "_contentID",
                    "type": "string"
                },
                {
                    "internalType": "string",
                    "name": "_userID",
                    "type": "string"
                }
            ],
            "name": "returnBid",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "string",
                    "name": "_contentID",
                    "type": "string"
                }
            ],
            "name": "sendMana",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "string",
                    "name": "_contentID",
                    "type": "string"
                },
                {
                    "internalType": "string[]",
                    "name": "previousWinners",
                    "type": "string[]"
                }
            ],
            "name": "setAuctionToActive",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "string",
                    "name": "_contentID",
                    "type": "string"
                }
            ],
            "name": "setAuctionToInactive",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
        },
        {
            "stateMutability": "payable",
            "type": "receive"
        }
    ]
}