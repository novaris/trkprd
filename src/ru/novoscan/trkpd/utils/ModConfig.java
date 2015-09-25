package ru.novoscan.trkpd.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class ModConfig {
	static Logger logger = Logger.getLogger(ModConfig.class);

	private String configName = "trkprd.conf";

	private String modName;

	private static String serverType;

	private static int serverPort;

	private static String serverHost;

	private static int clientTimeout;

	private static int maxConn;

	private static int modType; // NULL - any GELIX, 100 - Global Sat

	//
	private static String loginUser;

	private static String loginPasswd;

	// Подключение postgres
	private static String pgDatabaseName;

	private static String pgHost;

	private static String pgUrl;

	private static String pgUser;

	private static String pgPasswd;

	private static int pgPort;

	private static int maxPacketSize;

	private static byte begChar;

	private static byte endChar;

	private static int readInterval;

	public ModConfig() {
	}

	public void init() {
		final Properties configFile = new Properties();
		try {
			configFile.loadFromXML(new FileInputStream(configName));
			this.modName = configFile.getProperty("ModuleName");
			serverPort = Integer.parseInt(configFile.getProperty("Port"));
			serverHost = configFile.getProperty("Host");
			clientTimeout = Integer.parseInt(configFile
					.getProperty("ClientTimeout"));
			maxConn = Integer.parseInt(configFile
					.getProperty("ClientConnection"));
			modType = Integer.parseInt(configFile.getProperty("TypeID"));
			// Подключение postgres
			pgDatabaseName = configFile.getProperty("DatabaseName");
			pgHost = configFile.getProperty("DatabaseHost");
			pgUrl = "jdbc:postgresql://" + pgHost + "/" + pgDatabaseName;
			pgUser = configFile.getProperty("DatabaseUser");
			pgPasswd = configFile.getProperty("DatabasePasswd");
			pgPort = Integer.parseInt(configFile.getProperty("DatabasePort"));
			maxPacketSize = Integer.parseInt(configFile
					.getProperty("MaxPacketSize"));
			begChar = Byte.parseByte(configFile.getProperty("BegChar"));
			endChar = Byte.parseByte(configFile.getProperty("EndChar"));
			readInterval = Integer.parseInt(configFile
					.getProperty("ReadInterval"));
			if ((readInterval < 1000) || (readInterval > 10000000)) {
				readInterval = 1000;
			}
			serverType = configFile.getProperty("ServerType");

		} catch (InvalidPropertiesFormatException e) {
			logger.fatal("Неверный XML в файле : " + configName + "\n"
					+ e.getMessage());
			System.exit(1);
		} catch (FileNotFoundException e) {
			logger.fatal("Файл конфигурации не найден : " + configName);
			System.exit(1);
		} catch (IOException e) {
			logger.fatal("Ошибка чтения файла :  " + configName + "\n"
					+ e.getMessage());
			System.exit(1);
		}
	}

	public String getPgHost() {
		return pgHost;
	}

	public int getPort() {
		return serverPort;
	}

	public InetAddress getHost() throws UnknownHostException {
		InetAddress inetAddr = InetAddress.getByName(serverHost);
		return inetAddr;
	}

	public int getTimeout() {
		return clientTimeout;
	}

	public int getMaxConn() {
		return maxConn;
	}

	public int getModType() {
		return modType;
	}

	public String getModName() {
		return modName;
	}

	public String getPgUrl() {
		return pgUrl;
	}

	public String getPgUser() {
		return pgUser;
	}

	public String getPgPasswd() {
		return pgPasswd;
	}

	public String getPgDatabaseName() {
		return pgDatabaseName;
	}

	public int getPgPort() {
		return pgPort;
	}

	public int getMaxSize() {
		return maxPacketSize;
	}

	public byte getBegChar() {
		return begChar;
	}

	public byte getEndChar() {
		return endChar;
	}

	public String getPasswd() {
		return loginPasswd;
	}

	public String getUser() {
		return loginUser;
	}

	public void setConfigFile(String filename) {
		this.configName = filename;
	}

	public String getConfigFile() {
		return configName;
	}

	public int getReadInterval() {
		return readInterval;
	}

	public String getServerType() {
		return serverType;
	}

}
