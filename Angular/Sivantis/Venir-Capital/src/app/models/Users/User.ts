
//All the necessary User information
export interface User{
    userId: string;
    username: string,
    email: string,
    rank: number;
    totalHype: number;
    personalWallet: string;
    role: string;
    payLater: Array<string>
    channelSubscribedTo: Array<string>
    allowedDevelopingVideos: number
    mfaEnabled: boolean
    videosPosted: number
}

