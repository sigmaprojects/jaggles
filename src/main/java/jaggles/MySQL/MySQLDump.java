package jaggles.MySQL;

import jaggles.Jaggles;
import jaggles.OSValidator;
import jaggles.items.MySQLHost;
import jaggles.util.ProcessLog;
import jaggles.util.TimeKeeper;

import java.io.*;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author Don Quist https://www.sigmaprojects.org
 */
public class MySQLDump {

    private String hostname;
    private String username;
    private String password;
    private MySQLHost mysqlHost;
    private String fullBackupPath;
    private File binPath;
    private String mysqlDumpBin;
    private int perHostThreadCount;

    MySQLDump(MySQLHost mysqlHost, File binPath, String mysqlDumpBin, int perHostThreadCount) {
        this.mysqlHost = mysqlHost;
        this.hostname = mysqlHost.getHost();
        this.username = mysqlHost.getUser();
        this.password = mysqlHost.getPass();
        this.fullBackupPath = Jaggles.backupFilePath.getAbsolutePath() + "/mysql/"  + this.hostname;
        this.binPath = binPath;
        this.mysqlDumpBin = mysqlDumpBin;
        this.perHostThreadCount = perHostThreadCount;
    }


    public void start() throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool( this.perHostThreadCount );

        List<Callable<String>> taskList = new ArrayList<>();

        TimeKeeper allTk = new TimeKeeper();
        for (String db : this.mysqlHost.getDatabases()) {
            Callable<String> task = () -> {
                TimeKeeper tk = new TimeKeeper();
                backupDatabase(db);
                ProcessLog.add("Database Backup Complete: " + db + " took: " + tk.getAgeInSeconds() + " seconds", true);
                return "All Databases Finished";
            };
            taskList.add(task);
        }

        List<Future<String>> futures = executorService.invokeAll(taskList);
        //noinspection LoopStatementThatDoesntLoop
        for(Future<String> future: futures) {
            // The result is printed only after all the futures are complete.
            ProcessLog.add(future.get() + " took " + allTk.getAgeInSeconds() + " seconds", true);
            break;
        }

        executorService.shutdown();
    }


    private boolean backupDatabase(String database) {
        boolean success = false;

        File backupPath = new File( fullBackupPath );
        backupPath.mkdirs(); // create the directory if it doesn't exist

        File outputFile = new File(backupPath.getAbsolutePath() + "/" + database + ".sql");

        ArrayList<String> args = new ArrayList<>();
        if( OSValidator.isWindows() ) {
            args.add("cmd");
            args.add("/c");
        }
        args.add(mysqlDumpBin);
        args.add("-h");
        args.add(hostname);
        args.add("-u");
        args.add(username);
        args.add("-p" + password);
        args.add("-c");     // Use complete insert statements.
        args.add("-E");     // dump events
        args.add("-R");     // dump routines and functions
        args.add("--force"); // continue even if sql error
        args.add("--opt");  // --opt is Same as --add-drop-table, --add-locks, --create-options,
                            // --quick, --extended-insert, --lock-tables, --set-charset,
                            // and --disable-keys. Enabled by default, disable with
                            // --skip-opt.
        args.add("--max-allowed-packet"); //The maximum packet length to send to or receive from server.
        args.add("1024M");
        args.add("--databases");
        args.add(database);
        args.add("-r");     // Direct output to a given file
        args.add(outputFile.getAbsolutePath());


        ProcessBuilder ps = new ProcessBuilder(args);
        ps.directory(binPath);

        // this is false by default, redirect it
        ps.redirectErrorStream(true);

        try {
            Process pr = ps.start();

            BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                if( !line.contains("line interface can be insecure") ) { // ignore this specifically
                    ProcessLog.add("Output From " + this.mysqlDumpBin + " on host " + this.hostname + ": " + line, true);
                }
            }
            pr.waitFor();
            in.close();

            try {
                Jaggles.zipFile(outputFile,true);
                success = true;
            } catch(IOException io) {
                ProcessLog.add("Error zipping file: " + outputFile.getAbsolutePath(), true, io);
            }

        } catch(Exception e) {
            ProcessLog.add("Uncaught MySQLDump error backing up: " + database, true, e);
        }

        return success;
    }




}

