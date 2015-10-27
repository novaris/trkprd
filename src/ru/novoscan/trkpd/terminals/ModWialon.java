package ru.novoscan.trkpd.terminals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import javax.management.RuntimeErrorException;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.domain.Terminal;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.TrackPgUtils;

public class ModWialon extends Terminal {
	static Logger logger = Logger.getLogger(ModWialon.class);

	private final byte[] packetData;

	private int fullreadbytes;

	private int maxPacketSize = 32768;

	private TrackPgUtils pgcon;

	private ModConfig conf;

	private StringBuffer askPacket = new StringBuffer();

	private DatagramSocket clientSocket;

	private DatagramPacket askDatagram;

	@SuppressWarnings("unused")
	private int readbytes;

	private DatagramPacket dataPacket;

	private StringBuffer packetString = new StringBuffer();

	private static final String FIELD_NA = "NA";

	private static final Pattern PAT_PACKET = Pattern
			.compile("(?i)#(.+)#(.+)?\\r\\n");

	private static final Pattern PAT_PACKET_NOGPS = Pattern
			.compile("(?i)#(.+)#(.+)?");

	private static final Pattern PAT_PARAMS = Pattern
			.compile("(.*):[1-2]:(.*)"); // интересуют только цифровые данные

	private static final String PAT_POW = "Upow";

	private static final String PAT_TEMP = "t";

	private static final Pattern PAT_PACKET_BIN = Pattern
			.compile("(?i)#(.+)#(.+)\\r?\\n?");

	private static final Pattern PAT_DATA_D = Pattern
			.compile("(\\d{2})(\\d{2})(\\d{2});" + // Date (DDMMYY)
					"(\\d{2})(\\d{2})(\\d{2});" + // Time (HHMMSS)
					"(\\d{2})(\\d{2}\\.\\d+);" + // Latitude (DDMM.MMMM)
					"([NS]);" + "(\\d{3})(\\d{2}\\.\\d+);" + // Longitude
																// (DDDMM.MMMM)
					"([EW]);" + "(\\d+\\.?\\d*)?;" + // Speed
					"(\\d+\\.?\\d*)?;" + // Course
					"(?:" + FIELD_NA + "|(\\d+\\.?\\d*));" + // Altitude
					"(?:" + FIELD_NA + "|(\\d+))" + // Satellites
					"(?:;" + "(?:" + FIELD_NA + "|(\\d+\\.?\\d*));" + // hdop
					"(?:" + FIELD_NA + "|(\\d+));" + // inputs
					"(?:" + FIELD_NA + "|(\\d+));" + // outputs
					"(?:" + FIELD_NA + "|([^;]*));" + // adc
					"(?:" + FIELD_NA + "|([^;]*));" + // ibutton
					"(?:" + FIELD_NA + "|(.*))" + // params
					")?");

	private static final Pattern PAT_DATA_L = Pattern.compile("(\\w+);(\\w+)");

	private static final String PACKET_L = "L";

	private static final String PACKET_D = "D";

	private static final String PACKET_P = "P";

	private static final String PACKET_SD = "SD";

	private static final String PACKET_B = "B";

	private static final String PACKET_M = "M";

	private static final String PACKET_I = "I";

	private static final int RETURN_OK = 1; // пакет успешно зафиксировался

	@SuppressWarnings("unused")
	private static final int RETURN_ERR = -1; // ошибка структуры пакета

	@SuppressWarnings("unused")
	private static final int RETURN_ERR_DATE = 0; // некорректное время

	@SuppressWarnings("unused")
	private static final int RETURN_ERR_NAV = 10; // ошибка получения координат

	@SuppressWarnings("unused")
	private static final int RETURN_ERR_SOG = 11; // ошибка получения скорости,
													// курса или высоты

	@SuppressWarnings("unused")
	private static final int RETURN_ERR_SAT = 12; // ошибка получения количества
													// спутников или hdop

	@SuppressWarnings("unused")
	private static final int RETURN_ERR_IO = 13; // ошибка получения inputs или
													// outputs

	@SuppressWarnings("unused")
	private static final int RETURN_ERR_ADC = 14; // – ошибка получения adc

	@SuppressWarnings("unused")
	private static final int RETURN_ERR_PAR = 15; // ошибка получения

	private static final int ADC_FACT = 1000; // 1 м.в.
												// дополнительных параметров

	private DataOutputStream oDs;

	private DataInputStream iDs;

	private int packetLength;

	private Matcher m;

	private Matcher mp;

	private String passwd;

	private int dasnPackerCount;

	private Matcher pm;

