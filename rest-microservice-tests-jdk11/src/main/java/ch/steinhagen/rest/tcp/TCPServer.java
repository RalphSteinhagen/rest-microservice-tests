package ch.steinhagen.rest.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author rstein
 */
public class TCPServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(TCPServer.class);
	private final AtomicBoolean runServer = new AtomicBoolean(false);
	private Thread serverThread;
	private Selector selector;

	private InetSocketAddress listenAddress;
	private ServiceHandler handler;
	private List<SocketChannel> clientList = new ArrayList<>();

	public TCPServer(final String host, final int port) {
		listenAddress = new InetSocketAddress(host, port);
	}

	public void setServiceHandler(ServiceHandler handler) {
		this.handler = handler;
	}

	public ServiceHandler getServiceHandler() {
		return this.handler;
	}

	public boolean isRunning() {
		synchronized (runServer) {
			return runServer.get();
		}
	}

	public List<SocketChannel> getClientList() {
		return clientList;
	}

	public void start() {
		final String serverName = TCPServer.class.getSimpleName() + "@" + listenAddress.getHostString();
		Runnable runnable = () -> {
			synchronized (runServer) {
				runServer.set(true);
			}
			try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
				this.selector = Selector.open();
				serverChannel.configureBlocking(false);

				// retrieve server socket and bind to port
				serverChannel.socket().bind(listenAddress);
				serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("started {}", serverName);
				}

				runInnerLoop();

			} catch (IOException e) {
				if (LOGGER.isErrorEnabled()) {
					LOGGER.atError().setCause(e).addArgument(serverName).log("error during start for {}");
				}
			}
		};
		serverThread = new Thread(runnable, serverName);
		serverThread.start();
	}

	public void stop() {
		synchronized (runServer) {
			runServer.set(false);
		}
		try {
			if (selector != null) {
				selector.close();
				selector = null;
			}
		} catch (IOException e) {
			if (LOGGER.isErrorEnabled()) {
				final String serverName = TCPServer.class.getSimpleName() + "@"
						+ (listenAddress == null ? "unknwon" : listenAddress.getHostString());
				LOGGER.atError().setCause(e).addArgument(serverName).log("error during stop of {}");
			}
		}
		serverThread.interrupt();
		serverThread = null;
	}

	public static final byte[] toByteArray(final int value) {
		return ByteBuffer.allocate(4).putInt(value).array(); 
	}

	public static final ByteBuffer toByteBuffer(final int value) {
		return ByteBuffer.wrap(toByteArray(value));
	}

	public static final int fromByteArray(byte[] bytes) {
		return ByteBuffer.wrap(bytes).getInt();
	}
	
	public static final int fromByteBuffer(ByteBuffer buffer) {
		return buffer.getInt();
	}

	private void closeChannelForKey(SelectionKey key) {
		try (SocketChannel channel = (SocketChannel) key.channel()) {
			synchronized (clientList) {
				clientList.remove(channel);
			}
			key.cancel();
			if (LOGGER.isDebugEnabled()) {
				SocketAddress remoteAddr = channel.socket().getRemoteSocketAddress();
				LOGGER.debug("Connection closed by client: {}", remoteAddr);
			}
		} catch (IOException e) {
			LOGGER.error("could not close channel for key: " + key, e);
		}

	}

	// accept a connection made to this channel's socket
	@SuppressWarnings("resource")
	private void accept(SelectionKey key) throws IOException {
		ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
		SocketChannel channel = serverChannel.accept();
		if (channel == null) {
			return;
		}
		channel.configureBlocking(false);

		SocketAddress remoteAddr = channel.socket().getRemoteSocketAddress();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.atDebug().addArgument(remoteAddr).log("connected to: {}");
		}

		// register channel with selector for further IO
		synchronized (clientList) {
			clientList.add(channel);
		}
		channel.register(this.selector, SelectionKey.OP_READ);
	}

	private void runInnerLoop() throws IOException {
		while (isRunning()) {
			// wait for events
			if (selector == null) {
				return;
			}
			this.selector.select(); // N.B. blocking call

			// work on selected keys
			List<SelectionKey> selectedKeys;
			try {
				selectedKeys = new ArrayList<>(selector.selectedKeys());
			} catch (ClosedSelectorException e) {
				synchronized (runServer) {
					runServer.set(false);
				}
				selectedKeys = new ArrayList<>();
			}
			for (SelectionKey key : selectedKeys) {
				if (!key.isValid()) {
					continue;
				}

				if (key.isAcceptable()) {
					this.accept(key);
				} else if (key.isReadable() && key.isValid()) {
					@SuppressWarnings("resource") // channel is not created here
					SocketChannel channel = (SocketChannel) key.channel();
					this.processRequest(key, channel);
				}
			}
		}
		if (LOGGER.isDebugEnabled()) {
			final String serverName = TCPServer.class.getSimpleName() + "@" + listenAddress.getHostString();
			LOGGER.atDebug().addArgument(serverName).log("stop server: {}");
		}
	}

	// read from the socket channel
	private void processRequest(final SelectionKey key, final SocketChannel channel) {
		if (handler == null) {
			return;
		}
		try {
			handler.handle(key, channel);

			if (!key.channel().isOpen()) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("channel closed in handler channel");
				}
				closeChannelForKey(key);
			}
		} catch (IOException e) {
			closeChannelForKey(key);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("close channel because: {}", e.getLocalizedMessage());
			}
		}
	}

	// read from the socket channel

	public interface ServiceHandler {

		void handle(final SelectionKey key, final SocketChannel channel) throws IOException;

	}

	public static void main(String[] argv) {
		final TCPServer server = new TCPServer("localhost", 8090);

		server.setServiceHandler((key, channel) -> {
			ByteBuffer buffer = ByteBuffer.allocate(1024);
			int numRead = -1;
			numRead = channel.read(buffer);
			if (numRead == 0) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.atDebug().log("no data received, server running = " + server.isRunning());
				}
				return;
			} else if (numRead == -1) {
				SocketAddress remoteAddr = channel.socket().getRemoteSocketAddress();
				if (LOGGER.isDebugEnabled()) {
					LOGGER.atDebug().addArgument(remoteAddr).log("connection closed by client: {}");
				}
				channel.close();
				return;
			}

			byte[] data = new byte[numRead];
			System.arraycopy(buffer.array(), 0, data, 0, numRead);
			if (LOGGER.isInfoEnabled()) {
				LOGGER.atInfo().addArgument(numRead).addArgument(new String(data))
						.log("server received {}-bytes: '{}'");
			}
			channel.write(buffer);
		});

		server.start();

		Runnable clientTask = () -> {
			try {
				Thread.sleep(1000);
				new SocketClientExample().startClient();
			} catch (IOException e) {
				LOGGER.atError().setCause(e).log("Client IOException");
			} catch (InterruptedException e) {
				LOGGER.atError().log("Client was interrupted");
				// do nothing
			}
		};
		new Thread(clientTask, "client-A").start();
		new Thread(clientTask, "client-B").start();

		// TODO: work on how to properly close server
		// server.stop();
	}
}
