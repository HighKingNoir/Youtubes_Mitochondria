package Project_Noir.Athena.Model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document()

public class Users implements UserDetails {

    @Id
    //The user's ID
    private String userId;

    //The user's username
    private String username;

    private Boolean deleteRequest;

    private Instant deleteAccountDate;

    //The user's password
    private String password;

    private Instant creationDate;

    private Boolean mfaEnabled;

    private String mfaSecret;

    private String secret;

    private Instant secretExpiration;

    private String email;

    //Weather the user is an Admin, Customer, or Employee
    private Role role;

    //Is enabled after verifying the user's email
    @Getter
    private boolean enabled;

    //List of all created content IDs
    private ArrayList<String> createdContent;

    //Map between content ID and purchase ID
    private Map<String, String> purchasedContent;

    //The name the user decides to call their channel
    private ArrayList<String> channels;

    //The crypto wallet mana will be sent to
    private String personalWallet;

    //IDs of users that are subscribed to you
    private ArrayList<String> channelSubscribedTo;

    //Content IDs of the videos you want to pay for later
    private ArrayList<String> payLater;

    //The state of weather the user violated any content policy
    private Boolean isViolator;

    //The current tier of the user based on the amount of hype generated
    private Integer rank;

    //Total number of videos posted
    private Integer videosPosted;

    //The total hype the user has generated
    private Double totalHype;

    //The number of allowed undeveloped videos the user can create
    private Integer allowedDevelopingVideos;

    private ArrayList<String> messages;

    private boolean isContentCreator;

    private Instant nextWalletChangeTime;

    private String language;

    private Boolean isBanned;

    private LocalDate unbanDate;

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role.getAuthorities();
    }

    public boolean isAccountNonExpired() {
        return true;
    }

    public boolean isAccountNonLocked() {
        return !isBanned;
    }

    public boolean isCredentialsNonExpired() {
        return true;
    }

}
