import { CreatedContentDetails } from "./CreatedContentDetails";
import { PurchasedContentDetails } from "./PurchasedContentDetails";

export interface PurchasedContentResponse{
    content: Array<CreatedContentDetails>;
    payment: Array<PurchasedContentDetails>;
}