package kyu.grade;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Thread-safe marks store. No RMI here, so it can be unit-tested directly. */
public class MarkStore {
    public static final int MIN_MARK = 0, MAX_MARK = 100;

    private final ConcurrentHashMap<String, Integer> marks = new ConcurrentHashMap<>();
    private final Set<String> seenRequests = ConcurrentHashMap.newKeySet();
    private final Set<String> units = ConcurrentHashMap.newKeySet();
    private final Set<String> published = ConcurrentHashMap.newKeySet();
    private final AtomicInteger submitted = new AtomicInteger();

    static String key(String sid, String unit) { return sid + ":" + unit; }

    private static void needText(String name, String v) throws GradeException {
        if (v == null || v.trim().isEmpty()) throw new GradeException(name + " must not be empty");
    }

    /** @return true if applied, false if this requestId was already applied (duplicate retry). */
    public boolean submit(String requestId, String sid, String unit, int mark) throws GradeException {
        needText("studentId", sid);
        needText("unitCode", unit);
        if (mark < MIN_MARK || mark > MAX_MARK)
            throw new GradeException("Invalid mark " + mark + " (must be " + MIN_MARK + "-" + MAX_MARK + ")");
        if (requestId != null && !seenRequests.add(requestId)) return false; // atomic duplicate check
        marks.put(key(sid, unit), mark);
        units.add(unit);
        submitted.incrementAndGet();
        return true;
    }

    public int get(String sid, String unit) throws GradeException {
        needText("studentId", sid);
        needText("unitCode", unit);
        Integer m = marks.get(key(sid, unit));
        if (m == null) throw new GradeException("No mark for student " + sid + " in unit " + unit);
        return m;
    }

    public void publish(String unit) throws GradeException {
        needText("unitCode", unit);
        if (!units.contains(unit)) throw new GradeException("No marks submitted for unit " + unit);
        published.add(unit);
    }

    public boolean isPublished(String unit) { return published.contains(unit); }
    public int submittedCount() { return submitted.get(); }
}
