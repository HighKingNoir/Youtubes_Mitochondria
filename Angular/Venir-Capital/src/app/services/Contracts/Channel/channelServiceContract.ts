import { environment } from "src/Environment/environment";

export const channelServiceContract = {
    contractAddress: environment.Contract_Channel_Address,
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
                    "internalType": "string",
                    "name": "_channelName",
                    "type": "string"
                },
                {
                    "internalType": "uint256",
                    "name": "_averageWeeklyViewers",
                    "type": "uint256"
                }
            ],
            "name": "addChannel",
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
            "name": "contractWarChestAddressSet",
            "outputs": [
                {
                    "internalType": "bool",
                    "name": "",
                    "type": "bool"
                }
            ],
            "stateMutability": "view",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "string",
                    "name": "channelName",
                    "type": "string"
                }
            ],
            "name": "findChannelByName",
            "outputs": [
                {
                    "internalType": "int256",
                    "name": "balance",
                    "type": "int256"
                },
                {
                    "internalType": "uint256",
                    "name": "averageWeeklyViewers",
                    "type": "uint256"
                },
                {
                    "internalType": "string",
                    "name": "channelName",
                    "type": "string"
                }
            ],
            "stateMutability": "view",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "string",
                    "name": "channelName",
                    "type": "string"
                },
                {
                    "internalType": "string",
                    "name": "contentID",
                    "type": "string"
                }
            ],
            "name": "findPaymentPlans",
            "outputs": [
                {
                    "internalType": "string",
                    "name": "channelName",
                    "type": "string"
                },
                {
                    "internalType": "string",
                    "name": "contentID",
                    "type": "string"
                },
                {
                    "internalType": "uint256",
                    "name": "paymentInWei",
                    "type": "uint256"
                },
                {
                    "internalType": "uint8",
                    "name": "increments",
                    "type": "uint8"
                },
                {
                    "internalType": "uint256",
                    "name": "lastActionTime",
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
                    "name": "_channelName",
                    "type": "string"
                },
                {
                    "internalType": "uint256",
                    "name": "_manaAmountInWei",
                    "type": "uint256"
                }
            ],
            "name": "fundChannel",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
        },
        {
            "inputs": [],
            "name": "getAllChannels",
            "outputs": [
                {
                    "components": [
                        {
                            "internalType": "int256",
                            "name": "balance",
                            "type": "int256"
                        },
                        {
                            "internalType": "uint256",
                            "name": "averageWeeklyViewers",
                            "type": "uint256"
                        },
                        {
                            "internalType": "string",
                            "name": "channelName",
                            "type": "string"
                        }
                    ],
                    "internalType": "struct ChannelService.Channel[]",
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
                    "name": "_channelName",
                    "type": "string"
                }
            ],
            "name": "getChannelBalance",
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
                    "name": "_channelName",
                    "type": "string"
                },
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
                    "internalType": "uint256",
                    "name": "_pricePerHundredAWV",
                    "type": "uint256"
                }
            ],
            "name": "payForContent",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "string",
                    "name": "_channelName",
                    "type": "string"
                },
                {
                    "internalType": "uint256",
                    "name": "refundAmount",
                    "type": "uint256"
                }
            ],
            "name": "receiveRefundPayment",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "string",
                    "name": "_channelName",
                    "type": "string"
                },
                {
                    "internalType": "string",
                    "name": "_contentID",
                    "type": "string"
                },
                {
                    "internalType": "uint256",
                    "name": "refundAmount",
                    "type": "uint256"
                }
            ],
            "name": "receiveWatchNowPayLaterRefundPayment",
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
                    "internalType": "address payable",
                    "name": "_warChestServiceAddress",
                    "type": "address"
                }
            ],
            "name": "setWarChestServiceAddress",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "string",
                    "name": "_channelName",
                    "type": "string"
                },
                {
                    "internalType": "uint256",
                    "name": "_newAverageWeeklyViewers",
                    "type": "uint256"
                }
            ],
            "name": "updateAverageWeeklyViewers",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
        },
        {
            "inputs": [],
            "name": "warChestAddress",
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
                    "name": "_channelName",
                    "type": "string"
                },
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
                    "internalType": "uint256",
                    "name": "_pricePerHundredAWV",
                    "type": "uint256"
                },
                {
                    "internalType": "uint8",
                    "name": "paymentIncrements",
                    "type": "uint8"
                }
            ],
            "name": "watchNowPayLater",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
        },
        {
            "inputs": [
                {
                    "internalType": "string",
                    "name": "_channelName",
                    "type": "string"
                },
                {
                    "internalType": "string",
                    "name": "_contentID",
                    "type": "string"
                }
            ],
            "name": "watchNowPayLaterPayment",
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