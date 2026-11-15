package kyu.grade;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GradeService extends Remote {
    /** Scenario signature. The server generates a fresh request ID, so it is NOT retry-safe. */
    void submitMark(String studentId, String unitCode, int mark) throws RemoteException, GradeException;

    /** Retry-safe version: the same requestId is applied at most once (at-most-once semantics). */
    void submitMark(String requestId, String studentId, String unitCode, int mark)
            throws RemoteException, GradeException;

    int getMark(String studentId, String unitCode) throws RemoteException, GradeException;

    void publishMarks(String unitCode) throws RemoteException, GradeException;

    /** Subscribes with a lease. Call again before the lease ends to renew. */
    void subscribe(String studentId, StudentListener listener) throws RemoteException, GradeException;

    void unsubscribe(String studentId) throws RemoteException;

    /** Admin/test helper: number of distinct submissions applied. */
    int submittedCount() throws RemoteException;
}
