import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

// Reproduction: a single instant rendered in two zones matches the two
// observed displays - the assignment email shows "Thu, Nov 5, 07:28 AM" with
// no timezone label, the candidate portal shows "Thu, Nov 5, 12:58 PM".
// The instant below is chosen to reproduce both minute-precision displays;
// the real stored value is not observable (and seconds are not shown), so
// this demonstrates CONSISTENCY, not the confirmed cause on Cynbit's side.
public class DeadlineDisplayReproTest {
    public static void main(String[] args) {
        Instant stored = Instant.parse("2026-11-05T07:28:00Z");
        // Locale is pinned: month, weekday and AM/PM text are locale-sensitive.
        DateTimeFormatter fmt = DateTimeFormatter
            .ofPattern("EEE, MMM d, hh:mm a z", Locale.ENGLISH);

        String asUtc = fmt.withZone(ZoneId.of("UTC")).format(stored);
        String asIst = fmt.withZone(ZoneId.of("Asia/Kolkata")).format(stored);

        System.out.println("chosen instant:  " + stored);
        System.out.println("rendered in UTC: " + asUtc + "  <- matches the email display (no zone shown)");
        System.out.println("rendered in IST: " + asIst + "  <- matches the portal display");

        check(asUtc.contains("07:28 AM"), "UTC rendering matches the email's 07:28 AM");
        check(asIst.contains("12:58 PM"), "IST rendering matches the portal's 12:58 PM");
        check(ZoneId.of("Asia/Kolkata").getRules().getOffset(stored).getTotalSeconds() == 5 * 3600 + 1800,
              "Asia/Kolkata is UTC+05:30 at that instant, the exact gap between the two displays");

        System.out.println("PASS: one stored instant reproduces both observed deadline displays");
        System.out.println("(minute precision - consistency with the observations, not a confirmed cause)");
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError("FAILED: " + what);
        System.out.println("ok - " + what);
    }
}
