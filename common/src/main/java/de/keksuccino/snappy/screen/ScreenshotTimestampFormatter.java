package de.keksuccino.snappy.screen;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/**
 * Formats screenshot timestamps with the system's formatting locale, time zone, and preferred localized date/time conventions.
 */
final class ScreenshotTimestampFormatter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(Locale.getDefault(Locale.Category.FORMAT));

    private ScreenshotTimestampFormatter() {
    }

    @NotNull
    static String format(long epochMillis) {
        return format(Instant.ofEpochMilli(epochMillis));
    }

    @NotNull
    static String format(@NotNull Instant instant) {
        return FORMATTER.format(instant.atZone(ZoneId.systemDefault()));
    }

}
