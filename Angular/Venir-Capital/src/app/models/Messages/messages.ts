export interface Messages{
    
        messageId: string,
        transactionHash: string | null,
        extraInfo: Array<string>
        hasRead: boolean,
        messageEnum: string
        creationDate: number
}