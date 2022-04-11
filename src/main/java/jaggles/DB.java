/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jaggles;

import jaggles.items.Directory;
import jaggles.items.Git;
import jaggles.items.MySQLHost;
import jaggles.items.SMB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author don
 */
public class DB {

    private String userName = "jaggles";
    private String password = "jaggleslocalhost";
    final private String database = "snaggles";
    private String server = "192.168.1.75";
    final private int portNumber = 3306;
    final private String dbms = "mysql";
    final private Boolean useSSL = true;

    DB() {
        /*
        try {
            Connection conn = getConnection();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        */
    }

    DB(String server, String username, String password) {
        this.server = server;
        this.userName = username;
        this.password = password;
    }


    public List<MySQLHost> getMySQLHosts() {
        List<MySQLHost> mysqls = new ArrayList<MySQLHost>();

        Statement stmt = null;
        String query = "SELECT id, host, user, pass, backupdir FROM mysqlhosts";
        try {
            Connection con = getConnection();
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                MySQLHost mysql = new MySQLHost();
                mysql.setId( rs.getInt("id") );
                mysql.setHost( rs.getString("host") );
                mysql.setUser( rs.getString("user") );
                mysql.setPass( rs.getString("pass") );
                mysql.setBackupPath( rs.getString("backupdir") );

                // gather all the databases for this mysql host
                DB db = new DB(mysql.getHost(), mysql.getUser(), mysql.getPass());
                mysql.setDatabases( db.getSchemas() );
                mysqls.add(mysql);

                System.out.println("mysql host loaded: " + mysql.toString() );
            }
        } catch (SQLException e ) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) { stmt.close(); }
            } catch(SQLException ex) {
                ex.printStackTrace();
            }
        }

        return mysqls;
    }


    public List<Directory> getDirectories() {
        List<Directory> dirs = new ArrayList<Directory>();

        Statement stmt = null;
        String query = "SELECT directoryid, directoryPath, backupPath FROM directories";
        try {
            Connection con = getConnection();
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                Directory dir = new Directory();
                dir.setId( rs.getInt("directoryid") );
                dir.setDirectoryPath( rs.getString("directoryPath") );
                dir.setBackupPath( rs.getString("backupPath") );
                dirs.add(dir);

                System.out.println("dir loaded: " + dir.toString() );
            }
        } catch (SQLException e ) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) { stmt.close(); }
            } catch(SQLException ex) {
                ex.printStackTrace();
            }
        }

        return dirs;
    }


    public List<SMB> getSMBs() {
        List<SMB> smbs = new ArrayList<SMB>();

        Statement stmt = null;
        String query = "SELECT cifsid, server, username, password, domain, sharename, path FROM smbs";
        try {
            Connection con = getConnection();
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                SMB smb = new SMB();
                smb.setId( rs.getInt("cifsid") );
                smb.setServer( rs.getString("server") );
                smb.setUsername( rs.getString("username") );
                smb.setPassword( rs.getString("password") );
                smb.setDomain( rs.getString("domain") );
                smb.setShareName( rs.getString("sharename") );
                smb.setPath( rs.getString("path") );
                smbs.add(smb);
                System.out.println("smb loaded: " + smbs.toString() );
            }
        } catch (SQLException e ) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) { stmt.close(); }
            } catch(SQLException ex) {
                ex.printStackTrace();
            }
        }

        return smbs;
    }


    public List<Git> getGits() {
        List<Git> gits = new ArrayList<Git>();

        Statement stmt = null;
        String query = "SELECT id, url, repoName, backupPath FROM gits";
        try {
            Connection con = getConnection();
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                Git git = new Git();
                git.setId( rs.getInt("id") );
                git.setUrl( rs.getString("url") );
                git.setRepoName( rs.getString("repoName") );
                git.setBackupPath( rs.getString("backupPath") );
                gits.add(git);

                System.out.println("git loaded: " + git.toString() );
            }
        } catch (SQLException e ) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) { stmt.close(); }
            } catch(SQLException ex) {
                ex.printStackTrace();
            }
        }

        return gits;
    }


    public List<String> getSchemas() {
        List<String> schemas = new ArrayList<String>();
        Statement stmt = null;
        String query = "SHOW DATABASES";
        try {
            Connection con = getConnection();
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                String schema = rs.getString("Database");
                schemas.add(schema);

                System.out.println("Database "+this.server+" found schema: " + schema );
            }
        } catch (SQLException e ) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) { stmt.close(); }
            } catch(SQLException ex) {
                ex.printStackTrace();
            }
        }
        schemas.remove("information_schema"); // ignore
        schemas.remove("sys"); // ignore
        schemas.remove("performance_schema"); // ignore
        return schemas;
    }


    private Connection getConnection() throws SQLException {
        Connection conn = null;
        Properties connectionProps = new Properties();
        connectionProps.put("user", this.userName);
        connectionProps.put("password", this.password);
        conn = DriverManager.getConnection(
                "jdbc:" + this.dbms + "://" +
                        this.server +
                        ":" + this.portNumber +
                        "/" + this.database +
                        "?useSSL=" + this.useSSL +
                        "&useLegacyDatetimeCode=false&serverTimezone=UTC&enabledTLSProtocols=TLSv1.2",
                        // useLegacyDatetimeCode bug with mysql 6.0.6 https://bugs.mysql.com/bug.php?id=81214
                        // enabledTLSProtocols https://stackoverflow.com/questions/67332909/why-can-java-not-connect-to-mysql-5-7-after-the-latest-jdk-update-and-how-should
                connectionProps
        );

        System.out.println("Connected to database");
        return conn;
    }
}
