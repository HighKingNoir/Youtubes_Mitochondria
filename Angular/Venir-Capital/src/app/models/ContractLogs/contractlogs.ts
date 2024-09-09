export interface ContractLogs{
    
    logId: string,
    contentID: string | undefined
    userID: string | undefined
    channelName: string | undefined
    creationDate: number
    contractEnum: string,
    contractFunctionEnum: string; 
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