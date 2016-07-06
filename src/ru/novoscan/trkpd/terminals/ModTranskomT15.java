package ru.novoscan.trkpd.terminals;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.domain.Terminal;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.ModUtils;
import ru.novoscan.trkpd.utils.TrackPgUtils;

public class ModTranskomT15 extends Terminal {
	static Logger logger = Logger.getLogger(ModTranskomT15.class);

	/*
	 * imei#SD#date;time;lat1;lat2;lon1;lon2;speed;course;height;sats\r\n imei –
	 * imei номер маяка (15 цифр) ‘SD’, ‘#’,’;’ - разделители date дата в
	 * формате DDMMYY, в UTC time время в формате HHMMSS, в UTC lat1;lat2 широта
	 * (5544.6025;N) lon1;lon2 долгота (03739.6834;E) speed скорость, целое
	 * число, км/ч course курс, целое число, градусы height высота, целое число,
	 * в метрах sats количество спутников, целое число
	 */
	private final SimpleDateFormat DSF = new SimpleDateFormat(
			DATE_SIMPLE_FORMAT);

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

	public ModTranskomT15(DatagramPacket dataPacket,
			DatagramSocket clientSocket, ModConfig conf, TrackPgUtils pgcon)
			throws ParseException {
		this.setDasnType(conf.getModType());
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
		Matcher m = patternTranskomT15.matcher(data);
		if (m.matches()) {
			dasnLatitude = Double.valueOf(ModUtils.getGGMM(m.group(4)
					+ m.group(5)));
			if (m.group(6).equals("S")) {
				dasnLatitude = -dasnLatitude;
			}
			dasnLongitude = Double.valueOf(ModUtils.getGGMM(m.group(7)
					+ m.group(8)));
			if (m.group(9).equals("W")) {
				dasnLongitude = -dasnLongitude;
			}
			dasnSatUsed = Long.parseLong(m.group(13), 10);
			dasnStatus = DATA_STATUS.ERR;
			if (dasnSatUsed > 3) {
				dasnStatus = DATA_STATUS.OK;
			}
			if (dasnStatus == DATA_STATUS.OK) {
				dataSensor.setDasnUid(m.group(1));
				dataSensor.setDasnDatetime(DSF.parse(m.group(2) + m.group(3)));
				dataSensor.setDasnLatitude(dasnLatitude);
				dataSensor.setDasnLongitude(dasnLongitude);
				dataSensor.setDasnStatus(1L);
				dataSensor.setDasnSatUsed(dasnSatUsed);

				dataSensor.setDasnSog(Double.valueOf(m.group(10)));
				dataSensor.setDasnCourse(Double.valueOf(m.group(11)));
				dataSensor.setDasnHgeo(Double.valueOf(m.group(12)));
				pgcon.setDataSensorValues(dataSensor);
				// Ответ блоку
				try {
					pgcon.addDataSensor();
					logger.debug("Writing Database : " + data);
				} catch (SQLException e) {
					logger.warn("Error Writing Database : " + e.getMessage());
				}
			}
		} else if (data.equals("+++")) {
			logger.debug("Ignore data : \"" + data + "\"");
		} else {
			logger.error("Unknown packet data : \"" + data + "\"");
		}
		this.clear();

	}

	public float getReadBytes() {
		// TODO Auto-generated method stub
		return data.length();
	}

}