	public ModWialon(DatagramPacket dataPacket, DatagramSocket clientSocket,
			ModConfig conf, TrackPgUtils pgcon) throws IOException {
		this.conf = conf;
		this.pgcon = pgcon;
		this.dataPacket = dataPacket;
		this.clientSocket = clientSocket;
		maxPacketSize = conf.getMaxSize();
		packetData = new byte[maxPacketSize];
		try {
			while (true) {
				readData();
			}
		} catch (DataFormatException e) {
			logger.error("Неверный формат данных!");
		}
	}

	public ModWialon(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader inputStreamReader, ModConfig conf,
			TrackPgUtils pgcon) throws IOException {
		this.conf = conf;
		this.pgcon = pgcon;
		this.oDs = oDs;
		this.iDs = iDs;
		logger.debug("Чтение потока..");
		maxPacketSize = conf.getMaxSize();
		packetData = new byte[maxPacketSize];
		fullreadbytes = 0;
		readbytes = 0;
		try {
			while (true) {
				readData();
			}
		} catch (DataFormatException e) {
			logger.error("Неверный формат данных!");
		}

	}

	private void readData() throws IOException, DataFormatException {
		logger.debug("Чтение...");
		dasnPacketType = "";
		if (conf.getServerType().equalsIgnoreCase(SERVER.UDP.toString())) {
			for (int i = 0; i < dataPacket.getLength(); i++) {
				packetData[i] = dataPacket.getData()[i];
				logger.debug("Data [" + i + "] : "
						+ Byte.toString(packetData[i]));
			}
			if ((packetData[0] & 0xff) == 0xff) {
				int packetLengthZip = (packetData[1] & 0xff)
						+ ((packetData[2] & 0xff) << 8);
				if (packetLengthZip > maxPacketSize)
					throw new RuntimeErrorException(new Error("Lenght Error"),
							"Неверный размер пакета.");
				byte[] packetDataZip = packetData;
				packetLength = unzip(packetDataZip, packetLengthZip);
			} else {
				int i = 0;
				while (!PAT_PACKET.matcher(packetString).matches()) {
					if (packetLength > maxPacketSize)
						throw new RuntimeErrorException(new Error(
								"Lenght Error"), "Неверный размер пакета.");
					packetLength++;
					packetString.append((char) packetData[i]);
				}
			}
		} else if (conf.getServerType().equalsIgnoreCase(SERVER.TCP.toString())) {
			int i = 0;
			packetString.setLength(0);
			packetData[i] = (byte) readByte();
			if (packetData[i] == 0) {
				int packetLengthZip = readByte() + (readByte() << 8);
				if (packetLengthZip > maxPacketSize)
					throw new RuntimeErrorException(new Error("Lenght Error"),
							"Неверный размер пакета.");
				byte[] packetDataZip = new byte[packetLengthZip];
				for (int k = 0; k < packetLengthZip; k++) {
					packetDataZip[k] = (byte) readByte();
				}
				packetLength = unzip(packetDataZip, packetLengthZip);
			} else {
				packetString.append((char) packetData[i]);
				packetLength = 1;
				while (!PAT_PACKET.matcher(packetString).matches()) {
					i++;
					packetLength++;
					if (packetLength > maxPacketSize)
						throw new RuntimeErrorException(new Error(
								"Lenght Error"), "Неверный размер пакета.");
					packetData[i] = (byte) readByte();
					packetString.append((char) packetData[i]);
					// logger.debug("Строка : " + packetString);
				}
			}
		}
		logger.debug("Пакет данных : " + packetString);
		logger.debug("Размер пакета данных : " + packetLength);
		if (parsePacket()) {
			logger.debug("Типа пакета : " + dasnPacketType);
			sendAck();
		}
		fullreadbytes = packetLength;
	}

	private int unzip(byte[] packetDataZip, int packetZipLength)
			throws DataFormatException {
		Inflater decompress = new Inflater();
		decompress.setInput(packetDataZip, 0, packetZipLength);
		return decompress.inflate(packetData);
	}

