import { Injectable } from '@angular/core';
import { NgbAlertConfig } from '@ng-bootstrap/ng-bootstrap';
import { Alert } from 'src/app/models/Alerts/alerts';

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  alerts: Alert[] = [];

  constructor(alertConfig: NgbAlertConfig) {
    alertConfig.animation = false;
  }

  addAlert(message: string, type: string) {
    // Add a new alert at the beginning of the array
    const alert: Alert = { message, type };
    this.alerts.unshift(alert);

    // Remove the oldest alert if there are more than 3
    if (this.alerts.length > 3) {
      this.alerts.pop();
    }

    // Automatically remove the alert after 5 seconds
    setTimeout(() => {
      this.removeAlert(alert);
    }, 4000);
  }

  removeAlert(alert: Alert) {
    const index = this.alerts.indexOf(alert);
    if (index > -1) {
      this.alerts.splice(index, 1);
    }
  }
}
