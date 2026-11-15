package kyu.grade;

import java.rmi.Remote;
import java.rmi.RemoteException;

/** Callback interface. The student client exports an object of this type. */
public interface StudentListener extends Remote {
    void markPublished(String unitCode) throws RemoteException;
}
