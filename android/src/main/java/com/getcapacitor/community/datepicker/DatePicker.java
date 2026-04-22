package com.getcapacitor.community.datepicker;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import androidx.fragment.app.FragmentActivity;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DatePicker {

    private Calendar calendar;
    private DatePickerOptions options;
    private Context context;
    private int theme;

    public DatePicker(DatePickerOptions options, Context context) {
        calendar = Calendar.getInstance();
        this.options = options;
        this.context = context;
        theme = DatePickerTheme.get(this.options.theme, context);

        if (this.options.locale != null) {
            Locale locale = new Locale(this.options.locale);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
        }
    }

    /**
     * Opens a Material design time picker and resolves the result via the provided callback.
     *
     * Behavior
     * - Initializes the internal Calendar from options.date when provided.
     * - Honors 12/24 hour mode based on options.is24h using Material TimeFormat.
     * - Applies optional title (options.title) to the picker.
     * - Avoids overriding positive/negative button texts (not reliably supported by MaterialTimePicker).
     * - Avoids applying full dialog themes. MaterialTimePicker expects a ThemeOverlay; relying on the host
     *   app theme here is safer and prevents crashes due to missing attributes.
     * - Returns the formatted date string on positive, or null on negative/cancel.
     */
    public void launchTime(DatePickerResolve callback) {
        // Initialize calendar with provided date if available
        if (options.date != null) {
            calendar.setTime(options.date);
        }

        // Determine 12/24h format expected by MaterialTimePicker
        int timeFormat = options.is24h
            ? TimeFormat.CLOCK_24H
            : TimeFormat.CLOCK_12H;

        // Build MaterialTimePicker with minimal theming to avoid crashes on OEM/custom themes
        MaterialTimePicker picker = null;
        Exception lastError = null;

        // Attempt 1: build normally (no explicit full dialog theme)
        try {
            MaterialTimePicker.Builder b1 = new MaterialTimePicker.Builder();
            b1.setTimeFormat(timeFormat);
            b1.setHour(calendar.get(Calendar.HOUR_OF_DAY));
            b1.setMinute(calendar.get(Calendar.MINUTE));
            if (options.title != null) b1.setTitleText(options.title);
            // Do NOT set custom positive/negative texts; not supported across all Material versions.
            // Do NOT apply full dialog themes; TimePicker expects a ThemeOverlay and wrong theme may crash.
            picker = b1.build();
        } catch (Exception e) {
            lastError = e;
        }

        // Attempt 2: retry with a fresh builder as a minimal fallback
        if (picker == null) {
            try {
                MaterialTimePicker.Builder b2 = new MaterialTimePicker.Builder();
                b2.setTimeFormat(timeFormat);
                b2.setHour(calendar.get(Calendar.HOUR_OF_DAY));
                b2.setMinute(calendar.get(Calendar.MINUTE));
                if (options.title != null) b2.setTitleText(options.title);
                // Leave other settings to defaults for compatibility
                picker = b2.build();
            } catch (Exception e) {
                lastError = e;
            }
        }

        if (picker == null) {
            // If both attempts failed, reject with the last error message (if any)
            callback.reject(lastError != null ? lastError.getMessage() : "Failed to open time picker");
            return;
        }

        // Listeners: resolve on positive, return null on negative/cancel
        MaterialTimePicker finalPicker = picker;
        picker.addOnPositiveButtonClickListener(v -> {
            int hour = finalPicker.getHour();
            int minute = finalPicker.getMinute();
            // Keep current Y/M/D but update H/M with chosen values
            calendar.set(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                hour,
                minute
            );
            // Format according to options.format and resolve
            callback.resolve(Parse.dateToString(calendar.getTime(), options.format));
        });
        picker.addOnNegativeButtonClickListener(v -> callback.resolve(null));
        picker.addOnCancelListener(dialog -> callback.resolve(null));

        // Show via FragmentActivity on the UI thread to avoid lifecycle crashes
        androidx.fragment.app.FragmentActivity activity = toFragmentActivity(context);
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            callback.resolve(null);
            return;
        }
        try {
            activity.runOnUiThread(() -> {
                try {
                    picker.show(activity.getSupportFragmentManager(), "TIME_PICKER");
                } catch (Exception e) {
                    callback.reject(e.getMessage());
                }
            });
        } catch (Exception e) {
            callback.reject(e.getMessage());
        }
    }

    /**
     * Opens a Material design date picker and resolves the result via the provided callback.
     *
     * Behavior
     * - Initializes the internal Calendar from options.date when provided.
     * - Applies min/max constraints (inclusive) if options.min/options.max are set. Values are normalized
     *   to UTC midnight because MaterialDatePicker expects UTC-based epoch millis.
     * - Applies optional title and custom positive/negative button texts (supported by MaterialDatePicker).
     * - Uses a safe theming strategy with small fallbacks to avoid crashes on devices/themes that miss
     *   certain attributes. Falls back to a bundled light dialog theme, then to host defaults.
     * - If mode is "dateAndTime", chains to launchTime after the user picks the date; otherwise resolves
     *   the formatted date string immediately.
     */
    public void launchDate(DatePickerResolve callback) {
        // Initialize calendar with provided date if available
        if (options.date != null) {
            calendar.setTime(options.date);
        }

        // Build calendar constraints based on min/max. Normalize to UTC midnight for correctness
        // because MaterialDatePicker operates on UTC epoch millis.
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
        if (options.min != null) {
            constraintsBuilder.setStart(toUtcMidnight(options.min));
        }
        if (options.max != null) {
            constraintsBuilder.setEnd(toUtcMidnight(options.max));
        }

        // Build MaterialDatePicker with a few safe fallbacks (theme -> bundled light -> no theme)
        MaterialDatePicker<Long> datePicker = null;
        Exception lastError = null;

        // Attempt 1: use resolved theme (if any). MaterialDatePicker supports full dialog themes safely.
        try {
            MaterialDatePicker.Builder<Long> b1 = MaterialDatePicker.Builder.datePicker();
            b1.setSelection(toUtcMidnight(calendar.getTime()));
            b1.setCalendarConstraints(constraintsBuilder.build());
            if (options.title != null) b1.setTitleText(options.title);
            if (options.doneText != null) b1.setPositiveButtonText(options.doneText);
            if (options.cancelText != null) b1.setNegativeButtonText(options.cancelText);
            if (theme != 0) b1.setTheme(theme);
            datePicker = b1.build();
        } catch (Exception e) {
            lastError = e;
        }

        // Attempt 2: try safe light theme from this library, which declares calendar overlays
        if (datePicker == null) {
            try {
                MaterialDatePicker.Builder<Long> b2 = MaterialDatePicker.Builder.datePicker();
                b2.setSelection(toUtcMidnight(calendar.getTime()));
                b2.setCalendarConstraints(constraintsBuilder.build());
                if (options.title != null) b2.setTitleText(options.title);
                if (options.doneText != null) b2.setPositiveButtonText(options.doneText);
                if (options.cancelText != null) b2.setNegativeButtonText(options.cancelText);
                b2.setTheme(R.style.MaterialLightTheme);
                datePicker = b2.build();
            } catch (Exception e) {
                lastError = e;
            }
        }

        // Attempt 3: build without any explicit theme (use host defaults)
        if (datePicker == null) {
            try {
                MaterialDatePicker.Builder<Long> b3 = MaterialDatePicker.Builder.datePicker();
                b3.setSelection(toUtcMidnight(calendar.getTime()));
                b3.setCalendarConstraints(constraintsBuilder.build());
                if (options.title != null) b3.setTitleText(options.title);
                if (options.doneText != null) b3.setPositiveButtonText(options.doneText);
                if (options.cancelText != null) b3.setNegativeButtonText(options.cancelText);
                datePicker = b3.build();
            } catch (Exception e) {
                lastError = e;
            }
        }

        if (datePicker == null) {
            // If all attempts failed, reject with the last error message (if any)
            callback.reject(lastError != null ? lastError.getMessage() : "Failed to open date picker");
            return;
        }

        // Handle result selection
        datePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) {
                callback.resolve(null);
                return;
            }
            // Convert UTC millis at midnight to a local date in our Calendar
            Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utcCal.setTimeInMillis(selection);
            calendar.set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH));

            // If the overall mode requires time as well, reuse the selected date as the base
            // and then open the time picker. Otherwise, resolve immediately.
            if ("dateAndTime".equals(options.mode)) {
                options.date = calendar.getTime();
                launchTime(callback);
            } else {
                callback.resolve(Parse.dateToString(calendar.getTime(), options.format));
            }
        });

        // Resolve null if user cancels or presses negative
        datePicker.addOnNegativeButtonClickListener(v -> callback.resolve(null));
        datePicker.addOnCancelListener(dialog -> callback.resolve(null));

        // Show safely via FragmentActivity (MaterialDatePicker manages its own dialog fragment)
        FragmentActivity activity = toFragmentActivity(context);
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            callback.resolve(null);
            return;
        }
        try {
            datePicker.show(activity.getSupportFragmentManager(), "DATE_PICKER");
        } catch (Exception e) {
            callback.reject(e.getMessage());
        }
    }

    private long toUtcMidnight(Date date) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private FragmentActivity toFragmentActivity(Context ctx) {
        // Unwrap context chain to find a FragmentActivity
        while (ctx instanceof ContextWrapper) {
            if (ctx instanceof FragmentActivity) return (FragmentActivity) ctx;
            ctx = ((ContextWrapper) ctx).getBaseContext();
        }
        return null;
    }

    public void open(DatePickerResolve callback) throws java.text.ParseException {
        if (options.mode.equals("time")) {
            launchTime(callback);
        } else {
            launchDate(callback);
        }
    }
}
