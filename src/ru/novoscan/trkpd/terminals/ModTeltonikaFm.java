package ru.novoscan.trkpd.terminals;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;

import javax.management.RuntimeErrorException;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.resources.ModConstats;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.ModUtils;
import ru.novoscan.trkpd.utils.TrackPgUtils;

public class ModTeltonikaFm implements ModConstats {
	static Logger logger = Logger.getLogger(ModTeltonikaFm.class);

	// private int navPacketSize;

	private int navPacketType;

	private int navDeviceStatus;

	private float navLatitude;

	private float navLongitude;

	// private float navHeight;

	private float navSpeed;

	private float navCource;

	// private float navHdop;

	private int navSatellitesCount;

	private float navTemp;

	private final static int TYPE_DATA_NOT_ACK = 0x00;

	private final static int TYPE_DATA_ACK = 0x01;

	private final static int TYPE_ACK = 0x02;

	private byte[] packetData;

	private HashMap<String, String> map = new HashMap<String, String>();

	private int fullreadbytes;

	private int maxPacketSize;

	private TrackPgUtils pgcon;

	private ModConfig conf;

	private byte[] askPacket = new byte[7];

	private DatagramSocket clientSocket;

	private DatagramPacket askDatagram;

	private int navPacketId;

	private String navIMEI;

	private int avlCodecId;

	private String navDateTime;

	private String navPriority;

	private int navHgeo;

	// private String navData;

	private int readbytes;

	private int avlDataCount;

	private DatagramPacket dataPacket;

	private int navAvlId;

	private Date date = new Date();

	private HashMap<Integer, BigInteger> values = new HashMap<>();

	private final ModUtils utils = new ModUtils();

	public ModTeltonikaFm(DatagramPacket dataPacket,
			DatagramSocket clientSocket, ModConfig conf, TrackPgUtils pgcon) {
		this.conf = conf;
		this.pgcon = pgcon;
		TrackPgUtils.setDateSqlFormat(SQL_DATE_FORMAT);
		this.dataPacket = dataPacket;
		this.clientSocket = clientSocket;
		logger.debug("Чтение потока..");
		maxPacketSize = conf.getMaxSize();
		fullreadbytes = 0;
		readbytes = 0;
		packetData = dataPacket.getData();
		try {
			int packetLength = (readByte() << 8) + readByte();
			logger.debug("Lenght : " + packetLength);
			if (packetLength > dataPacket.getLength()
					|| packetLength > maxPacketSize) {
				throw new RuntimeErrorException(new Error("Lenght Error"),
						"Неверный размер пакета.");
			}
			navPacketId = (readByte() << 8) + readByte();
			logger.debug("Id : " + navPacketId);
			navPacketType = readByte();
			logger.debug("Type : " + navPacketType);
			if (navPacketType == TYPE_DATA_ACK
					|| navPacketType == TYPE_DATA_NOT_ACK) {
				parsePacket();
				if (avlDataCount == (readByte() + (readByte() << 8))) {
					logger.debug("Успешная обработка : " + avlDataCount
							+ " пакетов данных.");

				} else {
					logger.error("Ошибка! Количество обработанных пакетов не равно количеству зявленных : "
							+ avlDataCount);
				}
			} else if (navPacketType == TYPE_ACK) {
				logger.debug("Пакет ACK.");
			}
			if (navPacketType == TYPE_DATA_ACK) {
				sendAck();
			}

			fullreadbytes = packetLength;
		} catch (Exception e) {
			logger.warn("Ошибка : " + e.getMessage());
		}

	}

