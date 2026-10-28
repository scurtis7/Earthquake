import { HttpErrorResponse } from "@angular/common/http";
import { ErrorResponse } from "../model/error-response";

export function getErrorMessage(error: HttpErrorResponse): string {
  if (error.status === 0) {
    return 'Unable to reach the earthquake server. Check that it is running.';
  }
  // Text requests get the server's ErrorResponse JSON as a string, JSON requests as an object
  const body = parseErrorResponse(error.error);
  if (body?.message) {
    return `${body.message} (${error.status} ${body.status ?? error.statusText})`;
  }
  return `The server returned ${error.status} ${error.statusText}.`;
}

function parseErrorResponse(body: unknown): Partial<ErrorResponse> | null {
  if (typeof body !== 'string') {
    return body && typeof body === 'object' ? body as Partial<ErrorResponse> : null;
  }
  try {
    return JSON.parse(body);
  } catch {
    return body.trim() ? { message: body.trim() } : null;
  }
}
