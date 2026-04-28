package com.getcapacitor.community.datepicker;

import android.content.Context;
import android.content.res.Resources;

public class DatePickerTheme {

    public static int get(String theme, Context context) {
        Integer result = context.getResources().getIdentifier(theme, "style", context.getPackageName());

        if (result != 0) return result;

        switch (theme) {
          case "dark":
              result = R.style.MaterialDarkTheme;
              break;
          case "light":
              result = R.style.MaterialLightTheme;
              break;
          case "legacyDark":
              result = R.style.SpinnerDarkTheme;
              break;
          case "legacyLight":
              result = R.style.SpinnerLightTheme;
              break;
          default:
              result = R.style.MaterialLightTheme;
              break;
        }

          return result;
    }

    public static int getTimePickerTheme(String theme, Context context) {
        if (theme == null || theme.isEmpty()) {
            return R.style.MyCustomLightTimePicker;
        }
        int result = context.getResources().getIdentifier(theme, "style", context.getPackageName());
        if (result != 0) return result;
        return R.style.MyCustomLightTimePicker;
    }
}
