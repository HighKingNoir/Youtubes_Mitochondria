import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import * as SockJS from 'sockjs-client';
import { environment } from 'src/Environment/environment';
import * as Stomp from 'stompjs';

@Injectable({
  providedIn: 'root'
})
export class PricingService {

  private SivantisBackendURL = environment.BackendURL + "sse/manaPrice";
  
  constructor(){
    
  }
  
  
  
  getManaPrices(): Observable<string> {
    return new Observable<string>((observer) => {
      const jwtToken = localStorage.getItem('token');
      const eventSource = new EventSource(`${this.SivantisBackendURL}?token=${jwtToken}`);
      eventSource.onmessage = (event) => {
        observer.next(event.data);
      };

      eventSource.onerror = (error) => {
        observer.error(error);
      };

      eventSource.close = () => {
        observer.complete();
      };

      return () => {
        eventSource.close();
      };
    });
  }

 


}
