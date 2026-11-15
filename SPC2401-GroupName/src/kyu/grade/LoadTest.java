package kyu.grade;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/** Usage: LoadTest <clients> <submitsPerClient>. N concurrent clients, then verifies nothing was lost. */
public class LoadTest {
    public static void main(String[] a) throws Exception {
        int clients = Integer.parseInt(a[0]), per = Integer.parseInt(a[1]);
        Config c = Config.load();
        GradeClient probe = new GradeClient(c);
        int before = probe.submittedCount();
        ExecutorService pool = Executors.newFixedThreadPool(clients);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> fs = new ArrayList<>();
        for (int i = 0; i < clients; i++) {
            final int id = i;
            fs.add(pool.submit(() -> {
                GradeClient g = new GradeClient(c);
                go.await();                                   // all clients start together
                int ok = 0;
                for (int j = 0; j < per; j++) { g.submitMark("S" + id + "_" + j, "LOAD0", 50 + (j % 50)); ok++; }
                return ok;
            }));
        }
        long t0 = System.nanoTime();
        go.countDown();
        int sent = 0;
        for (Future<Integer> f : fs) sent += f.get();
        long ms = (System.nanoTime() - t0) / 1_000_000;
        pool.shutdown();
        int applied = probe.submittedCount() - before;
        int bad = 0;
        for (int i = 0; i < clients; i++)
            for (int j = 0; j < per; j++)
                if (probe.getMark("S" + i + "_" + j, "LOAD0") != 50 + (j % 50)) bad++;
        System.out.println("clients=" + clients + " sent=" + sent + " applied=" + applied
                + " wrong=" + bad + " timeMs=" + ms);
        System.out.println(sent == applied && bad == 0 ? "PASS: no lost update, no deadlock" : "FAIL");
        System.exit(sent == applied && bad == 0 ? 0 : 1);
    }
}
