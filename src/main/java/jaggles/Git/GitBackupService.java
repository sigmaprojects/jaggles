package jaggles.Git;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import jaggles.Jaggles;
import jaggles.items.Git;
import jaggles.util.ProcessLog;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.util.FS;

import java.io.File;

public class GitBackupService {

    //private File backupPath;
    private String privateKeyPath = "./git.key";

    public GitBackupService() {

        File dir = Jaggles.getJarDir(Jaggles.class);
        if( new File(privateKeyPath).exists() ) {
            ProcessLog.add("Gits Backup: Found `git.key` in working directory, using it.", true);
        } else if( new File(dir.getAbsolutePath() + "/git.key").exists() ) {
            ProcessLog.add("Gits Backup: Found `git.key` in jar directory, using it.", true);
            this.privateKeyPath = new File(dir.getAbsolutePath() + "/git.key").getAbsolutePath();
        }
    }

    public GitBackup createWorker(Git git) {
        return new GitBackup(git, getSshSessionFactory());
    }

    private SshSessionFactory getSshSessionFactory() {
        return new JschConfigSessionFactory() {
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
    }

}
