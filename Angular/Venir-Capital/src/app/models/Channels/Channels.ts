export interface Channels{
    channelId:string;
    ownerID: string;
    channelName: string;
    channelLogo: string;
    streamerInfo: Array<StreamerInfo>;
    streamerChangeInfo: Array<StreamerInfo>;
    isBanned: boolean;
    channelEvents: Array<ChannelEvents>;
    timezone: string;
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