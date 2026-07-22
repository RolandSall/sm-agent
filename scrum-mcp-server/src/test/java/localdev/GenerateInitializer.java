package localdev;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Writes {@code development/mockserver/initializer.json} from the SAME fixtures the in-JVM harness
 * uses ({@link LocalJiraFixtures#initializerJson}). Run via {@code ./gradlew generateMockInitializer}
 * before {@code docker compose -f development/docker-compose.yml up} so the standalone MockServer
 * container serves identical data. Dates are stamped at generation time — regenerate if the sprint
 * window drifts out of "today" (the window is now-5d .. now+9d).
 */
public final class GenerateInitializer {

    private GenerateInitializer() {
    }

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "development/mockserver/initializer.json");
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        Files.writeString(out, LocalJiraFixtures.initializerJson(Instant.now()));
        System.out.println("Wrote MockServer initializer: " + out.toAbsolutePath());
    }
}
