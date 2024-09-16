package ru.novoscan.trkpd.terminals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.Date;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.domain.Terminal;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.TrackPgUtils;

/**
 * @author kur
 * 
 */
public class ModGalileoSky extends Terminal {

	static Logger logger = Logger.getLogger(ModGalileoSky.class);

	private int readbytes;

	private float fullreadbytes = 0;

	private static int[] packet;

	private static int packetMaxLength = 1003; // максимальная длина пакета с
												// заголовком

	private static int packetHeaderLength = 3; // максимальная длина пакета с

	private String navIMEI = "";

	// private String navPhone;

	// private String navPass;

	private int packetCount;

	private int[] packetHeader = new int[packetHeaderLength];

	private int packetSize;

	private int[] responceData = new int[packetHeaderLength];

	private final int PACKET_HEAD = 0x01;

	private final int PACKET_MAIN = 0x02;

	private DataInputStream iDsLocal;

	private int navVersion;

	private int navVersionRaw;

	private static TrackPgUtils pgcon;

	public ModGalileoSky(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader unbconsole, ModConfig conf, TrackPgUtils pgcon)
			throws IOException, SQLException {
		this.setDasnType(conf.getModType());
		ModGalileoSky.pgcon = pgcon;
		iDsLocal = iDs;
		logger.debug("Read streems..");
		fullreadbytes = 0;
		while (true) {
			for (int i = 0; i < packetHeaderLength; i++) {
				packetHeader[i] = readByte();
			}
			// Обработаем заголовок
			parseHeader();
			if (packetSize <= packetMaxLength) {
				String cmd = "";
				// Считаем данные
				for (int i = 0; i < packetSize; i++) {
					packet[i] = readByte();
				}
				// ответим
				/*
				 * Формат ответа: Заголовок 1байт + Контрольная Сумма 2байта
				 */
				parseData();
				writeData();
				sendResponce(oDs, cmd);
				packetCount++;
				readbytes = 0;
			} else {
				logger.error("Неверная длина пакета : " + packetSize);
				logger.debug("Количество принятых пакетов : " + packetCount);
				return;
			}
			/*
			 * // Сохраним в БД данные map.put("vehicleId",
			 * String.valueOf(navDeviceID)); map.put("dasnUid",
			 * String.valueOf(navDeviceID)); map.put("dasnLatitude",
			 * String.valueOf(navLatitude)); map.put("dasnLongitude",
			 * String.valueOf(navLongitude)); map.put("dasnStatus",
			 * Integer.toString(navDeviceStatus)); map.put("dasnSatUsed",
			 * Integer.toString(navSatellitesCount)); map.put("dasnZoneAlarm",
			 * null); map.put("dasnMacroId", null); map.put("dasnMacroSrc",
			 * null); map.put("dasnSog", String.valueOf(navSpeed));
			 * map.put("dasnCource", String.valueOf(navCource));
			 * map.put("dasnHdop", null); map.put("dasnHgeo", null);
			 * map.put("dasnHmet", null); map.put("dasnGpio", null);
			 * map.put("dasnAdc", String.valueOf(navPower)); map.put("dasnTemp",
			 * String.valueOf(navTemp)); map.put("i_spmt_id",
			 * Integer.toString(conf.getModType())); // запись в БД
			 * pgcon.setDataSensor(map); try { pgcon.addDataSensor();
			 * logger.debug("Write Database OK"); } 
			 * logger.warn("Error Writing Database : " + e.getMessage()); }
			 * map.clear();
			 */
			// sendCRC(oDs, navCRC);
		}

	}

	private void parseData() throws IOException {
		logger.debug("Парсим...");
		int tagType = 0;
		int tagLength = 0;
		for (int i = 0; i < packetSize; i++) {
			getTagType(i, tagType, tagLength);
		}
	}

	/*
	 * Обработка тэгов
	 */

