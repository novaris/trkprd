package ru.novoscan.trkpd.terminals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.resources.ModConstats;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.ModUtils;
import ru.novoscan.trkpd.utils.TrackPgUtils;

public class ModGnsMinitrack implements ModConstats {
	// private static int maxPacketSize;
	static Logger logger = Logger.getLogger(ModGs.class);

	private int readbytes = 0;

	private int maxPacketSize;

	private HashMap<String, String> map = new HashMap<String, String>();

	private static String getIMEI = "#IM;";

	private static String getPacket = "#SL1;";

	private static final Pattern PatCIO = Pattern.compile("(?im)CIO;");

	private static final Pattern PatNotFound = Pattern.compile("(?im)SLE;");

	private static final Pattern PatExists = Pattern.compile("(?im)SLO;");

	private static final Pattern PatIMEI = Pattern
			.compile("(?im)IMO(\\d{15});");

	private static final char PatCTRN = 0x0D;

	private static final char PatCTRL = 0x0A;

	/* 00381$GPRMC,145954.467,V,8960.0000,N,00000.0000,E,0.00,0.00,260209,,,N*73 */
	private static final Pattern PatPacket = Pattern
			.compile("(?i)(\\d+)\\$GPRMC,(\\d+)\\.({0,1}\\d*),(V|A),(\\d+\\.{0,1}\\d*),N,(\\d+\\.{0,1}\\d*),E,(\\d+\\.{0,1}\\d*),(\\d+\\.{0,1}\\d*),(\\d+\\.{0,1}),\\S*,\\S*,\\S+\\*(\\S+)");

	private String packet;

	private String navDate;

	private String navTime;

	private SimpleDateFormat sdf = new SimpleDateFormat(DATE_SIMPLE_FORMAT);

	private int navStatus;

	private String navSog;

	private String navLatitude;

	private String navLongitude;

	private String navCource;

	private String IMEI;

	private ModUtils utl = new ModUtils();

	public ModGnsMinitrack(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader console, ModConfig conf, TrackPgUtils pgcon)
			throws ParseException, IOException {
		maxPacketSize = conf.getMaxSize();

		String data = "";
		int cread;

		// Получение от устройства CIO;
		readbytes = 0;

		while ((cread = console.read()) != -1) {
			readbytes = readbytes + 1;
			logger.debug("Byte [" + readbytes + "] : " + (char) cread);
			if (PatCTRN == cread) {
				logger.debug("CTRN found");
			} else if (PatCTRL == cread) {
				logger.debug("CTRL found");
				break;
			} else {
				data = data + (char) cread;
				if (readbytes > maxPacketSize) {
					logger.error("Incorrect Size packet data : " + data);
					data = "";
					map.clear();
					return;
				}
				if (PatCIO.matcher(data).matches()) {
					logger.debug("CIO found : " + data);
				}
			}

		}
		logger.debug("Get IMEI : " + getIMEI);
		oDs.writeUTF(getIMEI);
		data = "";
		while ((cread = console.read()) != -1) {
			readbytes = readbytes + 1;
			if (PatCTRN == cread) {
				logger.debug("CTRN found");
				// теперь проверим - пока заглушка
				if (PatIMEI.matcher(data).matches()) {
					Matcher m = PatIMEI.matcher(data);
					if (m.matches()) {
						IMEI = m.group(1);
					}
					logger.debug("IMEI : " + IMEI);
				} else {
					logger.error("IMEI incorrect : " + data);
					return;
				}
				if (pgcon.getImeiModule(IMEI) > 0) {
					logger.debug("IMEI found in database : " + IMEI);
				} else {
					logger.error("IMEI not found : " + IMEI);
					return;
				}
			} else if (PatCTRL == cread) {
				logger.debug("CTRL found");
				break;
			} else {
				data = data + (char) cread;
			}

		}
		oDs.writeUTF(getPacket);
		data = "";
		while ((cread = console.read()) != -1) {
			readbytes = readbytes + 1;
			logger.debug("Read[" + readbytes + "] " + (char) cread);
			if (PatCTRN == cread) {
				logger.debug("CTRN found");
			} else if (PatCTRL == cread) {
				if (PatPacket.matcher(data).matches()) {
					logger.debug("Read packet : " + data);
					// Разберём пакет
					packet = data;
					parsePacket();
					/*
					 * String vehicleId; int dasnUid; String dasnDateTime; float
					 * dasnLatitude; float dasnLongitude; int dasnStatus; int
					 * dasnSatUsed; int dasnZoneAlarm; int dasnMacroId; int
					 * dasnMacroSrc; float dasnSog; float dasnCource; float
					 * dasnHdop; float dasnHgeo; float dasnHmet; int dasnGpio;
					 * int dasnAdc; float dasnTemp; int8 i_spmt_id;
					 */
					map.put("vehicleId", IMEI);
					map.put("dasnUid", IMEI);
					map.put("dasnLatitude", navLatitude);
					map.put("dasnLongitude", navLongitude);
					map.put("dasnStatus", String.valueOf(navStatus));
					map.put("dasnSatUsed", null);
					map.put("dasnZoneAlarm", null);
					map.put("dasnMacroId", null);
					map.put("dasnMacroSrc", null);
					map.put("dasnSog", navSog);
					map.put("dasnCource", navCource);
					map.put("dasnHdop", null);
					map.put("dasnHgeo", null);
					map.put("dasnHmet", null);
					map.put("dasnGpio", null);
					map.put("dasnAdc", null);
					map.put("dasnTemp", null);
					map.put("i_spmt_id", Integer.toString(conf.getModType()));
					// запись в БД
					if (navStatus == 1) {
						pgcon.setDataSensor(map, sdf.parse(navDate + navTime));
						try {
							pgcon.addDataSensor();
							logger.debug("Write Database OK");
						} catch (SQLException e) {
							logger.warn("Error Writing Database : "
									+ e.getMessage());
						}
						map.clear();
					} else {
						logger.error("Status data incorrect : " + IMEI);
					}
					packet = "";
				} else if (PatExists.matcher(data).matches()) {
					logger.debug("Exists data : " + data);
					logger.debug("Send " + getPacket);
					oDs.writeUTF(getPacket);
				} else if (PatNotFound.matcher(data).matches()) {
					logger.debug("End of data : " + data);
					return;
				} else {
					logger.debug("Incorrect data : " + data);
					logger.debug("Send " + getPacket);
					oDs.writeUTF(getPacket);
				}
				data = "";
			} else {
				data = data + (char) cread;
			}
		}

		readbytes = 0;
		logger.debug("Close reader console");

	}

	private void parsePacket() throws ParseException {

		if (PatPacket.matcher(packet).matches()) {
			Matcher m = PatPacket.matcher(packet);
			if (m.matches()) {
				navTime = m.group(2);
				navLatitude = utl.getLL(m.group(5));
				navLongitude = utl.getLL(m.group(6));
				navDate = m.group(9);
				navCource = m.group(8);
				if (m.group(4).equalsIgnoreCase("V")) {
					navStatus = 0;
				} else {
					navStatus = 1;
				}

				navSog = utl.getSpeed(m.group(7));

			}
			logger.debug("Packet format correct.");
		} else {
			logger.error("Packet format incorrect.");
			navStatus = 0;
		}

	}

	public float getReadBytes() {
		return readbytes;
	}

}
