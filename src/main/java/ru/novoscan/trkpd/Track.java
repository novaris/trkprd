/**
 * 
 */
package ru.novoscan.trkpd;

import java.io.IOException;
import java.sql.SQLException;

import javax.management.RuntimeErrorException;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import ru.novoscan.trkpd.resources.ModConstats.SERVER;
import ru.novoscan.trkpd.snmp.TrackSnmpAgent;
import ru.novoscan.trkpd.snmp.TrackSnmpInfo;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.TrackPgUtils;

/**
 * @author kurensky
 * 
 */
public class Track {

	static Logger logger = LogManager.getLogger(Track.class);

	static private final StringBuffer sb = new StringBuffer();

	static ModConfig config = new ModConfig();

	private static TrackSnmpAgent trackSnmpAgent;

	private static TrackSnmpInfo trackSnmpInfo;

	/**
	 * @param args
	 * @throws IOException
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public static void main(String arg[]) throws IOException,
			ClassNotFoundException, SQLException {
		parse(arg);
		//Configurator.configure();
		logger.debug("Обработка конфигурационного файла.");
		config.init();
		while (true) {
			sb.setLength(0);
			sb.append("Запуск сервера сбора данных ")
					.append(config.getServerType())
					.append(" Сервер Novoscan Track ")
					.append(" Версия сервера : ")
					.append(TrackVersion.getVersion())
					.append(" Сборка : " + TrackVersion.getSvnVersion())
					.append(" Дата : " + TrackVersion.getBuildDate());
			logger.info(sb);
			parse(arg);
			sb.setLength(0);
			sb.append("Подключение к базе данных Postgresql : ").append(
					config.getPgUrl());
			TrackPgUtils pgconn = new TrackPgUtils(config);
			sb.append("\r\nПодключение выполнено.");
			logger.info(sb);
			TrackServer trackServer;
			sb.setLength(0);
			sb.append("Протокол сервера : ").append(config.getServerType());
			logger.info(sb);
			runSnmpServer();
			if (config.getServerType().equalsIgnoreCase(SERVER.UDP.toString())) {
				trackServer = new TrackServerUdp(config, pgconn);
			} else if (config.getServerType().equalsIgnoreCase(
					SERVER.TCP.toString())) {
				trackServer = new TrackServerTcp(config, pgconn);
			} else {
				throw new RuntimeErrorException(new Error("Error Server Type"),
						"Неверный протокол : " + config.getServerType());
			}
			trackServer.run();
		}
	}

	private static void runSnmpServer() {
		trackSnmpInfo = new TrackSnmpInfo("Novaris Track Server", config);
		trackSnmpInfo.setStartTime(System.currentTimeMillis());
		trackSnmpAgent = new TrackSnmpAgent(config.getSnmpPort(),
				config.getSnmpCommunity(), trackSnmpInfo);

		while (true) {
			if (trackSnmpAgent.isSocketCreated()) {
				break;
			}
			if (trackSnmpAgent.isSocketException()) {
				throw new RuntimeErrorException(new Error("Error Server Type"),
						"Приложение SNMP Novaris Track Server уже запущено на этом порту : "
								+ config.getSnmpPort());
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException ex) {
			}
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
