package ru.novoscan.trkpd.terminals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.ParseException;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.domain.Terminal;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.TrackPgUtils;

public class ModScoutOpen extends Terminal {
	/*	
*/
	static Logger logger = Logger.getLogger(ModScoutOpen.class);

	private float fullreadbytes;

	private final static int READ_OK = 0x55;

	private static final RuntimeException InvalidLength = new RuntimeException(
			"Превышение размера пакета.");

	private int[] packet = new int[SCOUT_MAX_PACKET_LENGTH];

	private DataInputStream iDsLocal;

	private int readbytes;

	private final TrackPgUtils pgcon;

	private final DataOutputStream oDs;

	private long navStat0;

	private long navStat1;

	private String navAdc0;

	private String navAdc1;

	public ModScoutOpen(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader unbconsole, ModConfig conf, TrackPgUtils pgcon)
			throws IOException, ParseException {
		this.pgcon = pgcon;
		this.oDs = oDs;
		iDsLocal = iDs;
		logger.debug("Read streems..");
		fullreadbytes = 0;
		readbytes = 0;
		while (true) {
			int packetCount = 0;
			for (int i = 0; i < INT32; i++) {
				packetCount = (readByte() << (i * 8)) + packetCount;
			}
			logger.debug("Количество пакетов : " + packetCount);
			for (int k = 0; k < packetCount; k++) {
				int lenghtId = readByte();
				dasnUid = "";
				logger.debug("Длина ID терминала : " + lenghtId);
				for (int i = 0; i < lenghtId; i++) {
					dasnUid = dasnUid + (char) readByte();
				}
				logger.debug("ID терминала : " + dasnUid);
				int protocolId = 0;
				for (int i = 0; i < INT32; i++) {
					protocolId = (readByte() << (i * 8)) + protocolId;
				}
				logger.debug("Тип оборудования : " + protocolId);
				/*
				 * ScoutMT500 = 1, ScoutMT501 = 2, Voyager = 3, Unknown = 4,
				 * TeltonikaFM4XXX = 5, Omega = 6, Autograph = 7, ScoutRx = 8,
				 * Voyager2 = 9, Tr102 = 10, LineGuard = 11, Gelix = 12,
				 * IgevskTM4 = 13, ScoutMT600 = 14, TeltonikaGH1201 = 15,
				 * NaviTechUTP4 = 16, ScoutMT510 = 17, Portman = 18, Fort300 =
				 * 200, FalcomStepp2 = 201, TitanT10 = 202, AutographWiFi = 203,
				 * Autograph2 = 204, ScoutMt600Test = 205, Galileo = 206, Hiton
				 * = 207, SkyWave = 208, SkyWaveFtpAndConecte = 209,
				 * TeltonikaGh3000 = 210, Azimut = 211, Aplicom = 212, Granit7 =
				 * 213, ArkanMt5 = 214, Tr203 = 215, Granit4 = 217, NovacomGns =
				 * 218,
				 */
				long timestamp = 0;
				for (int i = 0; i < INT64; i++) {
					timestamp = (((long) readByte()) << (i * 8)) + timestamp;
				}
				timestamp = (timestamp - TICKS_AT_EPOCH)
						/ TICKS_PER_MILLISECOND;
				dasnDatetime.setTime(timestamp - TZ_OFFSET);
				logger.debug("Дата : " + dasnDatetime);

				int longitude = 0;
				for (int i = 0; i < INT32; i++) {
					longitude = (readByte() << (i * 8)) + longitude;
				}
				dasnLongitude =  (double) Float.intBitsToFloat(longitude);
				logger.debug("Широта : " + dasnLongitude);

				int latitude = 0;
				for (int i = 0; i < INT32; i++) {
					latitude = (readByte() << (i * 8)) + latitude;
				}
				dasnLatitude = (double) Float.intBitsToFloat(latitude);
				logger.debug("Долгота : " + dasnLatitude);

				int speed = 0;
				for (int i = 0; i < INT32; i++) {
					speed = (readByte() << (i * 8)) + speed;
				}
				dasnSog = (double) Float.intBitsToFloat(speed);
				logger.debug("Скорость : " + dasnSog);
				dasnCourse = (double) (readByte() + (readByte() << 8));
				logger.debug("Курс : " + dasnCourse);
				dasnGpio = (long) (readByte() + (readByte() << 8));
				logger.debug("IO : " + dasnGpio);
				navAdc0 =  String.valueOf(readByte() + (readByte() << 8));
				logger.debug("ADC0 : " +  navAdc0);
				navAdc1 =  String.valueOf(readByte() + (readByte() << 8));
				logger.debug("ADC1 : " +  navAdc1);
				navStat0 = readByte() + (readByte() << 8);
				logger.debug("Stat0 : " + navStat0);
				navStat1 = readByte() + (readByte() << 8);
				logger.debug("Stat1 : " + navStat1);
				dasnSatUsed = navStat1 & 0x1f;
				logger.debug("Спутники : " + dasnSatUsed);
				if (((navStat1 & 0xff) >> 5) > 0) {
					dasnStatus = DATA_STATUS.OK;
				} else {
					dasnStatus = DATA_STATUS.ERR;
				}
				logger.debug("Статус GPS : " + dasnStatus);
				int dataLen = readByte();
				int dataSize = 0;
				if ((dataLen >> 7) == 0) {
					dataSize = dataLen;
				} else {
					dataSize = (dataLen & 0x7F) + (readByte() << 7);
				}
				logger.debug("Длина данных : " + dataSize);
				int stringLen = Integer.parseInt(readChar(), 16);
				logger.debug("Строка длиной : " + stringLen);
				String data = "";
				for (int n = 0; n < stringLen; n++) {
					data = data + readChar();
				}
				logger.debug("Датчики : " + data);
				parseData(data);
				writeData();
			}

		}
	}

