package ru.novoscan.trkpd.terminals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import ru.novoscan.trkpd.domain.Terminal;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.ModUtils;
import ru.novoscan.trkpd.utils.TrackPgUtils;

/**
 * @author kur
 * 
 */
public class ModMajak extends Terminal {

	private int readbytes;

	private float fullreadbytes = 0;

	// private static int maxPacketSize = 18; // длина пакета авторизации

	private ModUtils utl = new ModUtils();

	private int[] packet;

	private static int packetDataLength = 34; // длина пакета с данными

	private String navIMEI;

	private String navPhone;

	private String navPass;

	private SimpleDateFormat DSF = new SimpleDateFormat(DATE_SIMPLE_FORMAT);

	private int navPacketType;

	private int navSoftVersion;

	private int navCRC;

	private int navValid;

	public ModMajak(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader unbconsole, ModConfig conf, TrackPgUtils pgcon)
			throws ParseException, IOException {
		this.setDasnType(conf.getModType());
		int cread;
		logger.debug("Read data..");
		packet = new int[packetDataLength]; // пакет с нулевого считается
		fullreadbytes = 0;
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
					navIMEI = navIMEI + String.valueOf(cread >> 4).toString()
							+ String.valueOf(cread & 0x0f).toString();
				}
				navIMEI = navIMEI.substring(1);
				logger.debug("IMEI : " + navIMEI);
				dasnUid = navIMEI;
				logger.debug("navDeviceID : " + dasnUid);
				cread = readByte(iDs);
				logger.debug("Protocol : "
						+ String.valueOf(cread >> 4).toString() + " Hardware: "
						+ String.valueOf(cread & 0x0f).toString());
				cread = readByte(iDs);
				navSoftVersion = cread;
				logger.debug("Version : " + (char) navSoftVersion);
				navPhone = "";
				for (int i = 1; i < 6; i++) {
					cread = readByte(iDs);
					navPhone = navPhone + String.valueOf(cread >> 4).toString()
							+ String.valueOf(cread & 0x0f).toString();
				}
				logger.debug("Phone : " + navPhone);
				navPass = "";
				for (int i = 1; i < 3; i++) {
					cread = readByte(iDs);
					navPass = navPass + String.valueOf(cread >> 4).toString()
							+ String.valueOf(cread & 0x0f).toString();
				}
				logger.debug("Password : " + navPass);
				cread = readByte(iDs);
				navCRC = cread;
				logger.debug("CRC : " + navCRC);
				sendCRC(oDs, navCRC);
			} else if (navPacketType == 0x02) {
				// пакет данных - читаем 33 байта
				dataSensor.clear();
				logger.debug("----------------------");
				for (int i = 1; i < packetDataLength; i++) {
					packet[i] = readByte(iDs);
				}
				dasnAdc = Long.valueOf(packet[1]);
				logger.debug("Заряд : " + dasnAdc + " %");
				dasnTemp = Double.valueOf(packet[4]);
				logger.debug("Температура : " + dasnTemp + " C");
				logger.debug("Интервал пробуждения (0 - без сна) : "
						+ packet[5]);
				logger.debug("Единицы измерения инервала пробуждения (M - минуты, иначе часы) : "
						+ packet[6]);
				logger.debug("Режим работы : " + Integer.toHexString(packet[7]));
				logger.debug("Интервал передачи : " + packet[8] + " сек");
				logger.debug("MCC : " + packet[9]);
				logger.debug("MNC : " + packet[10]);
				logger.debug("LAC : "
						+ Integer.toHexString(((packet[11] << 8) + packet[12])));
				logger.debug("CID : "
						+ Integer.toHexString(((packet[13] << 8) + packet[14])));
				navValid = packet[15] >> 6;
				logger.debug("GPS статус : " + navValid);
				dasnSatUsed = Long.valueOf(packet[15] & 0x3f);
				logger.debug("Количество спутников : " + dasnSatUsed);
				String navDateTime = utl.getLastnCharacters(
						"000000"
								+ String.valueOf((packet[19] << 16)
										+ (packet[20] << 8) + packet[21]), 6)
						+ utl.getLastnCharacters(
								"000000"
										+ String.valueOf((packet[16] << 16)
												+ (packet[17] << 8)
												+ packet[18]), 6);
				logger.debug("DateTime : " + navDateTime);
				dasnSog = (((double) packet[30]) * utl.getNmi());
				logger.debug("Скорость : " + String.valueOf(dasnSog));
				dasnCourse = (double) ((packet[32] << 8) + packet[31]);
				dasnLatitude = (double) packet[22]
						+ ((float) ((packet[23] << 12) + (packet[24] << 4) + (packet[25] >> 4)))
						/ (600000);
				dasnLongitude = (double) packet[26]
						+ ((float) ((packet[27] << 12) + (packet[28] << 4) + (packet[29] >> 4)))
						/ (600000);
				;
				logger.debug("Широта (градусы) : "
						+ String.valueOf(dasnLatitude));
				logger.debug("Долгота (градусы) : "
						+ String.valueOf(dasnLongitude));
				logger.debug("Курс (градусы) : " + String.valueOf(dasnSog));
				navCRC = packet[33];
				logger.debug("CRC : " + navCRC);
				// Сохраним в БД данные
				dasnDatetime = DSF.parse(navDateTime);
				dataSensor.setDasnDatetime(dasnDatetime);
				dataSensor.setDasnUid(dasnUid);
				dataSensor.setDasnLatitude(dasnLatitude);
				dataSensor.setDasnLongitude(dasnLongitude);
				dataSensor.setDasnSatUsed(dasnSatUsed);
				dataSensor.setDasnSog(dasnSog);
				dataSensor.setDasnCourse(dasnCourse);
				dataSensor.setDasnAdc(dasnAdc);
				dataSensor.setDasnTemp(dasnTemp);
				dataSensor.setDasnValues(dasnValues);
				// запись в БД
				pgcon.setDataSensorValues(dataSensor);
				try {
					pgcon.addDataSensor();
					logger.debug("Write Database OK");
				} catch (SQLException e) {
					logger.warn("Error Writing Database : " + e.getMessage());
				}
				// sendCRC(oDs, navCRC);
			} else {
				// неверный тип пакета - вызываем исключение
				logger.error("Incorrect packet type : "
						+ Integer.toHexString(cread));
				return;
			}
			this.clear();

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
