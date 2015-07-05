/**
 * Реализация обработки блока UTP-5
 */
package ru.novoscan.trkpd.terminals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
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
public class ModUtp5 implements ModConstats {

	private int navDeviceID;

	private int navPacketSize;

	private int navPacketType;

	private int navSoftVersion;

	private int navDeviceStatus;

	private int navValid;

	private long navTime;

	private long navDate;

	private float navLatitude;

	private float navLongitude;

	private float navHeight;

	private float navSpeed;

	private float navCource;

	private float navHdop;

	private int navSatellitesCount;

	private int navCRC;

	private int navCheckCRC;

	private float navAcs;

	private float navPower;

	static Logger logger = Logger.getLogger(ModUtp5.class);

	private int readbytes;

	private float fullreadbytes = 0;

	private static int maxPacketSize;

	private HashMap<String, String> map = new HashMap<String, String>();

	private ModUtils utl = new ModUtils();

	private int[] packet;

	private static int packetDataLength = 66; // длина пакета с данными

	private int packetLength; // длина пакета по умолчанию

	private String navIMEI;

	private String getOnePacketOK = "***1*\r\n";

	private String getAllPacketOK;

	private int packetCount;

	private int navBatt;

	private SimpleDateFormat sdf = new SimpleDateFormat(DATE_SIMPLE_FORMAT);

