import { environment } from "src/Environment/environment";

export const warchestServiceContract = {
    contractAddress: environment.Contract_Warchest_Address,
    abi: [
        {
            "inputs": [
                {
                    "internalType": "address payable",
                    "name": "_channelServiceAddress",
                    "type": "address"
                }
            ],
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
                    "internalType": "string",
                    "name": "_userID",
                    "type": "string"
                },
                {
                    "internalType": "address",
                    "name": "_personalWallet",
                    "type": "address"
                },
                {
                    "internalType": "uint8",
                    "name": "_rank",
                    "type": "uint8"
                }
            ],
            "name": "addContentCreator",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
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
            "inputs": [],
            "name": "basePay",
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
            "inputs": [],
            "name": "channelServiceAddress",
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
                    "name": "contentID",
                    "type": "string"
                },
                {
                    "internalType": "string",
                    "name": "channelName",
                    "type": "string"
                }
            ],
            "name": "findBalance",
            "outputs": [
                {
                    "internalType": "uint256",
                    "name": "balance",
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
                    "name": "userID",
                    "type": "string"
                }
            ],
            "name": "findContentCreatorByID",
            "outputs": [
                {
                    "internalType": "string",
                    "name": "userID",
                    "type": "string"
                },
                {
                    "internalType": "address",
                    "name": "personalWallet",
                    "type": "address"
                },
                {
                    "internalType": "int256",
                    "name": "balance",
                    "type": "int256"
                },
                {
                    "internalType": "uint8",
                    "name": "rank",
                    "type": "uint8"
                }
            ],
            "stateMutability": "view",
            "type": "function"
        },
        {
            "inputs": [],
            "name": "getAllContentCreators",
            "outputs": [
                {
                    "components": [
                        {
                            "internalType": "string",
                            "name": "userID",
                            "type": "string"
                        },
                        {
                            "internalType": "address",
                            "name": "personalWallet",
                            "type": "address"
                        },
                        {
                            "internalType": "int256",
                            "name": "balance",
                            "type": "int256"
                        },
                        {
                            "internalType": "uint8",
                            "name": "rank",
                            "type": "uint8"
                        }
                    ],
                    "internalType": "struct WarChestService.ContentCreator[]",
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
                    "internalType": "string",
                    "name": "_userID",
                    "type": "string"
                }
            ],
            "name": "getCreatorsBalance",
            "outputs": [
                {
                    "internalType": "int256",
                    "name": "",
                    "type": "int256"
                }
            ],
            "stateMutability": "view",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "string",
                    "name": "_userID",
                    "type": "string"
                }
            ],
            "name": "increaseCreatorRank",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "uint256",
                    "name": "_newBasePay",
                    "type": "uint256"
                }
            ],
            "name": "raiseBasePay",
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
            "name": "reactivateContent",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "string",
                    "name": "_contentCreatorID",
                    "type": "string"
                },
                {
                    "internalType": "string",
                    "name": "_contentID",
                    "type": "string"
                },
                {
                    "internalType": "string",
                    "name": "_channelName",
                    "type": "string"
                },
                {
                    "internalType": "uint256",
                    "name": "amount",
                    "type": "uint256"
                }
            ],
            "name": "receivePayment",
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
                    "name": "_contentCreatorID",
                    "type": "string"
                },
                {
                    "internalType": "string",
                    "name": "_contentID",
                    "type": "string"
                },
                {
                    "internalType": "string",
                    "name": "_channelName",
                    "type": "string"
                }
            ],
            "name": "sendRefundPayment",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "string",
                    "name": "_contentCreatorID",
                    "type": "string"
                },
                {
                    "internalType": "string",
                    "name": "_contentID",
                    "type": "string"
                },
                {
                    "internalType": "string",
                    "name": "_channelName",
                    "type": "string"
                }
            ],
            "name": "sendWatchNowPayLaterRefundPayment",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "string",
                    "name": "creatorID",
                    "type": "string"
                }
            ],
            "name": "sendWeeklyMana",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "string",
                    "name": "_userID",
                    "type": "string"
                },
                {
                    "internalType": "address",
                    "name": "_newPersonalWallet",
                    "type": "address"
                }
            ],
            "name": "updatePersonalWallet",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "string",
                    "name": "_userID",
                    "type": "string"
                },
                {
                    "internalType": "uint256",
                    "name": "_dollarAmountInWei",
                    "type": "uint256"
                }
            ],
            "name": "userWithdraw",
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