package kyu.grade;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/** No host or port is hard-coded: -Dkey=value beats config.properties beats the defaults below. */
public final class Config {
    private final Properties file = new Properties();

    private Config() {}

    public static Config load() {
        Config c = new Config();
        String path = System.getProperty("kyu.config", "config.properties");
        try (FileInputStream in = new FileInputStream(path)) {
            c.file.load(in);
        } catch (IOException e) {
            // file is optional; defaults apply
        }
        return c;
    }

    public String get(String key, String def) {
        String v = System.getProperty(key);
        return v != null ? v : file.getProperty(key, def);
    }

    public int getInt(String key, int def) { return Integer.parseInt(get(key, String.valueOf(def))); }

    public String registryHost() { return get("registry.host", "localhost"); }
    public int registryPort()    { return getInt("registry.port", 1099); }
    public String serviceName()  { return get("service.name", "GradeService"); }
    public long leaseMs()        { return getInt("lease.ms", 60000); }
    public int timeoutMs()       { return getInt("rmi.timeout.ms", 3000); }
    public int retries()         { return getInt("client.retries", 3); }
    public int notifyPool()      { return getInt("notify.pool", 20); }
    public int workers()         { return getInt("server.workers", 4); }
    public int simCpuMs()        { return getInt("sim.cpu.ms", 0); }
    public int simDiskMs()       { return getInt("sim.disk.ms", 0); }
}
