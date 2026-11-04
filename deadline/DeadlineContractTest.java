import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// Contract test: two surfaces reading the same service can never disagree.
public class DeadlineContractTest {
    public static void main(String[] args) {
        DeadlineService svc = new DeadlineService(
            Instant.parse("2026-11-05T07:28:00Z"), ZoneId.of("Asia/Kolkata"));

        String email  = svc.render();   // email template calls this
        String portal = svc.render();   // portal API calls this

        if (!email.equals(portal)) {
            throw new AssertionError("surfaces disagree: " + email + " vs " + portal);
        }
        System.out.println("PASS: email and portal render the same deadline -> " + email);
    }
}

// One instant, stored once, rendered one way everywhere.
final class DeadlineService {
    private final Instant deadline;
    private final ZoneId displayZone;

    DeadlineService(Instant deadline, ZoneId displayZone) {
        this.deadline = deadline;
        this.displayZone = displayZone;
    }

    // The single formatter every surface calls: email, portal, API.
    String render() {
        return DateTimeFormatter
            .ofPattern("EEE, MMM d, hh:mm a z")
            .withZone(displayZone)
            .format(deadline);
    }
}