	public ModUtp5(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader unbconsole, ModConfig conf, TrackPgUtils pgcon)
			throws ParseException {
		int cread;
		packetLength = packetDataLength;
		packet = new int[packetDataLength * 2]; // пакет с нулевого считается
		maxPacketSize = conf.getMaxSize();
		navPacketSize = conf.getMaxSize() + 1;
		try {
			// Читаем первый пакет и определяем IMSI
			readFirstPacket(iDs, oDs, pgcon);
			packetCount = 1;
			readbytes = 0;
			while (true) {
				while ((cread = iDs.readByte()) != -1) {
					fullreadbytes = fullreadbytes + 1;
					if (readbytes > maxPacketSize) {
						logger.error("Over size " + maxPacketSize);
						map.clear();
						return;
					} else {
						/*
						 * String vehicleId; int dasnUid; String dasnDateTime;
						 * float dasnLatitude; float dasnLongitude; int
						 * dasnStatus; int dasnSatUsed; int dasnZoneAlarm; int
						 * dasnMacroId; int dasnMacroSrc; float dasnSog; float
						 * dasnCource; float dasnHdop; float dasnHgeo; float
						 * dasnHmet; int dasnGpio; int dasnAdc; float dasnTemp;
						 * int8 i_spmt_id;
						 */

						/*
						 * поле и его тип описание смещение (байт) размер (байт)
						 * unsigned int DeviceID Идентификатор устройства 0 2
						 * unsigned char Size Размер пакета в байтах 2 1
						 * unsigned int GPS_pntr Текущий номер записываемой
						 * точки 3 2 unsigned int GPRS_pntr Текущий номер
						 * передаваемой точки 5 2 unsigned char SoftVersion
						 * Версия FirmWare 7 1 unsigned int Status Состояние
						 * устройства, события 8 2 unsigned long Acc Ускорение:
						 * по байтам Axyz, Az, Ay, Ax 10 4 unsigned int pin[0]
						 * Значение АЦП на входе IN0 в мВ 14 2 unsigned int
						 * pin[1] Значение АЦП на входе IN1 в мВ 16 2 unsigned
						 * int pin[2] Значение АЦП на входе IN2 в мВ 18 2
						 * unsigned int pin[3] Значение АЦП на входе IN3 в мВ 20
						 * 2 unsigned int pin[4] Значение АЦП на входе IN4 в мВ
						 * 22 2 unsigned int pin[5] Значение АЦП на входе IN5 в
						 * мВ 24 2 unsigned char PinSet Значения выходов, биты
						 * 0..5 26 1 unsigned int Power Напряжение питания в мВ
						 * 27 2 unsigned int Battery Напряжение на аккумуляторе
						 * в мВ 29 2 unsigned char NavValid GPS: 0-координаты
						 * верны, >0 - не верны 31 1 unsigned long Time Время по
						 * Гринвичу 32 4 unsigned long Date Дата по Гринвичу 36
						 * 4 float Latitude широта 40 4 float Longitude долгота
						 * 44 4 float Height высота 48 4 float Speed Скорость в
						 * км/ч 52 4 float Course Направление в градусах 56 4
						 * float HDOP Точность 60 4 unsigned char
						 * SatellitesCount Количество спутников 64 1 unsigned
						 * char crc CRC, xor от 0 до последнего байта 65 1
						 */

						logger.debug("Packet [" + readbytes + "] : "
								+ Integer.toHexString(cread));
						if (readbytes == (packetLength - 1)) {
							packet[readbytes] = cread;
							if (readbytes == (packetDataLength - 1)) {
								packetCount = packetCount + 1;
								logger.debug("Parsing packet : " + packetCount);
								parsePacket();
							} else {
								logger.debug("Not Parsing packet : "
										+ (packetCount + 1));
							}

							if (navSoftVersion == 0xA4
									&& (navLatitude != 0 && navLongitude != 0)
									&& navValid == 0
									&& navPacketSize == packetDataLength) {
								map.put("vehicleId",
										Integer.toString(navDeviceID));
								map.put("dasnUid",
										Integer.toString(navDeviceID));
								map.put("dasnLatitude",
										String.valueOf(navLatitude));
								map.put("dasnLongitude",
										String.valueOf(navLongitude));
								map.put("dasnStatus",
										Integer.toString(navDeviceStatus));
								map.put("dasnSatUsed",
										Integer.toString(navSatellitesCount));
								map.put("dasnZoneAlarm", null);
								map.put("dasnMacroId", null);
								map.put("dasnMacroSrc", null);
								map.put("dasnSog", String.valueOf(navSpeed));
								map.put("dasnCource", String.valueOf(navCource));
								map.put("dasnHdop", String.valueOf(navHdop));
								map.put("dasnHgeo", String.valueOf(navHeight));
								map.put("dasnHmet", null);
								map.put("dasnGpio", null);
								map.put("dasnAdc", Float.toString(navPower));
								map.put("dasnTemp", null);
								map.put("i_spmt_id",
										Integer.toString(conf.getModType()));
								map.put("dasnXML", "<xml><acs>" + navAcs
										+ "</acs><pw>" + navBatt
										+ "</pw></xml>");
								// запись в БД

								pgcon.setDataSensor(
										map,
										sdf.parse(utl.formatDate((int) navDate)
												+ utl.cTime((int) navTime)));
								try {
									pgcon.addDataSensor();
									logger.debug("Write Database OK");
								} catch (SQLException e) {
									logger.warn("Error Writing Database : "
											+ e.getMessage());
								}
								map.clear();
							}
							readbytes = 0;
						} else {
							packet[readbytes] = cread;
							if (readbytes == 2) {
								navPacketSize = 0x000000FF & packet[readbytes];
								if (navPacketSize == packetDataLength
										|| navPacketSize == (packetDataLength * 2)) {
									packetLength = navPacketSize;
									logger.debug("Packet Length : "
											+ navPacketSize);
								} else {
									logger.debug("Incorrect Packet Length : "
											+ navPacketSize + " : "
											+ packetDataLength);
									packetLength = packetDataLength;
									getAllPacketOK = "***" + packetCount
											+ "*\r\n";
									logger.debug("Send " + getAllPacketOK);
									oDs.writeBytes(getAllPacketOK);
									return;
								}

							}
							readbytes = readbytes + 1;
						}
					}
				}
				logger.debug("Close reader console");
				logger.debug("Reading packet count : " + packetCount);
				// ответим солько пакетов прочитали
				getAllPacketOK = "***" + packetCount + "*\r\n";
				logger.debug("Send " + getAllPacketOK);
				oDs.writeBytes(getAllPacketOK);
				oDs.flush();
				// обработаем команды из очереди команд для данного блока
				try {
					logger.debug("Getting command");
					pgcon.getCommand(Integer.toString(navDeviceID),
							conf.getModType());
					long[] cmdIdTab = pgcon.getCommandId();
					int commandCount = 0;
					for (int i = 0; i < cmdIdTab.length; i++) {
						long cmdId = cmdIdTab[i];
						String cmdString = pgcon.getCommandString(i);
						if (cmdString != null) {
							logger.debug("Send command to device : "
									+ cmdString);
							// Обработка ответа до 0x0d 0x0a
							String cmdRes;
							try {
								oDs.writeChars(cmdString + "\r\n");
								logger.debug("Change status CMD in database : CMD_OK");
								oDs.flush();
								cmdRes = getCmdResult(iDs, oDs, pgcon);
								pgcon.setCommandStatus(cmdId, "CMD_OK", cmdRes);
							} catch (IOException e1) {
								logger.error("Error Send Command " + cmdString
										+ " : " + e1.getMessage());
							}

							commandCount = commandCount + 1;
						}
					}
					if (commandCount > 0) {
						logger.debug("Sending " + commandCount + " command");
					}
				} catch (SQLException m) {
					logger.warn("Error Get Command : " + m.getMessage());
				}
				if (readbytes > 0) {
					// Некорректно закрыт канал чтения есть непрочитанные байты
					// Закрываем соединение.
					logger.error("Buffer length incorrect : " + readbytes);
					logger.debug("Close connection.");
					return;
				}
			}

		} catch (EOFException e1) {
			logger.error("EOF  : " + e1.getMessage());
		} catch (SocketTimeoutException e2) {
			logger.error("Close connection : " + e2.getMessage());
		} catch (IOException e3) {
			logger.warn("IO socket error : " + e3.getMessage());
		}
	}

