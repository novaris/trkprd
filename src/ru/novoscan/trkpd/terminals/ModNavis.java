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
import java.util.HashMap;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.resources.ModConstats;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.ModUtils;
import ru.novoscan.trkpd.utils.TrackPgUtils;

public class ModNavis implements ModConstats {

	private static final Logger logger = Logger.getLogger(ModNavis.class);

	private int readbytes = 0;

	private final int PACKET_HANDSHAKE_LENGTH = 20;

	private final int PACKET_HEADER_LENGTH = 6;

	private final int PACKET_IMEI_LENGTH = 8;

	private String navSoftVersion;

	private final int maxPacketSize;

	private String navDateTime;

	private final DateFormat df = new SimpleDateFormat(DATE_FORMAT);

	private ModConfig conf;

	private TrackPgUtils pgcon;

	private String navIMEI;

	private int[] packetHadnshake;

	private int[] packetHeader;

	private int[] packetData;

	private int[] crcData;

	private DataInputStream iDsLocal;

	private int fullreadbytes;

	private int headeSize;

	private String cmd;

	private int dataSize;

	private int k;

	private DecimalFormat dfIMEI = new DecimalFormat();

	private int navReason;

	private float navLongitude;

	private float navLatitude;

	private int navCource;

	private int navSatellitesCount;

	private int navSpeed;

	private int navDeviceStatus;

	private int navHgeo;

	private int navSNS;

	private HashMap<String, String> map = new HashMap<String, String>();

	private int navAdc;

	private int navGpio;

	private String navData;

	private int navTemp;

	public String getCmd() {
		return cmd;
	}

	public void setCmd(String cmd) {
		this.cmd = cmd;
	}

	public ModNavis(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader console, ModConfig conf, TrackPgUtils pgcon)
			throws ParseException, IOException {
		dfIMEI.setMaximumIntegerDigits(15);
		dfIMEI.setMinimumIntegerDigits(15);
		dfIMEI.setGroupingSize(15);
		setConf(conf);
		setPgcon(pgcon);
		setiDsLocal(iDs);
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
						navData = "";
						getGnss();
						getIO();
						if (navDeviceStatus == 1) {
							map.clear();
							map.put("vehicleId", navIMEI);
							map.put("dasnUid", navIMEI);
							map.put("dasnLatitude", String.valueOf(navLatitude));
							map.put("dasnLongitude",
									String.valueOf(navLongitude));
							map.put("dasnStatus",
									String.valueOf(navDeviceStatus));
							map.put("dasnSatUsed",
									String.valueOf(navSatellitesCount));
							map.put("dasnZoneAlarm", null);
							map.put("dasnMacroId", null);
							map.put("dasnMacroSrc", null);
							map.put("dasnSog", String.valueOf(navSpeed));
							map.put("dasnCource", String.valueOf(navCource));
							map.put("dasnHdop", null);
							map.put("dasnHgeo", String.valueOf(navHgeo));// Высота
							map.put("dasnHmet", null);
							map.put("dasnGpio", String.valueOf(navGpio));
							map.put("dasnAdc", String.valueOf(navAdc));
							map.put("dasnTemp", String.valueOf(navTemp));
							map.put("i_spmt_id",
									Integer.toString(conf.getModType()));
							map.put("dasnXML", "<xml><i>" + navData
									+ "</i></xml>");
							// запись в БД
							pgcon.setDataSensor(map, df.parse(navDateTime));
							// Ответ блоку
							try {
								pgcon.addDataSensor();
								logger.debug("Writing Database : " + navIMEI);
							} catch (SQLException e) {
								logger.warn("Error Writing Database : "
										+ e.getMessage());
							}
							map.clear();
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
					navAdc = value;
				} else if (id == 65) {
					navGpio = value;
				} else if (id == 70) {
					navTemp = value;
				} else if (id == 105 || id == 106) {
					// Уже порядковый номер и дата
				} else {
					navData = navData + id + "=" + value + ";";
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
		navLongitude = ModUtils.getDegreeFromInt(ModUtils.getIntU32L(
				packetData, k));
		k = k + 4;
		logger.debug("Долгота : " + navLongitude);
		navLatitude = ModUtils.getDegreeFromInt(ModUtils.getIntU32L(packetData,
				k));
		k = k + 4;
		logger.debug("Широта : " + navLatitude);
		navHgeo = ModUtils.getIntU16L(packetData, k);
		k = k + 2;
		logger.debug("Высота : " + navHgeo);
		navCource = ModUtils.getIntU16L(packetData, k);
		k = k + 2;
		logger.debug("Курс : " + navCource);
		navSatellitesCount = packetData[k++];
		logger.debug("Спутники : " + navSatellitesCount);
		navSpeed = ModUtils.getIntU16L(packetData, k);
		k = k + 2;
		logger.debug("Скорость : " + navSpeed);
		navDeviceStatus = packetData[k++];
		logger.debug("Достоверность : " + navDeviceStatus);
		navSNS = packetData[k++];
		logger.debug("СНС : " + navSNS);
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
		imeiString = dfIMEI.format(imei);
		logger.debug("IMEI пакета : " + imeiString);
		return imeiString;
	}

	private void parseHeader() {
		int pr = 0xcc;
		for (int i = 0; i < 4; i++) {
			if ((packetHeader[i] & 0xff) != pr) {
				throw new RuntimeException("Неверная преамбула : "
						+ packetHeader[i]);
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
		navIMEI = "";
		for (int i = 0; i < 15; i++) {
			navIMEI = navIMEI + (char) packetHadnshake[k];
			k++;
		}
		navSoftVersion = "";
		for (int i = 0; i < 3; i++) {
			navSoftVersion = navSoftVersion + (char) packetHadnshake[k];
			k++;
		}

		logger.debug("IMEI терминала : " + navIMEI);
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
		return iDsLocal;
	}

	/**
	 * @param iDsLocal
	 *            the iDsLocal to set
	 */
	public void setiDsLocal(DataInputStream iDsLocal) {
		this.iDsLocal = iDsLocal;
	}

	private int readByte() throws IOException {
		byte bread = iDsLocal.readByte();
		int packet = bread & 0xff;
		logger.debug("packet[" + readbytes + "] : "
				+ Integer.toHexString(packet));
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
