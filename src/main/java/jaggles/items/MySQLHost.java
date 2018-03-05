/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jaggles.items;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author don
 */

public class MySQLHost extends Item implements Serializable {

    private static final long serialVersionUID = 1L;
    private Integer id;
    private String host;
    private String user;
    private String pass;
    private String backupPath;
    private List<String> databases;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getPass() { return pass; }
    public void setPass(String pass) { this.pass = pass; }

    public String getBackupPath() { return backupPath; }
    public void setBackupPath(String backupPath) { this.backupPath = backupPath; }

    public List<String> getDatabases() { return databases; }
    public void setDatabases(List<String> databases) { this.databases= databases; }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof MySQLHost)) {
            return false;
        }
        MySQLHost other = (MySQLHost) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "jaggles.items.MySQLHost[ id=" + id + " ]";
    }

}
