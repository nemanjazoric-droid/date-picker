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

    public void launchTime(DatePickerResolve callback) {
        // Initialize calendar with provided date if available
        if (options.date != null) {
            calendar.setTime(options.date);
        }

        // Determine 12/24h format
        int timeFormat = options.is24h
            ? TimeFormat.CLOCK_24H
            : TimeFormat.CLOCK_12H;

        // Try to build MaterialTimePicker with safe fallbacks to avoid crashes
        MaterialTimePicker picker = null;
        Exception lastError = null;

        // Attempt 1: use resolved theme (if any)
        try {
            MaterialTimePicker.Builder b1 = new MaterialTimePicker.Builder();
            b1.setTimeFormat(timeFormat);
            b1.setHour(calendar.get(Calendar.HOUR_OF_DAY));
            b1.setMinute(calendar.get(Calendar.MINUTE));
            if (options.title != null) b1.setTitleText(options.title);
            if (options.doneText != null) b1.setPositiveButtonText(options.doneText);
            if (options.cancelText != null) b1.setNegativeButtonText(options.cancelText);
            if (theme != 0) b1.setTheme(theme);
            picker = b1.build();
        } catch (Exception e) {
            lastError = e;
        }

        // Attempt 2: try safe light theme
        if (picker == null) {
            try {
                MaterialTimePicker.Builder b2 = new MaterialTimePicker.Builder();
                b2.setTimeFormat(timeFormat);
                b2.setHour(calendar.get(Calendar.HOUR_OF_DAY));
                b2.setMinute(calendar.get(Calendar.MINUTE));
                if (options.title != null) b2.setTitleText(options.title);
                if (options.doneText != null) b2.setPositiveButtonText(options.doneText);
                if (options.cancelText != null) b2.setNegativeButtonText(options.cancelText);
                b2.setTheme(R.style.MaterialLightTheme);
                picker = b2.build();
            } catch (Exception e) {
                lastError = e;
            }
        }

        // Attempt 3: build without any theme (use host defaults)
        if (picker == null) {
            try {
                MaterialTimePicker.Builder b3 = new MaterialTimePicker.Builder();
                b3.setTimeFormat(timeFormat);
                b3.setHour(calendar.get(Calendar.HOUR_OF_DAY));
                b3.setMinute(calendar.get(Calendar.MINUTE));
                if (options.title != null) b3.setTitleText(options.title);
                if (options.doneText != null) b3.setPositiveButtonText(options.doneText);
                if (options.cancelText != null) b3.setNegativeButtonText(options.cancelText);
                picker = b3.build();
            } catch (Exception e) {
                lastError = e;
            }
        }

        if (picker == null) {
            callback.reject(lastError != null ? lastError.getMessage() : "Failed to open time picker");
            return;
        }

        // Attach listeners
        MaterialTimePicker finalPicker = picker;
        picker.addOnPositiveButtonClickListener(v -> {
            int hour = finalPicker.getHour();
            int minute = finalPicker.getMinute();
            calendar.set(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                hour,
                minute
            );
            callback.resolve(Parse.dateToString(calendar.getTime(), options.format));
        });
        picker.addOnNegativeButtonClickListener(v -> callback.resolve(null));
        picker.addOnCancelListener(dialog -> callback.resolve(null));

        // Show via FragmentActivity
        androidx.fragment.app.FragmentActivity activity = toFragmentActivity(context);
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            callback.resolve(null);
            return;
        }
        try {
            picker.show(activity.getSupportFragmentManager(), "TIME_PICKER");
        } catch (Exception e) {
            callback.reject(e.getMessage());
        }
    }

    public void launchDate(DatePickerResolve callback) {
        if (options.date != null) {
            calendar.setTime(options.date);
        }

        // Build calendar constraints based on min/max
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
        if (options.min != null) {
            constraintsBuilder.setStart(toUtcMidnight(options.min));
        }
        if (options.max != null) {
            constraintsBuilder.setEnd(toUtcMidnight(options.max));
        }

        // Try to build MaterialDatePicker with multiple safe fallbacks to avoid crashes
        MaterialDatePicker<Long> datePicker = null;
        Exception lastError = null;

        // Attempt 1: use resolved theme (if any)
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

        // Attempt 2: try safe light theme
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

        // Attempt 3: build without any theme (use host defaults)
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
            callback.reject(lastError != null ? lastError.getMessage() : "Failed to open date picker");
            return;
        }

        // Handle result
        datePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) {
                callback.resolve(null);
                return;
            }
            // Convert UTC millis at midnight to local calendar date
            Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utcCal.setTimeInMillis(selection);
            calendar.set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH));

            if ("dateAndTime".equals(options.mode)) {
                options.date = calendar.getTime();
                launchTime(callback);
            } else {
                callback.resolve(Parse.dateToString(calendar.getTime(), options.format));
            }
        });

        datePicker.addOnNegativeButtonClickListener(v -> callback.resolve(null));
        datePicker.addOnCancelListener(dialog -> callback.resolve(null));

        // Show safely via FragmentActivity
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
