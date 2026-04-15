package com.getcapacitor.community.datepicker;

import android.content.Context;
import android.content.res.Resources;

public class DatePickerTheme {

    /**
     * Resolve and validate a theme for MaterialDatePicker.
     * Only full MaterialComponents themes are safe for the picker dialog.
     * Unknown or incompatible custom theme names fall back to our safe defaults.
     */
    public static int get(String theme, Context context) {
        int safeLight = R.style.MaterialLightTheme;
        int safeDark = R.style.MaterialDarkTheme;

        if (theme == null || theme.trim().isEmpty()) return safeLight;

        switch (theme) {
            case "dark":
                return safeDark;
            case "light":
                return safeLight;
            case "legacyDark":
                return R.style.SpinnerDarkTheme;
            case "legacyLight":
                return R.style.SpinnerLightTheme;
        }

        // Try resolve custom style id from host app
        int resolved = 0;
        try {
            resolved = context.getResources().getIdentifier(theme, "style", context.getPackageName());
        } catch (Exception ignored) {}

        if (resolved == 0) return safeLight;

        // Heuristic validation by resource entry name
        try {
            Resources res = context.getResources();
            String entry = res.getResourceEntryName(resolved);
            if (entry != null) {
                String lower = entry.toLowerCase();
                boolean mentionsCalendar = lower.contains("materialcalendar");
                boolean isOverlay = lower.contains("themeoverlay");
                boolean looksLikeFullTheme = lower.contains("theme_materialcomponents") || lower.startsWith("theme_materialcomponents") || lower.contains("theme_material3") || lower.startsWith("theme_material3");
                if (mentionsCalendar && !isOverlay && looksLikeFullTheme) {
                    return resolved; // likely a proper full theme including calendar styling
                }
            }
        } catch (Resources.NotFoundException ignored) {}

        return safeLight;
    }
}
