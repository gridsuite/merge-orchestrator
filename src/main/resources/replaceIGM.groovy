/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */

/**
 * PARAMETERS :
 *      timestamp : process timestamp
 *      processName : process name
 *      businessProcess : business process
 */

zonedDate = ZonedDateTime.parse(timestamp)
day_of_week = zonedDate.getDayOfWeek()

// extract hour:minute from timestamp in argument
tmp = timestamp.substring(timestamp.lastIndexOf("T") + 1)
hm = tmp.substring(0, tmp.lastIndexOf(":"))


// Replacement rules based on the EMF specifications :

NumberFormat formatter = new DecimalFormat("00")

// 1 - first replacement rules : same timeframe of the same day, following priorities defined here
//
first_rules = ["00:30": ["01:30", "02:30", "03:30", "04:30", "05:30"],
               "01:30": ["02:30", "03:30", "04:30", "00:30", "05:30"],
               "02:30": ["03:30", "01:30", "04:30", "00:30", "05:30"],
               "03:30": ["02:30", "04:30", "01:30", "05:30", "00:30"],
               "04:30": ["03:30", "02:30", "01:30", "05:30", "00:30"],
               "05:30": ["04:30", "06:30", "03:30", "02:30"],
               "06:30": ["07:30", "08:30", "09:30", "10:30", "11:30", "12:30"],
               "07:30": ["08:30", "09:30", "10:30", "11:30", "12:30"],
               "08:30": ["09:30", "10:30", "11:30", "12:30"],
               "09:30": ["10:30", "08:30", "11:30", "12:30"],
               "10:30": ["09:30", "08:30", "11:30", "12:30"],
               "11:30": ["10:30", "12:30", "13:30", "09:30"],
               "12:30": ["11:30", "10:30", "13:30", "09:30"],
               "13:30": ["12:30", "14:30", "11:30", "10:30"],
               "14:30": ["15:30", "16:30", "13:30", "12:30", "11:30", "10:30"],
               "15:30": ["16:30", "17:30", "14:30", "13:30", "12:30"],
               "16:30": ["17:30", "15:30", "14:30", "13:30", "12:30"],
               "17:30": ["18:30", "16:30", "15:30", "14:30", "13:30", "12:30"],
               "18:30": ["17:30", "16:30", "15:30", "14:30", "13:30", "12:30", "19:30"],
               "19:30": ["20:30", "18:30", "21:30", "22:30"],
               "20:30": ["19:30", "21:30", "22:30", "23:30"],
               "21:30": ["20:30", "22:30", "19:30", "23:30"],
               "22:30": ["21:30", "23:30", "20:30", "19:30"],
               "23:30": ["22:30", "21:30", "20:30", "19:30"]]

hm_1 = hm
first_replacing_timestamps_tmp = first_rules.get(hm_1)
first_replacing_timestamps = []
month = formatter.format(zonedDate.getMonthValue())
dayOfMonth = formatter.format(zonedDate.getDayOfMonth())

for (item in first_replacing_timestamps_tmp) {
    first_replacing_timestamps.add(zonedDate.getYear() + "-" + month + "-" + dayOfMonth + "T" + item + ":00Z" + " " + businessProcess)
}

// 2 - second replacement rules : same timeframe of a past day (same day type : working day, saturday, sunday)
//
ZonedDateTime newDate
if (day_of_week == DayOfWeek.SATURDAY || day_of_week == DayOfWeek.SUNDAY) {  // previous saturday or sunday
    TemporalAdjuster adjuster = TemporalAdjusters.previous(day_of_week)
    newDate = zonedDate.with(adjuster)
} else if (day_of_week == DayOfWeek.MONDAY) {  // previous friday
    TemporalAdjuster adjuster = TemporalAdjusters.previous(DayOfWeek.FRIDAY);
    newDate = zonedDate.with(adjuster)
} else {  // previous day
    newDate = zonedDate.minusDays(1)
}

hm_2 = String.format("%02d", newDate.getHour()) + ":" + String.format("%02d", newDate.getMinute())
second_replacing_timestamps_tmp = first_rules.get(hm_2)
second_replacing_timestamps = []
month = formatter.format(newDate.getMonthValue())
dayOfMonth = formatter.format(newDate.getDayOfMonth())

second_replacing_timestamps.add(newDate.getYear() + "-" + month + "-" + dayOfMonth + "T" + hm + ":00Z" + " " + businessProcess)
for (item in second_replacing_timestamps_tmp) {
    second_replacing_timestamps.add(newDate.getYear() + "-" + month + "-" + dayOfMonth + "T" + item + ":00Z" + " " + businessProcess)
}

