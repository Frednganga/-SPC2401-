package kyu.grade;

import static org.junit.jupiter.api.Assertions.*;

import java.rmi.RemoteException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class GradeServiceImplTest {
    private static GradeServiceImpl svc() { System.setProperty("lease.ms", "400"); return new GradeServiceImpl(Config.load()); }

    @Test void publishNotifiesSubscriber() throws Exception {            // TC09
        GradeServiceImpl s = svc();
        CountDownLatch got = new CountDownLatch(1);
        s.subscribe("S1", u -> got.countDown());
        s.submitMark("r1", "S1", "U", 70);
        s.publishMarks("U");
        assertTrue(got.await(2, TimeUnit.SECONDS));
    }

    @Test void deadListenerSkippedOthersStillNotified() throws Exception { // TC10
        GradeServiceImpl s = svc();
        CountDownLatch good = new CountDownLatch(1);
        s.subscribe("DEAD", u -> { throw new RemoteException("connection refused"); });
        s.subscribe("GOOD", u -> good.countDown());
        s.submitMark("r1", "S1", "U", 70);
        s.publishMarks("U");
        assertTrue(good.await(2, TimeUnit.SECONDS));
    }

    @Test void slowListenerDoesNotBlockGetMark() throws Exception {      // TC11: 4.2 fix - no lock held in callback
        GradeServiceImpl s = svc();
        s.submitMark("r1", "S1", "U", 70);
        s.subscribe("S1", u -> {
            try { assertEquals(70, s.getMark("S1", u)); } catch (GradeException e) { fail(e); }  // re-enter server
        });
        ExecutorService ex = Executors.newSingleThreadExecutor();
        Future<?> f = ex.submit(() -> { s.publishMarks("U"); return null; });
        f.get(2, TimeUnit.SECONDS);                                      // would hang if a lock were held
    }

    @Test void leaseExpiryStopsNotifications() throws Exception {        // TC12
        GradeServiceImpl s = svc();
        AtomicInteger n = new AtomicInteger();
        s.subscribe("S1", u -> n.incrementAndGet());
        s.submitMark("r1", "S1", "U", 70);
        Thread.sleep(600);                                               // > lease.ms
        s.publishMarks("U");
        Thread.sleep(300);
        assertEquals(0, n.get());
    }
}
