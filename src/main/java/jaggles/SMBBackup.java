package jaggles;

import jaggles.items.SMB;
import jaggles.util.ProcessLog;
import jaggles.util.TimeKeeper;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class SMBBackup {

    SMB smb;
    String fullBackupPath;

    SMBBackup(SMB smb) {
        this.smb = smb;
        // make sure the backup root destination exists
        this.fullBackupPath = Jaggles.backupFilePath.getAbsolutePath() + "/smb/"  + smb.getServer() + "/" + smb.getShareName();
    }


    public void backup(int threadCount) {
        try {
            StaticUserAuthenticator auth = new StaticUserAuthenticator(smb.getDomain(), smb.getUsername(), smb.getPassword());
            FileSystemOptions opts = new FileSystemOptions();
            DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, auth);
            FileSystemManager fs = VFS.getManager();

            if (!fs.hasProvider("smb")) throw new RuntimeException("Provide missing");
            System.out.println("Connecting " + smb.getServer() + " with " + opts);
            FileObject smbShare = fs.resolveFile("smb://" + smb.getServer() + "/" + smb.getShareName() + "/" + smb.getPath(), opts); // added opts!

            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            List<Callable<String>> taskList = new ArrayList<Callable<String>>();
            TimeKeeper allTk = new TimeKeeper();
            for(FileObject fo : smbShare.getChildren() ) {
                Callable<String> task = () -> {
                    //ProcessLog.add("Backing Up SMB: " + fo.getPublicURIString(), true);
                    TimeKeeper tk = new TimeKeeper();
                    backupSMBFile(fo);
                    ProcessLog.add("SMB Backup Complete: " + fo.getPublicURIString() + " took: " + tk.getAgeInSeconds() + " seconds", true);
                    return "All SMB Files Finished For: " + smb.getServer() + "/" + smb.getShareName() + "/" + smb.getPath();
                };
                taskList.add(task);
            }

            List<Future<String>> futures = executorService.invokeAll(taskList);
            for(Future<String> future: futures) {
                // The result is printed only after all the futures are complete.
                try {
                    smbShare.close();
                } catch(FileSystemException fse) {
                    ProcessLog.add("Error closing smbFile in futures close", true, fse);
                }

                executorService.shutdown();

                /*
                ProcessLog.add("SMB Copy Finished for: " + smb.getServer() + "/" + smb.getShareName() + "/" + smb.getPath() + " took " + allTk.getAgeInSeconds(), true);

                String dirToZip = this.fullBackupPath + "/" + smb.getPath();

                ProcessLog.add("Now Zipping: " + dirToZip);
                ZipUtil.pack(new File(dirToZip), new File(dirToZip + ".zip"));
                */

                ProcessLog.add(future.get() + " took " + allTk.getAgeInSeconds() + " seconds", true);
                break;
            }


        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void backupSMBFile(FileObject smbFile) {
        try {
            //String removeFromPath = "smb://" + smb.getServer() + "/" + smb.getPath() + "/";
            // just get the last file or folder name
            String copyToPath = smbFile.getPublicURIString().substring(smbFile.getPublicURIString().lastIndexOf("/") + 1);

            String fullCopyToPath = this.fullBackupPath + "/" + smb.getPath() + "/" + copyToPath;

            FileObject dest = VFS.getManager().resolveFile(fullCopyToPath);

            // make sure the directory exists
            File fullyCopyToPathFile = new File(fullCopyToPath);
            fullyCopyToPathFile.mkdir();

            if( smbFile.isFolder() ) {
                new File(dest.getPublicURIString().replace("file:///", "")).mkdir();
            }

            FileSelector fileSelector = new FileSelector() {
                @Override
                public boolean includeFile(FileSelectInfo fileSelectInfo) throws Exception {
                    return true;
                }

                @Override
                public boolean traverseDescendents(FileSelectInfo fileSelectInfo) throws Exception {
                    return true;
                }
            };
            dest.copyFrom(smbFile, fileSelector);
            dest.close();
            smbFile.close();

        } catch (Exception e) {
            ProcessLog.add("Error copying file", true, e);
            //e.printStackTrace();
            try {
                smbFile.close();
            } catch(FileSystemException fse) {
                ProcessLog.add("Error closing smbFile in backupSMBFile", true, e);
                //e.printStackTrace();
            }
        }

    }

}