	private void parseData(String data) throws ArrayIndexOutOfBoundsException {
		int seek = 0;
		int typeCode = 0;
		int id = 0;
		int value = 0;
		int endIndex = 0;
		int lenght = 0;
		
		try {
			while (seek < data.length()) {
				logger.debug("Ид : " + id);
				endIndex = seek + 2;
				typeCode = Integer.valueOf(data.substring(seek, endIndex));
				logger.debug(" Тип : " + typeCode);
				seek = endIndex;
				endIndex = seek + 2;
				lenght = Integer.valueOf(data.substring(seek, endIndex));
				logger.debug(" Длина : " + typeCode);
				seek = endIndex;
				endIndex = seek + 2 * lenght;
				value = Integer.parseInt(data.substring(seek, endIndex), 16);
				logger.debug(" Значение : " + value);
				dasnValues.put(String.valueOf(id), String.valueOf(value));
				seek = endIndex;
				id++;
			}
		} catch (NumberFormatException nfe) {
			logger.error("Неверное значение : " + value);
			seek = data.length();
		}

	}

	public float getReadBytes() {
		return fullreadbytes;
	}

	private int readByte() throws IOException, ArrayIndexOutOfBoundsException,
			RuntimeException {
		int bread = iDsLocal.readByte() & 0xff;
		packet[readbytes] = bread;
		logger.debug("packet[" + readbytes + "] : "
				+ Integer.toHexString(packet[readbytes]));
		readbytes++;
		fullreadbytes++;
		if (readbytes > SCOUT_MAX_PACKET_LENGTH) {
			throw InvalidLength;
		}

		return bread;
	}

	private String readChar() throws IOException {
		String cread = (char) readByte() + "" + (char) readByte();
		return cread;
	}

	private void writeData() throws IOException, ParseException {
		// Сохраним в БД данные
		dataSensor.setDasnDatetime(dasnDatetime);
		dataSensor.setDasnUid(dasnUid);
		dataSensor.setDasnLatitude(dasnLatitude);
		dataSensor.setDasnLongitude(dasnLongitude);
		dataSensor.setDasnSog(dasnSog);
		dataSensor.setDasnSatUsed(dasnSatUsed);
		dataSensor.setDasnCourse(dasnCourse);
		dataSensor.setDasnGpio(dasnGpio);
		dasnValues.put("Adc0", navAdc0);
		dasnValues.put("Adc1", navAdc1);
		dataSensor.setDasnValues(dasnValues);
		pgcon.setDataSensorValues(dataSensor);
		try {
			pgcon.addDataSensor();
			logger.debug("Write Database OK");

		} catch (SQLException e) {
			logger.warn("Error Writing Database : " + e.getMessage());
		}
		this.clear();
		oDs.write(READ_OK);
		readbytes = 0;
	}

}
