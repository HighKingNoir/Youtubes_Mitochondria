export interface AuthenticationResponse{
    jwtToken: string;
    mfaEnabled: boolean;
    secretImageUri: string;
    newUser: boolean;
    invalidEmail: boolean;
    secret: string;
    email: string;
}