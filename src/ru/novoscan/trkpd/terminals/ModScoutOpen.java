package ru.novoscan.trkpd.terminals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.resources.ModConstats;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.ModUtils;
import ru.novoscan.trkpd.utils.TrackPgUtils;

public class ModScoutOpen implements ModConstats {
	/*	
*/
	static Logger logger = Logger.getLogger(ModScoutOpen.class);

	private float fullreadbytes;

	private HashMap<String, String> map = new HashMap<String, String>();

	private final static int readOK = 0x55;

	private int[] packet = new int[32768];

	private DataInputStream iDsLocal;

	private int navSatellitesCount;

	private int readbytes;

	private String navDeviceID;

	private String navDateTime;

	private float navLatitude;

	private float navLongitude;

	private float navSpeed;

	private int navCource;

	private int navGPIO;

	private int navAdc0;

	private int navAdc1;

	private int navStat0;

	private int navStat1;

	private int navDeviceStatus;

	private Date date = new Date();

	private final ModConfig conf;

	private final TrackPgUtils pgcon;

	private final DataOutputStream oDs;

	private HashMap<Integer, BigInteger> values = new HashMap<>();

	private final ModUtils utils = new ModUtils();

	public ModScoutOpen(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader unbconsole, ModConfig conf, TrackPgUtils pgcon) {
		this.conf = conf;
		this.pgcon = pgcon;
		this.oDs = oDs;
		iDsLocal = iDs;
		logger.debug("Read streems..");
		fullreadbytes = 0;
		readbytes = 0;
		TrackPgUtils.setDateSqlFormat(SQL_DATE_FORMAT);
		try {
			while (true) {
				int packetCount = 0;
				for (int i = 0; i < INT32; i++) {
					packetCount = (readByte() << (i * 8)) + packetCount;
				}
				logger.debug("Количество пакетов : " + packetCount);
				for (int k = 0; k < packetCount; k++) {
					map.clear();
					int lenghtId = readByte();
					navDeviceID = "";
					logger.debug("Длина ID терминала : " + lenghtId);
					for (int i = 0; i < lenghtId; i++) {
						navDeviceID = navDeviceID + (char) readByte();
					}
					logger.debug("ID терминала : " + navDeviceID);
					int protocolId = 0;
					for (int i = 0; i < INT32; i++) {
						protocolId = (readByte() << (i * 8)) + protocolId;
					}
					logger.debug("Тип оборудования : " + protocolId);
					/*
					 * ScoutMT500 = 1, ScoutMT501 = 2, Voyager = 3, Unknown = 4,
					 * TeltonikaFM4XXX = 5, Omega = 6, Autograph = 7, ScoutRx =
					 * 8, Voyager2 = 9, Tr102 = 10, LineGuard = 11, Gelix = 12,
					 * IgevskTM4 = 13, ScoutMT600 = 14, TeltonikaGH1201 = 15,
					 * NaviTechUTP4 = 16, ScoutMT510 = 17, Portman = 18, Fort300
					 * = 200, FalcomStepp2 = 201, TitanT10 = 202, AutographWiFi
					 * = 203, Autograph2 = 204, ScoutMt600Test = 205, Galileo =
					 * 206, Hiton = 207, SkyWave = 208, SkyWaveFtpAndConecte =
					 * 209, TeltonikaGh3000 = 210, Azimut = 211, Aplicom = 212,
					 * Granit7 = 213, ArkanMt5 = 214, Tr203 = 215, Granit4 =
					 * 217, NovacomGns = 218,
					 */
					long timestamp = 0;
					for (int i = 0; i < INT64; i++) {
						timestamp = (((long) readByte()) << (i * 8))
								+ timestamp;
					}
					timestamp = (timestamp - TICKS_AT_EPOCH)
							/ TICKS_PER_MILLISECOND;
					date.setTime(timestamp);
					navDateTime = utils.getDate(date);
					logger.debug("Дата : " + navDateTime);

					int longitude = 0;
					for (int i = 0; i < INT32; i++) {
						longitude = (readByte() << (i * 8)) + longitude;
					}
					navLongitude = Float.intBitsToFloat(longitude);
					logger.debug("Широта : " + navLongitude);

					int latitude = 0;
					for (int i = 0; i < INT32; i++) {
						latitude = (readByte() << (i * 8)) + latitude;
					}
					navLatitude = Float.intBitsToFloat(latitude);
					logger.debug("Долгота : " + navLatitude);

					int speed = 0;
					for (int i = 0; i < INT32; i++) {
						speed = (readByte() << (i * 8)) + speed;
					}
					navSpeed = Float.intBitsToFloat(speed);
					logger.debug("Скорость : " + navSpeed);
					navCource = readByte() + (readByte() << 8);
					logger.debug("Курс : " + navCource);
					navGPIO = (readByte() << 8) + (readByte());
					logger.debug("IO : " + navGPIO);
					navAdc0 = readByte() + (readByte() << 8);
					logger.debug("ADC0 значение : " + navAdc0);
					navAdc1 = readByte() + (readByte() << 8);
					logger.debug("ADC1 значение : " + navAdc1);
					navStat0 = readByte() + (readByte() << 8);
					logger.debug("Stat0 значение : " + navStat0);
					navStat1 = readByte() + (readByte() >> 8);
					navSatellitesCount = navStat1 & 0x1f;
					logger.debug("Спутники : " + navSatellitesCount);
					if (((navStat1 & 0xff) >> 5) > 0) {
						navDeviceStatus = 1;
					} else {
						navDeviceStatus = 0;
					}
					logger.debug("Статус GPS : " + navDeviceStatus);
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
					values.clear();
					parseData(data);
					writeData();
				}

			}
		} catch (SocketTimeoutException e) {
			logger.error("Close connection : " + e.getMessage());
		} catch (IOException e) {
			logger.warn("IO socket error : " + e.getMessage());
		} catch (Exception e) {
			logger.fatal("Exception : " + e.toString());
		}
	}

