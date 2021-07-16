/**
 * 
 */
package ru.novoscan.trkpd.terminals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.novoscan.trkpd.domain.Terminal;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.ModUtils;
import ru.novoscan.trkpd.utils.TrackPgUtils;

/**
 * @author kurensky
 * 
 */
public class ModMarker extends Terminal {

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

	private SimpleDateFormat DSF = new SimpleDateFormat(DATE_SIMPLE_FORMAT);

	public ModMarker(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader console, ModConfig conf, TrackPgUtils pgcon)
			throws ParseException, NumberFormatException, IOException {
		this.setDasnType(conf.getModType());
		logger.debug("Read streems..");
		maxPacketSize = conf.getMaxSize();
		int cread;
		String slog = "";
		packetSize = 0;
		while ((cread = console.read()) != -1) {
			readbytes = readbytes + 1;
			if (packetSize > maxPacketSize) {
				logger.error("Over size : " + packetSize);
				this.clear();
				return;
			} else if (cread == 0x0d) {
				System.out.println("Parse packet..." + slog);
				Matcher m = patternMarker.matcher(slog);
				if (m.matches()) {
					/*
					 * String vehicleId; int dasnUid; String dasnDateTime; float
					 * dasnLatitude; float dasnLongitude; int dasnStatus; int
					 * dasnSatUsed; int dasnZoneAlarm; int dasnMacroId; int
					 * dasnMacroSrc; float dasnSog; float dasnCource; float
					 * dasnHdop; float dasnHgeo; float dasnHmet; int dasnGpio;
					 * int dasnAdc; float dasnTemp; int8 i_spmt_id;
					 */
					dasnLatitude = Double.valueOf(ModUtils.getGGMM(m.group(6)));
					if (m.group(5).equals("S")) {
						dasnLatitude = -dasnLatitude;
					}
					dasnLongitude = Double
							.valueOf(ModUtils.getGGMM(m.group(8)));
					if (m.group(7).equals("W")) {
						dasnLongitude = -dasnLongitude;
					}
					dasnSatUsed = Long.parseLong("0" + m.group(11), 16);
					dasnStatus = DATA_STATUS.ERR;
					if (dasnSatUsed > 3) {
						dasnStatus = DATA_STATUS.OK;
					}
					dasnTemp = Double.parseDouble(m.group(15)) - 273;
					dasnUid = m.group(3);
					dasnSog = Double.valueOf(m.group(9));
					dasnCourse = Double.valueOf(m.group(10));
					dasnGpio = Long.valueOf(m.group(13));
					dasnAdc = Long.valueOf(m.group(12)) / 10;
					dasnDatetime = DSF.parse(m.group(4));

					dasnValues.put("OUT", m.group(14));
					dataSensor.setDasnUid(dasnUid);
					dataSensor.setDasnDatetime(dasnDatetime);
					dataSensor.setDasnLatitude(dasnLatitude);
					dataSensor.setDasnLongitude(dasnLongitude);
					dataSensor.setDasnSatUsed(dasnSatUsed);
					dataSensor.setDasnSog(dasnSog);
					dataSensor.setDasnCourse(dasnCourse);
					dataSensor.setDasnTemp(dasnTemp);
					dataSensor.setDasnAdc(dasnAdc);
					dataSensor.setDasnValues(dasnValues);
					// запись в БД
					pgcon.setDataSensorValues(dataSensor);
					// Ответ блоку
					try {
						pgcon.addDataSensor();
						logger.debug("Writing Database : " + slog);
					} catch (SQLException e) {
						logger.warn("Error Writing Database : "
								+ e.getMessage());
					}
					this.clear();
					logger.debug("Send reply : " + "__" + m.group(2)
							+ (char) (0x0d) + (char) (0x0a));
					oDs.writeBytes("__" + m.group(2) + (char) (0x0d)
							+ (char) (0x0a));
					oDs.flush();
					slog = "";
					packetSize = 0;
				} else {
					logger.error("Unknown packet type : " + slog);
					this.clear();
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
	}

	public float getReadBytes() {
		return readbytes;
	}

}
