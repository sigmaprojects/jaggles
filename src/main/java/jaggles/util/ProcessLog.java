package jaggles.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public final class ProcessLog {
    private static List<String> log = new ArrayList<String>();

    public static void add(String l) {
        log.add(l);
    }

    public static void add(String l, boolean systemOutPrintLn) {
        log.add(l);
        System.out.println(l);
    }

    public static void add(String l, boolean SystemOutPrintLn, Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String sStackTrace = sw.toString(); // stack trace as a string
        add(sStackTrace,SystemOutPrintLn);
    }

    public static List<String> getLog() {
        return log;
    }

}
