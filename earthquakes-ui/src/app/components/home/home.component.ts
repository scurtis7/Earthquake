import { Component, OnInit } from '@angular/core';
import { Button } from "primeng/button";
import { Count } from "../../model/count";
import { RestService } from "../../service/rest.service";

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [Button],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {

  counts: Count[];

  constructor(private restService: RestService) {
  }

  ngOnInit(): void {
    // this.getCounts("year", "2000-01-01", "2026-07-01");
    // this.getCounts("month", "2000-01-01", "2026-07-01");
    this.getCounts("day", "1950-01-01", "1980-07-01");
  }

  private getCounts(period: string, fromDate: string, toDate: string): void {
    this.restService.getCounts(period, fromDate, toDate)
      .subscribe(counts => {
        this.counts = counts;
        console.log(`Number of counts: ${counts.length}`);
      });
  }

}
