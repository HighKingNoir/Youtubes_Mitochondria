import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http'
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoginAccountComponent } from './components/Login/login-account/login-account.component';
import { HomeComponent } from './components/HomePages/home/home.component';
import { ChannelSidebarComponent } from './components/Accessories/Layout/channel-sidebar/channel-sidebar.component';
import { FooterComponent } from './components/Accessories/Layout/footer/footer.component';
import { HeaderComponent } from './components/Accessories/Layout/header/header.component';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { SearchComponent } from './components/Accessories/Layout/search/search.component';
import { AccountSignUpComponent } from './components/CreateAccount/account-sign-up/account-sign-up.component';
import { EmailPopupComponent } from './components/CreateAccount/email-popup/email-popup.component';
import { CommonModule } from '@angular/common';
import { ForgotPasswordComponent } from './components/Login/forgot-password/forgot-password.component';
import { TOSComponent } from './components/Info/tos/tos.component';
import { JWTinterceptorProvider } from './Interceptor/jwtinterceptor.interceptor';
import { OAuthModule } from 'angular-oauth2-oidc';
import { ImportYoutubeVideoComponent } from './components/Accessories/Popups/CreateVideo/import-youtube-video/import-youtube-video.component';
import { SelectMainVideoComponent } from './components/Accessories/Popups/CreateVideo/SelectVideo/select-main-video/select-main-video.component';
import { SelectTrailerVideoComponent } from './components/Accessories/Popups/CreateVideo/SelectVideo/select-trailer-video/select-trailer-video.component';
import { HomeSidebarComponent } from './components/Accessories/Layout/home-sidebar/home-sidebar.component';
import { FormateDatePipe } from './Pipe/FormateDate.pipe';
import { AddressShortenerPipe } from './Pipe/address-shortener.pipe';
import { ProfileSidebarComponent } from './components/Accessories/Layout/profile-sidebar/profile-sidebar.component';
import { MessageMainPageComponent } from './components/MessagePages/message-main-page/message-main-page.component';
import { ProfilePageComponent } from './components/ProfilePages/profile-page/profile-page.component';
import { NumberFormatPipe } from './Pipe/number-format.pipe';
import { HeaderSearchComponent } from './components/Accessories/Layout/header-search/header-search.component';
import { ContentPageComponent } from './components/Content/content-page/content-page.component';
import { PurchasedPageComponent } from './components/Content/purchased-page/purchased-page.component';
import { DashboardPageComponent } from './components/Content/dashboard/dashboard-page.component';
import { ChannelPageComponent } from './components/Channel/channel-page/channel-page.component';
import { SingleBuyerComponent } from './components/ContentVideoPages/single-buyer/single-buyer.component';
import { ChannelBuyerComponent } from './components/ContentVideoPages/channel-buyer/channel-buyer.component';
import { DecimalRoundPipe } from './Pipe/decimal-round.pipe';
import { FundChannelComponent } from './components/Accessories/Popups/fund-channel/fund-channel.component';
import { ConnectPersonalWalletComponent } from './components/Accessories/Popups/PersonalWallet/connect-personal-wallet/connect-personal-wallet.component';
import { RefundContentComponent } from './components/Accessories/Popups/ChannelContent/refund-content/refund-content.component';
import { WarchestWithdrawComponent } from './components/Accessories/Popups/warchest-withdraw/warchest-withdraw.component';
import { CompleteVideoComponent } from './components/Accessories/Popups/CreateVideo/complete-video/complete-video.component';
import { InDevelopmentVideoComponent } from './components/Accessories/Popups/CreateVideo/in-development-video/in-development-video.component';
import { RankComponent } from './components/Accessories/Popups/rank/rank.component';
import { AllLogsComponent } from './components/Authorized-Access/ContractLogs/all-logs/all-logs.component';
import { FailedLogsComponent } from './components/Authorized-Access/ContractLogs/failed-logs/failed-logs.component';
import { AwvUpdatesComponent } from './components/Authorized-Access/Channels/awv-updates/awv-updates.component';
import { ChannelRequestComponent } from './components/Authorized-Access/Channels/channel-request/channel-request.component';
import { AuthorizedUserHomeComponent } from './components/Authorized-Access/authorized-user-home/authorized-user-home.component';
import { AuthorizedAccessSidebarComponent } from './components/Authorized-Access/Accessories/Layout/authorized-access-sidebar/authorized-access-sidebar.component';
import { AdminDashboardComponent } from './components/Authorized-Access/admin-dashboard/admin-dashboard.component';
import { SecurityPageComponent } from './components/ProfilePages/security-page/security-page.component';
import { CryptoWalletPageComponent } from './components/ProfilePages/crypto-wallet-page/crypto-wallet-page.component';
import { ConfirmationComponent } from './components/Accessories/Popups/confirmation/confirmation.component';
import { TwoFactorAuthenticationComponent } from './components/Accessories/Popups/two-factor-authentication/two-factor-authentication.component';
import { NgOtpInputModule } from 'ng-otp-input';
import { ForgotPasswordPopupComponent } from './components/Login/forgot-password-popup/forgot-password-popup.component';
import { EditVideoComponent } from './components/Accessories/Popups/edit-video/edit-video.component';
import { ReportVideoComponent } from './components/Accessories/Popups/report-video/report-video.component';
import { MostHypedComponent } from './components/HomePages/most-hyped/most-hyped.component';
import { UpcomingReleasesComponent } from './components/HomePages/upcoming-releases/upcoming-releases.component';
import { InventionsComponent } from './components/HomePages/inventions/inventions.component';
import { InnovationsComponent } from './components/HomePages/innovations/innovations.component';
import { ShortFilmsComponent } from './components/HomePages/short-films/short-films.component';
import { PayLaterComponent } from './components/HomePages/pay-later/pay-later.component';
import { TimestampConverterPipe } from './Pipe/TimeStamp/timestamp-converter.pipe';
import { EnablePageComponent } from './components/CreateAccount/enable-page/enable-page.component';
import { CreateChannelComponent } from './components/Channel/create-channel/create-channel.component';
import { SelectPlatformComponent } from './components/Accessories/Popups/select-platform/select-platform.component';
import { ExternalRedirectComponent } from './components/Accessories/Redirects/external-redirect/external-redirect.component';
import { StreamerRedirectComponent } from './components/Accessories/Redirects/streamer-redirect/streamer-redirect.component';
import { GoogleLoginRedirectComponent } from './components/Accessories/Redirects/google-login-redirect/google-login-redirect.component';
import { NewChannelRequestedComponent } from './components/Accessories/Popups/new-channel-requested/new-channel-requested.component';
import { DisapproveChannelComponent } from './components/Accessories/Popups/disapprove-channel/disapprove-channel.component';
import { ChangeStreamerInfoComponent } from './components/Channel/change-streamer-info/change-streamer-info.component';
import { EditStreamerRedirectComponent } from './components/Accessories/Redirects/edit-streamer-redirect/edit-streamer-redirect.component';
import { EditChannelComponent } from './components/Accessories/Popups/edit-channel/edit-channel.component';
import { JwtModule } from '@auth0/angular-jwt';
import { environment } from 'src/Environment/environment';
import { BrowseChannelComponent } from './components/Channel/browse-channel/browse-channel.component';
import { ChannelSubscriptionsComponent } from './components/Channel/channel-subscriptions/channel-subscriptions.component';
import { FirstVideosComponent } from './components/Accessories/Popups/first-videos/first-videos.component';
import { manaStringFormatPipe } from './Pipe/manaString.pipe';
import { ReportsComponent } from './components/Authorized-Access/reports/reports.component';
import { UnresolvedLogsComponent } from './components/Authorized-Access/ContractLogs/unresolved-logs/unresolved-logs.component';
import { PrivacyPolicyComponent } from './components/Info/privacy-policy/privacy-policy.component';
import { RouterModule } from '@angular/router';
import { TransferManaComponent } from './components/Accessories/Popups/transfer-mana/transfer-mana.component';
import { LimitedUsePolicyComponent } from './components/Accessories/Popups/limited-use-policy/limited-use-policy.component';
import { ChangeChannelComponent } from './components/Authorized-Access/Channels/change-channel/change-channel.component';
import { MoviesComponent } from './components/HomePages/movies/movies.component';
import { SportsComponent } from './components/HomePages/sports/sports.component';
import { WatchNowPayLaterComponent } from './components/Accessories/Popups/ChannelContent/watch-now-pay-later/watch-now-pay-later.component';
import { RECAPTCHA_V3_SITE_KEY, RecaptchaV3Module } from "ng-recaptcha";
import { ResolveReportsComponent } from './components/Authorized-Access/Accessories/Popups/resolve-reports/resolve-reports.component';
import { ConcertsComponent } from './components/HomePages/concerts/concerts.component';
import { LeaderboardComponent } from './components/Leaderboard/leaderboard/leaderboard.component';
import { UserDetailsComponent } from './components/Leaderboard/user-details/user-details.component';
import { TransakPopupComponent } from './components/Accessories/Popups/transak-popup/transak-popup.component';


