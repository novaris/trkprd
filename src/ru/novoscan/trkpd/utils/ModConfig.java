package ru.novoscan.trkpd.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.resources.ModConstats.SERVER;

public class ModConfig {
	static Logger logger = Logger.getLogger(ModConfig.class);

	private String configName;

	private String modName;

	private String snmpCommunity;

	private int snmpPort;

	private static int pgMaxConn;

	private static int pgInitConn;

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

	private Byte defaultValue = 0;

	public ModConfig() {
		configName = "trkprd.conf";
	}

	public void init() {
		final Properties configFile = new Properties();
		try {
			configFile.loadFromXML(new FileInputStream(configName));
			this.modName = configFile.getProperty("ModuleName");
			serverPort = Integer.parseInt(configFile.getProperty("Port"));
			serverHost = configFile.getProperty("Host");
			clientTimeout = getIntProp(configFile, "ClientTimeout", 3600);
			maxConn = Integer.parseInt(configFile
					.getProperty("ClientConnection"));
			modType = Integer.parseInt(configFile.getProperty("TypeID"));
			// Подключение postgres
			pgDatabaseName = getStringProp(configFile, "DatabaseName",
					"postgres");
			pgHost = getStringProp(configFile, "DatabaseHost", "127.0.0.1");
			pgUrl = "jdbc:postgresql://" + pgHost + "/" + pgDatabaseName;
			pgUser = getStringProp(configFile, "DatabaseUser", "owner_track");
			pgPasswd = getStringProp(configFile, "DatabasePasswd", "");
			pgPort = getIntProp(configFile, "DatabasePort", 5432);
			pgInitConn = getIntProp(configFile, "DatabaseInitConnection", 1);
			pgMaxConn = getIntProp(configFile, "DatabaseMaxConnection", 10);
			maxPacketSize = getIntProp(configFile, "MaxPacketSize", 65535);
			begChar = getByteProp(configFile, "BegChar", defaultValue);
			endChar = getByteProp(configFile, "EndChar", defaultValue);
			readInterval = getIntProp(configFile, "ReadInterval", 1000);
			serverType = getStringProp(configFile, "ServerType",
					SERVER.TCP.toString());
			snmpCommunity = getStringProp(configFile, "SnmpCommunity", "public");
			snmpPort = getIntProp(configFile, "SnmpPort", 1161);
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

	public int getPgInitConn() {
		return pgInitConn;
	}

	public int getPgMaxConn() {
		return pgMaxConn;
	}

	private int getIntProp(Properties properties, String optionName,
			int defaultValue) {
		String value;
		try {
			value = properties.getProperty(optionName);
		} catch (NullPointerException e) {
			value = null;
			logger.warn("Устанавливается значение по умолчанию ("
					+ defaultValue + ") для параметра : " + optionName);
		}
		if (value == null) {
			return defaultValue;
		}
		try {
			int intValue = Integer.parseInt(value);
			return intValue;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private String getStringProp(Properties properties, String optionName,
			String defaultValue) {
		String value;
		try {
			value = properties.getProperty(optionName);
		} catch (NullPointerException e) {
			value = null;
			logger.warn("Устанавливается значение по умолчанию ("
					+ defaultValue + ") для параметра : " + optionName);
		}
		if (value == null) {
			return defaultValue;
		}
		try {
			return value;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private byte getByteProp(Properties properties, String optionName,
			byte defaultValue) {
		String value;
		try {
			value = properties.getProperty(optionName);
		} catch (NullPointerException e) {
			value = null;
			logger.warn("Устанавливается значение по умолчанию ("
					+ defaultValue + ") для параметра : " + optionName);
		}
		if (value == null) {
			return defaultValue;
		}
		try {
			byte byteValue = Byte.parseByte(value);
			return byteValue;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public String getSnmpCommunity() {
		return snmpCommunity;
	}

	public int getSnmpPort() {
		return snmpPort;
	}

}
