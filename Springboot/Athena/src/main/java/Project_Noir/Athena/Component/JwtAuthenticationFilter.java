package Project_Noir.Athena.Component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final Project_Noir.Athena.Service.JwtService JwtService;
    private final UserDetailsService userDetailsService;


    @Override
    protected void doFilterInternal(
             @NonNull HttpServletRequest request,
             @NonNull HttpServletResponse response,
             @NonNull FilterChain filterChain)
             throws ServletException, IOException{
        final String AuthHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String Jwt, Username;

        if(AuthHeader == null || !AuthHeader.startsWith("Bearer ")){
            if (request.getRequestURI().equals("/sse")) {
                // Extract the JWT token from the query parameter
                checkServerSideEmitterJWT(request, response, filterChain);
            }
            else{
                filterChain.doFilter(request,response);
                return;
            }
        }
        Jwt = AuthHeader.substring(7);
            Username = JwtService.extractUsername(Jwt);
            if(Username != null && SecurityContextHolder.getContext().getAuthentication() == null){
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(Username);
                    if(JwtService.isTokenValid(Jwt, userDetails) && JwtService.isVerified(Jwt)){
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails.getUsername(),
                                null,
                                userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);}}



        filterChain.doFilter(request,response);

    }

    private void checkServerSideEmitterJWT(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String jwtTokenFromQuery = request.getParameter("token");
        if (jwtTokenFromQuery == null) {
            filterChain.doFilter(request,response);
            return;
        }
        var Username = JwtService.extractUsername(jwtTokenFromQuery);
        if(Username != null && SecurityContextHolder.getContext().getAuthentication() == null){
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(Username);
            if(JwtService.isTokenValid(jwtTokenFromQuery, userDetails) && JwtService.isVerified(jwtTokenFromQuery)){
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails.getUsername(),
                        null,
                        userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);}}



        filterChain.doFilter(request,response);
    }

}