@NgModule({ declarations: [
        AppComponent,
        LoginAccountComponent,
        HomeComponent,
        ChannelSidebarComponent,
        FooterComponent,
        HeaderComponent,
        SearchComponent,
        AccountSignUpComponent,
        EmailPopupComponent,
        ForgotPasswordComponent,
        TOSComponent,
        ImportYoutubeVideoComponent,
        SelectMainVideoComponent,
        SelectTrailerVideoComponent,
        HomeSidebarComponent,
        FormateDatePipe,
        AddressShortenerPipe,
        ProfileSidebarComponent,
        MessageMainPageComponent,
        ProfilePageComponent,
        NumberFormatPipe,
        HeaderSearchComponent,
        ContentPageComponent,
        PurchasedPageComponent,
        DashboardPageComponent,
        AuthorizedAccessSidebarComponent,
        ChannelPageComponent,
        SingleBuyerComponent,
        ChannelBuyerComponent,
        DecimalRoundPipe,
        FundChannelComponent,
        ConnectPersonalWalletComponent,
        RefundContentComponent,
        WarchestWithdrawComponent,
        CompleteVideoComponent,
        InDevelopmentVideoComponent,
        RankComponent,
        AllLogsComponent,
        FailedLogsComponent,
        AwvUpdatesComponent,
        ChannelRequestComponent,
        AuthorizedUserHomeComponent,
        AdminDashboardComponent,
        SecurityPageComponent,
        CryptoWalletPageComponent,
        ConfirmationComponent,
        TwoFactorAuthenticationComponent,
        ForgotPasswordPopupComponent,
        EditVideoComponent,
        ReportVideoComponent,
        MostHypedComponent,
        UpcomingReleasesComponent,
        InventionsComponent,
        InnovationsComponent,
        ShortFilmsComponent,
        PayLaterComponent,
        TimestampConverterPipe,
        EnablePageComponent,
        CreateChannelComponent,
        SelectPlatformComponent,
        ExternalRedirectComponent,
        StreamerRedirectComponent,
        GoogleLoginRedirectComponent,
        NewChannelRequestedComponent,
        DisapproveChannelComponent,
        ChangeStreamerInfoComponent,
        EditStreamerRedirectComponent,
        EditChannelComponent,
        BrowseChannelComponent,
        ChannelSubscriptionsComponent,
        FirstVideosComponent,
        manaStringFormatPipe,
        ReportsComponent,
        UnresolvedLogsComponent,
        PrivacyPolicyComponent,
        TransferManaComponent,
        LimitedUsePolicyComponent,
        ChangeChannelComponent,
        MoviesComponent,
        SportsComponent,
        WatchNowPayLaterComponent,
        ResolveReportsComponent,
        ConcertsComponent,
        LeaderboardComponent,
        UserDetailsComponent,
        
    ],
    bootstrap: [AppComponent], imports: [BrowserModule,
        AppRoutingModule,
        NgbModule,
        FormsModule,
        CommonModule,
        ReactiveFormsModule,
        OAuthModule.forRoot(),
        NgOtpInputModule,
        JwtModule.forRoot({
            config: {
                tokenGetter: () => {
                    return localStorage.getItem('token');
                },
                allowedDomains: [environment.FrontEndURL],
                disallowedRoutes: [],
            },
        }),
        RecaptchaV3Module], providers: [
        JWTinterceptorProvider,
        { provide: RECAPTCHA_V3_SITE_KEY, useValue: environment.ReCaptcha_Site_Key },
        provideHttpClient(withInterceptorsFromDi())
    ] })
export class AppModule {
  

 }