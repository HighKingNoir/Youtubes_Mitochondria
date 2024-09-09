//The Content details of the requested Content
export interface PurchasedContentDetails{
    
    paymentId: string,

    //The ID of the content being purchased
    contentId: string,

    //The userID who purchased the content
    userId: string,

    //The amount of polygon mana being transferred
    manaAmount: string,

    //The dollar equivalent to the amount of mana sent
    dollarAmount: number,

    //The date at which the refund was submitted
    refundDate:Date | null;

    paymentDate: number;
    status: string,
}