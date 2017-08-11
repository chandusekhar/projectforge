/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.framework.calendar;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.projectforge.business.teamcal.event.RecurrenceFrequencyModeOne;
import org.projectforge.business.teamcal.event.RecurrenceFrequencyModeTwo;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.RecurrenceFrequency;

import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.WeekDay;
import net.fortuna.ical4j.model.WeekDayList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.util.Dates;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class ICal4JUtils
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ICal4JUtils.class);

  private static TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();

  /**
   * @return The timeZone (ical4j) built of the default java timeZone of the user.
   * @see ThreadLocalUserContext#getTimeZone()
   */
  public static TimeZone getUserTimeZone()
  {
    return registry.getTimeZone(ThreadLocalUserContext.getTimeZone().getID());
  }

  /**
   * @return The timeZone (ical4j) built of the default java timeZone of the user.
   * @see ThreadLocalUserContext#getTimeZone()
   */
  public static TimeZone getTimeZone(final java.util.TimeZone timeZone)
  {
    return registry.getTimeZone(timeZone.getID());
  }

  public static TimeZone getUTCTimeZone()
  {
    return registry.getTimeZone("UTC");
  }

  public static VEvent createVEvent(final Date startDate, final Date endDate, final String uid, final String summary)
  {
    return createVEvent(startDate, endDate, uid, summary, false);
  }

  public static VEvent createVEvent(final Date startDate, final Date endDate, final String uid, final String summary,
      final boolean allDay)
  {
    final TimeZone timezone = getUserTimeZone();
    return createVEvent(startDate, endDate, uid, summary, allDay, timezone);
  }

  public static VEvent createVEvent(final Date startDate, final Date endDate, final String uid, final String summary,
      final boolean allDay,
      final TimeZone timezone)
  {
    VEvent vEvent;
    if (allDay == true) {
      final Date startUtc = CalendarUtils.getUTCMidnightDate(startDate);
      final Date endUtc = CalendarUtils.getUTCMidnightDate(endDate);
      final net.fortuna.ical4j.model.Date fortunaStartDate = new net.fortuna.ical4j.model.Date(startUtc);
      final org.joda.time.DateTime jodaTime = new org.joda.time.DateTime(endUtc);
      // requires plus 1 because one day will be omitted by calendar.
      final net.fortuna.ical4j.model.Date fortunaEndDate = new net.fortuna.ical4j.model.Date(
          jodaTime.plusDays(1).toDate());
      vEvent = new VEvent(fortunaStartDate, fortunaEndDate, summary);
    } else {
      final net.fortuna.ical4j.model.DateTime fortunaStartDate = new net.fortuna.ical4j.model.DateTime(startDate);
      fortunaStartDate.setTimeZone(timezone);
      final net.fortuna.ical4j.model.DateTime fortunaEndDate = new net.fortuna.ical4j.model.DateTime(endDate);
      fortunaEndDate.setTimeZone(timezone);
      vEvent = new VEvent(fortunaStartDate, fortunaEndDate, summary);
      vEvent.getProperties().add(timezone.getVTimeZone().getTimeZoneId());
    }
    vEvent.getProperties().add(new Uid(uid));
    return vEvent;
  }

  public static WeekDayList getDayListForRecurrenceFrequencyModeTwo(RecurrenceFrequencyModeTwo mode)
  {
    WeekDayList weekDays = new WeekDayList();
    if (mode == RecurrenceFrequencyModeTwo.MONDAY)
      weekDays.add(WeekDay.MO);
    else if (mode == RecurrenceFrequencyModeTwo.TUESDAY)
      weekDays.add(WeekDay.TU);
    else if (mode == RecurrenceFrequencyModeTwo.WEDNESDAY)
      weekDays.add(WeekDay.WE);
    else if (mode == RecurrenceFrequencyModeTwo.THURSDAY)
      weekDays.add(WeekDay.TH);
    else if (mode == RecurrenceFrequencyModeTwo.FRIDAY)
      weekDays.add(WeekDay.FR);
    else if (mode == RecurrenceFrequencyModeTwo.SATURDAY)
      weekDays.add(WeekDay.SA);
    else if (mode == RecurrenceFrequencyModeTwo.SUNDAY)
      weekDays.add(WeekDay.SU);
    else if (mode == RecurrenceFrequencyModeTwo.WEEKDAY) {
      weekDays.add(WeekDay.MO);
      weekDays.add(WeekDay.TU);
      weekDays.add(WeekDay.WE);
      weekDays.add(WeekDay.TH);
      weekDays.add(WeekDay.FR);
    } else if (mode == RecurrenceFrequencyModeTwo.WEEKEND) {
      weekDays.add(WeekDay.SA);
      weekDays.add(WeekDay.SU);
    } else if (mode == RecurrenceFrequencyModeTwo.DAY) {
      weekDays.add(WeekDay.SA);
      weekDays.add(WeekDay.SU);
      weekDays.add(WeekDay.MO);
      weekDays.add(WeekDay.TU);
      weekDays.add(WeekDay.WE);
      weekDays.add(WeekDay.TH);
      weekDays.add(WeekDay.FR);
    }
    return weekDays;
  }

  public static RecurrenceFrequencyModeTwo getRecurrenceFrequencyModeTwoForDay(WeekDayList dayList)
  {
    if (dayList.size() == 1) {
      for (WeekDay wd : dayList) {
        if (wd.getDay() == WeekDay.MO.getDay()) {
          return RecurrenceFrequencyModeTwo.MONDAY;
        } else if (wd.getDay() == WeekDay.TU.getDay()) {
          return RecurrenceFrequencyModeTwo.TUESDAY;
        } else if (wd.getDay() == WeekDay.WE.getDay()) {
          return RecurrenceFrequencyModeTwo.WEDNESDAY;
        } else if (wd.getDay() == WeekDay.TH.getDay()) {
          return RecurrenceFrequencyModeTwo.THURSDAY;
        } else if (wd.getDay() == WeekDay.FR.getDay()) {
          return RecurrenceFrequencyModeTwo.FRIDAY;
        } else if (wd.getDay() == WeekDay.SA.getDay()) {
          return RecurrenceFrequencyModeTwo.SATURDAY;
        } else if (wd.getDay() == WeekDay.SU.getDay()) {
          return RecurrenceFrequencyModeTwo.SUNDAY;
        }
      }
    } else if (dayList.size() == 2) {
      return RecurrenceFrequencyModeTwo.WEEKEND;
    } else if (dayList.size() == 5) {
      return RecurrenceFrequencyModeTwo.WEEKDAY;
    } else if (dayList.size() == 7) {
      return RecurrenceFrequencyModeTwo.DAY;
    }
    return null;
  }

  public static RecurrenceFrequencyModeOne getRecurrenceFrequencyModeOneByOffset(int offset)
  {
    if (offset == 1 || offset == 0) {
      return RecurrenceFrequencyModeOne.FIRST;
    } else if (offset == 2) {
      return RecurrenceFrequencyModeOne.SECOND;
    } else if (offset == 3) {
      return RecurrenceFrequencyModeOne.THIRD;
    } else if (offset == 4) {
      return RecurrenceFrequencyModeOne.FOURTH;
    } else if (offset == 5) {
      return RecurrenceFrequencyModeOne.FIFTH;
    } else if (offset == -1) {
      return RecurrenceFrequencyModeOne.LAST;
    }
    return null;
  }

  public static int getOffsetForRecurrenceFrequencyModeOne(RecurrenceFrequencyModeOne mode)
  {
    if (mode == RecurrenceFrequencyModeOne.FIRST)
      return 1;
    else if (mode == RecurrenceFrequencyModeOne.SECOND)
      return 2;
    else if (mode == RecurrenceFrequencyModeOne.THIRD)
      return 3;
    else if (mode == RecurrenceFrequencyModeOne.FOURTH)
      return 3;
    else if (mode == RecurrenceFrequencyModeOne.FIFTH)
      return 5;
    else
      return -1;
  }

  /**
   * @param rruleString
   * @return null if rruleString is empty, otherwise new RRule object.
   */
  public static RRule calculateRRule(final String rruleString)
  {
    if (StringUtils.isBlank(rruleString) == true) {
      return null;
    }
    try {
      return new RRule(rruleString);
    } catch (final ParseException ex) {
      log.error("Exception encountered while parsing rrule '" + rruleString + "': " + ex.getMessage(), ex);
      return null;
    }
  }

  public static String getCal4JFrequencyString(final RecurrenceFrequency interval)
  {
    if (interval == RecurrenceFrequency.DAILY) {
      return Recur.DAILY;
    } else if (interval == RecurrenceFrequency.WEEKLY) {
      return Recur.WEEKLY;
    } else if (interval == RecurrenceFrequency.MONTHLY) {
      return Recur.MONTHLY;
    } else if (interval == RecurrenceFrequency.YEARLY) {
      return Recur.YEARLY;
    }
    return null;
  }

  /**
   * @param recur
   * @return
   */
  public static RecurrenceFrequency getFrequency(final Recur recur)
  {
    if (recur == null) {
      return null;
    }
    final String freq = recur.getFrequency();
    if (Recur.WEEKLY.equals(freq) == true) {
      return RecurrenceFrequency.WEEKLY;
    } else if (Recur.MONTHLY.equals(freq) == true) {
      return RecurrenceFrequency.MONTHLY;
    } else if (Recur.DAILY.equals(freq) == true) {
      return RecurrenceFrequency.DAILY;
    } else if (Recur.YEARLY.equals(freq) == true) {
      return RecurrenceFrequency.YEARLY;
    }
    return null;
  }

  /**
   * @param interval
   * @return
   */
  public static String getFrequency(final RecurrenceFrequency interval)
  {
    if (interval == null) {
      return null;
    }
    if (interval == RecurrenceFrequency.WEEKLY) {
      return Recur.WEEKLY;
    } else if (interval == RecurrenceFrequency.DAILY) {
      return Recur.DAILY;
    } else if (interval == RecurrenceFrequency.MONTHLY) {
      return Recur.MONTHLY;
    } else if (interval == RecurrenceFrequency.YEARLY) {
      return Recur.YEARLY;
    }
    return null;
  }

  public static java.sql.Date getSqlDate(final net.fortuna.ical4j.model.Date ical4jDate)
  {
    if (ical4jDate == null) {
      return null;
    }
    return new java.sql.Date(ical4jDate.getTime());
  }

  public static java.sql.Timestamp getSqlTimestamp(final net.fortuna.ical4j.model.Date ical4jDate)
  {
    if (ical4jDate == null) {
      return null;
    }
    return new java.sql.Timestamp(ical4jDate.getTime());
  }

  public static net.fortuna.ical4j.model.DateTime getICal4jDateTime(final java.util.Date javaDate,
      final java.util.TimeZone timeZone)
  {
    if (javaDate == null) {
      return null;
    }
    final String dateString = DateHelper.formatIsoTimestamp(javaDate, timeZone);
    final String pattern = DateFormats.ISO_TIMESTAMP_SECONDS;
    try {
      final net.fortuna.ical4j.model.DateTime dateTime = new net.fortuna.ical4j.model.DateTime(dateString, pattern,
          getTimeZone(timeZone));
      return dateTime;
    } catch (final ParseException ex) {
      log.error("Can't parse date '" + dateString + "' with pattern '" + pattern + "': " + ex.getMessage(), ex);
      return null;
    }
  }

  // TODO remove, date should not use timezone!
  @Deprecated
  public static net.fortuna.ical4j.model.Date getICal4jDate(final java.util.Date javaDate,
      final java.util.TimeZone timeZone)
  {
    if (javaDate == null) {
      return null;
    }
    return new MyIcal4JDate(javaDate, timeZone);
  }

  public static net.fortuna.ical4j.model.Date parseICal4jDate(final String dateString)
  {
    if (dateString == null) {
      return null;
    }
    net.fortuna.ical4j.model.Date date;
    try {
      date = new net.fortuna.ical4j.model.Date(dateString);
    } catch (final ParseException ex) {
      log.error("Unable to parse date string: '" + dateString + "': " + ex.getMessage(), ex);
      return null;
    }
    return date;
  }

  /**
   * Format is '20130327T090000'
   *
   * @param dateString
   * @param timeZone
   * @return
   */
  public static Date parseICalDateString(final String dateString, final java.util.TimeZone timeZone)
  {
    if (StringUtils.isBlank(dateString) == true) {
      return null;
    }
    String pattern;
    java.util.TimeZone tz = timeZone;
    if (dateString.indexOf('T') > 0) {
      pattern = DateFormats.ICAL_DATETIME_FORMAT;
    } else {
      pattern = DateFormats.COMPACT_DATE;
      tz = DateHelper.UTC;
    }
    final DateFormat df = new SimpleDateFormat(pattern);
    df.setTimeZone(tz);
    try {
      return df.parse(dateString);
    } catch (final ParseException ex) {
      log.error("Can't parse ical date ('" + pattern + "': " + ex.getMessage(), ex);
      return null;
    }
  }

  public static Date parseISODateString(final String isoDateString)
  {
    if (StringUtils.isBlank(isoDateString) == true) {
      return null;
    }
    String pattern;
    if (isoDateString.indexOf(':') > 0) {
      pattern = DateFormats.ISO_TIMESTAMP_SECONDS;
    } else {
      pattern = DateFormats.ISO_DATE;
    }
    final DateFormat df = new SimpleDateFormat(pattern);
    df.setTimeZone(DateHelper.UTC);
    try {
      return df.parse(isoDateString);
    } catch (final ParseException ex) {
      log.error("Can't parse ISO date ('" + pattern + "': " + ex.getMessage(), ex);
      return null;
    }
  }

  public static String asISODateString(final Date date)
  {
    return asISODateString(date, DateHelper.UTC);
  }

  public static String asISODateString(final Date date, final java.util.TimeZone timeZone)
  {
    if (date == null) {
      return null;
    }
    final DateFormat df = new SimpleDateFormat(DateFormats.ISO_DATE);
    df.setTimeZone(timeZone);
    return df.format(date);
  }

  public static String asISODateTimeString(final Date date)
  {
    if (date == null) {
      return null;
    }
    final DateFormat df = new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_SECONDS);
    df.setTimeZone(DateHelper.UTC);
    return df.format(date);
  }

  public static String asICalDateString(final Date date, final java.util.TimeZone timeZone, final boolean withoutTime)
  {
    if (date == null) {
      return null;
    }
    DateFormat df = null;
    if (withoutTime) {
      df = new SimpleDateFormat(DateFormats.COMPACT_DATE);
    } else {
      df = new SimpleDateFormat(DateFormats.ICAL_DATETIME_FORMAT);
    }
    if (timeZone != null) {
      df.setTimeZone(timeZone);
    } else {
      df.setTimeZone(DateHelper.UTC);
    }
    return df.format(date);
  }

  public static String[] splitExDates(final String csv)
  {
    if (StringUtils.isBlank(csv) == true) {
      return null;
    }
    final String[] sa = StringHelper.splitAndTrim(csv, ",;|");
    if (sa == null || sa.length == 0) {
      return null;
    }
    return sa;
  }

  public static List<net.fortuna.ical4j.model.Date> parseCSVDatesAsICal4jDates(final String csvDates, boolean dateTime,
      final TimeZone timeZone)
  {
    final String[] sa = splitExDates(csvDates);
    if (sa == null) {
      return null;
    }
    final List<net.fortuna.ical4j.model.Date> result = new ArrayList<net.fortuna.ical4j.model.Date>();
    for (final String str : sa) {
      if (StringUtils.isEmpty(str)) {
        continue;
      }
      Date date = null;
      if (str.matches("\\d{8}.*") == true) {
        date = parseICalDateString(str, timeZone);
      } else {
        date = parseISODateString(str);
      }
      if (date == null) {
        continue;
      }
      if (dateTime) {
        result.add(getICal4jDateTime(date, timeZone));
      } else {
        result.add(new net.fortuna.ical4j.model.Date(date));
      }
    }
    return result;
  }

  public static List<Date> parseCSVDatesAsJavaUtilDates(final String csvDates, final java.util.TimeZone timeZone)
  {
    final String[] sa = splitExDates(csvDates);
    if (sa == null) {
      return null;
    }
    final List<Date> result = new ArrayList<>();
    for (final String str : sa) {
      if (StringUtils.isEmpty(str)) {
        continue;
      }

      Date date = null;
      if (str.matches("\\d{8}.*") == true) {
        date = parseICalDateString(str, timeZone);
      } else {
        date = parseISODateString(str);
      }

      if (date == null) {
        continue;
      }

      result.add(date);
    }
    return result;
  }

  private static class MyIcal4JDate extends net.fortuna.ical4j.model.Date
  {
    private static final long serialVersionUID = 341788808291157447L;

    MyIcal4JDate(final java.util.Date javaDate, final java.util.TimeZone timeZone)
    {
      super(javaDate.getTime(), Dates.PRECISION_DAY, timeZone);
    }
  }

}
