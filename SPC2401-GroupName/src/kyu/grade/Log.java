package kyu.grade;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;

/** Timestamped log to console and (optionally) a file. */
public final class Log {
    private static PrintWriter out;

    private Log() {}

    public static synchronized void init(String file) {
        try {
            out = new PrintWriter(new FileWriter(file, true), true);
        } catch (IOException e) {
            System.err.println("Cannot open log file " + file + ": " + e);
        }
    }

    public static synchronized void log(String level, String msg) {
        String line = Instant.now() + " [" + level + "] [" + Thread.currentThread().getName() + "] " + msg;
        System.out.println(line);
        if (out != null) out.println(line);
    }
}
