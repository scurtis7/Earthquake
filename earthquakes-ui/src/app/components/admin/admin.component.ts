import { Component, OnDestroy, OnInit } from '@angular/core';
import { NgTemplateOutlet } from "@angular/common";
import { DatePicker } from "primeng/datepicker";
import { FormsModule } from "@angular/forms";
import { Button } from "primeng/button";
import { Message } from "primeng/message";
import { ProgressBar } from "primeng/progressbar";
import { HttpErrorResponse } from "@angular/common/http";
import { finalize, Observable } from "rxjs";
import { RestService } from "../../service/rest.service";
import { getErrorMessage } from "../../service/error-message";

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    DatePicker,
    FormsModule,
    Button,
    Message,
    ProgressBar,
    NgTemplateOutlet
  ],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss'
})
export class AdminComponent implements OnInit, OnDestroy {

  readonly minDate = new Date(1900, 0, 1);
  readonly maxDate = new Date();

  lastDate: string;
  lastDateError = "";
  startDate: Date = this.getYesterday();
  endDate: Date = this.maxDate;
  fromDate: Date = this.getYesterday();

  loadResponse = "";
  loadError = "";
  loading = false;
  activeSection: 'period' | 'from' | null = null;
  progressMessage = "";
  elapsedSeconds = 0;
  private elapsedTimer: ReturnType<typeof setInterval> | undefined;

  constructor(private restService: RestService) {
  }

  ngOnInit(): void {
    this.getLastDate();
  }

  ngOnDestroy(): void {
    this.stopElapsedTimer();
  }

  private getLastDate() {
    this.lastDateError = "";
    this.restService.getLastDate()
      .subscribe({
        next: response => {
          this.lastDate = response;
        },
        error: (error: HttpErrorResponse) => {
          this.lastDateError = `Failed to get the last date: ${getErrorMessage(error)}`;
        }
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
    this.runUpdate('period', this.restService.updateEarthquakes(fromDate, toDate), 'load earthquake data',
      `Loading earthquake data for ${fromDate} to ${toDate}…`, () => this.getLastDate());
  }

  updateCounts() {
    const fromDate = this.formatDate(this.startDate);
    const toDate = this.formatDate(this.endDate);
    this.runUpdate('period', this.restService.updateEarthquakeCounts(fromDate, toDate), 'update earthquake counts',
      `Updating earthquake counts for ${fromDate} to ${toDate}…`);
  }

  loadDataFromDate(): void {
    const startDate = this.formatDate(this.fromDate);
    this.runUpdate('from', this.restService.updateEarthquakesFromDate(startDate), 'load earthquake data',
      `Loading earthquake data from ${startDate}…`, () => this.getLastDate());
  }

  private runUpdate(section: 'period' | 'from', request: Observable<string>, action: string, progressMessage: string,
                    onSuccess?: () => void) {
    this.activeSection = section;
    this.loading = true;
    this.loadResponse = "";
    this.loadError = "";
    this.progressMessage = progressMessage;
    this.startElapsedTimer();
    request
      .pipe(finalize(() => {
        this.loading = false;
        this.stopElapsedTimer();
      }))
      .subscribe({
        next: response => {
          this.loadResponse = response;
          onSuccess?.();
        },
        error: (error: HttpErrorResponse) => {
          this.loadError = `Failed to ${action}: ${getErrorMessage(error)}`;
        }
      });
  }

  get elapsedTime(): string {
    const minutes = Math.floor(this.elapsedSeconds / 60);
    const seconds = String(this.elapsedSeconds % 60).padStart(2, '0');
    return `${minutes}:${seconds}`;
  }

  private startElapsedTimer() {
    this.stopElapsedTimer();
    this.elapsedSeconds = 0;
    this.elapsedTimer = setInterval(() => this.elapsedSeconds++, 1000);
  }

  private stopElapsedTimer() {
    clearInterval(this.elapsedTimer);
    this.elapsedTimer = undefined;
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

}
