package ru.novoscan.trkpd.terminals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.domain.Terminal;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.ModUtils;
import ru.novoscan.trkpd.utils.TrackPgUtils;

public class ModNavis extends Terminal {

	private static final Logger logger = Logger.getLogger(ModNavis.class);

	private int readbytes = 0;

	private final int PACKET_HANDSHAKE_LENGTH = 20;

	private final int PACKET_HEADER_LENGTH = 6;

	private final int PACKET_IMEI_LENGTH = 8;

	private String navSoftVersion;

	private final int maxPacketSize;

	private String navDateTime;

	private final DateFormat df = new SimpleDateFormat(DATE_FORMAT);

	private int[] packetHadnshake;

	private int[] packetHeader;

	private int[] packetData;

	private int[] crcData;

	private DataInputStream iDs;

	private int fullreadbytes;

	private int headeSize;

	private String cmd;

	private int dataSize;

	private int k;

	private DecimalFormat dfImei = new DecimalFormat();

	private int navReason;

	private int navSns;

	public String getCmd() {
		return cmd;
	}

	public void setCmd(String cmd) {
		this.cmd = cmd;
	}

	public ModNavis(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader console, ModConfig conf, TrackPgUtils pgcon)
			throws ParseException, IOException, SQLException {
		this.setDasnType(conf.getModType());
		dfImei.setMaximumIntegerDigits(15);
		dfImei.setMinimumIntegerDigits(15);
		dfImei.setGroupingSize(15);
		this.iDs = iDs;
		logger.debug("Чтение потока..");
		setPacketHandshake(new int[PACKET_HANDSHAKE_LENGTH]); // пакет с
																// нулевого
		setPacketHeader(new int[PACKET_HEADER_LENGTH]); // пакет с нулевого
		fullreadbytes = 0;
		maxPacketSize = conf.getMaxSize();
		crcData = new int[2];
		for (int i = 0; i < PACKET_HANDSHAKE_LENGTH; i++) {
			packetHadnshake[i] = readByte();
		}
		parseHandshake();
		sendResponce(oDs, cmd);
		while (true) {

			for (int i = 0; i < PACKET_HEADER_LENGTH; i++) {
				packetHeader[i] = readByte();
			}
			logger.debug("Данные прочтены.");
			dataSize = 0;
			parseHeader();
			packetData = new int[dataSize]; // пакет с нулевого считается
			for (int i = 0; i < dataSize; i++) {
				packetData[i] = readByte();
			}
			crcData[0] = readByte();
			crcData[1] = readByte();
			logger.debug("Контрольная сумма (HEX): "
					+ Integer.toHexString(crcData[0]) + ""
					+ Integer.toHexString(crcData[1]));
			int crcAVL = ModUtils.getCrc16(packetData);
			int crc = (crcData[0] * 256) + crcData[1];
			logger.debug("Контрольная сумма вычисленная : " + crcAVL
					+ " в данных " + crc);
			if (crc != crcAVL) {
				logger.warn("Контрольные суммы не совпадают!");
			} else {
				k = 0; // обработка пакета обнуление счётчика.
				parseIMEI();
				if (getCodec() == 2) {
					int countPacket = getCountPacket();
					logger.debug("Количество пакетов : " + countPacket);
					for (int l = 0; l < countPacket; l++) {
						logger.debug("Парсим пакет : " + l);
						navDateTime = getDateTime();
						logger.debug("Дата : " + navDateTime);
						navReason = getReason();
						logger.debug("Причина : " + navReason);
						getGnss();
						getIO();
						if (dasnStatus == NavigationStatus.OK) {
							dataSensor.setDasnUid(dasnUid);
							dataSensor.setDasnLatitude(dasnLatitude);
							dataSensor.setDasnLongitude(dasnLongitude);
							dataSensor.setDasnSatUsed(dasnSatUsed);
							dataSensor.setDasnSog(dasnSog);
							dataSensor.setDasnCourse(dasnCourse);
							dataSensor.setDasnHgeo(dasnHgeo);
							dataSensor.setDasnGpio(dasnGpio);
							dataSensor.setDasnAdc(dasnAdc);
							dataSensor.setDasnTemp(dasnTemp);
							dataSensor.setDasnValues(dasnValues);
							// запись в БД
							pgcon.setDataSensorValues(dataSensor);
							// Ответ блоку
							pgcon.addDataSensor();
							this.clear();
						}
					}
				}
			}
		}
	}

	private void getIO() {
		int eventElemetID = packetData[k++];
		logger.debug("Событие : " + eventElemetID);
		int byteLength = 1;
		for (int i = 0; i < 4; i++) {
			int infoCount = packetData[k++];
			logger.debug("Событий " + byteLength + " байтовых : " + infoCount);
			for (int m = 0; m < infoCount; m++) {
				int id = readId();
				int value = readValue(byteLength);
				if (id == 66) {
					dasnAdc = (long) value;
				} else if (id == 65) {
					dasnGpio = (long) value;
				} else if (id == 70) {
					dasnTemp = (double) value;
				} else if (id == 105 || id == 106) {
					// Уже порядковый номер и дата
				} else {
					dasnValues.put(String.valueOf(id), String.valueOf(value));
				}
			}
			byteLength = 2 * byteLength;
		}

	}

	private int readValue(int byteLength) {
		int data = 0;
		int len = byteLength - 1;
		for (int i = 0; i < byteLength; i++) {
			data = data + packetData[k++] << ((len - i) * 8);
		}
		return data;
	}

	private int readId() {
		int id = packetData[k++];
		return id;
	}

