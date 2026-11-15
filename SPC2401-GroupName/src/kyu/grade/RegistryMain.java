package kyu.grade;

import java.rmi.registry.LocateRegistry;

/** Step 1: run the RMI registry on its own. */
public class RegistryMain {
    public static void main(String[] args) throws Exception {
        Config c = Config.load();
        LocateRegistry.createRegistry(c.registryPort());
        Log.log("INFO", "Registry listening on port " + c.registryPort());
        Thread.currentThread().join();
    }
}
