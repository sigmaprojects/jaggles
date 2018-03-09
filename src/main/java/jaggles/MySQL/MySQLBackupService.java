package jaggles.MySQL;

import jaggles.OSValidator;
import jaggles.items.MySQLHost;
import jaggles.util.ProcessLog;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 *
 * @author Don Quist https://www.sigmaprojects.org
 */
public class MySQLBackupService {

    private File binPath;
    private String mysqlDumpBin = "mysqldump";
    private boolean isReady = false;
    private int perHostThreadCount;

    public MySQLBackupService(int perHostThreadCount, File binPath) {
        this.binPath = binPath;
        this.perHostThreadCount = perHostThreadCount;
        // windows will have the .exe for the bin
        if( OSValidator.isWindows() ) {
            this.mysqlDumpBin = "mysqldump.exe";
        }
        validatePaths();
    }

    public MySQLDump createWorker(MySQLHost mysqlHost) {
        return new MySQLDump(
                mysqlHost,
                getBinPath(),
                getMysqlDumpBin(),
                getPerHostThreadCount()
        );
    }

    private File getBinPath() { return this.binPath; }

    private String getMysqlDumpBin() { return this.mysqlDumpBin; }

    private int getPerHostThreadCount() { return this.perHostThreadCount; }

    public boolean getIsReady() {
        return this.isReady;
    }


    private void validatePaths() {
        // do sanity checks to verify the dump tool exists in a path somewhere
        if( checkBinPath(this.binPath) ) {
            // do nothing, it exists in the first passed reference
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


    private boolean checkBinPath(File path) {
        boolean exists = true;

        if( path.exists() && path.isFile()  ) {
            return true;
        }

        ArrayList<String> cmdArgs = new ArrayList<>();
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
            pr.destroy();
        } catch(Exception e) {
            exists = false;
            //e.printStackTrace();
        }
        return exists;
    }


    private List<File> getPossibleBinLocations() {
        List<File> locations = new ArrayList<>();

        locations.add( new File(Paths.get(".").toAbsolutePath().normalize().toString() ) );

        String separator = ( OSValidator.isWindows() ? ";" : ":" );

        List<String> sysPaths = new ArrayList<>(Arrays.asList(System.getenv("PATH").split(separator)));
        for (String sysPath : sysPaths) {
            locations.add(new File(sysPath));
        }

        return locations;
    }

}

