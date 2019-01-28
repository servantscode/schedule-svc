package org.servantscode.schedule;

import org.junit.Test;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static java.time.DayOfWeek.*;
import static java.time.temporal.TemporalAdjusters.next;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.servantscode.schedule.Recurrence.RecurrenceCycle.*;

public class RecurrenceIteratorTest {

    @Test
    public void testDaily() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime nextWeek = now.plusDays(7).toLocalDate().atStartOfDay(ZoneId.systemDefault());
        Recurrence r = new Recurrence(DAILY, 1, nextWeek);

        RecurrenceIterator calc = new RecurrenceIterator(r, now);

        int i=0;
        while(calc.hasNext()) {
            ZonedDateTime nextDate = calc.next();
            assertEquals(String.format("Response %d is not correct", i), now.plusDays(i), nextDate);
            assertTimeCarried(now, nextDate, i);
            i++;
        }
         assertEquals("Incorrect number of repetitions", 8, i);
    }


    @Test
    public void testEveryOtherDay() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime nextWeek = now.plusDays(8).toLocalDate().atStartOfDay(ZoneId.systemDefault());
        Recurrence r = new Recurrence(DAILY, 2, nextWeek);

        RecurrenceIterator calc = new RecurrenceIterator(r, now);

        int i=0;
        while(calc.hasNext()) {
            assertEquals(String.format("Response %d is not correct", i), now.plusDays(i*2), calc.next());
            i++;
        }
        assertEquals("Incorrect number of repetitions", 5, i);
    }

    @Test
    public void testMonthly() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = now.plusDays(64).toLocalDate().atStartOfDay(ZoneId.systemDefault());
        Recurrence r = new Recurrence(DAY_OF_MONTH, 1, end);

        RecurrenceIterator calc = new RecurrenceIterator(r, now);

        int i=0;
        while(calc.hasNext()) {
            ZonedDateTime nextDate = calc.next();
            assertEquals(String.format("Response %d is not correct", i), now.plusMonths(i), nextDate);
            assertTimeCarried(now, nextDate, i);
            i++;
        }
        assertEquals("Incorrect number of repetitions", 3, i);
    }

    @Test
    public void testEveryFourthMonthly() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = now.plusDays(366).toLocalDate().atStartOfDay(ZoneId.systemDefault());
        Recurrence r = new Recurrence(DAY_OF_MONTH, 4, end);

        RecurrenceIterator calc = new RecurrenceIterator(r, now);

        int i=0;
        while(calc.hasNext()) {
            assertEquals(String.format("Response %d is not correct", i), now.plusMonths(i*4), calc.next());
            i++;
        }
        assertEquals("Incorrect number of repetitions", 4, i);
    }

    @Test
    public void testEveryFourthSundayPancakes() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).with(TemporalAdjusters.dayOfWeekInMonth(4, SUNDAY));
        ZonedDateTime end = now.plusDays(366).toLocalDate().atStartOfDay(ZoneId.systemDefault());
        Recurrence r = new Recurrence(WEEKDAY_OF_MONTH, 1, end);

        RecurrenceIterator calc = new RecurrenceIterator(r, now);

        int i=0;
        while(calc.hasNext()) {
            ZonedDateTime nextDate = calc.next();
            System.out.println("Calculated: " + nextDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            assertEquals(String.format("Response %d has incorrect month", i), now.plusMonths(i).getMonth(), nextDate.getMonth());
            assertEquals(String.format("Response %d has incorrect dayOfWeek", i), now.getDayOfWeek(), nextDate.getDayOfWeek());
            assertTrue(String.format("Response %d has day of month to early", i), nextDate.getDayOfMonth() > 21);
            assertTrue(String.format("Response %d has day of month to late", i),  nextDate.getDayOfMonth() < 29);
            assertTimeCarried(now, nextDate, i);
            i++;
        }
        assertEquals("Incorrect number of repetitions", 13, i);
    }

    @Test
    public void testEveryOtherThirdMonday() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).with(TemporalAdjusters.dayOfWeekInMonth(3, MONDAY));
        ZonedDateTime end = now.plusDays(366).toLocalDate().atStartOfDay(ZoneId.systemDefault());
        Recurrence r = new Recurrence(WEEKDAY_OF_MONTH, 2, end);

        RecurrenceIterator calc = new RecurrenceIterator(r, now);

        int i=0;
        while(calc.hasNext()) {
            ZonedDateTime nextDate = calc.next();
            assertEquals(String.format("Response %d has incorrect month", i), now.plusMonths(i*2).getMonth(), nextDate.getMonth());
            assertEquals(String.format("Response %d has incorrect dayOfWeek", i), now.getDayOfWeek(), nextDate.getDayOfWeek());
            assertTrue(String.format("Response %d has day of month to early", i), nextDate.getDayOfMonth() > 14);
            assertTrue(String.format("Response %d has day of month to late", i),  nextDate.getDayOfMonth() < 22);
            i++;
        }
        assertEquals("Incorrect number of repetitions", 7, i);
    }

    @Test
    public void testYearly() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = now.plusDays(750).toLocalDate().atStartOfDay(ZoneId.systemDefault());
        Recurrence r = new Recurrence(YEARLY, 1, end);

        RecurrenceIterator calc = new RecurrenceIterator(r, now);

        int i=0;
        while(calc.hasNext()) {
            ZonedDateTime nextDate = calc.next();
            assertEquals(String.format("Response %d is not correct", i), now.plusYears(i), nextDate);
            assertTimeCarried(now, nextDate, i);
            i++;
        }
        assertEquals("Incorrect number of repetitions", 3, i);
    }

    @Test
    public void testOlympics() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = now.plusDays(2000).toLocalDate().atStartOfDay(ZoneId.systemDefault());
        Recurrence r = new Recurrence(YEARLY, 4, end);

        RecurrenceIterator calc = new RecurrenceIterator(r, now);

        int i=0;
        while(calc.hasNext()) {
            assertEquals(String.format("Response %d is not correct", i), now.plusYears(i*4), calc.next());
            i++;
        }
        assertEquals("Incorrect number of repetitions", 2, i);
    }

    @Test
    public void testWeekly() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).with(next(MONDAY));
        ZonedDateTime end = now.plusDays(34).toLocalDate().atStartOfDay(ZoneId.systemDefault());
        Recurrence r = new Recurrence(WEEKLY, 1, end, asList(MONDAY));

        RecurrenceIterator calc = new RecurrenceIterator(r, now);

        int i=0;
        while(calc.hasNext()) {
            ZonedDateTime date = calc.next();
            assertEquals(String.format("Response %d is not correct", i), now.plusWeeks(i), date);
            i++;
        }
        assertEquals("Incorrect number of repetitions", 5, i);
    }

    @Test
    public void testWeeklyNotOnStartDate() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).with(next(SUNDAY));
        ZonedDateTime end = now.plusDays(34).toLocalDate().atStartOfDay(ZoneId.systemDefault());
        Recurrence r = new Recurrence(WEEKLY, 1, end, asList(MONDAY));

        RecurrenceIterator calc = new RecurrenceIterator(r, now);

        int i=0;
        while(calc.hasNext()) {
            ZonedDateTime date = calc.next();
            assertEquals(String.format("Response %d is not correct", i), now.plusWeeks(i).with(next(MONDAY)), date);
            i++;
        }
        assertEquals("Incorrect number of repetitions", 5, i);
    }

    @Test
    public void testMWFWeekly() {
        List<DayOfWeek> testDays = asList(MONDAY, WEDNESDAY, FRIDAY);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).with(next(MONDAY));
        ZonedDateTime end = now.plusDays(34).toLocalDate().atStartOfDay(ZoneId.systemDefault());
        Recurrence r = new Recurrence(WEEKLY, 1, end, testDays);

        RecurrenceIterator calc = new RecurrenceIterator(r, now);

        int i=0;
        while(calc.hasNext()) {
            ZonedDateTime date = calc.next();
            assertEquals(String.format("Response %d has incorrect day of week", i), testDays.get(i%3), date.getDayOfWeek());
            assertEquals(String.format("Response %d is not correct", i), now.plusWeeks(i/3).with(next(SUNDAY)), date.with(next(SUNDAY)));
            i++;
        }
        assertEquals("Incorrect number of repetitions", 15, i);
    }

    @Test
    public void testMWFWeeklyStartingFriday() {
        List<DayOfWeek> testDays = asList(FRIDAY, MONDAY, WEDNESDAY);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).with(next(FRIDAY));
        ZonedDateTime end = now.plusDays(34).toLocalDate().atStartOfDay(ZoneId.systemDefault());
        Recurrence r = new Recurrence(WEEKLY, 1, end, testDays);

        RecurrenceIterator calc = new RecurrenceIterator(r, now);

        int i=0;
        while(calc.hasNext()) {
            ZonedDateTime date = calc.next();
            assertEquals(String.format("Response %d has incorrect day of week", i), testDays.get(i%3), date.getDayOfWeek());
            assertEquals(String.format("Response %d is not correct", i), now.plusWeeks((i+2)/3).with(next(SUNDAY)), date.with(next(SUNDAY)));
            i++;
        }
        assertEquals("Incorrect number of repetitions", 15, i);
    }

    @Test
    public void testMWFWeeklyStartingSaturday() {
        List<DayOfWeek> testDays = asList(MONDAY, WEDNESDAY, FRIDAY);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).with(next(SATURDAY));
        ZonedDateTime end = now.plusDays(34).toLocalDate().atStartOfDay(ZoneId.systemDefault());
        Recurrence r = new Recurrence(WEEKLY, 1, end, testDays);

        RecurrenceIterator calc = new RecurrenceIterator(r, now);

        int i=0;
        while(calc.hasNext()) {
            ZonedDateTime date = calc.next();
            assertEquals(String.format("Response %d has incorrect day of week", i), testDays.get(i%3), date.getDayOfWeek());
            assertEquals(String.format("Response %d is not correct", i), now.plusWeeks(i/3 + 1).with(next(SUNDAY)), date.with(next(SUNDAY)));
            i++;
        }
        assertEquals("Incorrect number of repetitions", 15, i);
    }

    @Test
    public void testEveryOtherMonday() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).with(next(MONDAY));
        ZonedDateTime end = now.plusDays(34).toLocalDate().atStartOfDay(ZoneId.systemDefault());
        Recurrence r = new Recurrence(WEEKLY, 2, end, asList(MONDAY));

        RecurrenceIterator calc = new RecurrenceIterator(r, now);

        int i=0;
        while(calc.hasNext()) {
            ZonedDateTime date = calc.next();
            assertEquals(String.format("Response %d is not correct", i), now.plusWeeks(i*2), date);
            i++;
        }
        assertEquals("Incorrect number of repetitions", 3, i);
    }

    @Test
    public void testEveryOtherMWFWeeklyStartingWednesday() {
        List<DayOfWeek> testDays = asList(WEDNESDAY, FRIDAY, MONDAY);
        ZonedDateTime now = LocalDateTime.parse("2019-01-30T15:00:00").atZone(ZoneId.of("America/Chicago"));
        ZonedDateTime end = now.plusDays(34).toLocalDate().atStartOfDay(ZoneId.systemDefault());
        Recurrence r = new Recurrence(WEEKLY, 2, end, testDays);

        RecurrenceIterator calc = new RecurrenceIterator(r, now);

        int i=0;
        while(calc.hasNext()) {
            ZonedDateTime date = calc.next();
            assertEquals(String.format("Response %d has incorrect day of week", i), testDays.get(i%3), date.getDayOfWeek());
            assertEquals(String.format("Response %d is not correct", i), now.plusWeeks(2*((i+1)/3)).with(next(SUNDAY)), date.with(next(SUNDAY)));
            i++;
        }
        assertEquals("Incorrect number of repetitions", 8, i);
    }

    @Test
    public void testEveryOtherMWFWeeklyStartingSaturday() {
        List<DayOfWeek> testDays = asList(MONDAY, WEDNESDAY, FRIDAY);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).with(next(SATURDAY));
        ZonedDateTime end = now.plusDays(34).toLocalDate().atStartOfDay(ZoneId.systemDefault());
        Recurrence r = new Recurrence(WEEKLY, 2, end, testDays);

        RecurrenceIterator calc = new RecurrenceIterator(r, now);

        int i=0;
        while(calc.hasNext()) {
            ZonedDateTime date = calc.next();
            assertEquals(String.format("Response %d has incorrect day of week", i), testDays.get(i%3), date.getDayOfWeek());
            assertEquals(String.format("Response %d is not correct", i), now.plusWeeks(2*(i/3) + 1).with(next(SUNDAY)), date.with(next(SUNDAY)));
            i++;
        }
        assertEquals("Incorrect number of repetitions", 9, i);
    }

    @Test
    public void testSameTimeAcrossDayLightSavings() {
        ZonedDateTime now = ZonedDateTime.parse("2019-02-01T15:00:00-06").withZoneSameInstant(ZoneId.systemDefault());
        ZonedDateTime end = now.plusDays(200).toLocalDate().atStartOfDay(ZoneId.systemDefault());
        Recurrence r = new Recurrence(DAY_OF_MONTH, 6, end);

        RecurrenceIterator calc = new RecurrenceIterator(r, now);

        ZonedDateTime date = calc.next();
        System.out.println("Now Date: " + date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        assertEquals("Now date has incorrect time offset", "-06:00", getZoneOffset(date).toString());
        assertEquals("Now date has incorrect localtime", now.getHour(), date.getHour());

        ZonedDateTime futureDate = calc.next();
        System.out.println("Future Date: " + futureDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        assertEquals("Future date has incorrect time offset", "-05:00", getZoneOffset(futureDate).toString());
        assertEquals("Future date has incorrect localtime", now.getHour(), futureDate.getHour());
    }

    // ----- Private -----
    private void assertTimeCarried(ZonedDateTime expected, ZonedDateTime actual, int i) {
        assertEquals(String.format("Response %d has incorrect hours", i), expected.getHour(), actual.getHour());
        assertEquals(String.format("Response %d has incorrect minutes", i), expected.getMinute(), actual.getMinute());
        assertEquals(String.format("Response %d has incorrect seconds", i), expected.getSecond(), actual.getSecond());
        assertEquals(String.format("Response %d has incorrect TimeZone", i), expected.getZone(), actual.getZone());
    }

    private ZoneOffset getZoneOffset(ZonedDateTime input) {
        //This is REALLY painful... Thanks Oracle. You really did the community a solid here...
        return input.getZone().getRules().getOffset(input.toInstant());
    }
}
