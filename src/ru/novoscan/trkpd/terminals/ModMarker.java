/**
 * 
 */
package ru.novoscan.trkpd.terminals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.resources.ModConstats;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.ModUtils;
import ru.novoscan.trkpd.utils.TrackPgUtils;

/**
 * @author kurensky
 * 
 */
public class ModMarker implements ModConstats {
	static Logger logger = Logger.getLogger(ModMarker.class);

	// $GM20B869158005299603T111112144458N55021806E08258139800002956630308#
	private static final Pattern patternMarker = Pattern.compile("^\\$GM"
			+ "(\\d{1})" // Тип данных навигации для 2 типа - GPS
			+ "(..)" // ИД пакета 2
			+ "(\\d{15})" // IMEI 3
			+ "T" + "(\\d{12})" // DDMMYYHHMMSS - дата время с секундами 4
			+ "(N|S|_)" // 5
			+ "(\\d{8})" // широта: N|S GGMMmmmm 6
			+ "(E|W|_)" // 7
			+ "(\\d{9})" // долгота: E/W GGGMMmmmm 8
			+ "(\\d{3})" // км/ч 9
			+ "(\\d{3})" // азимут градусы 10
			+ "([0-9a-fA-F])" // спутники 11
			+ "(\\d{2})" // заряд батареи % 12
			+ "([0-9a-fA-F])" // битовые входы 13
			+ "([0-9a-fA-F])" // выход 14
			+ "(\\d{3})" // температура кельвины 15
			+ "#");

	char enddata = '#';

	private float readbytes = 0;

	private float packetSize;

	private static int maxPacketSize;

	private HashMap<String, String> map = new HashMap<String, String>();

	public ModMarker(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader console, ModConfig conf, TrackPgUtils pgcon) {
		logger.debug("Read streems..");
		TrackPgUtils.setDateSqlFormat(SQL_DATE_SIMPLE_FORMAT);
		maxPacketSize = conf.getMaxSize();
		try {
			int cread;
			String slog = "";
			packetSize = 0;
			while ((cread = console.read()) != -1) {
				readbytes = readbytes + 1;
				if (packetSize > maxPacketSize) {
					logger.error("Over size : " + packetSize);
					map.clear();
					return;
				} else if (cread == 0x0d) {
					System.out.println("Parse packet..." + slog);
					Matcher m = patternMarker.matcher(slog);
					if (m.matches()) {
						/*
						 * String vehicleId; int dasnUid; String dasnDateTime;
						 * float dasnLatitude; float dasnLongitude; int
						 * dasnStatus; int dasnSatUsed; int dasnZoneAlarm; int
						 * dasnMacroId; int dasnMacroSrc; float dasnSog; float
						 * dasnCource; float dasnHdop; float dasnHgeo; float
						 * dasnHmet; int dasnGpio; int dasnAdc; float dasnTemp;
						 * int8 i_spmt_id;
						 */
						float dasnLatitude = ModUtils.getGGMM(m.group(6));
						if (m.group(5).equals("S")) {
							dasnLatitude = -dasnLatitude;
						}
						float dasnLongitude = ModUtils.getGGMM(m.group(8));
						if (m.group(7).equals("W")) {
							dasnLongitude = -dasnLongitude;
						}
						int dasnSatUsed = Integer.parseInt("0" + m.group(11),
								16);
						int dasnStatus = 0;
						if (dasnSatUsed > 3) {
							dasnStatus = 1;
						}
						int dasnTemp = Integer.valueOf(m.group(15));
						dasnTemp = dasnTemp - 273;
						map.put("vehicleId", m.group(3));
						map.put("dasnUid", m.group(3));
						map.put("dasnDateTime", m.group(4));
						map.put("dasnLatitude", String.valueOf(dasnLatitude));
						map.put("dasnLongitude", String.valueOf(dasnLongitude));
						map.put("dasnStatus", String.valueOf(dasnStatus));
						map.put("dasnSatUsed", String.valueOf(dasnSatUsed));
						map.put("dasnZoneAlarm", null);
						map.put("dasnMacroId", null);
						map.put("dasnMacroSrc", null);
						map.put("dasnSog", m.group(9));
						map.put("dasnCource", m.group(10));
						map.put("dasnHdop", null);
						map.put("dasnHgeo", null);
						map.put("dasnHmet", null);
						map.put("dasnGpio", m.group(13));
						map.put("dasnAdc", m.group(12));
						map.put("dasnTemp", String.valueOf(dasnTemp));
						map.put("i_spmt_id",
								Integer.toString(conf.getModType()));
						map.put("dasnXML", "<xml><out>" + m.group(14)
								+ "</out><pw>"
								+ (Float.valueOf(m.group(12)) / ((float) 10.0))
								+ "</pw><temp>" + dasnTemp + "</temp></xml>");
						// запись в БД
						pgcon.setDataSensor(map);
						// Ответ блоку
						try {
							pgcon.addDataSensor();
							logger.debug("Writing Database : " + slog);
						} catch (SQLException e) {
							logger.warn("Error Writing Database : "
									+ e.getMessage());
						}
						map.clear();
						logger.debug("Send reply : " + "__" + m.group(2)
								+ (char) (0x0d) + (char) (0x0a));
						oDs.writeBytes("__" + m.group(2) + (char) (0x0d)
								+ (char) (0x0a));
						oDs.flush();
						slog = "";
						packetSize = 0;
					} else {
						logger.error("Unknown packet type : " + slog);
						map.clear();
						slog = "";
						packetSize = 0;
					}
				} else {
					packetSize = packetSize + 1;
					slog = slog + (char) cread;
					logger.debug("Data[" + packetSize + "] : " + (char) cread);
				}

			}
			logger.debug("Close reader console");
		} catch (SocketTimeoutException e) {
			logger.error("Close connection : " + e.getMessage());
		} catch (IOException e) {
			logger.warn("IO socket error : " + e.getMessage());
		} catch (Exception e) {
			logger.warn("Exception : " + e.getMessage());
		}
	}

	public float getReadBytes() {
		return readbytes;
	}

}
