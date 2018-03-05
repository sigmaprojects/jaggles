package jaggles;

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
    MySQLHost mysqlHost;
    private File binPath = new File(".");
    private String mysqlDumpBin = "mysqldump";
    private boolean isReady = false;
    private String fullBackupPath;

    public MySQLDump(MySQLHost mysqlHost, File binPath) {
        this.mysqlHost = mysqlHost;
        this.hostname = mysqlHost.getHost();
        this.username = mysqlHost.getUser();
        this.password = mysqlHost.getPass();
        this.binPath = binPath;
        this.fullBackupPath = Jaggles.backupFilePath.getAbsolutePath() + "/mysql/"  + this.hostname;
        // windows will have the .exe for the bin
        if( OSValidator.isWindows() ) {
            this.mysqlDumpBin = "mysqldump.exe";
        }
        validatePaths();
    }

    public boolean getIsReady() {
        return this.isReady;
    }

    private void validatePaths() {
        // do sanity checks to verify the dump tool exists in a path somewhere
        if( checkBinPath(this.binPath) ) {
            // do nothing, it exists in the first passed reference
            //System.out.println("found in passed");
            this.isReady = true;
        } else {
            List<File> possibleLocations = getPossibleBinLocations();
            for(File possiblePath : possibleLocations) {
                System.out.println("Checking for `" +this.mysqlDumpBin+ "` in: " + possiblePath.getAbsolutePath());
                if( checkBinPath( possiblePath ) ) {
                    this.binPath = possiblePath;
                    ProcessLog.add("MySQLDump bin not found in passed location, found location: " + possiblePath, true);
                    this.isReady = true;
                    break;
                }
            }
            ProcessLog.add("MySQLDump bin NOT FOUND!", true);
        }
    }



    public void backupThreaded(int threadCount) throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        List<Callable<String>> taskList = new ArrayList<Callable<String>>();

        TimeKeeper allTk = new TimeKeeper();
        for (String db : this.mysqlHost.getDatabases()) {
            Callable<String> task = () -> {
                //System.out.println("Backing Up Database: " + db);
                TimeKeeper tk = new TimeKeeper();
                backupDatabase(db);
                ProcessLog.add("Database Backup Complete: " + db + " took: " + tk.getAgeInSeconds() + " seconds", true);
                return "All Databases Finished";
            };
            taskList.add(task);
        }

        List<Future<String>> futures = executorService.invokeAll(taskList);
        for(Future<String> future: futures) {
            // The result is printed only after all the futures are complete. (i.e. after 5 seconds)
            //System.out.println(future.get());
            ProcessLog.add(future.get() + " took " + allTk.getAgeInSeconds() + " seconds", true);
            break;
        }

        executorService.shutdown();
    }


    public boolean backupDatabase(String database) {
        boolean success = false;

        File backupPath = new File( fullBackupPath );
        backupPath.mkdirs(); // create the directory if it doesn't exist

        File outputFile = new File(backupPath.getAbsolutePath() + "/" + database + ".sql");

        ArrayList<String> args = new ArrayList<String>();
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
                if( line.contains("line interface can be insecure") ) {
                    // ignore this
                } else {
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




    public boolean checkBinPath(File path) {
        boolean exists = true;

        if( path.exists() && path.isFile()  ) {
            return true;
        }

        ArrayList<String> cmdArgs = new ArrayList<String>();
        if( OSValidator.isWindows() ) {
            cmdArgs.add("cmd");
            cmdArgs.add("/c");
        }
        cmdArgs.add(mysqlDumpBin);
        ProcessBuilder ps = new ProcessBuilder(cmdArgs);
        ps.directory(path);
        ps.redirectErrorStream(true);
        try {
            Process pr = ps.start();

            BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                if( line.contains("is not recognized") || line.contains("not found") ) {
                    exists = false;
                    break;
                }
            }
            pr.waitFor();
            in.close();

        } catch(Exception e) {
            exists = false;
            //e.printStackTrace();
        }
        return exists;
    }


    private List<File> getPossibleBinLocations() {
        List<File> locs = new ArrayList<>();

        locs.add( new File(Paths.get(".").toAbsolutePath().normalize().toString() ) );

        if( OSValidator.isWindows() ) {
            // add all the paths produced by windows %PATH%
            List<String> sysPaths = new ArrayList<String>(Arrays.asList(System.getenv("PATH").split(";")));
            for (String sysPath : sysPaths) {
                locs.add(new File(sysPath));
            }
        } else {
            // add all other paths (most likely linux-ish)
            // same process as Windows, just different separator
            List<String> sysPaths = new ArrayList<String>(Arrays.asList(System.getenv("PATH").split(":")));
            for (String sysPath : sysPaths) {
                locs.add(new File(sysPath));
            }
        }

        return locs;
    }

}

