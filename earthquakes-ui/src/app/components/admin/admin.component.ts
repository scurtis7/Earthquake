import { Component, OnInit } from '@angular/core';
import { DatePicker } from "primeng/datepicker";
import { FormsModule } from "@angular/forms";
import { Button } from "primeng/button";
import { RestService } from "../../service/rest.service";

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    DatePicker,
    FormsModule,
    Button
  ],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss'
})
export class AdminComponent implements OnInit {

  readonly minDate = new Date(1900, 0, 1);
  readonly maxDate = new Date();

  lastDate: string;
  startDate: Date = this.getYesterday();
  endDate: Date = this.maxDate;

  loadResponse = "";

  constructor(private restService: RestService) {
  }

  ngOnInit(): void {
    this.getLastDate();
  }

  private getLastDate() {
    this.restService.getLastDate()
      .subscribe(response => {
        this.lastDate = response;
      });
  }

  get startMaxDate(): Date {
    return this.endDate ?? this.maxDate;
  }

  get endMinDate(): Date {
    return this.startDate ?? this.minDate;
  }

  private getYesterday(): Date {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    return yesterday;
  }

  loadData(): void {
    const fromDate = this.formatDate(this.startDate);
    const toDate = this.formatDate(this.endDate);
    this.restService.updateEarthquakes(fromDate, toDate)
      .subscribe(response => {
        this.loadResponse = response;
      });
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  protected updateCounts() {
    alert('Update Counts');
  }
}
