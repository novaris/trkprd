package ru.novoscan.trkpd.terminals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.resources.ModConstats;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.ModUtils;
import ru.novoscan.trkpd.utils.TrackPgUtils;

/**
 * @author kur
 * 
 */
public class ModMajak implements ModConstats {
	private String navDeviceID;

	// private int navPacketSize;

	private int navPacketType;

	private int navSoftVersion;

	private int navDeviceStatus;

	private int navValid;

	private long navTime;

	private long navDate;

	private float navLatitude;

	private float navLongitude;

	// private float navHeight;

	private float navSpeed;

	private float navCource;

	// private float navHdop;

	private int navSatellitesCount;

	private int navCRC;

	// private int navCheckCRC;

	// private float navAcc;

	private int navPower;

	private float navTemp;

	static Logger logger = Logger.getLogger(ModMajak.class);

	private int readbytes;

	private float fullreadbytes = 0;

	// private static int maxPacketSize = 18; // длина пакета авторизации

	private HashMap<String, String> map = new HashMap<String, String>();

	private ModUtils utl = new ModUtils();

	private int[] packet;

	private static int packetDataLength = 34; // длина пакета с данными

	private String navIMEI;

	private String navPhone;

	private String navPass;

	private int packetCount;

	private SimpleDateFormat sdf = new SimpleDateFormat(DATE_SIMPLE_FORMAT);

