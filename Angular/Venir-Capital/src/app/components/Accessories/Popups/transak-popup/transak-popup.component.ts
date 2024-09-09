import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-transak-popup',
  standalone: true,
  imports: [],
  templateUrl: './transak-popup.component.html',
  styleUrl: './transak-popup.component.css'
})
export class TransakPopupComponent {
example = "assets/Buy_Mana.png"

constructor(
  public activeModal: NgbActiveModal,
  private router: Router, 
){

}

routeToTransak(){
  window.open(`https://global.transak.com`, '_blank');
}
}
