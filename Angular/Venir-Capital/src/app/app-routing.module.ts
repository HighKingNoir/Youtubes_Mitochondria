import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SearchComponent } from './components/Accessories/Layout/search/search.component';
import { AccountSignUpComponent } from './components/CreateAccount/account-sign-up/account-sign-up.component';
import { TOSComponent } from './components/Info/tos/tos.component';
import { ForgotPasswordComponent } from './components/Login/forgot-password/forgot-password.component';
import { LoginAccountComponent } from './components/Login/login-account/login-account.component';
import { MessageMainPageComponent } from './components/MessagePages/message-main-page/message-main-page.component';
import { ProfilePageComponent } from './components/ProfilePages/profile-page/profile-page.component';
import { ContentPageComponent } from './components/Content/content-page/content-page.component';
import { PurchasedPageComponent } from './components/Content/purchased-page/purchased-page.component';
import { DashboardPageComponent } from './components/Content/dashboard/dashboard-page.component';
import { ChannelPageComponent } from './components/Channel/channel-page/channel-page.component';
import { SingleBuyerComponent } from './components/ContentVideoPages/single-buyer/single-buyer.component';
import { ChannelBuyerComponent } from './components/ContentVideoPages/channel-buyer/channel-buyer.component';
import { authorizedPersonnel, adminOnly } from './services/AuthGuard/authorizedPersonnel';
import { AllLogsComponent } from './components/Authorized-Access/ContractLogs/all-logs/all-logs.component';
import { FailedLogsComponent } from './components/Authorized-Access/ContractLogs/failed-logs/failed-logs.component';
import { AwvUpdatesComponent } from './components/Authorized-Access/Channels/awv-updates/awv-updates.component';
import { ChannelRequestComponent } from './components/Authorized-Access/Channels/channel-request/channel-request.component';
import { AdminDashboardComponent } from './components/Authorized-Access/admin-dashboard/admin-dashboard.component';
import { AuthorizedUserHomeComponent } from './components/Authorized-Access/authorized-user-home/authorized-user-home.component';
import { SecurityPageComponent } from './components/ProfilePages/security-page/security-page.component';
import { CryptoWalletPageComponent } from './components/ProfilePages/crypto-wallet-page/crypto-wallet-page.component';
import { HomeComponent } from './components/HomePages/home/home.component';
import { InnovationsComponent } from './components/HomePages/innovations/innovations.component';
import { InventionsComponent } from './components/HomePages/inventions/inventions.component';
import { ShortFilmsComponent } from './components/HomePages/short-films/short-films.component';
import { UpcomingReleasesComponent } from './components/HomePages/upcoming-releases/upcoming-releases.component';
import { MostHypedComponent } from './components/HomePages/most-hyped/most-hyped.component';
import { PayLaterComponent } from './components/HomePages/pay-later/pay-later.component';
import { EnablePageComponent } from './components/CreateAccount/enable-page/enable-page.component';
import { CreateChannelComponent } from './components/Channel/create-channel/create-channel.component';
import { GoogleLoginRedirectComponent } from './components/Accessories/Redirects/google-login-redirect/google-login-redirect.component';
import { StreamerRedirectComponent } from './components/Accessories/Redirects/streamer-redirect/streamer-redirect.component';
import { ExternalRedirectComponent } from './components/Accessories/Redirects/external-redirect/external-redirect.component';
import { ChangeStreamerInfoComponent } from './components/Channel/change-streamer-info/change-streamer-info.component';
import { EditStreamerRedirectComponent } from './components/Accessories/Redirects/edit-streamer-redirect/edit-streamer-redirect.component';
import { BrowseChannelComponent } from './components/Channel/browse-channel/browse-channel.component';
import { ChannelSubscriptionsComponent } from './components/Channel/channel-subscriptions/channel-subscriptions.component';
import { ReportsComponent } from './components/Authorized-Access/reports/reports.component';
import { UnresolvedLogsComponent } from './components/Authorized-Access/ContractLogs/unresolved-logs/unresolved-logs.component';
import { PrivacyPolicyComponent } from './components/Info/privacy-policy/privacy-policy.component';
import { ChangeChannelComponent } from './components/Authorized-Access/Channels/change-channel/change-channel.component';
import { MoviesComponent } from './components/HomePages/movies/movies.component';
import { SportsComponent } from './components/HomePages/sports/sports.component';
import { ConcertsComponent } from './components/HomePages/concerts/concerts.component';
import { LeaderboardComponent } from './components/Leaderboard/leaderboard/leaderboard.component';
import { UserDetailsComponent } from './components/Leaderboard/user-details/user-details.component';






