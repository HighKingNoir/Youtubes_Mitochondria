import { environment } from "src/Environment/environment";

export const payToRankUpContract = {
    contractAddress: environment.Contract_Pay_To_Rank_Up_Address,
    abi:[
	{
		"inputs": [
			{
				"internalType": "address",
				"name": "tokenAddress",
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
		"inputs": [
			{
				"internalType": "string",
				"name": "_userID",
				"type": "string"
			}
		],
		"name": "ArchonPass",
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
			}
		],
		"name": "MasterPass",
		"outputs": [],
		"stateMutability": "nonpayable",
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
		"name": "hasPaidArchonPassList",
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
				"name": "userID",
				"type": "string"
			}
		],
		"name": "hasPaidMasterPassList",
		"outputs": [
			{
				"internalType": "bool",
				"name": "",
				"type": "bool"
			}
		],
		"stateMutability": "view",
		"type": "function"
	}
]
}