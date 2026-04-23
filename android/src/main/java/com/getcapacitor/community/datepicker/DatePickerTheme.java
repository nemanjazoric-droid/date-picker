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

     /**
     * Returns a ThemeOverlay resource id suitable for MaterialTimePicker.setTheme().
     * MaterialTimePicker requires a ThemeOverlay, not a full dialog theme.
     */
    public static int getForTimePicker(String theme, Context context) {
        // Allow fully custom styles declared in the host app
        Integer result = context.getResources().getIdentifier(theme, "style", context.getPackageName());
        if (result != 0) return result;

        switch (theme) {
            case "dark":
            case "legacyDark":
                return R.style.MaterialTimePickerDark;
            case "light":
            case "legacyLight":
            default:
                return R.style.MaterialTimePickerLight;
        }
    }
}
