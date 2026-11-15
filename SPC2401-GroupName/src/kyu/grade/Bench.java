package kyu.grade;

import java.rmi.registry.LocateRegistry;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Usage: Bench <local|remote> <clientThreads> <callsPerThread>
 * Prints: mode,threads,calls,meanLatencyMs,throughputPerSec
 * Server worker threads are set on the SERVER with -Dserver.workers=N (see scripts/bench.sh).
 */
public class Bench {
    public static void main(String[] a) throws Exception {
        String mode = a[0];
        int threads = Integer.parseInt(a[1]), calls = Integer.parseInt(a[2]);
        Config c = Config.load();
        final GradeService shared;
        if (mode.equals("local")) shared = new GradeServiceImpl(c);
        else shared = (GradeService) LocateRegistry.getRegistry(c.registryHost(), c.registryPort())
                .lookup(c.serviceName());
        shared.submitMark("BENCH-SEED-" + mode, "B1", "U1", 70);
        for (int i = 0; i < 200; i++) shared.getMark("B1", "U1");            // JVM warm-up

        AtomicLong totalNs = new AtomicLong();
        Thread[] ts = new Thread[threads];
        long t0 = System.nanoTime();
        for (int i = 0; i < threads; i++) {
            ts[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < calls; j++) {
                        long s = System.nanoTime();
                        shared.getMark("B1", "U1");
                        totalNs.addAndGet(System.nanoTime() - s);
                    }
                } catch (Exception e) { System.err.println("call failed: " + e); }
            });
            ts[i].start();
        }
        for (Thread t : ts) t.join();
        double wallS = (System.nanoTime() - t0) / 1e9;
        long n = (long) threads * calls;
        System.out.printf("%s,%d,%d,%.3f,%.1f%n", mode, threads, n, totalNs.get() / 1e6 / n, n / wallS);
        System.exit(0);
    }
}
