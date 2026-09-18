import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/** Measurement-only watchdog: save live Jazzer probes before stopping a hung JVM. */
public final class CoverageDeadline {
    public static void premain(String args, Instrumentation instrumentation) {
        final int separator = args.indexOf(',');
        final long delay = Long.parseLong(args.substring(0, separator));
        final String destination = args.substring(separator + 1);
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(delay);
                Class<?> recorder = null;
                // Jazzer may load its instrumentor through a dedicated class loader.
                for (Class<?> cls : instrumentation.getAllLoadedClasses()) {
                    if (cls.getName().equals("com.code_intelligence.jazzer.instrumentor.CoverageRecorder")) {
                        recorder = cls;
                        break;
                    }
                }
                if (recorder == null) throw new IllegalStateException("Jazzer coverage recorder not loaded");
                recorder.getMethod("updateCoveredIdsWithCoverageMap").invoke(null);
                Path temporary = Paths.get(destination + ".tmp");
                recorder.getMethod("dumpJacocoCoverage", String.class).invoke(null, temporary.toString());
                Files.move(temporary, Paths.get(destination), StandardCopyOption.REPLACE_EXISTING);
                System.err.println("[coverage-replay] Saved live coverage at deadline");
                System.err.flush();
                // The Python parent kills the JVM after this atomic snapshot.
            } catch (Throwable failure) {
                failure.printStackTrace();
                System.err.flush();
                // The parent enforces a final timeout if capture fails.
            }
        }, "coverage-replay-deadline");
        watchdog.setDaemon(true);
        watchdog.start();
    }
}
