
//The Content details of the requested Content
export interface CreatedContentDetails{
    contentId:string;
    creatorID: string;
    contentName: string;
    youtubeMainVideoID: string | null;
    youtubeTrailerVideoID: string;
    numbBidders: number;
    description: string;
    startingCost: number;
    releaseDate: number[];
    hype: number;
    contentType:string;
    thumbnail: string;
    createdDate: number;
    contentEnum: string;
    isComplete: boolean;
    duration: number | null;
    youtubeUsername: string
    youtubeProfilePicture: string
    listOfBuyerIds: Map<string, string>;
    isViolator: boolean;
    sentEmails: boolean
    reportRate: number
    contentReports: ContentReports[]
}

export interface ContentReports{
    reporterID: string;
    report: string;
    timeStamp: number;
    isResolved: boolean;
}
