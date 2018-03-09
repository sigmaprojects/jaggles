/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jaggles;

import jaggles.Git.GitBackup;
import jaggles.Git.GitBackupService;
import jaggles.MySQL.MySQLBackupService;
import jaggles.MySQL.MySQLDump;
import jaggles.items.Git;
import jaggles.items.Item;
import jaggles.items.MySQLHost;
import jaggles.items.SMB;
import jaggles.util.ProcessLog;
import jaggles.util.SendEmail;
import jaggles.util.TimeKeeper;
import org.apache.commons.cli.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author don
 */
public class Jaggles {

    public static File backupFilePath;

    /**
     * @param args the command line arguments
     */
    @SuppressWarnings("LoopStatementThatDoesntLoop")
    public static void main(String[] args) {

        Options options = new Options();

        Option inputPath = new Option("p", "path", true, "backup path");
        inputPath.setRequired(false);
        options.addOption(inputPath);


        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("jaggles", options);

            System.exit(1);
            return;
        }

        String inputFilePath = cmd.getOptionValue("path");

        String useBackupPath = "./backups/";

        if( inputFilePath == null || inputFilePath.length() == 0 ) {
            ProcessLog.add("Argument --path not supplied, using default: " + useBackupPath, true);
        } else {
            ProcessLog.add("Argument --path supplied, using: " + useBackupPath, true);
        }

        backupFilePath = getBackupFilePath( useBackupPath );

        DB db = new DB();

        // gather up all the backup items
        List<? extends Item> gits = db.getGits();
        List<? extends Item> mysqlHosts = db.getMySQLHosts();
        List<? extends Item> smbs = db.getSMBs();

        // merge them together
        List<? extends Item> backupItems = Stream.of(gits, mysqlHosts, smbs)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        MySQLBackupService mySQLBackupService = new MySQLBackupService(10, getJarDir(Jaggles.class));
        GitBackupService gitBackupService = new GitBackupService();

        try {
            ExecutorService executorService = Executors.newFixedThreadPool(100);

            List<Callable<String>> taskList = new ArrayList<>();

            TimeKeeper allTk = new TimeKeeper();
            for (Item item : backupItems) {
                Callable<String> task = () -> {
                    if( item instanceof Git ) {
                        GitBackup gitBackup = gitBackupService.createWorker( (Git)item );
                       gitBackup.start();
                    } else if( item instanceof MySQLHost ) {
                        if( mySQLBackupService.getIsReady() ) {
                            MySQLDump mySQLDump = mySQLBackupService.createWorker(
                                (MySQLHost) item
                            );
                            mySQLDump.start();
                        }
                    } else if( item instanceof SMB ) {
                        new SMBBackup( (SMB)item ).backup(40);
                    }
                    return "All Backup Items Finished";
                };
                taskList.add(task);
            }

            List<Future<String>> futures = executorService.invokeAll(taskList);
            //noinspection LoopStatementThatDoesntLoop
            for (Future<String> future : futures) {
                // The result is printed only after all the futures are complete. (i.e. after 5 seconds)
                System.out.println(future.get());
                ProcessLog.add(future.get() + " took " + allTk.getAgeInSeconds() + " seconds", true);
                ProcessLog.add("Jaggles has completed all operations", true);
                break;
            }
            executorService.shutdown();
        } catch(InterruptedException | ExecutionException e) {
            System.out.println("Error executing all");
            e.printStackTrace();
        }

        SendEmail.sendLog();

    }


    private static File getBackupFilePath(String path) {
        Calendar ca1 = Calendar.getInstance();
        ca1.setMinimalDaysInFirstWeek(1);
        int wk = ca1.get(Calendar.WEEK_OF_MONTH);
        //System.out.println("Week of Month :" + wk);
        //System.out.println("date :" + ca1.getTime().toString());

        File f = new File(path + "/" + wk);
        System.out.println("Target backup path: " + f);
        f.mkdir();
        return f;
    }

    public static void zipFile(File fileToZip, boolean deleteSourceFile) throws IOException {
        File outputFile = new File( fileToZip.getParent() + "/" + fileToZip.getName() + ".zip" );
        FileOutputStream fos = new FileOutputStream(outputFile);
        ZipOutputStream zipOut = new ZipOutputStream(fos);

        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
        zipOut.putNextEntry(zipEntry);
        final byte[] bytes = new byte[1024];
        int length;
        while((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        zipOut.close();
        fis.close();
        fos.close();
        if( deleteSourceFile ) {
            fileToZip.delete();
        }
    }

    /**
     * Compute the absolute file path to the jar file.
     * The framework is based on http://stackoverflow.com/a/12733172/1614775
     * But that gets it right for only one of the four cases.
     *
     * @param aclass A class residing in the required jar.
     *
     * @return A File object for the directory in which the jar file resides.
     * During testing with NetBeans, the result is ./build/classes/,
     * which is the directory containing what will be in the jar.
     */
    public static File getJarDir(Class aclass) {
        URL url;
        String extURL;      //  url.toExternalForm();

        // get an url
        try {
            url = aclass.getProtectionDomain().getCodeSource().getLocation();
            // url is in one of two forms
            //        ./build/classes/   NetBeans test
            //        jardir/JarName.jar  froma jar
        } catch (SecurityException ex) {
            url = aclass.getResource(aclass.getSimpleName() + ".class");
            // url is in one of two forms, both ending "/com/physpics/tools/ui/PropNode.class"
            //          file:/U:/Fred/java/Tools/UI/build/classes
            //          jar:file:/U:/Fred/java/Tools/UI/dist/UI.jar!
        }

        // convert to external form
        extURL = url.toExternalForm();

        // prune for various cases
        if (extURL.endsWith(".jar"))   // from getCodeSource
            extURL = extURL.substring(0, extURL.lastIndexOf("/"));
        else {  // from getResource
            String suffix = "/"+(aclass.getName()).replace(".", "/")+".class";
            extURL = extURL.replace(suffix, "");
            if (extURL.startsWith("jar:") && extURL.endsWith(".jar!"))
                extURL = extURL.substring(4, extURL.lastIndexOf("/"));
        }

        // convert back to url
        try {
            url = new URL(extURL);
        } catch (MalformedURLException mux) {
            // leave url unchanged; probably does not happen
        }

        // convert url to File
        try {
            return new File(url.toURI());
        } catch(URISyntaxException ex) {
            return new File(url.getPath());
        }
    }

}

