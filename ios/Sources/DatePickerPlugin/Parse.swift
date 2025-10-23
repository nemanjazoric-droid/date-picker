//
//  Parse.swift
//  Plugin
//
//  Created by Daniel Rosa on 12/02/21.
//  Copyright © 2021 Max Lynch. All rights reserved.
//
//  Modified By Nemanja Zoric on 22/10/25.
//

import Foundation
public class Parse {
    public static func dateFromString(date: String, format: String? = nil, locale: String = "en_US_POSIX", timezone: String? = nil) -> Date {
        // Return current date if input is empty to avoid crashes
        let trimmed = date.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty {
            return Date()
        }

        let posix = Locale(identifier: locale)
        let tz: TimeZone? = timezone != nil ? TimeZone(identifier: timezone!) : nil

        // 1) Try provided format first
        if let fmt = format, !fmt.isEmpty {
            let formatter = DateFormatter()
            formatter.locale = posix
            if let tz = tz { formatter.timeZone = tz }
            formatter.dateFormat = fmt
            if let d = formatter.date(from: trimmed) {
                return d
            }
            // Try correcting common mistake: use SSS for milliseconds instead of sss
            let corrected = fmt.replacingOccurrences(of: "sss", with: "SSS")
            if corrected != fmt {
                formatter.dateFormat = corrected
                if let d = formatter.date(from: trimmed) {
                    return d
                }
            }
        }

        // 2) Try ISO8601 with and without fractional seconds
        let iso = ISO8601DateFormatter()
        if let tz = tz { iso.timeZone = tz }
        iso.formatOptions = [.withInternetDateTime, .withColonSeparatorInTimeZone]
        if let d = iso.date(from: trimmed) {
            return d
        }
        iso.formatOptions.insert(.withFractionalSeconds)
        if let d = iso.date(from: trimmed) {
            return d
        }

        // 3) Try common fallback patterns
        let fallbacks = [
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXXXX",
            "yyyy-MM-dd'T'HH:mmXXXXX",
            "yyyy-MM-dd"
        ]
        for pat in fallbacks {
            let f = DateFormatter()
            f.locale = posix
            if let tz = tz { f.timeZone = tz }
            f.dateFormat = pat
            if let d = f.date(from: trimmed) { return d }
        }

        // 4) As a last resort, let Date parse RFC 3339-like strings
        if let d = DateFormatter().date(from: trimmed) {
            return d
        }

        // 5) Fallback to now to avoid crashing the app
        return Date()
    }
    public static func dateToString(date: Date, format: String? = nil, locale: Locale? = nil) -> String {
        let formatter = DateFormatter()
        if let format = format, !format.isEmpty {
            formatter.dateFormat = format
        }
        if let locale = locale {
            formatter.locale = locale
        }
        return formatter.string(from: date)
    }
}
