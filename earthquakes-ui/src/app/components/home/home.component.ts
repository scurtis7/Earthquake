import { ChangeDetectorRef, Component, inject, OnInit, PLATFORM_ID } from '@angular/core';
import { Count } from "../../model/count";
import { RestService } from "../../service/rest.service";
import { ChartModule } from "primeng/chart";
import { Select } from "primeng/select";
import { DatePicker } from "primeng/datepicker";
import { FormsModule } from "@angular/forms";
import { isPlatformBrowser } from "@angular/common";

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [ChartModule, Select, DatePicker, FormsModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {

  counts: Count[];
  data: any;
  options: any;
  platformId = inject(PLATFORM_ID);

  periodOptions = [
    { label: 'Year', value: 'year' },
    { label: 'Month', value: 'month' },
    { label: 'Day', value: 'day' }
  ];

  readonly defaultDate = new Date(1950, 0, 1);
  readonly minDate = new Date(1900, 0, 1);
  readonly maxDate = new Date();

  period: string = 'year';
  startDate: Date = this.defaultDate;
  endDate: Date = this.maxDate;

  constructor(private restService: RestService, private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.refresh();
  }

  get startMaxDate(): Date {
    return this.endDate ?? this.maxDate;
  }

  get endMinDate(): Date {
    return this.startDate ?? this.minDate;
  }

  onFilterChange(): void {
    this.refresh();
  }

  private refresh(): void {
    this.getCounts(this.period, this.formatDate(this.startDate), this.formatDate(this.endDate));
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private getCounts(period: string, fromDate: string, toDate: string): void {
    this.restService.getCounts(period, fromDate, toDate)
      .subscribe(counts => {
        this.counts = counts;
        console.log(`Number of counts: ${counts.length}`);
        this.initChart();
      });
  }


  initChart() {
    if (isPlatformBrowser(this.platformId)) {
      const documentStyle = getComputedStyle(document.documentElement);
      const textColor = documentStyle.getPropertyValue('--p-text-color');
      const textColorSecondary = documentStyle.getPropertyValue('--p-text-muted-color');
      const surfaceBorder = documentStyle.getPropertyValue('--p-content-border-color');

      this.data = {
        labels: this.counts.map(count => count.date),
        datasets: [
          {
            label: 'Earthquakes',
            data: this.counts.map(count => count.count),
            fill: false,
            borderColor: documentStyle.getPropertyValue('--p-cyan-500'),
            tension: 0.4
          }
        ]
      };

      this.options = {
        maintainAspectRatio: false,
        aspectRatio: 0.6,
        plugins: {
          legend: {
            labels: {
              color: textColor
            }
          }
        },
        scales: {
          x: {
            ticks: {
              color: textColorSecondary
            },
            grid: {
              color: surfaceBorder,
              drawBorder: false
            }
          },
          y: {
            ticks: {
              color: textColorSecondary
            },
            grid: {
              color: surfaceBorder,
              drawBorder: false
            }
          }
        }
      };
      this.cd.markForCheck()
    }
  }



}
