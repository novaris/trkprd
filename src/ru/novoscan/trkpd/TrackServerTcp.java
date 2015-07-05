package ru.novoscan.trkpd;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.text.ParseException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import ru.novoscan.trkpd.resources.ModConstats;
import ru.novoscan.trkpd.terminals.ModAny;
import ru.novoscan.trkpd.terminals.ModAzimut;
import ru.novoscan.trkpd.terminals.ModEgts;
import ru.novoscan.trkpd.terminals.ModGelix;
// import ru.novoscan.trkpd.terminals.MODGnsMinitrack; // Устарело
import ru.novoscan.trkpd.terminals.ModGalileoSky;
import ru.novoscan.trkpd.terminals.ModGs;
import ru.novoscan.trkpd.terminals.ModNavis;
import ru.novoscan.trkpd.terminals.ModNavixyM7;
import ru.novoscan.trkpd.terminals.ModNovacom;
import ru.novoscan.trkpd.terminals.ModScout;
import ru.novoscan.trkpd.terminals.ModScoutOpen;
import ru.novoscan.trkpd.terminals.ModSignal;
import ru.novoscan.trkpd.terminals.ModSt270;
import ru.novoscan.trkpd.terminals.ModUtp5;
import ru.novoscan.trkpd.terminals.ModXml;
import ru.novoscan.trkpd.terminals.ModMajak;
import ru.novoscan.trkpd.terminals.ModMarker;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.TrackPgUtils;

public class TrackServerTcp {
	static private String Version = "1.2";

	static Logger logger = Logger.getLogger(TrackServerTcp.class);

	static ModConfig conf;

	static ServerSocket listenSocket;

