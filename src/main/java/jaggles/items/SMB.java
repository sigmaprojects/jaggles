package jaggles.items;

import java.io.Serializable;

/**
 *
 * @author don
 */
public class SMB extends Item implements Serializable {

    private static final long serialVersionUID = 1L;
    private Integer id;
    private String server;
    private String username;
    private String password;
    private String domain;
    private String shareName;
    private String path;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getServer() { return server; }
    public void setServer(String server) { this.server = server; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getShareName() { return shareName; }
    public void setShareName(String shareName) { this.shareName = shareName; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }


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
        SMB other = (SMB) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "jaggles.items.SMB[ id=" + id + " ]";
    }

}
