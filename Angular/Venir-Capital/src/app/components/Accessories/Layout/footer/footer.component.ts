import { Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.css']
})
export class FooterComponent {
  xLogo = "assets/twitter-x-logo.png"
  year:number = new Date().getFullYear();

  
}
