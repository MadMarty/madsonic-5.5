
package github.madmarty.madsonic.domain;

import java.util.ArrayList;
import java.util.List;

public class SrvSettings {

    private final List<ServerEntry> serverEntries = new ArrayList<ServerEntry>();

    public SrvSettings () {}
    
	public List<ServerEntry> getChildren() {
		return serverEntries;
	}
    
    public void addChild(ServerEntry child) {
        serverEntries.add(child);
    }
    
	public static class ServerEntry  {

		private String servername;
		private String serverURL;
		private String username;
		private String password;
		
		public String getServername() {
			return servername;
		}
		public void setServername(String servername) {
			this.servername = servername;
		}
		public String getServerURL() {
			return serverURL;
		}
		public void setServerURL(String serverURL) {
			this.serverURL = serverURL;
		}
		public String getUsername() {
			return username;
		}
		public void setUsername(String username) {
			this.username = username;
		}
		public String getPassword() {
			return password;
		}
		public void setPassword(String password) {
			this.password = password;
		}
	}

	
}