// 3 - third replacement rules : same day, other timeframe, following priorities defined here
//
third_rules = ["00:30": ["06:30", "07:30", "08:30", "09:30", "10:30", "11:30", "12:30", "13:30", "14:30", "15:30", "16:30", "17:30", "18:30", "19:30", "20:30", "21:30", "22:30", "23:30"],
               "01:30": ["06:30", "07:30", "08:30", "09:30", "10:30", "11:30", "12:30", "13:30", "14:30", "15:30", "16:30", "17:30", "18:30", "19:30", "20:30", "21:30", "22:30", "23:30"],
               "02:30": ["06:30", "07:30", "08:30", "09:30", "10:30", "11:30", "12:30", "13:30", "14:30", "15:30", "16:30", "17:30", "18:30", "19:30", "20:30", "21:30", "22:30", "23:30"],
               "03:30": ["06:30", "07:30", "08:30", "09:30", "10:30", "11:30", "12:30", "13:30", "14:30", "15:30", "16:30", "17:30", "18:30", "19:30", "20:30", "21:30", "22:30", "23:30"],
               "04:30": ["06:30", "07:30", "08:30", "09:30", "10:30", "11:30", "12:30", "13:30", "14:30", "15:30", "16:30", "17:30", "18:30", "19:30", "20:30", "21:30", "22:30", "23:30"],
               "05:30": ["00:30", "01:30", "07:30", "08:30", "09:30", "10:30", "11:30", "12:30", "13:30", "14:30", "15:30", "16:30", "17:30", "18:30", "19:30", "20:30", "21:30", "22:30", "23:30"],
               "06:30": ["00:30", "01:30", "02:30", "03:30", "04:30", "05:30", "13:30", "14:30", "15:30", "16:30", "17:30", "18:30", "19:30", "20:30", "21:30", "22:30", "23:30"],
               "07:30": ["00:30", "01:30", "02:30", "03:30", "04:30", "05:30", "06:30", "13:30", "14:30", "15:30", "16:30", "17:30", "18:30", "19:30", "20:30", "21:30", "22:30", "23:30"],
               "08:30": ["00:30", "01:30", "02:30", "03:30", "04:30", "05:30", "06:30", "07:30", "13:30", "14:30", "15:30", "16:30", "17:30", "18:30", "19:30", "20:30", "21:30", "22:30", "23:30"],
               "09:30": ["00:30", "01:30", "02:30", "03:30", "04:30", "05:30", "06:30", "07:30", "13:30", "14:30", "15:30", "16:30", "17:30", "18:30", "19:30", "20:30", "21:30", "22:30", "23:30"],
               "10:30": ["00:30", "01:30", "02:30", "03:30", "04:30", "05:30", "06:30", "07:30", "13:30", "14:30", "15:30", "16:30", "17:30", "18:30", "19:30", "20:30", "21:30", "22:30", "23:30"],
               "11:30": ["00:30", "01:30", "02:30", "03:30", "04:30", "05:30", "06:30", "07:30", "08:30", "14:30", "15:30", "16:30", "17:30", "18:30", "19:30", "20:30", "21:30", "22:30", "23:30"],
               "12:30": ["00:30", "01:30", "02:30", "03:30", "04:30", "05:30", "06:30", "07:30", "08:30", "14:30", "15:30", "16:30", "17:30", "18:30", "19:30", "20:30", "21:30", "22:30", "23:30"],
               "13:30": ["00:30", "01:30", "02:30", "03:30", "04:30", "05:30", "06:30", "07:30", "08:30", "09:30", "15:30", "16:30", "17:30", "18:30", "19:30", "20:30", "21:30", "22:30", "23:30"],
               "14:30": ["00:30", "01:30", "02:30", "03:30", "04:30", "05:30", "06:30", "07:30", "08:30", "09:30", "17:30", "18:30", "19:30", "20:30", "21:30", "22:30", "23:30"],
               "15:30": ["00:30", "01:30", "02:30", "03:30", "04:30", "05:30", "06:30", "07:30", "08:30", "09:30", "10:30", "11:30", "18:30", "19:30", "20:30", "21:30", "22:30", "23:30"],
               "16:30": ["00:30", "01:30", "02:30", "03:30", "04:30", "05:30", "06:30", "07:30", "08:30", "09:30", "10:30", "11:30", "18:30", "19:30", "20:30", "21:30", "22:30", "23:30"],
               "17:30": ["00:30", "01:30", "02:30", "03:30", "04:30", "05:30", "06:30", "07:30", "08:30", "09:30", "10:30", "11:30", "19:30", "20:30", "21:30", "22:30", "23:30"],
               "18:30": ["00:30", "01:30", "02:30", "03:30", "04:30", "05:30", "06:30", "07:30", "08:30", "09:30", "10:30", "11:30", "20:30", "21:30", "22:30", "23:30"],
               "19:30": ["00:30", "01:30", "02:30", "03:30", "04:30", "05:30", "06:30", "07:30", "08:30", "09:30", "10:30", "11:30", "12:30", "13:30", "14:30", "15:30", "16:30", "17:30", "23:30"],
               "20:30": ["00:30", "01:30", "02:30", "03:30", "04:30", "05:30", "06:30", "07:30", "08:30", "09:30", "10:30", "11:30", "12:30", "13:30", "14:30", "15:30", "16:30", "17:30", "18:30"],
               "21:30": ["00:30", "01:30", "02:30", "03:30", "04:30", "05:30", "06:30", "07:30", "08:30", "09:30", "10:30", "11:30", "12:30", "13:30", "14:30", "15:30", "16:30", "17:30", "18:30"],
               "22:30": ["00:30", "01:30", "02:30", "03:30", "04:30", "05:30", "06:30", "07:30", "08:30", "09:30", "10:30", "11:30", "12:30", "13:30", "14:30", "15:30", "16:30", "17:30", "18:30"],
               "23:30": ["00:30", "01:30", "02:30", "03:30", "04:30", "05:30", "06:30", "07:30", "08:30", "09:30", "10:30", "11:30", "12:30", "13:30", "14:30", "15:30", "16:30", "17:30", "18:30"]]

hm_3 = hm
third_replacing_timestamps_tmp = third_rules.get(hm_3)
third_replacing_timestamps = []
month = formatter.format(zonedDate.getMonthValue())
dayOfMonth = formatter.format(zonedDate.getDayOfMonth())

for (item in third_replacing_timestamps_tmp) {
    third_replacing_timestamps.add(zonedDate.getYear() + "-" + month + "-" + dayOfMonth + "T" + item + ":00Z" + " " + businessProcess)
}

// result is the concatenation of all replacement rules
return first_replacing_timestamps + second_replacing_timestamps + third_replacing_timestamps

