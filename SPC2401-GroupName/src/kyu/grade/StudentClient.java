package kyu.grade;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Usage: StudentClient get <studentId> <unit> | watch <studentId> */
public class StudentClient implements StudentListener {
    private final GradeClient g;
    private final String sid;

    StudentClient(GradeClient g, String sid) { this.g = g; this.sid = sid; }

    /** Called by the server. Calls getMark() back safely because the server holds no lock (4.2 fix). */
    @Override
    public void markPublished(String unit) throws RemoteException {
        try {
            System.out.println("NOTIFICATION markPublished(" + unit + ") " + sid + " mark=" + g.getMark(sid, unit));
        } catch (GradeException e) {
            System.out.println("NOTIFICATION markPublished(" + unit + ") " + sid + " - " + e.getMessage());
        }
    }

    public static void main(String[] a) throws Exception {
        Config c = Config.load();
        GradeClient g = new GradeClient(c);
        if (a.length == 3 && a[0].equals("get")) {
            try {
                System.out.println("MARK " + a[1] + "/" + a[2] + " = " + g.getMark(a[1], a[2]));
            } catch (GradeException e) {
                System.out.println("REJECTED: " + e.getMessage());
                System.exit(2);
            }
        } else if (a.length == 2 && a[0].equals("watch")) {
            System.setProperty("java.rmi.server.hostname", c.get("client.host", "localhost"));
            StudentClient me = new StudentClient(g, a[1]);
            StudentListener stub = (StudentListener) UnicastRemoteObject.exportObject(me, 0);
            g.subscribe(a[1], stub);
            System.out.println("SUBSCRIBED " + a[1]);
            ScheduledExecutorService renew = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "lease-renew"); t.setDaemon(true); return t; });
            long half = c.leaseMs() / 2;                // renew at half the lease
            renew.scheduleAtFixedRate(() -> {
                try { g.subscribe(a[1], stub); } catch (Exception e) { Log.log("WARN", "renew failed: " + e); }
            }, half, half, TimeUnit.MILLISECONDS);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try { g.unsubscribe(a[1]); } catch (Exception ignored) { }
            }));
            Thread.currentThread().join();
        } else {
            System.out.println("Usage: StudentClient get <studentId> <unit> | watch <studentId>");
        }
    }
}
