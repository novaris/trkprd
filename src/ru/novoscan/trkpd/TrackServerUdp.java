package ru.novoscan.trkpd;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.sql.SQLException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import ru.novoscan.trkpd.resources.ModConstats;
import ru.novoscan.trkpd.terminals.ModTeltonikaFm;
import ru.novoscan.trkpd.terminals.ModTranskomT15;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.TrackPgUtils;

public class TrackServerUdp {
	static private String Version = "1.2";

	static Logger logger = Logger.getLogger(TrackServerUdp.class);

	static ModConfig conf;

	static DatagramSocket listenSocket;

	public static void main(String arg[]) throws IOException,
			ClassNotFoundException {
		BasicConfigurator.configure();
		while (true) {
			try {
				logger.info("Starting UDP Track Server. Version \"" + Version
						+ "\"");
				conf = new ModConfig();
				parseArgs(arg);
				conf.CNFinit();
				logger.debug("Listen server on host : " + conf.getHost() + ":"
						+ conf.getPort());
				listenSocket = new DatagramSocket(conf.getPort(),
						conf.getHost());
				logger.debug("Connection Postgres : " + conf.getPgHost() + ":"
						+ conf.getPgPort() + " as " + conf.getPgUser());
				TrackPgUtils pgconn = new TrackPgUtils(conf);
				while (true) {
					new UDPReader(listenSocket, conf, pgconn);
				}
			} catch (SQLException e) {
				if (listenSocket.isBound()) {
					listenSocket.close();
				}
				logger.warn("Error SQL Connection : " + e.getMessage());
			}
			try {
				logger.debug("Sleep : " + conf.getReadInterval());
				Thread.sleep(conf.getReadInterval());
			} catch (InterruptedException e) {
				logger.warn("Interrupted Exception : " + e.getMessage());
			}
		}
	}

	private static void parseArgs(String[] argcmd) throws IOException {
		int i = 0;
		String arg;
		boolean vflag = false;
		boolean printUsage = false;

		while (i < argcmd.length && argcmd[i].startsWith("-")) {
			arg = argcmd[i++];

			// debug to log file
			if (arg.equals("-d")) {
				vflag = true;
				logger.info("Debug enable. ");
			} else if (arg.equals("-d")) {
				daemonize();
			}
			// use this type of check for arguments that require arguments
			else if (arg.equals("-f")) {
				if (i < argcmd.length)
					conf.setConfigFile(argcmd[i++]);
				else
					logger.warn("-f requires a filename");
				if (vflag)
					logger.info("Config file: " + argcmd[i++]);
			} else {
				logger.warn("Incorrect parmeter: " + argcmd[i++]);
				printUsage = true;
			}
		}
		if (printUsage)
			logger.warn("Usage: [-vd] [-f configfile]");
		else
			logger.info("Success parse parameters.");
	}

	private static void daemonize() throws IOException {
		System.out.close();
		System.err.close();
	}

}

class UDPReader extends Thread implements ModConstats {

	DatagramSocket clientSocket;

	DatagramPacket dataPacket;

	ModConfig conf;

	TrackPgUtils pgcon;

	static Logger logger = Logger.getLogger(Connection.class);

	public UDPReader(DatagramSocket aClientSocket, ModConfig modConf,
			TrackPgUtils pg) {
		try {
			clientSocket = aClientSocket;
			dataPacket = new DatagramPacket(new byte[1024], 1024);
			clientSocket.receive(dataPacket);
			conf = modConf;
			pgcon = pg;
			this.start();
		} catch (IOException e) {
			logger.warn("Error IO : " + e.getMessage());
		}
	}

	public void run() {
		try {
			int modType = conf.getModType();
			logger.debug("Module type is : " + conf.getModType());
			if (modType == TERM_TYPE_TRANSKOM_T15) {
				ModTranskomT15 mod = new ModTranskomT15(dataPacket,
						clientSocket, conf, pgcon);
				logger.info("Read bytes : " + mod.getReadBytes());
			} else if (modType == TERM_TYPE_TELTONIKA_FM) {
				ModTeltonikaFm mod = new ModTeltonikaFm(dataPacket,
						clientSocket, conf, pgcon);
				logger.info("Read bytes : " + mod.getReadBytes());
			} else { // Вызвать исключение
				logger.warn("Incorrect Module Type : " + modType);
			}
		} finally {
			logger.debug("Close connection");
		}

	}

}