	public ModMajak(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader unbconsole, ModConfig conf, TrackPgUtils pgcon)
			throws ParseException {
		int cread;
		logger.debug("Read data..");
		packet = new int[packetDataLength]; // пакет с нулевого считается
		fullreadbytes = 0;
		packetCount = 0;
		try {
			while (true) {
				fullreadbytes++;
				readbytes = 0;
				cread = readByte(iDs);
				navPacketType = cread;
				if (navPacketType == 0x41) {
					// пакет авторизации - читаем 17 байт
					navIMEI = "";
					for (int i = 1; i < 9; i++) {
						cread = readByte(iDs);
						navIMEI = navIMEI
								+ String.valueOf(cread >> 4).toString()
								+ String.valueOf(cread & 0x0f).toString();
					}
					navIMEI = navIMEI.substring(1);
					logger.debug("IMEI : " + navIMEI);
					navDeviceID = navIMEI;
					logger.debug("navDeviceID : " + navDeviceID);
					cread = readByte(iDs);
					logger.debug("Protocol : "
							+ String.valueOf(cread >> 4).toString()
							+ " Hardware: "
							+ String.valueOf(cread & 0x0f).toString());
					cread = readByte(iDs);
					navSoftVersion = cread;
					logger.debug("Version : " + (char) navSoftVersion);
					navPhone = "";
					for (int i = 1; i < 6; i++) {
						cread = readByte(iDs);
						navPhone = navPhone
								+ String.valueOf(cread >> 4).toString()
								+ String.valueOf(cread & 0x0f).toString();
					}
					logger.debug("Phone : " + navPhone);
					navPass = "";
					for (int i = 1; i < 3; i++) {
						cread = readByte(iDs);
						navPass = navPass
								+ String.valueOf(cread >> 4).toString()
								+ String.valueOf(cread & 0x0f).toString();
					}
					logger.debug("Password : " + navPass);
					cread = readByte(iDs);
					navCRC = cread;
					logger.debug("CRC : " + navCRC);
					sendCRC(oDs, navCRC);
				} else if (navPacketType == 0x02) {
					// пакет данных - читаем 33 байта
					map.clear();
					packetCount++;
					logger.debug("----------------------");
					for (int i = 1; i < packetDataLength; i++) {
						packet[i] = readByte(iDs);
					}
					navPower = packet[1];
					logger.debug("Заряд : " + navPower + " %");
					navTemp = packet[4];
					logger.debug("Температура : " + navTemp + " C");
					logger.debug("Интервал пробуждения (0 - без сна) : "
							+ packet[5]);
					logger.debug("Единицы измерения инервала пробуждения (M - минуты, иначе часы) : "
							+ packet[6]);
					logger.debug("Режим работы : "
							+ Integer.toHexString(packet[7]));
					logger.debug("Интервал передачи : " + packet[8] + " сек");
					logger.debug("MCC : " + packet[9]);
					logger.debug("MNC : " + packet[10]);
					logger.debug("LAC : "
							+ Integer
									.toHexString(((packet[11] << 8) + packet[12])));
					logger.debug("CID : "
							+ Integer
									.toHexString(((packet[13] << 8) + packet[14])));
					navValid = packet[15] >> 6;
					logger.debug("GPS статус : " + navValid);
					navSatellitesCount = packet[15] & 0x3f;
					logger.debug("Количество спутников : " + navSatellitesCount);
					navTime = (packet[16] << 16) + (packet[17] << 8)
							+ packet[18];
					navDate = (packet[19] << 16) + (packet[20] << 8)
							+ packet[21];
					String navDateTime = utl.getLastnCharacters("000000"
							+ String.valueOf(navDate), 6)
							+ utl.getLastnCharacters(
									"000000" + String.valueOf(navTime), 6);
					logger.debug("DateTime : " + navDateTime);
					navSpeed = ((float) packet[30]) * utl.getNmi();
					logger.debug("Скорость : " + String.valueOf(navSpeed));
					navCource = (packet[32] << 8) + packet[31];
					navLatitude = (float) packet[22]
							+ ((float) ((packet[23] << 12) + (packet[24] << 4) + (packet[25] >> 4)))
							/ (600000);
					navLongitude = (float) packet[26]
							+ ((float) ((packet[27] << 12) + (packet[28] << 4) + (packet[29] >> 4)))
							/ (600000);
					;
					logger.debug("Широта (градусы) : "
							+ String.valueOf(navLatitude));
					logger.debug("Долгота (градусы) : "
							+ String.valueOf(navLongitude));
					logger.debug("Курс (градусы) : " + String.valueOf(navSpeed));
					navCRC = packet[33];
					logger.debug("CRC : " + navCRC);
					// Сохраним в БД данные
					map.put("vehicleId", navDeviceID);
					map.put("dasnUid", navDeviceID);
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
					map.put("dasnGpio", null);
					map.put("dasnAdc", String.valueOf(navPower));
					map.put("dasnTemp", String.valueOf(navTemp));
					map.put("i_spmt_id", Integer.toString(conf.getModType()));
					// запись в БД
					pgcon.setDataSensor(map, sdf.parse(navDateTime));
					try {
						pgcon.addDataSensor();
						logger.debug("Write Database OK");
					} catch (SQLException e) {
						logger.warn("Error Writing Database : "
								+ e.getMessage());
					}
					map.clear();
					// sendCRC(oDs, navCRC);
				} else {
					// неверный тип пакета - вызываем исключение
					logger.error("Incorrect packet type : "
							+ Integer.toHexString(cread));
					return;
				}

			}
		} catch (SocketTimeoutException e) {
			logger.error("Close connection : " + e.getMessage());
			logger.debug("Количество принятых пакетов : " + packetCount);
		} catch (IOException e3) {
			logger.warn("IO socket error : " + e3.getMessage());
			logger.debug("Количество принятых пакетов : " + packetCount);
		}

	}

	private void sendCRC(DataOutputStream oDs, int navCRC2) throws IOException {
		// Send Result OK
		int respData[] = { 0x72, 0x65, 0x73, 0x5f, 0x63, 0x72, 0x63, 0x3d,
				navCRC2 };
		for (int i = 0; i < respData.length; i++) {
			oDs.write(respData[i]);
			logger.debug("Write byte[" + i + "] : " + respData[i]);
		}
		oDs.flush();
		logger.debug("Flush OK");
	}

	private int readByte(DataInputStream rDs) throws IOException {
		int bread = rDs.readUnsignedByte();
		packet[readbytes] = bread;
		logger.debug("packet[" + readbytes + "] : " + packet[readbytes]);
		readbytes++;
		fullreadbytes++;
		return bread;
	}

	public float getReadBytes() {
		return fullreadbytes;
	}

}
