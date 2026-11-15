package kyu.grade;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.UUID;

/**
 * Client-side wrapper: registry lookup, timeout, retry, and a request ID that stays the same
 * across retries so the server applies submitMark once (at-most-once).
 */
public class GradeClient {
    private final Config c;
    private GradeService svc;

    public GradeClient(Config c) {
        this.c = c;
        // Must be set before the first RMI call: bounds how long we wait for a reply.
        System.setProperty("sun.rmi.transport.tcp.responseTimeout", String.valueOf(c.timeoutMs()));
        System.setProperty("sun.rmi.transport.connectionTimeout", String.valueOf(c.timeoutMs()));
        System.setProperty("sun.rmi.transport.tcp.readTimeout", String.valueOf(c.timeoutMs()));
    }

    private interface Call<T> { T run(GradeService s) throws RemoteException, GradeException; }

    private synchronized GradeService svc() throws RemoteException, NotBoundException {
        if (svc == null)
            svc = (GradeService) LocateRegistry.getRegistry(c.registryHost(), c.registryPort())
                    .lookup(c.serviceName());
        return svc;
    }

    private synchronized void reset() { svc = null; }

    private <T> T invoke(String op, Call<T> call) throws RemoteException, GradeException {
        RemoteException last = null;
        int max = c.retries();
        for (int attempt = 1; attempt <= max; attempt++) {
            try {
                return call.run(svc());
            } catch (RemoteException e) {
                last = e;
            } catch (NotBoundException e) {
                last = new RemoteException("GradeService not bound", e);
            }
            Log.log("WARN", op + " attempt " + attempt + "/" + max + " failed: " + last);
            reset();                                   // re-lookup next time (server may have moved/restarted)
            try { Thread.sleep(200L * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
        }
        Log.log("ERROR", op + " gave up after " + max + " attempts");
        throw last;
    }

    public void submitMark(String sid, String unit, int mark) throws RemoteException, GradeException {
        final String requestId = UUID.randomUUID().toString();   // created ONCE, reused on every retry
        invoke("submitMark", s -> { s.submitMark(requestId, sid, unit, mark); return null; });
    }

    public int getMark(String sid, String unit) throws RemoteException, GradeException {
        return invoke("getMark", s -> s.getMark(sid, unit));
    }

    public void publishMarks(String unit) throws RemoteException, GradeException {
        invoke("publishMarks", s -> { s.publishMarks(unit); return null; });
    }

    public void subscribe(String sid, StudentListener l) throws RemoteException, GradeException {
        invoke("subscribe", s -> { s.subscribe(sid, l); return null; });
    }

    public void unsubscribe(String sid) throws RemoteException, GradeException {
        invoke("unsubscribe", s -> { s.unsubscribe(sid); return null; });
    }

    public int submittedCount() throws RemoteException, GradeException {
        return invoke("submittedCount", GradeService::submittedCount);
    }
}
