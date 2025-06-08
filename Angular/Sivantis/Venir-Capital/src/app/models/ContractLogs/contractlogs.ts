export interface ContractLogs{
    
    logId: string,
    creationDate: number,
    contractEnum: string,
    contractFunctionDetails: Array<ContractFunctionDetails>
    contractTransactionReceipt:ContractTransactionReceipt;
    totalManaAmount: number | undefined;
    manaToCompany: number | undefined;
}

export interface ContractTransactionReceipt{
    transactionHash: string;
    gasUsed: number;
    revertReason: string;
    contractStatusEnum:string;
}

export interface ContractFunctionDetails{
    contentID: string;
    userID: string;
    channelName: string;
    contractFunctionEnum: string;
    manaAmount: number;
}