	private boolean parsePacket() throws IOException {
		packetString.setLength(0);
		askPacket.setLength(0);
		for (int i = 0; i < packetLength; i++) {
			if (packetData[i] != CTRL && packetData[i] != CTRN) {
				packetString.append((char) packetData[i]);
			}
		}
		m = PAT_PACKET_BIN.matcher(packetString);
		mp = PAT_PACKET_NOGPS.matcher(packetString);
		if (m.matches()) {
			dasnPacketType = m.group(1);
			logger.debug("Тип : " + dasnPacketType);
			askPacket.setLength(0);
			if (dasnPacketType.equalsIgnoreCase(PACKET_L)) {
				askPacket.append("#A").append(PACKET_L).append("#")
						.append(RETURN_OK);
			} else if (dasnPacketType.equalsIgnoreCase(PACKET_D)) {
				askPacket.append("#A").append(PACKET_D).append("#")
						.append(RETURN_OK);
			} else if (dasnPacketType.equalsIgnoreCase(PACKET_SD)) {
				askPacket.append("#A").append(PACKET_D).append("#")
						.append(RETURN_OK);
			} else if (dasnPacketType.equalsIgnoreCase(PACKET_B)) {
				askPacket.append("#A").append(PACKET_B).append("#");
			} else {
				throw new RuntimeErrorException(new Error("Type Error"),
						"Неверный тип пакета.");
			}
			parseData();
		} else if (mp.matches()) {
			dasnPacketType = mp.group(1);
			if (dasnPacketType.equalsIgnoreCase(PACKET_P)) {
				askPacket.append("#A").append(PACKET_P).append("#");
			} else if (dasnPacketType.equalsIgnoreCase(PACKET_M)) {
				askPacket.append("#A").append(PACKET_M).append("#")
						.append(RETURN_OK);
			} else if (dasnPacketType.equalsIgnoreCase(PACKET_I)) {
				askPacket.append("#I").append(PACKET_M).append("#")
						.append(RETURN_OK);
			}
		} else {
			logger.debug("Подтверждение : " + dasnPacketType);
		}
		return true;
	}

	private void parseData() {
		logger.debug(m.group(2));
		String packetString = m.group(2);
		dasnStatus = DATA_STATUS.ERR;
		if (dasnPacketType.equalsIgnoreCase(PACKET_D)) {
			// расширенные данные
			dasnPackerCount = 0;
			mp = PAT_DATA_D.matcher(packetString);
			if (mp.matches()) {
				parse();
				writeData();
			}
		} else if (dasnPacketType.equalsIgnoreCase(PACKET_B)) {
			String[] messages = packetString.split("\\|");
			dasnPackerCount = 0;
			logger.debug("Обработка ящика сообщений : " + messages.length);
			for (String message : messages) {
				logger.debug(message);
				mp = PAT_DATA_D.matcher(message);
				if (mp.matches()) {
					parse();
					writeData();
				}
			}
			askPacket.append(messages.length);

		} else if (dasnPacketType.equalsIgnoreCase(PACKET_L)) {
			// авторизация
			// navDeviceStatus = 1;
			mp = PAT_DATA_L.matcher(packetString);
			if (mp.matches()) {
				dasnUid = mp.group(1);
				passwd = mp.group(2);
				logger.debug("Ид : " + dasnUid + " Пароль : " + passwd);
				if (!checkIMEI())
					throw new RuntimeErrorException(new Error("Login Error"),
							"Неверный Ид терминала или пароль.");
			} else {
				logger.error("Неверные данные авторизации " + packetString);
			}
		}
	}

