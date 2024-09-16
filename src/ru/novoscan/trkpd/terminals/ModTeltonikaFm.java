package ru.novoscan.trkpd.terminals;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.sql.SQLException;

import javax.management.RuntimeErrorException;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.domain.Terminal;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.ModUtils;
import ru.novoscan.trkpd.utils.TrackPgUtils;

public class ModTeltonikaFm extends Terminal {
	static Logger logger = Logger.getLogger(ModTeltonikaFm.class);

	// private int navPacketSize;

	private int navPacketType;

	private final static int TYPE_DATA_NOT_ACK = 0x00;

	private final static int TYPE_DATA_ACK = 0x01;

	private final static int TYPE_ACK = 0x02;

	private final static String ADC_KEY = "66";

	private byte[] packetData;

	private int fullreadbytes;

	private int maxPacketSize;

	private TrackPgUtils pgcon;

	private byte[] askPacket = new byte[7];

	private DatagramSocket clientSocket;

	private DatagramPacket askDatagram;

	private int navPacketId;

	private int avlCodecId;

	private String navPriority;

	// private String navData;

	private int readbytes;

	private int avlDataCount;

	private DatagramPacket dataPacket;

	private int navAvlId;

	private String uid;

	public ModTeltonikaFm(DatagramPacket dataPacket,
			DatagramSocket clientSocket, ModConfig conf, TrackPgUtils pgcon)
			throws IOException, SQLException {
		this.setDasnType(conf.getModType());
		this.pgcon = pgcon;
		this.dataPacket = dataPacket;
		this.clientSocket = clientSocket;
		logger.debug("Чтение потока..");
		maxPacketSize = conf.getMaxSize();
		fullreadbytes = 0;
		readbytes = 0;
		packetData = dataPacket.getData();
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
				logger.error(
						"Ошибка! Количество обработанных пакетов не равно количеству зявленных : "
								+ avlDataCount);
			}
		} else if (navPacketType == TYPE_ACK) {
			logger.debug("Пакет ACK.");
		}
		if (navPacketType == TYPE_DATA_ACK) {
			sendAck();
		}

		fullreadbytes = packetLength;

	}

	private void parsePacket() throws SQLException {
		navAvlId = readByte();
		logger.debug("AVL packet id : " + navAvlId);
		int of = (readByte() << 8) + readByte();
		logger.debug("0x000F : " + of);
		parseIMEI();
		parseCodecID();
		logger.debug("CodecId : " + avlCodecId);
		if (avlCodecId == 8) {
			avlDataCount = readByte();
			logger.debug("NumberOfData : " + avlDataCount);
			for (int i = 0; i < avlDataCount; i++) {
				dasnUid = uid;
				logger.debug("Обработка пакета : " + i);
				getGnss();
				getIO();
				if (dasnStatus == NavigationStatus.OK) {
					dataSensor.setDasnDatetime(dasnDatetime);
					dataSensor.setDasnUid(dasnUid);
					dataSensor.setDasnLatitude(dasnLatitude);
					dataSensor.setDasnLongitude(dasnLongitude);
					dataSensor.setDasnStatus(1L);
					dataSensor.setDasnSatUsed(dasnSatUsed);
					dataSensor.setDasnSog(dasnSog);
					dataSensor.setDasnCourse(dasnCourse);

					dataSensor.setDasnHgeo(dasnHgeo);// Высота

					if (dasnValues.containsKey(ADC_KEY)) {
						dataSensor.setDasnAdc(
								Long.valueOf(dasnValues.get(ADC_KEY)));
					}
					dataSensor.setDasnTemp(dasnTemp);
					dataSensor.setDasnValues(dasnValues);
					// запись в БД
					pgcon.setDataSensorValues(dataSensor);
					// Ответ блоку
					pgcon.addDataSensor();
					this.clear();
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
				dasnValues.put(String.valueOf(id), String.valueOf(value));
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
		uid = "";
		for (int i = 0; i < 15; i++) {
			uid = uid + (char) readByte();
		}
		logger.debug("IMEI : " + uid);
		if (!checkIMEI()) {
			logger.warn("IMEI incorrect");
		}

	}

	private void getGnss() {
		parseDateTime();
		navPriority = String.valueOf(readByte()); // приоритет
		logger.debug("Приоритет : " + navPriority);
		dasnLongitude = Double.valueOf(ModUtils.getDegreeFromInt(getIntU32()));
		logger.debug("Долгота : " + dasnLongitude);
		dasnLatitude = Double.valueOf(ModUtils.getDegreeFromInt(getIntU32()));
		logger.debug("Широта : " + dasnLatitude);
		dasnHgeo = (double) getIntU16();
		logger.debug("Высота : " + dasnHgeo);
		dasnCourse = (double) getIntU16();
		logger.debug("Курс : " + dasnCourse);
		dasnSatUsed = (long) readByte();
		logger.debug("Спутники : " + dasnSatUsed);
		dasnSog = (double) getIntU16();
		logger.debug("Скорость : " + dasnSog);
		dasnStatus = NavigationStatus.OK;
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

	private void parseDateTime() {
		double timestamp = 0;
		for (int i = 0; i < 8; i++) {
			timestamp = timestamp + readByte() * Math.pow(2, ((7 - i) * 8));
		}
		timestamp = timestamp - TZ_OFFSET;
		dasnDatetime.setTime((long) timestamp);
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
		logger.debug(
				"packet[" + readbytes + "] : " + Integer.toHexString(packet));
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
