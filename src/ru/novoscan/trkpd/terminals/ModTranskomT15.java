package ru.novoscan.trkpd.terminals;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.resources.ModConstats;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.ModUtils;
import ru.novoscan.trkpd.utils.TrackPgUtils;

public class ModTranskomT15 implements ModConstats {
	static Logger logger = Logger.getLogger(ModTranskomT15.class);

	/*
	 * imei#SD#date;time;lat1;lat2;lon1;lon2;speed;course;height;sats\r\n imei –
	 * imei номер маяка (15 цифр) ‘SD’, ‘#’,’;’ - разделители date дата в
	 * формате DDMMYY, в UTC time время в формате HHMMSS, в UTC lat1;lat2 широта
	 * (5544.6025;N) lon1;lon2 долгота (03739.6834;E) speed скорость, целое
	 * число, км/ч course курс, целое число, градусы height высота, целое число,
	 * в метрах sats количество спутников, целое число
	 */
	private SimpleDateFormat sdf = new SimpleDateFormat(DATE_SIMPLE_FORMAT);

	private static final Pattern patternTranskomT15 = Pattern.compile(
			"^(\\d{15})" // IMEI 1
					+ "#SD#" + "(\\d{6})" // DDMMYY - дата 2
					+ ";" //
					+ "(\\d{6})" // HHMMSS - время 3
					+ ";" //
					+ "(\\d{4})" // широта: N|S GGMM.mmmm 4
					+ "." //
					+ "(\\d{4})" // широта: N|S GGMM.mmmm 5
					+ ";" //
					+ "(N|S|_)" // 6
					+ ";" //
					+ "(\\d{5})" // долгота: E/W GGGMMmmmm 7
					+ "." //
					+ "(\\d{4})" // долгота: E/W GGGMMmmmm 8
					+ ";" //
					+ "(E|W|_)" // 9
					+ ";" //
					+ "(\\d+)" // км/ч 10
					+ ";" //
					+ "(\\d+)" // азимут градусы 11
					+ ";" //
					+ "(\\d+)" // высота 12
					+ ";" //
					+ "(\\d+)" // спутники 13
					// + (char) 10 + (char) 13
			, Pattern.MULTILINE);

	private String data;

	private byte[] dataByte;

	private HashMap<String, String> map = new HashMap<String, String>();

	public ModTranskomT15(DatagramPacket dataPacket,
			DatagramSocket clientSocket, ModConfig conf, TrackPgUtils pgcon) {
		data = "";
		dataByte = dataPacket.getData();
		for (int i = 0; i < dataPacket.getLength(); i++) {
			logger.debug("Data[" + i + "] : " + (char) dataByte[i]);
			if (dataByte[i] == 0x0d) {
				break;
			}
			data = data + (char) dataByte[i];
		}
		logger.debug("Read data: \"" + data + "\"");
		try {
			Matcher m = patternTranskomT15.matcher(data);
			if (m.matches()) {
				float dasnLatitude = ModUtils.getGGMM(m.group(4) + m.group(5));
				if (m.group(6).equals("S")) {
					dasnLatitude = -dasnLatitude;
				}
				float dasnLongitude = ModUtils.getGGMM(m.group(7) + m.group(8));
				if (m.group(9).equals("W")) {
					dasnLongitude = -dasnLongitude;
				}
				int dasnSatUsed = Integer.parseInt(m.group(13), 10);
				int dasnStatus = 0;
				if (dasnSatUsed > 3) {
					dasnStatus = 1;
				}
				map.put("vehicleId", m.group(1));
				map.put("dasnUid", m.group(1));
				map.put("dasnLatitude", String.valueOf(dasnLatitude));
				map.put("dasnLongitude", String.valueOf(dasnLongitude));
				map.put("dasnStatus", String.valueOf(dasnStatus));
				map.put("dasnSatUsed", String.valueOf(dasnSatUsed));
				map.put("dasnZoneAlarm", null);
				map.put("dasnMacroId", null);
				map.put("dasnMacroSrc", null);
				map.put("dasnSog", m.group(10));
				map.put("dasnCource", m.group(11));
				map.put("dasnHdop", null);
				map.put("dasnHgeo", m.group(12));
				map.put("dasnHmet", null);
				map.put("dasnGpio", null);
				map.put("dasnAdc", null);
				map.put("dasnTemp", null);
				map.put("i_spmt_id", Integer.toString(conf.getModType()));
				pgcon.setDataSensor(map, sdf.parse(m.group(2) + m.group(3)));
				// Ответ блоку
				try {
					pgcon.addDataSensor();
					logger.debug("Writing Database : " + data);
				} catch (SQLException e) {
					logger.warn("Error Writing Database : " + e.getMessage());
				}
				map.clear();
			} else if (data.equals("+++")) {
				logger.debug("Ignore data : \"" + data + "\"");
				map.clear();
			} else {
				logger.error("Unknown packet data : \"" + data + "\"");
				map.clear();
			}
		} catch (Exception e) {
			logger.warn("Exception : " + e.getMessage());
		}

	}

	public float getReadBytes() {
		// TODO Auto-generated method stub
		return data.length();
	}

}
