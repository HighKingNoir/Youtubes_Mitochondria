package Project_Noir.Athena.Model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum Role {
    USER(Collections.emptySet()),
    ADMIN(
            Set.of(
                    Permissions.VIEW_LOGS,
                    Permissions.VIEW_CHANNEL_UPDATES,
                    Permissions.APPROVE_CHANNEL,
                    Permissions.DISAPPROVE_CHANNEL,
                    Permissions.RESOLVE_LOGS,
                    Permissions.UPDATE_CHANNEL_AVERAGE_WEEKLY_VIEWERS,
                    Permissions.BAN_CHANNEL,
                    Permissions.DELEGATE_NEW_EMPLOYEE,
                    Permissions.BAN_USER,
                    Permissions.VIEW_CHANNEL_CHANGES,
                    Permissions.Resolve_Reports,
                    Permissions.View_Reports
            )
    ),
    LOGGER(
            Set.of(
                    Permissions.VIEW_LOGS,
                    Permissions.RESOLVE_LOGS,
                    Permissions.VIEW_CHANNEL_UPDATES,
                    Permissions.VIEW_CHANNEL_CHANGES,
                    Permissions.View_Reports
            )
    ),
    RENOVATOR(
            Set.of(
                    Permissions.VIEW_LOGS,
                    Permissions.VIEW_CHANNEL_UPDATES,
                    Permissions.UPDATE_CHANNEL_AVERAGE_WEEKLY_VIEWERS,
                    Permissions.VIEW_CHANNEL_CHANGES,
                    Permissions.View_Reports
            )
    );

    private final Set<Permissions> permissions;

    public List<SimpleGrantedAuthority> getAuthorities(){
        var authorities = getPermissions().stream()
                .map(permission -> new SimpleGrantedAuthority(permission.name()))
                .collect(Collectors.toList());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
        return authorities;
    }
}
