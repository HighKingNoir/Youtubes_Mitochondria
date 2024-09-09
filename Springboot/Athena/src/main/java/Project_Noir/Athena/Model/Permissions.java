package Project_Noir.Athena.Model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Permissions {

    VIEW_LOGS("log:read"),
    RESOLVE_LOGS("log:resolve"),

    View_Reports("reports:read"),
    Resolve_Reports("reports:read"),

    APPROVE_CHANNEL("channel:approve"),
    DISAPPROVE_CHANNEL("channel:disapprove"),

    VIEW_CHANNEL_UPDATES("channel:read"),
    VIEW_CHANNEL_CHANGES("channel:read"),
    UPDATE_CHANNEL_AVERAGE_WEEKLY_VIEWERS("channel:update"),
    BAN_CHANNEL("channel:ban"),

    DELEGATE_NEW_EMPLOYEE("delegate:User"),
    BAN_USER("user:ban");

    private final String permission;


}
