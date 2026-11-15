package kyu.grade;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class MarkStoreTest {
    @Test void submitThenGet() throws Exception {                       // TC01
        MarkStore s = new MarkStore();
        s.submit("r1", "KYU001", "SPC2401", 78);
        assertEquals(78, s.get("KYU001", "SPC2401"));
    }

    @Test void invalidMarkRejected() {                                  // TC02
        MarkStore s = new MarkStore();
        assertThrows(GradeException.class, () -> s.submit("r", "KYU001", "SPC2401", 150));
        assertThrows(GradeException.class, () -> s.submit("r", "KYU001", "SPC2401", -1));
    }

    @Test void unknownStudentRejected() {                               // TC03
        assertThrows(GradeException.class, () -> new MarkStore().get("NOPE", "SPC2401"));
    }

    @Test void boundaryMarksAccepted() throws Exception {               // TC04
        MarkStore s = new MarkStore();
        s.submit("a", "S1", "U", 0);
        s.submit("b", "S2", "U", 100);
        assertEquals(0, s.get("S1", "U"));
        assertEquals(100, s.get("S2", "U"));
    }

    @Test void duplicateRequestIdAppliedOnce() throws Exception {       // TC05
        MarkStore s = new MarkStore();
        assertTrue(s.submit("same", "S1", "U", 60));
        assertFalse(s.submit("same", "S1", "U", 60));
        assertEquals(1, s.submittedCount());
    }

    @Test void emptyIdsRejected() {                                     // TC06
        MarkStore s = new MarkStore();
        assertThrows(GradeException.class, () -> s.submit("r", "", "U", 50));
        assertThrows(GradeException.class, () -> s.submit("r", "S", null, 50));
    }

    @Test void publishUnknownUnitRejected() throws Exception {          // TC07
        MarkStore s = new MarkStore();
        assertThrows(GradeException.class, () -> s.publish("GHOST"));
        s.submit("r", "S1", "U", 50);
        s.publish("U");
        assertTrue(s.isPublished("U"));
    }

    @Test void concurrentSubmitsLoseNothing() throws Exception {        // TC08: the 4.1 race, fixed
        MarkStore s = new MarkStore();
        int threads = 50, per = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        for (int t = 0; t < threads; t++) {
            final int id = t;
            pool.submit(() -> {
                try {
                    start.await();
                    for (int j = 0; j < per; j++) s.submit("r" + id + "_" + j, "S" + id + "_" + j, "U", 50);
                } catch (Exception e) { throw new RuntimeException(e); }
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        assertEquals(threads * per, s.submittedCount());
    }
}