	private void getTagType(int id, int tagType, int tagLength) {
		if (packet[id] == 0x01) {
			tagLength = 1;
			id = id + tagLength;
			navVersion = packet[id];
			logger.debug("Версия : " + navVersion);
		} else if (packet[id] == 0x02) {
			tagLength = 1;
			id = id + tagLength;
			navVersionRaw = packet[id];
			logger.debug("Версия прошивки : " + navVersionRaw);
		} else if (packet[id] == 0x03) {
			tagLength = 15;
			id = id + 1;
			int idMax = id + tagLength;
			for (int i = id; i < idMax; i++) { // первый символ ;
				navIMEI = navIMEI + (char) packet[i];
				id++;
			}
			logger.debug("IMEI терминала : " + navIMEI);
		} else if (packet[id] == 0x04) {
			tagLength = 2;
			id = id + 1;
			dasnUid = String.valueOf(packet[id] + (packet[id + 1] << 8));
			id = id + tagLength;
			logger.debug("Идентификатор : " + dasnUid);
		} else if (packet[id] == 0x10) {
			tagLength = 2;
			id = id + 1;
			int navCount = packet[id] + (packet[id + 1] << 8);
			id = id + tagLength;
			logger.debug("Номер записи в архиве : " + navCount);
		} else if (packet[id] == 0x20) {
			tagLength = 4;
			id = id + 1;
			long seconds = packet[id] + (packet[id + 1] << 8)
					+ (packet[id + 2] << 16) + (packet[id + 2] << 24);
			long millis = seconds * 1000;
			Date navDateTime = new Date(millis);
			id = id + tagLength;
			logger.debug("Дата : " + navDateTime);
		} else if (packet[id] == 0x30) {
		}

	}

	private void sendResponce(DataOutputStream oDs, String cmd)
			throws IOException {
		oDs.write(responceData[0]);
		oDs.write(responceData[1]);
		oDs.write(responceData[2]);
		// oDs.flush();

	}

	private void parseHeader() {
		// Разбор заголовка - 3 Байт.
		if (packetHeader[0] == PACKET_HEAD) {
			logger.debug("Сигнатура HEAD : " + packetHeader[0]);
		} else if (packetHeader[0] == PACKET_MAIN) {
			logger.debug("Сигнатура MAIN : " + packetHeader[0]);
		} else {
			logger.error("Неизвестная сигнатура : " + packetHeader[0]);
			return;
		}
		responceData[0] = packetHeader[0];
		packetSize = (int) (packetHeader[1] & 0xff)
				+ (int) ((packetHeader[2] & 0xff) << 7);
		logger.debug("Размер пакета данных : " + packetSize);
	}

	private int readByte() throws IOException {
		byte bread = iDsLocal.readByte();
		packet[readbytes] = bread;
		logger.debug("packet[" + readbytes + "] : "
				+ Integer.toHexString(packet[readbytes]));
		readbytes++;
		fullreadbytes++;
		return bread;
	}

	public float getReadBytes() {
		return fullreadbytes;
	}

	/* Table of CRC values for high–order byte */
	private final int auchCRCHi[] = { 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80,
			0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0,
			0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40, 0x01,
			0xC0, 0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40,
			0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81,
			0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1,
			0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x00,
			0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40,
			0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81,
			0x40, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0,
			0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x00,
			0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41,
			0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81,
			0x40, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0,
			0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x00,
			0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0, 0x80, 0x41,
			0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81,
			0x40, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1,
			0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00,
			0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40,
			0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0, 0x80,
			0x41, 0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0,
			0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01,
			0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40 };

