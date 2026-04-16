package com.getcapacitor.community.datepicker;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.widget.Button;
import androidx.fragment.app.FragmentActivity;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
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
        final TimePickerDialog timePicker = new TimePickerDialog(
            context,
            theme,
            (TimePickerDialog.OnTimeSetListener) (view, hourOfDay, minute) -> {
                calendar.set(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH),
                    hourOfDay,
                    minute
                );
                callback.resolve(Parse.dateToString(calendar.getTime(), options.format));
            },
            calendar.get(Calendar.HOUR),
            calendar.get(Calendar.MINUTE),
            options.is24h
        );

        timePicker.create();

        Button doneButton = timePicker.getButton(Dialog.BUTTON_POSITIVE);
        Button cancelButton = timePicker.getButton(Dialog.BUTTON_NEGATIVE);

        if (options.date != null) {
            calendar.setTime(options.date);
        }

        if (options.title != null) {
            timePicker.setTitle(options.title);
        }

        if (options.doneText != null) {
            doneButton.setText(options.doneText);
        }

        if (options.cancelText != null) {
            cancelButton.setText(options.cancelText);
        }

        cancelButton.setOnClickListener(v -> {
            callback.resolve(null);
            timePicker.dismiss();
        });

        timePicker.updateTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));

        timePicker.show();
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

        // Build MaterialDatePicker once, without extra fallbacks
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
        builder.setSelection(toUtcMidnight(calendar.getTime()));
        builder.setCalendarConstraints(constraintsBuilder.build());
        if (options.title != null) builder.setTitleText(options.title);
        if (options.doneText != null) builder.setPositiveButtonText(options.doneText);
        if (options.cancelText != null) builder.setNegativeButtonText(options.cancelText);
        if (theme != 0) builder.setTheme(theme);
        MaterialDatePicker<Long> datePicker = builder.build();

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