	/**
	 * Обработка первого пакета при соединении блока
	 */
	private void readFirstPacket(DataInputStream iDs, DataOutputStream oDs,
			TrackPgUtils pgcon) throws IOException {
		readbytes = 0;
		logger.debug("Read first packet");
		while (readbytes < packetDataLength) {
			packet[readbytes] = iDs.readByte();
			logger.debug("Packet [" + readbytes + "] : "
					+ Integer.toHexString(packet[readbytes]));
			fullreadbytes = fullreadbytes + 1;
			readbytes = readbytes + 1;

		}
		if (readbytes == packetDataLength) {
			logger.debug("Parce first packet");
			navDeviceID = (0x000000FF & packet[0])
					+ ((0x000000FF & packet[1]) << 8);
			logger.debug("Module ID : " + navDeviceID);
			navPacketSize = (0x000000FF & packet[2]);
			logger.debug("Packet Size : " + Integer.toString(navPacketSize));
			navPacketType = (0x000000FF & packet[3]);
			logger.debug("Packet Type : " + Integer.toString(navPacketType));
			navIMEI = Integer.toString(0x000000FF & packet[4])
					+ Integer.toString(0x000000FF & packet[5])
					+ Integer.toString(0x000000FF & packet[6])
					+ Integer.toString(0x000000FF & packet[7])
					+ Integer.toString(0x000000FF & packet[8])
					+ Integer.toString(0x000000FF & packet[9])
					+ Integer.toString(0x000000FF & packet[10])
					+ Integer.toString(0x000000FF & packet[11])
					+ Integer.toString(0x000000FF & packet[12])
					+ Integer.toString(0x000000FF & packet[13])
					+ Integer.toString(0x000000FF & packet[14])
					+ Integer.toString(0x000000FF & packet[15]);
			logger.debug("IMEI : " + navIMEI);
			// ответим что пакет прочитали
			// logger.debug("Send " + getOnePacketOK);
			// oDs.writeChars(getOnePacketOK);

			if (pgcon.getImeiModule(navIMEI) > 0) {
				logger.debug("IMEI found in database : " + navIMEI);
			} else {
				logger.error("IMEI not found : " + navIMEI);
				return;
			}
		} else {
			logger.error("Incorrect first packet length : " + readbytes);
			return;
		}

	}

	public float getReadBytes() {
		return fullreadbytes;
	}