	private void parse() {
		Integer index = 1;
		String tmp = "";
		dasnStatus = DATA_STATUS.ERR;
		dasnPackerCount++;
		// Date and Time
		Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		time.clear();
		time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(mp.group(index++)));
		time.set(Calendar.MONTH, Integer.parseInt(mp.group(index++)) - 1);
		time.set(Calendar.YEAR, 2000 + Integer.parseInt(mp.group(index++)));
		time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(mp.group(index++)));
		time.set(Calendar.MINUTE, Integer.parseInt(mp.group(index++)));
		time.set(Calendar.SECOND, Integer.parseInt(mp.group(index++)));
		time.setTimeInMillis(time.getTimeInMillis() - TZ_OFFSET);
		dasnDatetime = time.getTime();
		dataSensor.setDasnDatetime(dasnDatetime);
		// Latitude
		dasnLatitude = Double.parseDouble(mp.group(index++));
		dasnLatitude += Double.parseDouble(mp.group(index++)) / 60;
		if (mp.group(index++).compareTo("S") == 0)
			dasnLatitude = -dasnLatitude;
		dataSensor.setDasnLatitude(dasnLatitude);
		// Longitude
		dasnLongitude = Double.parseDouble(mp.group(index++));
		dasnLongitude += Double.parseDouble(mp.group(index++)) / 60;
		if (mp.group(index++).compareTo("W") == 0)
			dasnLongitude = -dasnLongitude;
		dataSensor.setDasnLongitude(dasnLongitude);
		// Speed
		tmp = mp.group(index++);
		if ((tmp != null) && !(tmp.equalsIgnoreCase(FIELD_NA))) {
			dasnSog = Double.parseDouble(tmp);
			dataSensor.setDasnSog(dasnSog);
		}
		// Course
		tmp = mp.group(index++);
		if ((tmp != null) && !(tmp.equalsIgnoreCase(FIELD_NA))) {
			dasnCourse = Double.parseDouble(tmp);
			dataSensor.setDasnCourse(dasnCourse);
		}
		// Altitude
		tmp = mp.group(index++);
		if ((tmp != null) && !(tmp.equalsIgnoreCase(FIELD_NA))) {
			dasnHgeo = Double.parseDouble(tmp);
			dataSensor.setDasnHgeo(dasnHgeo);
		}
		// Satellites
		tmp = mp.group(index++);
		if ((tmp != null) && !(tmp.equalsIgnoreCase(FIELD_NA))) {
			dasnSatUsed = Long.parseLong(tmp);
			dataSensor.setDasnSatUsed(dasnSatUsed);
		}
		if (dasnSatUsed > 3) {
			dasnStatus = DATA_STATUS.OK;
		} else {
			dasnStatus = DATA_STATUS.ERR;
		}
		tmp = mp.group(index++);
		if ((tmp != null) && !(tmp.equalsIgnoreCase(FIELD_NA))) {
			dasnHdop = Double.parseDouble(tmp);
			dataSensor.setDasnHdop(dasnHdop);
		}
		tmp = mp.group(index++);
		if ((tmp != null) && !(tmp.equalsIgnoreCase(FIELD_NA))) {
			dasnGpio = (long) (Integer.parseInt(tmp) << 2);

		}
		tmp = mp.group(index++);
		if ((tmp != null) && !(tmp.equalsIgnoreCase(FIELD_NA))) {
			dasnGpio = dasnGpio + Integer.parseInt(tmp);
			dataSensor.setDasnGpio(dasnGpio);
		}
		tmp = mp.group(index++);
		if ((tmp != null) && !(tmp.equalsIgnoreCase(FIELD_NA))) {
			String[] adc = tmp.split(",");
			for (int i = 0; i < adc.length; i++) {
				dasnValues.put(String.valueOf(i), adc[i]);
			}
		}
		// iButton
		index++;
		// Params
		tmp = mp.group(index);
		if (tmp != null) {
			String[] params = tmp.split(",");
			for (String param : params) {
				pm = PAT_PARAMS.matcher(param);
				if (pm.matches()) {
					if (PAT_POW.equalsIgnoreCase(pm.group(1))) {
						dataSensor.setDasnAdc((long) Math.round(Float
								.parseFloat(pm.group(2)) * ADC_FACT));
					} else if (PAT_TEMP.equalsIgnoreCase(pm.group(1))) {
						dasnTemp = Double.parseDouble(pm.group(2));
						dataSensor.setDasnTemp(dasnTemp);
					} else {
						dasnValues.put(pm.group(1), pm.group(2));
					}
				}
			}
		}

	}

	private void writeData() {
		if (dasnStatus.equals(DATA_STATUS.OK)) {
			pgcon.setDataSensorValues(dataSensor);
			// Ответ блоку
			try {
				pgcon.addDataSensor();
				logger.debug("Writing Database : " + dasnUid);
			} catch (SQLException e) {
				logger.warn("Error Writing Database : " + e.getMessage());
			}
		} else {
			logger.debug("Данные не валидные : " + dasnPackerCount);
		}
		this.clear();

	}

	private void sendAck() throws IOException {
		askPacket.append("\r\n");
		if (conf.getServerType().equalsIgnoreCase(SERVER.UDP.toString())) {
			logger.debug("Отправка подтверждения для UDP : "
					+ askPacket.toString());
			askDatagram = new DatagramPacket(askPacket.toString().getBytes(),
					askPacket.length(), dataPacket.getAddress(),
					dataPacket.getPort());
			clientSocket.send(askDatagram);
		} else if (conf.getServerType().equalsIgnoreCase(SERVER.TCP.toString())) {
			logger.debug("Отправка подтверждения для TCP : "
					+ askPacket.toString());
			oDs.write(askPacket.toString().getBytes(), 0, askPacket.toString()
					.length());
			oDs.flush();
		}
	}

	/**
	 * @return the conf
	 */
	public ModConfig getConf() {
		return conf;
	}

	/**
	 * @param conf
	 *            the conf to set
	 */
	public void setConf(ModConfig conf) {
		this.conf = conf;
	}

	/**
	 * @return the pgcon
	 */
	public TrackPgUtils getPgcon() {
		return pgcon;
	}

	/**
	 * @param pgcon
	 *            the pgcon to set
	 */
	public void setPgcon(TrackPgUtils pgcon) {
		this.pgcon = pgcon;
	}

	public float getReadBytes() {
		return fullreadbytes;
	}

	private boolean checkIMEI() {
		// TODO проверить на сервере
		return true;
	}

	private int readByte() throws IOException {
		int packet = (iDs.readByte() & 0xff);
		// logger.debug("packet[" + readbytes + "] : "
		// + Integer.toHexString(packet));
		readbytes++;
		fullreadbytes++;
		return packet;
	}

}
