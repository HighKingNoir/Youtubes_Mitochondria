//The variables needed to create new content

export interface ContentRequestPayload{
    contentType:string;
    contentName: string;
    youtubeMainVideoID: string | null;
    youtubeTrailerVideoID: string;
    numbBidders: number;
    description: string;
    startingCost: number;
    releaseDate: releaseDateInfo;    
    thumbnail: string;
    duration: number | null
    youtubeUsername: string
    youtubeProfilePicture: string 
    privacyStatus: string | null
    googleSubject: string;
    liveBroadcastContent: string
}

export interface releaseDateInfo{
    year: number
    month: number
    day: number
}