	private void parseData(String data) {
		int seek = 0;
		int typeCode = 0;
		int id = 0;
		int value = 0;

		values.clear();
		while (seek < data.length()) {
			try {
				int endIndex = seek + 2;
				typeCode = Integer.valueOf(data.substring(seek, endIndex));
				seek = endIndex;
				endIndex = seek + 2;
				int lenght = Integer.valueOf(data.substring(seek, endIndex));
				seek = endIndex;
				endIndex = seek + 2 * lenght;
				value = Integer.parseInt(data.substring(seek, endIndex), 16);
				logger.debug("Ид : " + id + " Тип : " + typeCode + " Длина : "
						+ lenght + " Значение : " + value);
				values.put(id, BigInteger.valueOf(value));
				seek = endIndex;
				id++;
			} catch (NumberFormatException nfe) {
				logger.error("Неверное значение : " + value);
				seek = seek + 2;
			}
		}

	}

	public float getReadBytes() {
		return fullreadbytes;
	}

	private int readByte() throws IOException {
		int bread = iDsLocal.readByte() & 0xff;
		packet[readbytes] = bread;
		logger.debug("packet[" + readbytes + "] : "
				+ Integer.toHexString(packet[readbytes]));
		readbytes++;
		fullreadbytes++;
		return bread;
	}

	private String readChar() throws IOException {
		String cread = (char) readByte() + "" + (char) readByte();
		return cread;
	}

	private void writeData() throws IOException {
		// Сохраним в БД данные
		map.put("vehicleId", String.valueOf(navDeviceID));
		map.put("dasnUid", String.valueOf(navDeviceID));
		map.put("dasnDateTime", navDateTime);
		map.put("dasnLatitude", String.valueOf(navLatitude));
		map.put("dasnLongitude", String.valueOf(navLongitude));
		map.put("dasnStatus", Integer.toString(navDeviceStatus));
		map.put("dasnSatUsed", Integer.toString(navSatellitesCount));
		map.put("dasnZoneAlarm", null);
		map.put("dasnMacroId", null);
		map.put("dasnMacroSrc", null);
		map.put("dasnSog", String.valueOf(navSpeed));
		map.put("dasnCource", String.valueOf(navCource));
		map.put("dasnHdop", null);
		map.put("dasnHgeo", null);
		map.put("dasnHmet", null);
		map.put("dasnGpio", String.valueOf(navGPIO));
		map.put("dasnAdc", String.valueOf(navAdc0));
		map.put("dasnTemp", String.valueOf((int) navAdc1));
		map.put("i_spmt_id", Integer.toString(this.conf.getModType())); // запись
																		// в
		pgcon.setDataSensorValues(map, values);
		try {
			pgcon.addDataSensor();
			logger.debug("Write Database OK");

		} catch (SQLException e) {
			logger.warn("Error Writing Database : " + e.getMessage());
		}
		map.clear();
		oDs.write(readOK);
	}

}