const routes: Routes = [
  {path: '', component: HomeComponent},
  {path:'Innovations', component: InnovationsComponent},
  {path:'Inventions', component: InventionsComponent},
  {path:'ShortFilms', component: ShortFilmsComponent},
  {path:'UpcomingReleases', component: UpcomingReleasesComponent},
  {path:'MostHyped', component: MostHypedComponent},
  {path:'Movies', component: MoviesComponent},
  {path:'Sports', component: SportsComponent},
  {path:'Concerts', component: ConcertsComponent},


  {path:'PayLater', component: PayLaterComponent},

  {path:'Content', component: ContentPageComponent},
  {path:'Content/Purchased', component: PurchasedPageComponent},
  {path:'Content/Dashboard', component: DashboardPageComponent},

  {path:'Search', component: SearchComponent},

  //ContentVideoPages
  {path:'Auction/:id', component: SingleBuyerComponent},
  {path:'Buy/:id', component: ChannelBuyerComponent},
  
  {path:'ForgotPassword/:secret', component: ForgotPasswordComponent},  
  {path:'Enable/:secret', component: EnablePageComponent},
  
  //ChannelPages
  {path:'Channels', component: BrowseChannelComponent},
  {path:'Subscriptions/Channels', component: ChannelSubscriptionsComponent},
  {path:'Channel/:name', component: ChannelPageComponent},
  {path:'Create/Channel', component: CreateChannelComponent},
  {path:'Change/Channel/:name', component: ChangeStreamerInfoComponent},
  

  //Redirects
  {path:'Edit/Channel/:platform', component: EditStreamerRedirectComponent },
  {path:'Create/Channel/:platform', component: StreamerRedirectComponent},
  {path:'Login/Google', component: GoogleLoginRedirectComponent},
  {path: 'external-redirect', component: ExternalRedirectComponent},


  //Message Pages
  {path:'Messages', component: MessageMainPageComponent},

  //LeaderBoard
  {path:'LeaderBoard', component: LeaderboardComponent},
  {path:'LeaderBoard/:username', component: UserDetailsComponent},

  //Profile Pages
  {path:'Settings/Profile', component: ProfilePageComponent},
  {path:'Settings/Security', component: SecurityPageComponent},
  {path:'Settings/CryptoWallet', component: CryptoWalletPageComponent},

  //AuthorizedUser Pages
  {path:'AuthorizedUser', component: AuthorizedUserHomeComponent, canActivate:[authorizedPersonnel]},
  {path:'AuthorizedUser/Logs/All', component: AllLogsComponent, canActivate:[authorizedPersonnel]},
  {path:'AuthorizedUser/Logs/Unresolved', component: UnresolvedLogsComponent, canActivate:[authorizedPersonnel]},
  {path:'AuthorizedUser/Logs/Failed', component: FailedLogsComponent, canActivate:[authorizedPersonnel]},
  {path:'AuthorizedUser/Channels/Request', component: ChannelRequestComponent, canActivate:[authorizedPersonnel]},
  {path:'AuthorizedUser/Channels/Updates', component: AwvUpdatesComponent, canActivate:[authorizedPersonnel]},
  {path:'AuthorizedUser/Channels/Change', component: ChangeChannelComponent, canActivate:[authorizedPersonnel]},
  {path:'AuthorizedUser/Reports', component: ReportsComponent, canActivate:[authorizedPersonnel]},
  {path:'AuthorizedUser/AdminDashboard', component: AdminDashboardComponent, canActivate:[adminOnly]},
  
  //Information Pages
  {path: 'Info/TOS', component: TOSComponent},
  {path: 'Info/Privacy', component: PrivacyPolicyComponent},
];

@NgModule({
  imports: [RouterModule.forRoot(routes,
    {
      scrollPositionRestoration: 'enabled',
      anchorScrolling: 'enabled',
      onSameUrlNavigation: 'reload',
      scrollOffset: [0, 50],
  })],
  exports: [RouterModule]
})
export class AppRoutingModule { }
