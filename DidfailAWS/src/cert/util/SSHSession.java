package cert.util;

import java.io.InputStream;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class SSHSession {
	public class Output {
		String stdout;
		String stderr;

		public Output(String out, String err) {
			this.stdout = out;
			this.stderr = err;
		}

		@Override
		public String toString() {
			StringBuffer result = new StringBuffer();
			result.append("[STDOUT]\n");
			result.append(this.stdout);
			result.append("\n");
			result.append("[STDERR]\n");
			result.append(this.stderr);
			result.append("\n");
			return result.toString();
		}
	}

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

	public Output sendCommand(String command) {
		StringBuilder outputBuffer = new StringBuilder();
		StringBuilder errBuffer = new StringBuilder();
		try {
			ChannelExec channel = (ChannelExec) this.session.openChannel("exec");
			channel.setCommand(command);
			InputStream commandOutput = channel.getInputStream();
			InputStream commandError = channel.getErrStream();
			channel.connect();

			int readByte = commandOutput.read();
			while (readByte != 0xffffffff) {
				outputBuffer.append((char) readByte);
				readByte = commandOutput.read();
			}

			readByte = commandError.read();
			while (readByte != 0xffffffff) {
				errBuffer.append((char) readByte);
				readByte = commandError.read();
			}
			channel.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new Output(outputBuffer.toString(), errBuffer.toString());
	}

	public void close() {
		this.session.disconnect();
	}

	public void testLiveness() {
		String output;
		do {
			// TODO make this more robust to failure, retry count perhaps
			output = this.sendCommand("echo ping").stdout;
		} while (output == null || !output.trim().equals("ping"));
	}
}
