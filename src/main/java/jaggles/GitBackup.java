package jaggles;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
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

    public GitBackup(jaggles.items.Git git) {
        this.git = git;

        String fullBackupPath = Jaggles.backupFilePath.getAbsolutePath() + "/gits/" + git.getRepoName();
        backupPath = new File( fullBackupPath );
        backupPath.mkdirs(); // create the directory if it doesn't exist

        File dir = Jaggles.getJarDir(Jaggles.class);

        if( new File(privateKeyPath).exists() ) {
            ProcessLog.add("Gits Backup: Found `git.key` in working directory, using it.", true);
        } else if( new File(dir.getAbsolutePath() + "/git.key").exists() ) {
            ProcessLog.add("Gits Backup: Found `git.key` in jar directory, using it.", true);
            this.privateKeyPath = new File(dir.getAbsolutePath() + "/git.key").getAbsolutePath();
        }

    }

    public void backup() {
        TimeKeeper tk = new TimeKeeper();

        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI( git.getUrl() )
                .setDirectory(backupPath);

        try {

            cloneCommand.call();

        } catch (TransportException e) {
            //System.out.println( g.getRepoName() + " needs authentication");

            cloneCommand.setTransportConfigCallback(transport -> {
                SshTransport sshTransport = ( SshTransport )transport;
                sshTransport.setSshSessionFactory( getSshSessionFactory() );
            });
            try {
                cloneCommand.call();
                e.printStackTrace();
            } catch(TransportException te ) {
                ProcessLog.add( "Git Error: " + git.getUrl() + " transport error: " + te.getMessage(), true );
            } catch (Exception ce) {
                ProcessLog.add( "Git Error: " + git.getUrl() + " still failed authentication: ", true, ce);
                //ce.printStackTrace();
            }

        } catch (JGitInternalException e) {
            if (e.getMessage().contains("already exists")) {
                //System.out.println( g.getRepoName() + " already exists error");
                pull();
            } else {
                //e.printStackTrace();
                ProcessLog.add("Git Error: Unable to backup git: " + this.git.getUrl(), true, e);
            }
        } catch(InvalidRemoteException ire) {
            ProcessLog.add( "Git Error: Invalid Remote: " + git.getUrl() + " - " + ire.getMessage(), true);
        } catch(Exception e) {
            ProcessLog.add("Git Error: Uncaught: " + this.git.getUrl(), true, e);
            //e.printStackTrace();
        }
        ProcessLog.add( "Git Backup For Repo: " + git.getUrl() + " Has Finished.  Took " + tk.getAgeInSeconds() + " seconds", true);
    }

    private boolean pull() {
        try {
            Repository localRepo = new FileRepository(backupPath.getAbsolutePath() + "/.git");
            Git git = new Git(localRepo);

            PullCommand pullCmd = git.pull();

            pullCmd.setTransportConfigCallback( new TransportConfigCallback() {
                @Override
                public void configure( Transport transport ) {
                    SshTransport sshTransport = ( SshTransport )transport;
                    sshTransport.setSshSessionFactory( getSshSessionFactory() );
                }
            } );

            pullCmd.call();

        } catch (GitAPIException | IOException ex) {
            ProcessLog.add("Git Pull Error for: " + git.getUrl(), true, ex);
            //Logger.getLogger(Jaggles.class.getName()).log(Level.SEVERE, null, ex);
        }

        return true;
    }

    private SshSessionFactory getSshSessionFactory() {
        SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
            @Override
            protected void configure(OpenSshConfig.Host host, Session session ) {
                session.setConfig("StrictHostKeyChecking", "no");
            }
            @Override
            protected JSch createDefaultJSch(FS fs ) throws JSchException {
                JSch defaultJSch = super.createDefaultJSch( fs );
                defaultJSch.addIdentity( privateKeyPath );
                return defaultJSch;
            }

        };
        return sshSessionFactory;
    }

}