	/* Table of CRC values for low–order byte */
	private final int auchCRCLo[] = { 0x00, 0xC0, 0xC1, 0x01, 0xC3, 0x03, 0x02,
			0xC2, 0xC6, 0x06, 0x07, 0xC7, 0x05, 0xC5, 0xC4, 0x04, 0xCC, 0x0C,
			0x0D, 0xCD, 0x0F, 0xCF, 0xCE, 0x0E, 0x0A, 0xCA, 0xCB, 0x0B, 0xC9,
			0x09, 0x08, 0xC8, 0xD8, 0x18, 0x19, 0xD9, 0x1B, 0xDB, 0xDA, 0x1A,
			0x1E, 0xDE, 0xDF, 0x1F, 0xDD, 0x1D, 0x1C, 0xDC, 0x14, 0xD4, 0xD5,
			0x15, 0xD7, 0x17, 0x16, 0xD6, 0xD2, 0x12, 0x13, 0xD3, 0x11, 0xD1,
			0xD0, 0x10, 0xF0, 0x30, 0x31, 0xF1, 0x33, 0xF3, 0xF2, 0x32, 0x36,
			0xF6, 0xF7, 0x37, 0xF5, 0x35, 0x34, 0xF4, 0x3C, 0xFC, 0xFD, 0x3D,
			0xFF, 0x3F, 0x3E, 0xFE, 0xFA, 0x3A, 0x3B, 0xFB, 0x39, 0xF9, 0xF8,
			0x38, 0x28, 0xE8, 0xE9, 0x29, 0xEB, 0x2B, 0x2A, 0xEA, 0xEE, 0x2E,
			0x2F, 0xEF, 0x2D, 0xED, 0xEC, 0x2C, 0xE4, 0x24, 0x25, 0xE5, 0x27,
			0xE7, 0xE6, 0x26, 0x22, 0xE2, 0xE3, 0x23, 0xE1, 0x21, 0x20, 0xE0,
			0xA0, 0x60, 0x61, 0xA1, 0x63, 0xA3, 0xA2, 0x62, 0x66, 0xA6, 0xA7,
			0x67, 0xA5, 0x65, 0x64, 0xA4, 0x6C, 0xAC, 0xAD, 0x6D, 0xAF, 0x6F,
			0x6E, 0xAE, 0xAA, 0x6A, 0x6B, 0xAB, 0x69, 0xA9, 0xA8, 0x68, 0x78,
			0xB8, 0xB9, 0x79, 0xBB, 0x7B, 0x7A, 0xBA, 0xBE, 0x7E, 0x7F, 0xBF,
			0x7D, 0xBD, 0xBC, 0x7C, 0xB4, 0x74, 0x75, 0xB5, 0x77, 0xB7, 0xB6,
			0x76, 0x72, 0xB2, 0xB3, 0x73, 0xB1, 0x71, 0x70, 0xB0, 0x50, 0x90,
			0x91, 0x51, 0x93, 0x53, 0x52, 0x92, 0x96, 0x56, 0x57, 0x97, 0x55,
			0x95, 0x94, 0x54, 0x9C, 0x5C, 0x5D, 0x9D, 0x5F, 0x9F, 0x9E, 0x5E,
			0x5A, 0x9A, 0x9B, 0x5B, 0x99, 0x59, 0x58, 0x98, 0x88, 0x48, 0x49,
			0x89, 0x4B, 0x8B, 0x8A, 0x4A, 0x4E, 0x8E, 0x8F, 0x4F, 0x8D, 0x4D,
			0x4C, 0x8C, 0x44, 0x84, 0x85, 0x45, 0x87, 0x47, 0x46, 0x86, 0x82,
			0x42, 0x43, 0x83, 0x41, 0x81, 0x80, 0x40 };

	@SuppressWarnings("unused")
	private void getCRC16(char puchMsg, int usDataLen) {
		/* The function returns the CRC as a unsigned short type */
		/* puchMsg message to calculate CRC upon */
		/* usDataLen quantity of bytes in message */
		/* high byte of CRC initialized */
		int uchCRCLo = 0xFF; /* low byte of CRC initialized */
		int uIndex; /* will index into CRC lookup table */
		int uchCRCHi = 0xFF;

		while (usDataLen > 0) /* pass through message buffer */
		{
			/* calculate the CRC */
			usDataLen--;
			uchCRCHi = 0xFF;
			uIndex = uchCRCLo ^ puchMsg++;
			uchCRCLo = uchCRCHi ^ auchCRCHi[uIndex];
			uchCRCHi = auchCRCLo[uIndex];
		}
		responceData[1] = uchCRCHi;
		responceData[2] = uchCRCLo;
	}

	private void writeData() throws SQLException {
		// Сохраним в БД данные
		dataSensor.setDasnUid(dasnUid);
		dataSensor.setDasnDatetime(dasnDatetime);
		dataSensor.setDasnLatitude(dasnLatitude);
		dataSensor.setDasnLongitude(dasnLongitude);
		dataSensor.setDasnSatUsed(dasnSatUsed);
		dataSensor.setDasnSog(dasnSog);
		dataSensor.setDasnCourse(dasnCourse);
		dataSensor.setDasnHgeo(dasnHgeo);
		dataSensor.setDasnHmet(dasnHmet);
		dataSensor.setDasnAdc(dasnAdc);
		dataSensor.setDasnGpio(dasnGpio);
		dataSensor.setDasnTemp(dasnTemp);
		//
		dataSensor.setDasnMacroId(dasnMacroId);
		dataSensor.setDasnMacroSrc(dasnMacroSrc);
		//
		pgcon.setDataSensorValues(dataSensor);
		pgcon.addDataSensor();
		this.clear();
	}

}
