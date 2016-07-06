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

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.domain.Terminal;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.ModUtils;
import ru.novoscan.trkpd.utils.TrackPgUtils;

/**
 * 
 */

/**
 * @author Kurensky A. Evgeny
 * 
 */
public class ModGs extends Terminal {
	// $357248013716372,1,3,030109,164256,E08257.2380,N5502.5243,179.1,0.05,82.30,5*5.61!
	// или
	// $357248013716372,1,3,030109,164256,E08257.2380,N5502.5243,179.1,0.05,82.30,5,5.61!
	/*
	 * Поле Значение Пояснение Command_Head $ Начало отчёта IMEI IMEI трекера
	 * status Статус/назначение отчета 2: Stop connect 5: SOS(Alarm) 8: GPRS
	 * Immediate report 9: GPRS Period report 10.Disconnect and GPRS period
	 * 11.GPRS Geofence(Alarm) 14.Battery low(Alarm) 16: Motion
	 * report(vibration) 17. Motion report(regular) 18: Parking mode(Alarm) 19:
	 * Parking mode 21: SMS/GPRS Immediate report 22: SMS/GPRS period report 23:
	 * SMS/GPRS Geofence(Alarm) 24. Motion report(Activate)
	 * 
	 * GPS_fix 1 1: Позиция не определена 2 2: GPS 2D Fix 3 3: GPS 3D Fix date
	 * ddmmyy Дата, ддммгг time hhmmss Время, ччммсс longitude (E or
	 * W)dddmm.mmmm Долгота. Формат гггмм.мммм. Пример E12129.2186 à E 121°
	 * 29.2186’ latitude (N or S)ddmm.mmmm Широта. Формат ггмм.мммм. Пример:
	 * N2459.8915 à N 24° 59.8915’ altitude xxxxx.x Высота в метрах speed
	 * xxxxx.xx Скорость в узлах (1узел = 1.852 км) heading ddd Направление
	 * движения, курс (в градусах) satellites xx Количество используемых
	 * спутников HDOP xx.x Показатель HDOP Command_END ! Конец отчёта
	 */
	static Logger logger = Logger.getLogger(ModGs.class);

	private static final Pattern pattern = Pattern
			.compile("(?i)(\\d+),(\\d{1,2}),(\\d{1}),(\\d{6}),(\\d{6}),[EW](\\d+\\.{0,1}\\d*),[NS](\\d+\\.{0,1}\\d*),(\\d+\\.{0,1}\\d*),(\\d+\\.{0,1}\\d*),(\\d+\\.{0,1}\\d*),(\\d+\\.{0,1}\\d*),(\\d+\\.{0,1}\\d*)");

	private static final Pattern rquit = Pattern.compile("(?im)quit.*");

	private static final Pattern rbeg = Pattern.compile("(?im)\\$");

	private static final Pattern rend = Pattern.compile("(?im)\\!");

	private final SimpleDateFormat DSF = new SimpleDateFormat(
			DATE_SIMPLE_FORMAT);

	private float readbytes = 0;

	private float packetSize;

	private final int maxPacketSize;

	private static String getAllPacketOK = "$OK!";

	private ModUtils utl = new ModUtils();

	public ModGs(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader console, ModConfig conf, TrackPgUtils pgcon)
			throws ParseException, IOException {
		this.setDasnType(conf.getModType());
		this.maxPacketSize = conf.getMaxSize();
		int cread;
		char data;
		String slog = "";
		packetSize = 0;
		while ((cread = console.read()) != -1) {
			readbytes = readbytes + 1;
			packetSize = packetSize + 1;
			if (packetSize > maxPacketSize) {
				logger.error("Over size : " + packetSize);
				this.clear();
				return;
			} else {
				data = (char) cread;
				if (rbeg.matcher(Character.toString(data)).matches()) {
					slog = "";
					this.clear();
				} else if (rend.matcher(Character.toString(data)).matches()) {
					// System.out.println("Data : " + slog);
					Matcher m = pattern.matcher(slog);
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
						dasnUid = (m.group(1));
						dasnLatitude = Double.valueOf(utl.getLL(m.group(7)));
						dasnLongitude = Double.valueOf(utl.getLL(m.group(6)));
						dasnStatus = DATA_STATUS.OK;
						dasnSatUsed = Long.valueOf(m.group(11));
						dasnSog = Double.valueOf(utl.getSpeed(m.group(9)));
						dasnCourse = Double.valueOf(utl.getSpeed(m.group(10)));
						dasnHdop = Double.valueOf(m.group(12));
						dasnHgeo = Double.valueOf(m.group(8));
						dasnDatetime = DSF.parse(m.group(4) + m.group(5));

						// Сохраним в БД данные
						dataSensor.setDasnUid(dasnUid);
						dataSensor.setDasnDatetime(dasnDatetime);
						dataSensor.setDasnLatitude(dasnLatitude);
						dataSensor.setDasnLongitude(dasnLongitude);
						dataSensor.setDasnSatUsed(dasnSatUsed);
						dataSensor.setDasnSog(dasnSog);
						dataSensor.setDasnCourse(dasnCourse);
						dataSensor.setDasnHgeo(dasnHgeo);
						dataSensor.setDasnHmet(dasnHmet);
						dataSensor.setDasnAdc(dasnAdc);
						dataSensor.setDasnGpio(dasnGpio);
						dataSensor.setDasnTemp(dasnTemp);
						//
						dataSensor.setDasnMacroId(dasnMacroId);
						dataSensor.setDasnMacroSrc(dasnMacroSrc);
						dataSensor.setDasnValues(dasnValues);
						//
						pgcon.setDataSensorValues(dataSensor);
						// Ответ блоку
						try {
							pgcon.addDataSensor();
							logger.debug("Writing Database : " + dasnUid);
						} catch (SQLException e) {
							logger.warn("Error Writing Database : "
									+ e.getMessage());
						}
						slog = "";
						this.clear();
						packetSize = 0;
						logger.debug("Send " + getAllPacketOK);
						oDs.writeBytes(getAllPacketOK);
						oDs.flush();
					} else {
						logger.error("Incorrect data : " + slog);
						slog = "";
						this.clear();
					}
				} else {
					slog = slog + Character.toString(data);
					if (rquit.matcher(slog).matches()) {
						return;
					}
				}
			}

		}
		logger.debug("Close reader console");
	}

	public float getReadBytes() {
		return readbytes;
	}
}
