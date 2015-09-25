package ru.novoscan.trkpd;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.text.ParseException;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.resources.ModConstats;
import ru.novoscan.trkpd.terminals.ModTeltonikaFm;
import ru.novoscan.trkpd.terminals.ModTranskomT15;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.TrackPgUtils;

public class TrackServerUdp implements TrackServer {

	private static final Logger logger = Logger.getLogger(TrackServerUdp.class);

	private ModConfig config;

	private TrackPgUtils pgConnect;
	
	private DatagramSocket listenSocket;

	public TrackServerUdp() {
	}
	@Override
	public void setConfig(ModConfig config) {
		this.config = config;
	}
	@Override
	public ModConfig getConfig() {
		return this.config;
	}
	@Override
	public void setPgConnect(TrackPgUtils pgConnect) {
		this.pgConnect = pgConnect;
	}
	@Override
	public void run() throws IOException {
		logger.debug("Запуск UDP сервера : " + config.getHost() + ":"
				+ config.getPort());
		listenSocket = new DatagramSocket(config.getPort(),
				config.getHost());
		logger.debug("Сервер запущен.");
		while (true) {
			new UDPReader();
		}
	}

	class UDPReader extends Thread implements ModConstats {
		private DatagramPacket dataPacket;
		public UDPReader() {
			try {
				dataPacket = new DatagramPacket(new byte[1024], 1024);
				listenSocket.receive(dataPacket);
				this.start();
			} catch (IOException e) {
				logger.error("Ошибка ввода/вывода : " + e.getMessage());
			}
		}

		public void run() {
			try {
				logger.debug("Подключение клиента : "
						+ dataPacket.getSocketAddress().toString());
				int modType = config.getModType();
				double readBytes = 0;
				logger.debug("Тип модуля : " + config.getModType());
				if (modType == TERM_TYPE_TRANSKOM_T15) {
					ModTranskomT15 mod = new ModTranskomT15(dataPacket,
							listenSocket, config, pgConnect);
					readBytes = mod.getReadBytes();
				} else if (modType == TERM_TYPE_TELTONIKA_FM) {
					ModTeltonikaFm mod = new ModTeltonikaFm(dataPacket,
							listenSocket, config, pgConnect);
					readBytes = mod.getReadBytes();
				} else {
					throw new ParseException("Неподдерживаемый тип модуля",
							modType);
				}
				logger.debug("Получено байт : " + readBytes);
			} catch (ParseException e) {
				logger.error("Неподдерживаемый тип модуля : "
						+ e.getErrorOffset());
			} finally {
				logger.debug("Соединение закрыто.");
			}

		}
	}

}
