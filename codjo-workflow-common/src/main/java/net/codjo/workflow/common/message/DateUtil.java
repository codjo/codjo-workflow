package net.codjo.workflow.common.message;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateUtil {
    private DateUtil() {
    }


    public static String computeStringDateFromPeriod(String period) {
        Calendar calendar = prepareCalendar(period);
        return new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime()) + " 00:00:00";
    }


    public static Date computeSqlDateFromPeriod(String period) {
        Calendar calendar = prepareCalendar(period);
        return new Date(calendar.getTime().getTime());
    }


    private static Calendar prepareCalendar(String period) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        calendar.add(Calendar.MONTH, -Integer.parseInt(period));
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.AM_PM, Calendar.AM);
        return calendar;
    }
}