	private void parsePacket() {
		navAvlId = readByte();
		logger.debug("AVL packet id : " + navAvlId);
		int of = (readByte() << 8) + readByte();
		logger.debug("0x000F : " + of);
		values.clear();
		parseIMEI();
		parseCodecID();
		logger.debug("CodecId : " + avlCodecId);
		if (avlCodecId == 8) {
			avlDataCount = readByte();
			logger.debug("NumberOfData : " + avlDataCount);
			for (int i = 0; i < avlDataCount; i++) {
				logger.debug("Обработка пакета : " + i);
				values.clear();
				map.clear();
				getGnss();
				getIO();
				if (navDeviceStatus == 1) {
					map.put("vehicleId", navIMEI);
					map.put("dasnUid", navIMEI);
					map.put("dasnDateTime", navDateTime);
					map.put("dasnLatitude", String.valueOf(navLatitude));
					map.put("dasnLongitude", String.valueOf(navLongitude));
					map.put("dasnStatus", String.valueOf(navDeviceStatus));
					map.put("dasnSatUsed", String.valueOf(navSatellitesCount));
					map.put("dasnZoneAlarm", null);
					map.put("dasnMacroId", null);
					map.put("dasnMacroSrc", null);
					map.put("dasnSog", String.valueOf(navSpeed));
					map.put("dasnCource", String.valueOf(navCource));
					map.put("dasnHdop", null);
					map.put("dasnHgeo", String.valueOf(navHgeo));// Высота
					map.put("dasnHmet", null);
					// map.put("dasnGpio", String.valueOf(navGpio));
					// map.put("dasnAdc", String.valueOf(navAdc));
					map.put("dasnGpio", null);
					map.put("dasnAdc", null);
					map.put("dasnTemp", String.valueOf(navTemp));
					map.put("i_spmt_id", Integer.toString(conf.getModType()));
					// запись в БД
					pgcon.setDataSensorValues(map, values);
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
		} else {
			logger.warn("Codec incorrect: " + avlCodecId);
		}
	}

	private void getIO() {
		int eventElemetID = readByte();
		logger.debug("Событие : " + eventElemetID);
		int eventCount = readByte();
		logger.debug("Всего событий : " + eventCount);
		int byteLength = 1;
		for (int i = 0; i < 4; i++) {
			logger.debug("Событий " + byteLength + " байтовых : ");
			int infoCount = readByte();
			for (int m = 0; m < infoCount; m++) {
				int id = readId();
				int value = readValue(byteLength);
				values.put(id, BigInteger.valueOf(value));
			}
			byteLength = 2 * byteLength;
		}
	}

	private int readId() {
		int id = readByte();
		return id;
	}

	private int readValue(int byteLength) {
		int data = 0;
		int len = byteLength - 1;
		for (int i = 0; i < byteLength; i++) {
			data = data + (readByte() << ((len - i) * 8));
		}
		return data;
	}

	private void parseCodecID() {
		avlCodecId = readByte();
	}

	private void parseIMEI() {
		navIMEI = "";
		for (int i = 0; i < 15; i++) {
			navIMEI = navIMEI + (char) readByte();
		}
		logger.debug("IMEI : " + navIMEI);
		if (!checkIMEI()) {
			logger.warn("IMEI incorrect");
		}

	}

	private void getGnss() {
		navDateTime = getDateTime();
		navPriority = String.valueOf(readByte()); // приоритет
		logger.debug("Приоритет : " + navPriority);
		navLongitude = ModUtils.getDegreeFromInt(getIntU32());
		logger.debug("Долгота : " + navLongitude);
		navLatitude = ModUtils.getDegreeFromInt(getIntU32());
		logger.debug("Широта : " + navLatitude);
		navHgeo = getIntU16();
		logger.debug("Высота : " + navHgeo);
		navCource = getIntU16();
		logger.debug("Курс : " + navCource);
		navSatellitesCount = readByte();
		logger.debug("Спутники : " + navSatellitesCount);
		navSpeed = getIntU16();
		logger.debug("Скорость : " + navSpeed);
		navDeviceStatus = 1;
	}

	private void sendAck() throws IOException {
		logger.debug("Отправка подтверждения.");
		askPacket[0] = 0x00;
		askPacket[1] = 0x05;
		askPacket[2] = packetData[2];
		askPacket[3] = packetData[3];
		askPacket[4] = TYPE_DATA_ACK;
		askPacket[5] = (byte) navAvlId;
		askPacket[6] = (byte) avlDataCount;
		askDatagram = new DatagramPacket(askPacket, askPacket.length,
				dataPacket.getAddress(), dataPacket.getPort());
		clientSocket.send(askDatagram);
	}

	private String getDateTime() {
		double timestamp = 0;
		for (int i = 0; i < 8; i++) {
			timestamp = timestamp + readByte() * Math.pow(2, ((7 - i) * 8));
		}
		date.setTime((long) timestamp);
		return utils.getDate(date);
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

	private int readByte() {
		byte bread = packetData[readbytes];
		int packet = bread & 0xff;
		logger.debug("packet[" + readbytes + "] : "
				+ Integer.toHexString(packet));
		readbytes++;
		fullreadbytes++;
		return packet;
	}

	public int getIntU16() {
		int data = 0;
		for (int i = 0; i < 2; i++) {
			data = data + (readByte() << (8 * (1 - i)));
		}
		return data;
	}

	public int getIntU32() {
		int data = 0;
		for (int i = 0; i < 4; i++) {
			data = data + (readByte() << (8 * (3 - i)));
		}
		return data;
	}
}
