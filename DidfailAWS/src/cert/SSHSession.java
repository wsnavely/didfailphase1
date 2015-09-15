package cert;

import java.io.IOException;
import java.io.InputStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SSHSession {
	public enum AuthMethod {
		Password, Key
	}

	private String user;
	private String host;
	private String password;
	private int port = 22;
	private String keyFile;
	private AuthMethod authMethod = AuthMethod.Password;
	private JSch jsch;
	private Session session;

	public SSHSession() {
		jsch = new JSch();
	}

	public void setUser(String user) {
		this.user = user;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setAuthMethod(AuthMethod authMethod) {
		this.authMethod = authMethod;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setKeyFile(String keyFile) {
		this.keyFile = keyFile;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void connect() throws Exception {
		if (this.session != null) {
			return;
		}
		this.session = this.jsch.getSession(this.user, this.host, this.port);
		switch (this.authMethod) {
		case Key:
			jsch.addIdentity(this.keyFile);
			break;
		case Password:
			session.setPassword(this.password);
			break;
		}
		this.session.setConfig("StrictHostKeyChecking", "no");
		this.session.connect();
	}

	public String sendCommand(String command) {
		StringBuilder outputBuffer = new StringBuilder();

		try {
			Channel channel = this.session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);
			InputStream commandOutput = channel.getInputStream();
			channel.connect();
			int readByte = commandOutput.read();
			while (readByte != 0xffffffff) {
				outputBuffer.append((char) readByte);
				readByte = commandOutput.read();
			}

			channel.disconnect();
		} catch (IOException ioX) {
			return null;
		} catch (JSchException jschX) {
			return null;
		}

		return outputBuffer.toString();
	}

	public void close() {
		this.session.disconnect();
	}
}