	private void getGnss() {
		dasnLongitude = Double.valueOf(
				ModUtils.getDegreeFromInt(ModUtils.getIntU32L(packetData, k)));
		k = k + 4;
		logger.debug("Долгота : " + dasnLongitude);
		dasnLatitude = Double.valueOf(
				ModUtils.getDegreeFromInt(ModUtils.getIntU32L(packetData, k)));
		k = k + 4;
		logger.debug("Широта : " + dasnLatitude);
		dasnHgeo = Double.valueOf(ModUtils.getIntU16L(packetData, k));
		k = k + 2;
		logger.debug("Высота : " + dasnHgeo);
		dasnCourse = Double.valueOf(ModUtils.getIntU16L(packetData, k));
		k = k + 2;
		logger.debug("Курс : " + dasnCourse);
		dasnSatUsed = Long.valueOf(packetData[k++]);
		logger.debug("Спутники : " + dasnSatUsed);
		dasnSog = Double.valueOf(ModUtils.getIntU16L(packetData, k));
		k = k + 2;
		logger.debug("Скорость : " + dasnSog);
		if (packetData[k++] == 1) {
			dasnStatus = NavigationStatus.OK;
		} else {
			dasnStatus = NavigationStatus.ERR;

		}
		logger.debug("Достоверность : " + dasnStatus);
		navSns = packetData[k++];
		logger.debug("СНС : " + navSns);
	}

	private int getReason() {
		int res = packetData[k];
		k++;
		return res;
	}

	private String getDateTime() {
		double timestamp = 0;
		for (int i = 0; i < 4; i++) {
			timestamp = timestamp + packetData[k] * Math.pow(2, ((3 - i) * 8));
			k++;
		}
		Date date = new Date();
		date.setTime((long) timestamp * 1000);
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		return df.format(date);
	}

	private int getCountPacket() {
		int countPacket = packetData[k];
		k++;
		return countPacket;
	}

	private int getCodec() {
		int codec = packetData[k];
		k++;
		return codec;
	}

	private String parseIMEI() {
		double imei = 0;
		String imeiString = "";
		for (int i = 0; i < PACKET_IMEI_LENGTH; i++) {
			logger.debug("IMEI[" + i + "] " + packetData[i]);
			int offset = 8 * (PACKET_IMEI_LENGTH - (i + 1));
			imei = imei + (packetData[i] * Math.pow(2, offset));
			k++;
		}
		imeiString = dfImei.format(imei);
		logger.debug("IMEI пакета : " + imeiString);
		return imeiString;
	}

	private void parseHeader() {
		int pr = 0xcc;
		for (int i = 0; i < 4; i++) {
			if ((packetHeader[i] & 0xff) != pr) {
				throw new RuntimeException(
						"Неверная преамбула : " + packetHeader[i]);
			}
		}

		dataSize = ((packetHeader[4]) << 8) + (packetHeader[5]);
		logger.debug("Размер пакета : " + dataSize);

	}

	private void setPacketHeader(int[] packetHeader) {
		this.packetHeader = packetHeader;

	}

	private void parseHandshake() {
		// Разбор заголовка - 20 Байт.
		headeSize = 0;
		int k = 0;
		for (int i = 0; i < 2; i++) {
			headeSize = headeSize + packetHadnshake[i];
			k++;
		}
		logger.debug("Количество байт рукопожатия : " + headeSize);
		if (headeSize != 18) {
			logger.error("Неверный размер пакета рукопожатия: " + headeSize);
			throw new RuntimeException(
					"Неверные данные пакета HANDSHAKE : размер " + headeSize);
		}
		//
		dasnUid = "";
		for (int i = 0; i < 15; i++) {
			dasnUid = dasnUid + (char) packetHadnshake[k];
			k++;
		}
		navSoftVersion = "";
		for (int i = 0; i < 3; i++) {
			navSoftVersion = navSoftVersion + (char) packetHadnshake[k];
			k++;
		}

		logger.debug("IMEI терминала : " + dasnUid);
		logger.debug("Версия ПО терминала : " + navSoftVersion);
		if (!checkIMEI()) {
			setCmd("NO01");
		} else if (!navSoftVersion.equalsIgnoreCase("1.3")) {
			setCmd("NO02");
		} else {
			setCmd("OK");
		}
	}

	private boolean checkIMEI() {
		// TODO проверить на сервере
		return true;

	}

	public int getReadBytes() {
		return this.fullreadbytes;
	}

	/**
	 * @return the packetHeader
	 */
	public int[] getPacketHeader() {
		return packetHadnshake;
	}

	/**
	 * @param packetHeader
	 *            the packetHeader to set
	 */
	public void setPacketHandshake(int[] packetHeader) {
		this.packetHadnshake = packetHeader;
	}

	/**
	 * @return the maxPacketSize
	 */
	public int getMaxPacketSize() {
		return maxPacketSize;
	}

	/**
	 * @return the iDsLocal
	 */
	public DataInputStream getiDsLocal() {
		return iDs;
	}

	/**
	 * @param iDsLocal
	 *            the iDsLocal to set
	 */
	public void setiDsLocal(DataInputStream iDsLocal) {
		this.iDs = iDsLocal;
	}

	private int readByte() throws IOException {
		byte bread = iDs.readByte();
		int packet = bread & 0xff;
		logger.debug(
				"packet[" + readbytes + "] : " + Integer.toHexString(packet));
		readbytes++;
		fullreadbytes++;
		return packet;
	}

	private void sendResponce(DataOutputStream oDs, String cmd)
			throws IOException {
		oDs.writeBytes(cmd);
		logger.debug("Write : " + cmd);
		oDs.flush();
	}
}
