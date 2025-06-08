package Project_Noir.Athena.Service;

import Project_Noir.Athena.Exception.SivantisException;
import Project_Noir.Athena.Model.Users;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;


@Service
@Slf4j
public class JwtService {
    @Value("${jwt.key}")
    private String Key;

    @Value("${url}")
    private String url;

    // @dev Gets the username from the JWT token
    public String extractUsername(String Token){
            return JWT.decode(Token).getClaim("username").asString();
    }

    // @dev Gets the username from the JWT token
    public String extractUserId(String Token){
        return JWT.decode(Token).getSubject();
    }

    public String getJWTString(String token){
        return token.substring(7);
    }

    // @dev generates a new JWT
    public String generateToken(Users user){
        return JWT.create()
                .withIssuer(url)
                .withSubject(user.getUserId())
                .withClaim("username", user.getUsername())
                .withExpiresAt(new Date(System.currentTimeMillis() + 60000 * 180))
                .withIssuedAt(new Date(System.currentTimeMillis()))
                .sign(Algorithm.HMAC512(Key));
    }

    // @dev generates a new JWT refresh token
    public String generateRefreshToken(String ExpiredJWT) {
        isExpiredJWTVerified(ExpiredJWT);
        return JWT.create()
                .withIssuer(url)
                .withSubject(extractUserId(ExpiredJWT))
                .withClaim("username", extractUsername(ExpiredJWT))
                .withExpiresAt(new Date(System.currentTimeMillis() + 60000 * 90))
                .withIssuedAt(new Date(System.currentTimeMillis()))
                .sign(Algorithm.HMAC512(Key));
    }

    // @dev Checks if the JWT matches the username on file and hasn't expired
    public boolean isTokenValid(String token, UserDetails userDetails){
        final String Username = extractUsername(token);
        return (Username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    // @dev Checks if the JWT was issued from Sivantis
    public boolean isVerified(String token){
        try {
            JWT.require(Algorithm.HMAC512(Key)).withIssuer(url).build().verify(token);
        }catch (JWTVerificationException E) {
           throw new SivantisException("JWT token can't be confirmed");
        }
        return true;
    }

    private void isExpiredJWTVerified(String token) {
        try {
            JWT.require(Algorithm.HMAC512(Key)).withIssuer(url).build().verify(token);
        } catch (TokenExpiredException expiredException) {
            // Allow TokenExpiredException to propagate
        } catch (JWTVerificationException otherException) {
            throw new SivantisException("JWT token can't be confirmed");
        }
    }

    // @ Checks if the JWT expiration has been reached
    public boolean isTokenExpired(String token){
        return JWT.decode(token).getExpiresAt().before(new Date());
    }

}
