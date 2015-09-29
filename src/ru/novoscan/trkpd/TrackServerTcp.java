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

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.resources.ModConstats;
import ru.novoscan.trkpd.terminals.ModAny;
import ru.novoscan.trkpd.terminals.ModAzimut;
import ru.novoscan.trkpd.terminals.ModEgts;
import ru.novoscan.trkpd.terminals.ModGelix;
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

public class TrackServerTcp implements TrackServer {
	private static final Logger logger = Logger.getLogger(TrackServerTcp.class);

	private ModConfig config;

	private TrackPgUtils pgConnect;

	public TrackServerTcp(ModConfig config, TrackPgUtils pgConnect) {
		this.pgConnect = pgConnect;
		this.config = config;
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
		logger.debug("Запуск TCP сервера : " + config.getHost() + ":"
				+ config.getPort());
		ServerSocket listener = new ServerSocket(config.getPort(), config.getMaxConn(),
				config.getHost());
		int clientNumber = 0;
		logger.debug("Сервер запущен.");
        try {
            while (true) {
            	new TCPReader(listener.accept(), clientNumber++).start();
            }
        } finally {
            listener.close();
            logger.debug("Соединение закрыто");
        }
		
	}


	class TCPReader extends Thread implements ModConstats {

		
        private Socket clientSocket;

        private int clientNumber;


		public TCPReader(Socket clientSocket, int clientNumber) {
			this.clientSocket = clientSocket;
			this.clientNumber = clientNumber;
			logger.info("Подключение клиента " + this.clientNumber + " : "
					+ clientSocket.getRemoteSocketAddress().toString());

		}
		public void run() {
			double readBytes = 0;
			try {
				clientSocket.setSoTimeout(config.getTimeout());
				DataInputStream dataInputStream = new DataInputStream(
						clientSocket.getInputStream());
				DataOutputStream dataOutputStream = new DataOutputStream(
						clientSocket.getOutputStream());
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
						dataInputStream));
				InputStreamReader inputStreamReader = new InputStreamReader(dataInputStream);

				pgConnect.connect();
				int modType = config.getModType();
				logger.debug("Тип модуля : " + config.getModType());
				if (modType == TERM_TYPE_ANY) {
					ModAny mod = new ModAny(dataInputStream, dataOutputStream,
							inputStreamReader, config, pgConnect);
					readBytes = mod.getReadBytes();
				} else if (modType == TERM_TYPE_GELIX) {
					ModGelix mod = new ModGelix(dataInputStream,
							dataOutputStream, bufferedReader, config, pgConnect);
					readBytes = mod.getReadBytes();
				} else if (modType == TERM_TYPE_GS) {
					ModGs mod = new ModGs(dataInputStream, dataOutputStream,
							inputStreamReader, config, pgConnect);
					readBytes = mod.getReadBytes();
				} else if (modType == TERM_TYPE_UTP5) {
					ModUtp5 mod = new ModUtp5(dataInputStream,
							dataOutputStream, inputStreamReader, config,
							pgConnect);
					readBytes = mod.getReadBytes();
				} else if (modType == TERM_TYPE_NOVACOM) {
					ModNovacom mod = new ModNovacom(dataInputStream,
							dataOutputStream, inputStreamReader, config,
							pgConnect);
					readBytes = mod.getReadBytes();
				} else if (modType == TERM_TYPE_ST270) {
					ModSt270 mod = new ModSt270(dataInputStream,
							dataOutputStream, inputStreamReader, config,
							pgConnect);
					readBytes = mod.getReadBytes();
				} else if (modType == TERM_TYPE_SIGNAL_S21) {
					ModSignal mod = new ModSignal(dataInputStream,
							dataOutputStream, inputStreamReader, config,
							pgConnect);
					readBytes = mod.getReadBytes();
				} else if (modType == TERM_TYPE_MARKER) {
					ModMarker mod = new ModMarker(dataInputStream,
							dataOutputStream, inputStreamReader, config,
							pgConnect);
					readBytes = mod.getReadBytes();
				} else if (modType == TERM_TYPE_NAVIXY_M7) {
					ModNavixyM7 mod = new ModNavixyM7(dataInputStream,
							dataOutputStream, inputStreamReader, config,
							pgConnect);
					readBytes = mod.getReadBytes();
				} else if (modType == TERM_TYPE_GALILEO_SKY) {
					ModGalileoSky mod = new ModGalileoSky(dataInputStream,
							dataOutputStream, inputStreamReader, config,
							pgConnect);
					readBytes = mod.getReadBytes();
				} else if (modType == TERM_TYPE_NAVIS_UM4) {
					ModNavis mod = new ModNavis(dataInputStream,
							dataOutputStream, inputStreamReader, config,
							pgConnect);
					readBytes = mod.getReadBytes();
				} else if (modType == TERM_TYPE_SCOUT) {
					ModScout mod = new ModScout(dataInputStream,
							dataOutputStream, inputStreamReader, config,
							pgConnect);
					readBytes = mod.getReadBytes();
				} else if (modType == TERM_TYPE_XML) {
					ModXml mod = new ModXml(dataInputStream, dataOutputStream,
							inputStreamReader, config, pgConnect);
					readBytes = mod.getReadBytes();
				} else if (modType == TERM_TYPE_SCOUT_OPEN) {
					ModScoutOpen mod = new ModScoutOpen(dataInputStream,
							dataOutputStream, inputStreamReader, config,
							pgConnect);
					readBytes = mod.getReadBytes();
				} else if (modType == TERM_TYPE_MAJAK) {
					ModMajak mod = new ModMajak(dataInputStream,
							dataOutputStream, inputStreamReader, config,
							pgConnect);
					readBytes = mod.getReadBytes();
				} else if (modType == TERM_TYPE_AZIMUT) {
					ModAzimut mod = new ModAzimut(dataInputStream,
							dataOutputStream, inputStreamReader, config,
							pgConnect);
					readBytes = mod.getReadBytes();
				} else if (modType == TERM_TYPE_EGTS) {
					ModEgts mod = new ModEgts(dataInputStream,
							dataOutputStream, inputStreamReader, config,
							pgConnect);
					readBytes = mod.getReadBytes();
				} else {
					throw new ParseException("Неподдерживаемый тип модуля",
							modType);
				}
				logger.debug("Получено байт : " + readBytes);
				pgConnect.close();
			} catch (ParseException e) {
				logger.error("Неподдерживаемый тип модуля : "
						+ e.getErrorOffset());
			} catch (SQLException e) {
				logger.error("Ошибка БД : " + e.getMessage());
				e.printStackTrace();
			} catch (IOException e) {
				logger.error("Соединение закрыто : " + e.getMessage());
				e.printStackTrace();
			} finally {
				try {
					pgConnect.close();
					logger.debug("Соединение с базой закрыто");
				} catch (SQLException e) {
					
				}
			}

		}
	}
}