	private void parsePacket() {
		// Разбирается в первом пакете
		// navDeviceID = (0x000000FF & packet[0]) + ((0x000000FF & packet[1]) <<
		// 8);
		// logger.debug("Module ID : " + navDeviceID);
		navPacketType = (0x000000FF & packet[3]);
		logger.debug("Packet Type : " + Integer.toString(navPacketType));
		navPacketSize = (0x000000FF & packet[2]);
		logger.debug("Packet Size : " + Integer.toString(navPacketSize));
		navSoftVersion = (0x000000FF & packet[7]);
		logger.debug("Soft Version : "
				+ Integer.toString(navSoftVersion, 16).toUpperCase());
		navDeviceStatus = (0x000000FF & packet[8])
				+ ((0x000000FF & packet[9]) << 8);
		logger.debug("Status : " + navDeviceStatus);
		navAcs = 0x000000FF & packet[10];
		logger.debug("Acceleration : " + navAcs);
		navPower = (0x000000FF & packet[27]) + ((0x000000FF & packet[28]) << 8);
		navPower = navPower / 1000;
		logger.debug("Power : " + navPower + " V");
		navBatt = (0x000000FF & packet[29]) + ((0x000000FF & packet[30]) << 8);
		navBatt = navBatt / 1000;
		logger.debug("Batt : " + navBatt + " V");
		navValid = 0x000000FF & packet[31];
		logger.debug("Valid : " + navValid);
		navTime = utl.unsigned4Bytes(packet[32], packet[33], packet[34],
				packet[35]) & 0xFFFFFFFFL;
		logger.debug("Time : " + utl.cTime((int) navTime));
		navDate = utl.unsigned4Bytes(packet[36], packet[37], packet[38],
				packet[39]) & 0xFFFFFFFFL;
		logger.debug("Date : " + utl.formatDate((int) navDate));
		navLatitude = utl.unsignedFloat4Bytes(packet[40], packet[41],
				packet[42], packet[43]);
		logger.debug("Lat : " + Float.toString(navLatitude));
		navLongitude = utl.unsignedFloat4Bytes(packet[44], packet[45],
				packet[46], packet[47]);
		logger.debug("Lon  : " + navLongitude);
		navHeight = utl.unsignedFloat4Bytes(packet[48], packet[49], packet[50],
				packet[51]);
		logger.debug("Hei : " + Float.toString(navHeight));
		navSpeed = utl.unsignedFloat4Bytes(packet[52], packet[53], packet[54],
				packet[55]);
		logger.debug("Speed : " + navSpeed);
		navCource = utl.unsignedFloat4Bytes(packet[56], packet[57], packet[58],
				packet[59]);
		logger.debug("Cource : " + navCource);
		navHdop = utl.unsignedFloat4Bytes(packet[60], packet[61], packet[62],
				packet[63]);
		logger.debug("HDOP : " + navHdop);
		navSatellitesCount = 0x000000FF & packet[64];
		logger.debug("Satelites : " + navSatellitesCount);
		navCRC = packet[65];
		logger.debug("CRC : " + navCRC);
		// Подсчитаем реальную контрольную сумму.
		navCheckCRC = ModUtils.getSumXor(packet, 65);
		logger.debug("CRC Check : " + navCheckCRC);
		//
		if (navCRC != navCheckCRC) {
			navValid = 1;
			logger.error("CRC Incorrect. Real : " + navCheckCRC + " Data : "
					+ navCRC);
		}
	}

	private String getCmdResult(DataInputStream iDs, DataOutputStream oDs,
			TrackPgUtils pgcon) throws IOException {
		int readbytes = 0;
		int cread;
		String result = "";
		logger.debug("Read result of command");
		while (((cread = iDs.readByte()) != -1)
				&& (readbytes < (2 * packetDataLength - 1))) {
			logger.debug("Result Command [" + readbytes + "] : "
					+ Integer.toHexString(cread));
			result = result + " " + Integer.toHexString(0x000000FF & cread);
			fullreadbytes = fullreadbytes + 1;
			readbytes = readbytes + 1;

		}
		logger.debug("Result of command : " + result + " Bytes : "
				+ Integer.toString(readbytes));
		return result;
	}

	public void setGetOnePacketOK(String getOnePacketOK) {
		this.getOnePacketOK = getOnePacketOK;
	}

	public String getGetOnePacketOK() {
		return getOnePacketOK;
	}

}
