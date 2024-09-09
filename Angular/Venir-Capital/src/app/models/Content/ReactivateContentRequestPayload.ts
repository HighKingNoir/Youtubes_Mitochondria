import { releaseDateInfo } from "./ContentRequestPayload";

export interface ReactivateContentRequestPayload{
    contentID: string;
    releaseDate: releaseDateInfo;
}