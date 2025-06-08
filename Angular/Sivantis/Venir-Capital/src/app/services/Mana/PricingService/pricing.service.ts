import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import * as SockJS from 'sockjs-client';
import { environment } from 'src/Environment/environment';
import * as Stomp from 'stompjs';

@Injectable({
  providedIn: 'root'
})
export class PricingService {

  private SivantisBackendURL = environment.BackendURL + "sse";
  
  constructor(
  ){
    
  }

  getManaPrices(userId: string): Observable<string> {
    return new Observable<string>((observer) => {
      const params = new URLSearchParams({
        sessionId: userId,
      });

      const eventSource = new EventSource(`${this.SivantisBackendURL}/manaPrice?${params.toString()}`);

      eventSource.onmessage = (event) => {
        observer.next(event.data);
      };

      eventSource.onerror = (error) => {
        console.error('SSE error:', error);
        observer.error(error);
        eventSource.close();
      };

      return () => {
        eventSource.close();
        // Optional: tell backend to disconnect emitter
        fetch(`${this.SivantisBackendURL}/disconnect`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({ sessionId: userId }),
          }).catch(err => {
            console.warn('Error disconnecting SSE emitter:', err);
          });
          };
        });
  }


}
