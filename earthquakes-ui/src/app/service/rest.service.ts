import { Injectable } from '@angular/core';
import { HttpClient } from "@angular/common/http";
import { Count } from "../model/count";
import { Observable } from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class RestService {

  constructor(private http: HttpClient) {
  }

  public getCounts(period: string, fromDate: string, toDate: string): Observable<Count[]> {
    const url = this.getBaseUrl() + '/counts?period=' + period + '&fromDate=' + fromDate + '&toDate=' + toDate;
    return this.http.get<Count[]>(url);
  }

  private getBaseUrl(): string {
    return "http://localhost:9777/earthquake";
  }

}
