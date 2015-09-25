/**
 * 
 */
package ru.novoscan.trkpd;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.TrackPgUtils;

/**
 * @author kurensky
 * 
 */
public class Track {

	static private String version = "1.3";

	static Logger logger = Logger.getLogger(Track.class);

	static private final StringBuffer sb = new StringBuffer();

	static ModConfig config = new ModConfig();

	/**
	 * @param args
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String arg[]) throws IOException, ClassNotFoundException, SQLException {
		parse(arg);
		BasicConfigurator.configure();
		logger.debug("Обработка конфигурационного файла.");
		config.init();
		while (true) {
				sb.setLength(0);
				sb.append("Запуск сервера сбора данных ")
						.append(config.getServerType())
						.append(" Track Server ").append(" Версия сервера : ")
						.append(version);
				logger.info(sb);
				parse(arg);
				sb.setLength(0);
				sb.append("Подключение к базе данных Postgresql : ")
						.append(config.getPgUrl());
				TrackPgUtils pgconn = new TrackPgUtils();
				pgconn.setConfig(config);
				pgconn.connect();
				sb.append("Подключение выполнено.");
				TrackServer trackServer;
				if(config.getServerType().equalsIgnoreCase("UDP")) {
					trackServer = new TrackServerUdp();
				} else {
					trackServer = new TrackServerTcp();					
				}
				trackServer.setConfig(config);
				trackServer.setPgConnect(pgconn);
				trackServer.run();
		}
	}

	private static void parse(String[] argcmd) throws IOException {
		int i = 0;
		String arg;
		boolean debug = false;
		boolean usage = false;
		logger.debug("Обработка параметров запуска.");
		while (i < argcmd.length && argcmd[i].startsWith("-")) {
			arg = argcmd[i++];
			if (arg.equals("-d")) {
				debug = true;
				logger.debug("Отладка включена.");
			} else if (arg.equals("-d")) {
				daemonize();
			} else if (arg.equals("-f")) {
				if (i < argcmd.length)
					config.setConfigFile(argcmd[i++]);
				else
					logger.error("-f <имя файла конфигурации>");
				if (debug)
					logger.debug("Использцется конфигурационный файл : "
							+ argcmd[i++]);
			} else {
				logger.error("Неверный параметр запуска : " + argcmd[i++]);
				usage = true;
			}
		}
		if (usage)
			logger.warn("Использование : [-vd] [-f <имя файла конфигурации>]");
		else 
			logger.debug("Параметры запуска корректны.");
	}

	private static void daemonize() throws IOException {
		System.out.close();
		System.err.close();
	}

}
