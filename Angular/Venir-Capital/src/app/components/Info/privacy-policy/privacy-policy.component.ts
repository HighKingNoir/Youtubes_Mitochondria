import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-privacy-policy',
  templateUrl: './privacy-policy.component.html',
  styleUrls: ['./privacy-policy.component.css']
})
export class PrivacyPolicyComponent {
  Logo = "assets/Logo.png"

  constructor(
    private activatedRoute: ActivatedRoute
  ){
    this.activatedRoute.fragment.subscribe((value) => {
      this.jumpTo(value)
    })
  }

  jumpTo(section: string | null){
    document.getElementById(section!)?.scrollIntoView({behavior: 'smooth'})
  }
}