	public static void main(String arg[]) throws IOException,
			ClassNotFoundException {
		BasicConfigurator.configure();
		while (true) {
			try {
				logger.info("Starting TCP Track Server. Version \"" + Version
						+ "\"");
				conf = new ModConfig();
				parseArgs(arg);
				conf.CNFinit();
				logger.debug("Listen server on host : " + conf.getHost() + ":"
						+ conf.getPort());
				listenSocket = new ServerSocket(conf.getPort(),
						conf.getMaxConn(), conf.getHost());
				// modName = conf.getModName();
				// modType = conf.getModType();
				logger.debug("Connection Postgres : " + conf.getPgHost() + ":"
						+ conf.getPgPort() + " as " + conf.getPgUser());
				TrackPgUtils pgconn = new TrackPgUtils(conf);
				while (true) {
					Socket clientSocket = listenSocket.accept();
					clientSocket.setSoTimeout(conf.getTimeout());
					@SuppressWarnings("unused")
					Connection c = new Connection(clientSocket, conf, pgconn);
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

class Connection extends Thread implements ModConstats {

	DataInputStream inp;

	DataOutputStream out;

	BufferedReader console;

	InputStreamReader unbconsole;

	Socket clientSocket;

	ModConfig conf;

	TrackPgUtils pgcon;

	static Logger logger = Logger.getLogger(Connection.class);

	public Connection(Socket aClientSocket, ModConfig modConf, TrackPgUtils pg) {
		try {
			clientSocket = aClientSocket;
			inp = new DataInputStream(clientSocket.getInputStream());
			out = new DataOutputStream(clientSocket.getOutputStream());
			console = new BufferedReader(new InputStreamReader(inp));
			unbconsole = new InputStreamReader(inp);
			conf = modConf;
			pgcon = pg;

			this.start();
		} catch (IOException e) {
			logger.warn("Error IO : " + e.getMessage());
		}
	}

	public void run() {
		try {
			logger.info("Connecting Client : "
					+ clientSocket.getRemoteSocketAddress().toString());
			int modType = conf.getModType();
			logger.debug("Module type is : " + conf.getModType());
			if (modType == TERM_TYPE_ANY) {
				ModAny mod = new ModAny(inp, out, unbconsole, conf, pgcon);
				logger.info("Read bytes : " + mod.getReadBytes());
			} else if (modType == TERM_TYPE_GELIX) {
				ModGelix mod = new ModGelix(inp, out, console, conf, pgcon);
				logger.info("Read bytes : " + mod.getReadBytes());
			} else if (modType == TERM_TYPE_GS) {
				ModGs mod = new ModGs(inp, out, unbconsole, conf, pgcon);
				logger.info("Read bytes : " + mod.getReadBytes());
			} else if (modType == TERM_TYPE_UTP5) {
				ModUtp5 mod = new ModUtp5(inp, out, unbconsole, conf, pgcon);
				logger.info("Read bytes : " + mod.getReadBytes());
			} else if (modType == TERM_TYPE_NOVACOM) {
				ModNovacom mod = new ModNovacom(inp, out, unbconsole, conf,
						pgcon);
				logger.info("Read bytes : " + mod.getReadBytes());
			} else if (modType == TERM_TYPE_ST270) {
				ModSt270 mod = new ModSt270(inp, out, unbconsole, conf, pgcon);
				logger.info("Read bytes : " + mod.getReadBytes());
			} else if (modType == TERM_TYPE_SIGNAL_S21) {
				ModSignal mod = new ModSignal(inp, out, unbconsole, conf, pgcon);
				logger.info("Read bytes : " + mod.getReadBytes());
			} else if (modType == TERM_TYPE_MARKER) {
				ModMarker mod = new ModMarker(inp, out, unbconsole, conf, pgcon);
				logger.info("Read bytes : " + mod.getReadBytes());
			} else if (modType == TERM_TYPE_NAVIXY_M7) {
				ModNavixyM7 mod = new ModNavixyM7(inp, out, unbconsole, conf,
						pgcon);
				logger.info("Read bytes : " + mod.getReadBytes());
			} else if (modType == TERM_TYPE_GALILEO_SKY) {
				ModGalileoSky mod = new ModGalileoSky(inp, out, unbconsole,
						conf, pgcon);
				logger.info("Read bytes : " + mod.getReadBytes());
			} else if (modType == TERM_TYPE_NAVIS_UM4) {
				ModNavis mod = new ModNavis(inp, out, unbconsole, conf, pgcon);
				logger.info("Read bytes : " + mod.getReadBytes());
			} else if (modType == TERM_TYPE_SCOUT) {
				ModScout mod = new ModScout(inp, out, unbconsole, conf, pgcon);
				logger.info("Read bytes : " + mod.getReadBytes());
			} else if (modType == TERM_TYPE_XML) {
				ModXml mod = new ModXml(inp, out, unbconsole, conf, pgcon);
				logger.info("Read bytes : " + mod.getReadBytes());
			} else if (modType == TERM_TYPE_SCOUT_OPEN) {
				ModScoutOpen mod = new ModScoutOpen(inp, out, unbconsole, conf,
						pgcon);
				logger.info("Read bytes : " + mod.getReadBytes());
			} else if (modType == TERM_TYPE_MAJAK) {
				ModMajak mod = new ModMajak(inp, out, unbconsole, conf, pgcon);
				logger.info("Read bytes : " + mod.getReadBytes());
			} else if (modType == TERM_TYPE_AZIMUT) {
				ModAzimut mod = new ModAzimut(inp, out, unbconsole, conf, pgcon);
				logger.info("Read bytes : " + mod.getReadBytes());
			} else if (modType == TERM_TYPE_EGTS) {
				ModEgts mod = new ModEgts(inp, out, unbconsole, conf, pgcon);
				logger.info("Read bytes : " + mod.getReadBytes());
			} else {
				// Вызвать исключение
				logger.warn("Incorrect Module Type : " + modType);
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				inp.close();
				out.close();
				console.close();
				unbconsole.close();
				clientSocket.close();
				logger.debug("Close connection");
			} catch (IOException e) {
				logger.warn("Close connection error : " + e.getMessage());
			}
		}

	}

}