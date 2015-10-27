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
//import ru.novoscan.trkpd.utils.TRKUtils;
import ru.novoscan.trkpd.utils.TrackPgUtils;

/**
 * 
 */

/**
 * @author Kurensky A. Evgeny
 * 
 */
public class ModSt270 extends Terminal {
	// ST270STT;111111;031;20110907;07:21:54;06400;+37.479616;+126.886183;000.000;000.00;6;8;1;71.7;0;0;36;9.70;1000000000;0;0;
	// 0;0;0;0.00;0.00;2;00072
	// ST270STT;216314;032;20120310;08:33:49;983c08;+54.996794;+082.832318;000.000;000.00;5;4;1;58.2;0;0;235339;11.70;0000000000;0;0;0;0;0;0.00;0.00;1;09381
	/*
	 * Field Definitions Remark01 HDR “ST270STT” Status report header02 DEV_ID 6
	 * char Device ID03 SW_VER 3 char Software Release Version04 DATE 8 char GPS
	 * date (yyyymmdd)05 TIME 8 char GPS time (hh:mm:ss)06 CELL String Location
	 * Code ID(3 digits hex) + Serving Cell BSIC(2 digits decimal)07 LAT String
	 * Latitude (+/-xx.xxxxxx)08 LON String Longitude (+/-xxx.xxxxxx)09 SPD
	 * String Speed in km/h This value returns to 0 when it is over than 200,000
	 * km.10 CRS String Course over ground in degree11 SATT_GPS String Number of
	 * GPS satellites12 SATT_GLONASS String Number of GLONASS satellites13 FIX
	 * „1‟ or „0‟ Position is fixed (1), Position is not fixed (0)14 ALT String
	 * Altitude in meter15 PULSE_IN1 0 Pulse count16 PULSE_IN2 0 Pulse count17
	 * DIST String Traveled distance in meters.18 PWR_VOLT String Voltage value
	 * of main power19 I/O 10 char Current I/O status of inputs and outputs.
	 * Ignition+Input1+Input2+Input3+Input4+Input5+Out1+Out2+Out3+Out4 Ignition
	 * : „1‟ (ON), „0‟ (OFF) Input1 ~ Input5 : „1‟ (Ground, Shorted), „0‟
	 * (Opened) Out1 ~ Out4 : „1‟ (Active), „0‟ (Inactive)20 AN1 ~ AN4 String
	 * Analog to Digital converted value. 0 ~ 102321 TO String Odometer in Km/h
	 * 22 TF String Total fuel used in Lts23 VS String Vehicle Speed In Km/h24
	 * MODE 1 char „1‟ = Idle mode (Parking) „2‟ = Active Mode (Driving)25
	 * MSG_NUM 5 char Message number After “29999”, message number returns to
	 * „00000”.
	 */
	static Logger logger = Logger.getLogger(ModSt270.class);

	private static final Pattern patternSTT = Pattern.compile(".*(ST270STT)"
			+ ";(\\d{6})" + ";(\\d{3})" + ";(\\d{8})"
			+ ";(\\d{2}:\\d{2}:\\d{2})" + ";([0-9A-Fa-f]+)"
			+ ";([+-]\\d+\\.{0,1}\\d*)" + ";([+-]\\d+\\.{0,1}\\d*)"
			+ ";(\\d+\\.{0,1}\\d*)" + ";(\\d+\\.{0,1}\\d*)" + ";(\\d+)"
			+ ";(\\d+)" + ";(\\d+)" + ";([-+]*\\d+\\.{0,1}\\d*)"
			+ ";(0);(0);(\\d+)" + ";([-+]*\\d+\\.{0,1}\\d*)" + ";([0,1]{10})"
			+ ";(\\d+)" + ";(\\d+)" + ";(\\d+)" + ";(\\d+)" + ";(\\d+)"
			+ ";([-+]*\\d+\\.{0,1}\\d*)" + ";([-+]*\\d+\\.{0,1}\\d*)"
			+ ";(\\d)" + ";(\\d+).*");

	private float readbytes = 0;

	private float packetSize;

	private int maxPacketSize;

	private final SimpleDateFormat DSF = new SimpleDateFormat(DATE_FORMAT);

	// private TRKUtils utl = new TRKUtils();

	public ModSt270(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader console, ModConfig conf, TrackPgUtils pgcon)
			throws ParseException, IOException {
		maxPacketSize = conf.getMaxSize();

		int cread;
		String slog = "";
		packetSize = 0;
		while ((cread = console.read()) != -1) {
			readbytes = readbytes + 1;
			if (packetSize > maxPacketSize) {
				logger.error("Over size : " + packetSize);
				return;
			} else if (cread == 0x0d) {
				// System.out.println("Data : " + slog);
				Matcher m = patternSTT.matcher(slog);
				if (m.matches()) {
					/*
					 * String vehicleId; int dasnUid; String dasnDateTime; float
					 * dasnLatitude; float dasnLongitude; int dasnStatus; int
					 * dasnSatUsed; int dasnZoneAlarm; int dasnMacroId; int
					 * dasnMacroSrc; float dasnSog; float dasnCource; float
					 * dasnHdop; float dasnHgeo; float dasnHmet; int dasnGpio;
					 * int dasnAdc; float dasnTemp; int8 i_spmt_id;
					 */
					
					dataSensor.setDasnDatetime(DSF.parse(m.group(3)));
					dataSensor.setDasnUid(m.group(2));
					dataSensor.setDasnLatitude(Double.valueOf(m.group(7)));
					dataSensor.setDasnLongitude(Double.valueOf(m.group(8)));
					dataSensor.setDasnStatus(Long.valueOf(m.group(13)));
					dataSensor.setDasnSatUsed(Long.valueOf(m.group(11)));
					dataSensor.setDasnSog(Double.valueOf(m.group(9)));
					dataSensor.setDasnCourse(Double.valueOf(m.group(10)));
					dataSensor.setDasnHdop(Double.valueOf(m.group(21)));
					dataSensor.setDasnHgeo(Double.valueOf(m.group(14)));
					dataSensor.setDasnGpio(Long.valueOf(m.group(19)));
					dataSensor.setDasnAdc(Long.valueOf(m.group(20)));

					dasnValues.put("GL",m.group(12));
					dataSensor.setDasnValues(dasnValues);

					pgcon.setDataSensorValues(dataSensor);
					try {
						pgcon.addDataSensor();
						logger.debug("Writing Database : " + slog);
					} catch (SQLException e) {
						logger.warn("Error Writing Database : "
								+ e.getMessage());
					}
					slog = "";
					packetSize = 0;
				} else {
					logger.error("Unknown packet type : " + slog);
					slog = "";
					packetSize = 0;
				}
				this.clear();
			} else {
				packetSize = packetSize + 1;
				slog = slog + (char) cread;
				logger.debug("Data : " + (char) cread);
			}


		}
		logger.debug("Close reader console");
	}

	public float getReadBytes() {
		return readbytes;
	}
}
