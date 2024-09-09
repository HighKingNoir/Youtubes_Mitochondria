package Project_Noir.Athena.Configuration;

import Project_Noir.Athena.Component.JwtAuthenticationFilter;
import Project_Noir.Athena.Component.JwtExpiredAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static Project_Noir.Athena.Model.Permissions.*;
import static Project_Noir.Athena.Model.Role.ADMIN;


@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter JwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final JwtExpiredAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf().disable().authorizeHttpRequests().requestMatchers("/Auth/**").permitAll()
                .requestMatchers("/").permitAll()
                .requestMatchers("/Content/All/Active", "/Content/All/Active/**", "/Content/All/User", "/Content/Single/**", "/Content/Search/Videos", "/Content/sortBuyers/**", "/Content/Channel/Purchased/**").permitAll()
                .requestMatchers("/Channel/Active/Random", "/Channel/Search/Channels", "/Channel/Single/**").permitAll()
                .requestMatchers("/Users/Leaderboard, /Users/Leaderboard/**").permitAll()
                .requestMatchers("/sse/**").permitAll()
                .requestMatchers("/Content/Report/View").hasAuthority(View_Reports.name())
                .requestMatchers("/Content/Report/Resolve/**", "/Content/Report/Resolve", "/Content/Report/Ignore/**", "/Content/Report/Ignore").hasAuthority(Resolve_Reports.name())
                .requestMatchers("/Channel/Request/**","/Channel/Change", "/AdminDashboard/**").hasRole(ADMIN.name())
                .requestMatchers("/Channel/Ban").hasAuthority(BAN_CHANNEL.name())
                .requestMatchers("/Channel/AWV/**").hasAuthority(UPDATE_CHANNEL_AVERAGE_WEEKLY_VIEWERS.name())
                .requestMatchers("/Channel/Change/View").hasAuthority(VIEW_CHANNEL_CHANGES.name())
                .requestMatchers("/ContractLogs/View/**").hasAuthority(VIEW_LOGS.name())
                .requestMatchers("/ContractLogs/Resolve").hasAuthority(RESOLVE_LOGS.name())
                .requestMatchers("/Users/Ban").hasAuthority(BAN_USER.name())
                .requestMatchers("/Users/Delegate").hasAuthority(DELEGATE_NEW_EMPLOYEE.name())
                .anyRequest().authenticated()
                .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and().authenticationProvider(authenticationProvider)
                .cors(Customizer.withDefaults())
                .addFilterBefore(JwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint);
        return http.build();
    }
}
