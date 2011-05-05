package net.lump.lib.util;

/**
 * An Interval is a utility class used to define human-known intervals of time
 * and to easily output human-readable intervals.
 *
 * @author M. Troy Bowman
 */
public enum Interval {

  /**
   * A week in milliseconds.
   */
  WEEK("w", 604800000L),
  /**
   * A day in milliseconds.
   */
  DAY("d", 86400000L),
  /**
   * An hour in milliseconds.
   */
  HOUR("h", 3600000L),
  /**
   * A minute in milliseconds.
   */
  MINUTE("m", 60000L),
  /**
   * A second in milliseconds.
   */
  SECOND("s", 1000L),
  /**
   * A millisecond.
   */
  MILLISECOND("ms", 1L);

  /**
   * the abbreviation name
   */
  private String abbr;
  /**
   * the time in milliseconds
   */
  private long millis;

  /**
   * Construct a new span providing the abbreviation and milliseconds.
   *
   * @param abbreviation name
   * @param milliseconds of time
   */
  Interval(String abbreviation, long milliseconds) {
    abbr = abbreviation;
    millis = milliseconds;
  }

  public String getAbbr() {
    return abbr;
  }

  public long getMillis() {
    return millis;
  }

  /**
   * Returns a string which describes the interval, delimited by "d", "h", "m",
   * "s", or "ms", all corresponding to the different units of time.
   *
   * @param start the start epoch milliseconds
   * @param end   the end epoch milliseconds
   * @return String
   */
  public static String span(long start, long end) {
    if (start < end) return span(end - start);
    else if (start > end) return span(start - end);
    else return span(0);
  }


  /**
   * Returns a string which describes the interval, delimited by "d", "h", "m",
   * "s", or "ms", all corresponding to the different units of time.
   *
   * @param interval the millisecond interval
   * @return String
   */
  public static String span(long interval) {
    String out = "";

    for (Interval s : values()) {
      long unit = (interval - (interval % s.millis)) / s.millis;

      interval -= unit * s.millis;
      if (unit > 0L) {
        out += String.valueOf(unit) + s.abbr;
      }
    }
    return out;
  }
}
