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

	// Checks retention window.
	// - If the whole range is older than the cutoff  -> 410 GONE
	// - If the range partially overlaps the cutoff   -> 400 BAD_REQUEST (ask client to shift 'from')
	public static void enforceRetention(LocalDate fromDate, LocalDate toDate) {
		final LocalDate today = LocalDate.now(ZoneId.of("Europe/Rome"));
		final LocalDate cutoffInclusive = today.minusDays(60);

		// Entire range is older than (or equal to) cutoff -> data has been purged
		// Meaning: toDate <= cutoffInclusive
		if (!toDate.isAfter(cutoffInclusive)) {
			throw new AppException(
					HttpStatus.GONE,
					"GONE",
					String.format(
							"Requested range [%s, %s] is older than the 60-day retention (cutoff %s): data has been purged and is no longer available.",
							fromDate, toDate, cutoffInclusive
							)
					);
		}

		// Partial overlap: advise client to shift 'from' to the cutoff or later
		// Meaning: fromDate < cutoffInclusive < toDate
		if (fromDate.isBefore(cutoffInclusive)) {
			throw new AppException(
					HttpStatus.BAD_REQUEST,
					"BAD_REQUEST",
					String.format(
							"Requested 'from' (%s) is older than the 60-day retention (cutoff %s). Use from >= %s.",
							fromDate, cutoffInclusive, cutoffInclusive
							)
					);
		}
	}

}
