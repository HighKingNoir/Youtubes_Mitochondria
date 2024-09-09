import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AlertService } from 'src/app/services/Alerts/alert.service';

@Component({
  selector: 'app-external-redirect',
  templateUrl: './external-redirect.component.html',
  styleUrls: ['./external-redirect.component.css']
})
export class ExternalRedirectComponent implements OnInit {
  
  constructor(
    private activatedRoute: ActivatedRoute, 
    private router: Router,
    private alertService: AlertService
  ) {}

  ngOnInit(): void {
    this.activatedRoute.paramMap.subscribe((params) => {
      const externalUrl = params.get('externalUrl');
      if (externalUrl) {
        // Redirect to the external URL
        window.location.href = externalUrl;
      } else {
        this.alertService.addAlert("Unable to navigate to twitch", "danger")
        this.router.navigate(['/Create/Channel']);
      }
    });
  }

}
