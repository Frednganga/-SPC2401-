package kyu.grade;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/** Step 2: export GradeService and bind it in the registry. */
public class GradeServer {
    public static void main(String[] args) throws Exception {
        Config c = Config.load();
        Log.init(c.get("log.file", "evidence/server-" + System.currentTimeMillis() + ".log"));
        System.setProperty("java.rmi.server.hostname", c.get("server.host", "localhost"));
        // Callback timeout: a dead student must not hang a notifier thread forever.
        System.setProperty("sun.rmi.transport.tcp.responseTimeout", String.valueOf(c.timeoutMs()));
        System.setProperty("sun.rmi.transport.tcp.readTimeout", String.valueOf(c.timeoutMs()));
        System.setProperty("sun.rmi.transport.connectionTimeout", String.valueOf(c.timeoutMs()));

        GradeServiceImpl impl = new GradeServiceImpl(c);
        GradeService stub = (GradeService) UnicastRemoteObject.exportObject(impl, 0);
        Registry reg = LocateRegistry.getRegistry(c.registryHost(), c.registryPort());
        reg.rebind(c.serviceName(), stub);
        Log.log("INFO", "GradeService bound as '" + c.serviceName() + "' at "
                + c.registryHost() + ":" + c.registryPort());
        Runtime.getRuntime().addShutdownHook(new Thread(impl::shutdown));
    }
}
