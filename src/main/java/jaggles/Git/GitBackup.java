package jaggles.Git;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import jaggles.Jaggles;
import jaggles.util.ProcessLog;
import jaggles.util.TimeKeeper;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GitBackup {
    // https://www.programcreek.com/java-api-examples/?api=org.eclipse.jgit.api.PullCommand

    private jaggles.items.Git git;
    private File backupPath;
    private String privateKeyPath = "./git.key";
    private SshSessionFactory sshSessionFactory;

    GitBackup(jaggles.items.Git git, SshSessionFactory sshSessionFactory) {
        this.git = git;
        this.sshSessionFactory = sshSessionFactory;

        String fullBackupPath = Jaggles.backupFilePath.getAbsolutePath() + "/gits/" + git.getRepoName();
        backupPath = new File( fullBackupPath );
        backupPath.mkdirs(); // create the directory if it doesn't exist
    }


    public void start() {
        TimeKeeper tk = new TimeKeeper();

        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI( git.getUrl() )
                .setDirectory(backupPath)
                .setTransportConfigCallback(transport -> {
                    SshTransport sshTransport = ( SshTransport )transport;
                    sshTransport.setSshSessionFactory( sshSessionFactory );
                });

        try {

            cloneCommand.call();

        } catch (TransportException te) {
            ProcessLog.add("Git Error: " + git.getUrl() + " transport error: " + te.getMessage(), true);

        } catch (JGitInternalException e) {
            if (e.getMessage().contains("already exists")) {
                pull();
            } else {
                ProcessLog.add("Git Error: Unable to backup git: " + this.git.getUrl(), true, e);
            }
        } catch(InvalidRemoteException ire) {
            ProcessLog.add( "Git Error: Invalid Remote: " + git.getUrl() + " - " + ire.getMessage(), true);
        } catch(Exception e) {
            ProcessLog.add("Git Error: Uncaught: " + this.git.getUrl(), true, e);
        }
        ProcessLog.add( "Git Backup For Repo: " + git.getUrl() + " Has Finished.  Took " + tk.getAgeInSeconds() + " seconds", true);
    }


    private boolean pull() {
        try {
            Repository localRepo = new FileRepository(backupPath.getAbsolutePath() + "/.git");
            Git git = new Git(localRepo);

            PullCommand pullCmd = git.pull();

            pullCmd.setTransportConfigCallback(transport -> {
                SshTransport sshTransport = ( SshTransport )transport;
                sshTransport.setSshSessionFactory( sshSessionFactory );
            });

            pullCmd.call();

        } catch (GitAPIException | IOException ex) {
            ProcessLog.add("Git Pull Error for: " + git.getUrl(), true, ex);
        }

        return true;
    }


}
