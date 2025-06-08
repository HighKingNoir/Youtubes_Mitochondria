export interface Channels{
    channelId:string;
    ownerID: string;
    channelName: string;
    channelLogo: string;
    streamerInfo: Array<StreamerInfo>;
    isBanned: boolean;
    channelEvents: Array<ChannelEvents>;
    channelBanner:string
    channelDescription: string
    channelStatus: string
}

export interface StreamerInfo{
    streamerId: string;
    platform: string;
    username: string;
    averageWeeklyViewers: number;
    youtubeChannelId: string | null
}

export interface ChannelEvents{
    channelEventStatus: string;
    contentName: string;
    showTime: number;
    watcher: string
    timeZone: string;
}