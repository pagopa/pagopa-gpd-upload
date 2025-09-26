package it.gov.pagopa.gpd.upload.utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import io.micronaut.http.HttpStatus;
import it.gov.pagopa.gpd.upload.exception.AppException;

public class CommonCheck {
	
	private static final ZoneId TZ_EUROPE_ROME = ZoneId.of("Europe/Rome");
	
	private CommonCheck() {
		super();		
	}

	public static LocalDate parseOrDefaultToDate(String toDateStr) {
        if (toDateStr == null || toDateStr.isBlank()) {
            return LocalDate.now(TZ_EUROPE_ROME);
        }
        try {
            return LocalDate.parse(toDateStr); // expects YYYY-MM-DD
        } catch (DateTimeParseException ex) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "BAD_REQUEST",
                    "Invalid 'to' date. Expected format: YYYY-MM-DD"
            );
        }
    }

	public static LocalDate parseOrDefaultFromDate(String fromDateStr, LocalDate toDate) {
        if (fromDateStr == null || fromDateStr.isBlank()) {
            // default: 7-day window ending at 'to' (inclusive) => from = to - 6
            return toDate.minusDays(6);
        }
        try {
            return LocalDate.parse(fromDateStr);
        } catch (DateTimeParseException ex) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "BAD_REQUEST",
                    "Invalid 'from' date. Expected format: YYYY-MM-DD"
            );
        }
    }

}
