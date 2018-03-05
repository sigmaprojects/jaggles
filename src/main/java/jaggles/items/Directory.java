/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jaggles.items;

import java.io.Serializable;

/**
 *
 * @author don
 */
public class Directory extends Item implements Serializable {

    private static final long serialVersionUID = 1L;
    private Integer id;
    private String directoryPath;
    private String backupPath;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getDirectoryPath() { return directoryPath; }
    public void setDirectoryPath(String directoryPath) { this.directoryPath = directoryPath; }

    public String getBackupPath() { return backupPath; }
    public void setBackupPath(String backupPath) { this.backupPath = backupPath; }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Directory)) {
            return false;
        }
        Directory other = (Directory) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "jaggles.items.Directory[ id=" + id + " ]";
    }

}
