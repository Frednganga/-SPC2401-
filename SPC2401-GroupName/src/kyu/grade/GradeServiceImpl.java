package kyu.grade;

import java.rmi.RemoteException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class GradeServiceImpl implements GradeService {

    private static final class Subscription {
        final StudentListener listener;
        final long expiresAt;
        Subscription(StudentListener l, long e) { listener = l; expiresAt = e; }
    }

    private final MarkStore store = new MarkStore();
    private final Map<String, Subscription> subs = new HashMap<>();
    private final Object subLock = new Object();       // guards subs, held only briefly
    private final long leaseMs;
    private final ExecutorService notifier;            // separate pool for callbacks
    private final ScheduledExecutorService janitor;
    private final Semaphore workers;                   // models the server worker-thread count (Section 7.4)
    private final int simCpuMs, simDiskMs;

    public GradeServiceImpl(Config c) {
        this.leaseMs = c.leaseMs();
        this.notifier = Executors.newFixedThreadPool(c.notifyPool(), named("notifier"));
        this.janitor = Executors.newSingleThreadScheduledExecutor(named("lease-janitor"));
        this.workers = new Semaphore(c.workers());
        this.simCpuMs = c.simCpuMs();
        this.simDiskMs = c.simDiskMs();
        long period = Math.max(1000, leaseMs / 4);
        janitor.scheduleAtFixedRate(this::expireLeases, period, period, TimeUnit.MILLISECONDS);
    }

    private static java.util.concurrent.ThreadFactory named(String prefix) {
        AtomicInteger n = new AtomicInteger();
        return r -> { Thread t = new Thread(r, prefix + "-" + n.incrementAndGet()); t.setDaemon(true); return t; };
    }

    @Override
    public void submitMark(String sid, String unit, int mark) throws RemoteException, GradeException {
        submitMark(UUID.randomUUID().toString(), sid, unit, mark);
    }

    @Override
    public void submitMark(String requestId, String sid, String unit, int mark)
            throws RemoteException, GradeException {
        try {
            boolean applied = store.submit(requestId, sid, unit, mark);
            Log.log("INFO", (applied ? "submitMark applied " : "submitMark DUPLICATE ignored ")
                    + "req=" + requestId + " " + sid + "/" + unit + "=" + mark);
        } catch (GradeException e) {
            Log.log("WARN", "submitMark rejected: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public int getMark(String sid, String unit) throws RemoteException, GradeException {
        workers.acquireUninterruptibly();
        try {
            simulateWork();
            return store.get(sid, unit);
        } finally {
            workers.release();
        }
    }

    private void simulateWork() {
        if (simCpuMs > 0) {                       // busy-spin = CPU time
            long end = System.nanoTime() + simCpuMs * 1_000_000L;
            while (System.nanoTime() < end) { /* spin */ }
        }
        if (simDiskMs > 0) {                      // sleeping = disk wait
            try { Thread.sleep(simDiskMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    @Override
    public void publishMarks(String unit) throws RemoteException, GradeException {
        store.publish(unit);
        // Snapshot under a SHORT lock, then release it BEFORE any remote callback (fix for 4.2).
        List<Map.Entry<String, StudentListener>> targets = new ArrayList<>();
        long now = System.currentTimeMillis();
        synchronized (subLock) {
            for (Map.Entry<String, Subscription> e : subs.entrySet())
                if (e.getValue().expiresAt > now)
                    targets.add(new SimpleEntry<>(e.getKey(), e.getValue().listener));
        }
        Log.log("INFO", "publishMarks " + unit + ": notifying " + targets.size() + " subscribers");
        for (Map.Entry<String, StudentListener> t : targets)
            notifier.submit(() -> notifyOne(t.getKey(), t.getValue(), unit));
    }

    private void notifyOne(String sid, StudentListener l, String unit) {
        try {
            l.markPublished(unit);
            Log.log("INFO", "notified " + sid + " of " + unit);
        } catch (RemoteException | RuntimeException e) {
            Log.log("WARN", "listener " + sid + " unreachable (" + e.getClass().getSimpleName()
                    + "); removing subscription");
            synchronized (subLock) {
                Subscription s = subs.get(sid);
                if (s != null && s.listener == l) subs.remove(sid);
            }
        }
    }

    @Override
    public void subscribe(String sid, StudentListener l) throws RemoteException, GradeException {
        if (sid == null || sid.trim().isEmpty()) throw new GradeException("studentId must not be empty");
        if (l == null) throw new GradeException("listener must not be null");
        synchronized (subLock) {
            subs.put(sid, new Subscription(l, System.currentTimeMillis() + leaseMs));
        }
        Log.log("INFO", "subscribe " + sid + " lease=" + leaseMs + "ms");
    }

    @Override
    public void unsubscribe(String sid) throws RemoteException {
        synchronized (subLock) { subs.remove(sid); }
        Log.log("INFO", "unsubscribe " + sid);
    }

    @Override
    public int submittedCount() { return store.submittedCount(); }

    private void expireLeases() {
        long now = System.currentTimeMillis();
        synchronized (subLock) {
            subs.entrySet().removeIf(e -> {
                boolean dead = e.getValue().expiresAt <= now;
                if (dead) Log.log("INFO", "lease expired for " + e.getKey());
                return dead;
            });
        }
    }

    public void shutdown() { notifier.shutdownNow(); janitor.shutdownNow(); }
}
