package ru.novoscan.trkpd.terminals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.domain.Terminal;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.TrackPgUtils;

public class ModNavixyM7 extends Terminal {
	static Logger logger = Logger.getLogger(ModNavixyM7.class);

	private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

	private final SimpleDateFormat DSF = new SimpleDateFormat(DATE_SIMPLE_FORMAT);

	// 310000001,20100701180220,121.123456,12.654321,0,233,0,9,2,4.10,1
	// ид,дата YYYYMMDDHHMMSS
	// ,Longitude
	// ,Latitude
	// ,Скорость км./ч.
	// ,курс
	// ,высота
	// ,спутники
	// ,событие
	// ,напряжение(x.xx Вольт)
	// ,состояние кнопки

	private static final Pattern pattern = Pattern.compile("(?i)(\\d+)" + // ид
			",(\\d{8})(\\d{6})" + // баг!!! дата неверна!
			// ",(\\d{14})" + // дата
			",(\\d+\\.\\d+)" + // Longitude
			",(\\d+\\.\\d+)" + // Latitude
			",(\\d+)" + // скорость км/ч
			",(\\d+)" + // курс градусы 0-360
			",(\\d+)" + // высота 0
			",(\\d+)" + // спутники 0-12
			",(\\d+)" + // событие породившее запись
			",(\\d+\\.\\d+)\\D*" + // питание Вольт
			",(\\d+)" + // состояние кнопки 1 или 0
			",(.*)");

	// 3442843859,20130129064020,82.832570,54.996621,2,152,0,7,37,4.14V,0,41.9

	private int readbytes = 0;

	private int fullreadbytes = 0;

	private static int maxPacketSize = 200;

	private static int keepAliveLength = 8;

	int[] keepAlive = new int[keepAliveLength];

	int[] packet = new int[maxPacketSize];

	private DataOutputStream oDs;

	private DataInputStream iDs;

	private static int aliveId;

	private TrackPgUtils pgcon;

	public ModNavixyM7(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader console, ModConfig conf, TrackPgUtils pgcon)
			throws ParseException, IOException {
		this.pgcon = pgcon;
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		maxPacketSize = conf.getMaxSize();
		this.oDs = oDs;
		this.iDs = iDs;
		logger.debug("Read streems..");
		int i = 0;
		while (true) {
			packet[i] = readByte();
			if ((packet[i] & 0xff) == 0xd0) {
				keepAlive[0] = packet[i];
				i++;
				packet[i] = readByte();
				if ((packet[i] & 0xff) == 0xd7) {
					keepAlive[1] = packet[i];
					readKeepAlive();
					logger.debug("Is Keep Alive packet..");
					aliveId = (keepAlive[2] & 0xff)
							+ ((keepAlive[3] & 0xff) << 8);
					logger.debug("Alive Id : " + aliveId);
					dasnUid = String.valueOf(keepAlive[4] + ((keepAlive[5] & 0xff) << 8)
							+ ((keepAlive[6] & 0xff) << 16) + (((long) (keepAlive[7] & 0xff)) << 24));
					logger.debug("Device Id : " + dasnUid);
					sendKeepAlive();
					readbytes = 0;
					i = 0; // очистим массив
				}
			} else if (packet[i] == 0x0a) {
				// данные
				logger.debug("Разбор пакета данных. Размер пакета: "
						+ packet.length);
				parsePacket(i);
				readbytes = 0;
				i = 0;
			} else {
				i++;
			}

		}
	}

	private void parsePacket(int paketLength) throws ParseException {
		StringBuffer data = new StringBuffer();
		Date curr = new Date();
		paketLength = paketLength - 1;
		for (int i = 0; i < paketLength; i++) {
			data.append((char) packet[i]);
		}
		logger.debug("Данные терминала : " + data.toString());
		Matcher m = pattern.matcher(data.toString());
		if (m.matches()) {
			logger.debug("Данные терминала верны.");
			String date = m.group(2);
			try {
				if (curr.before(dateFormat.parse(date))) {
					logger.error("Баг терминала: " + date);
					date = dateFormat.format(curr);
				}
			} catch (ParseException e) {
				logger.error("Дата неверна: " + date);
				date = dateFormat.format(curr);
			}
			dasnUid = m.group(1);
			dasnDatetime = DSF.parse(date + m.group(3));
			dasnLongitude = Double.valueOf(m.group(4));
			dasnLatitude = Double.valueOf(m.group(5));
			dasnSog = Double.valueOf(m.group(6));
			dasnCourse = Double.valueOf(m.group(7));
			dasnHdop = Double.valueOf(m.group(8));
			dasnSatUsed = Long.parseLong(m.group(9));
			dasnAdc = Long.parseLong(m.group(11));
			dasnValues.put("STAT", m.group(12));
			dasnValues.put("GSM", m.group(13));
			writeData();
		} else {
			logger.warn("Данные терминала неверны : " + data);
		}
	}

	private int readByte() throws IOException {
		byte bread = iDs.readByte();
		packet[readbytes] = bread;
		logger.debug("packet[" + readbytes + "] : "
				+ Integer.toHexString(packet[readbytes]));
		readbytes++;
		fullreadbytes++;
		return bread;
	}

	private void readKeepAlive() throws IOException {
		for (int i = 2; i < keepAliveLength; i++) {
			keepAlive[i] = readByte();
		}
	}

	private void sendKeepAlive() throws IOException {
		for (int i = 0; i < keepAliveLength; i++) {
			oDs.write(keepAlive[i]);
			logger.debug("Write byte[" + i + "] : "
					+ Integer.toHexString(keepAlive[i]));
		}
		oDs.flush();

	}

	public float getReadBytes() {
		return fullreadbytes;
	}

	private void writeData() throws ParseException {
		// Сохраним в БД данные
		dataSensor.setDasnUid(dasnUid);
		dataSensor.setDasnDatetime(dasnDatetime);
		dataSensor.setDasnLatitude(dasnLatitude);
		dataSensor.setDasnLongitude(dasnLongitude);
		dataSensor.setDasnSog(dasnSog);
		dataSensor.setDasnCourse(dasnCourse);
		dataSensor.setDasnSatUsed(dasnSatUsed);
		dataSensor.setDasnHdop(dasnHdop);
		dataSensor.setDasnValues(dasnValues);
		dataSensor.setDasnAdc(dasnAdc);
		
		pgcon.setDataSensorValues(dataSensor);
		try {
			pgcon.addDataSensor();
			logger.debug("Write Database OK");
		} catch (SQLException e) {
			logger.warn("Error Writing Database : " + e.getMessage());
		}
		this.clear();
	}

}
