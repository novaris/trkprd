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

public class ModGnsMinitrack extends Terminal {
	// private static int maxPacketSize;
	static Logger logger = Logger.getLogger(ModGs.class);

	private int readbytes = 0;

	private int maxPacketSize;

	private static String getIMEI = "#IM;";

	private static String getPacket = "#SL1;";

	private final SimpleDateFormat DSF = new SimpleDateFormat(
			DATE_SIMPLE_FORMAT);

	private static final Pattern PAT_CIO = Pattern.compile("(?im)CIO;");

	private static final Pattern PAT_NOT_FOUND = Pattern.compile("(?im)SLE;");

	private static final Pattern PAT_EXISTS = Pattern.compile("(?im)SLO;");

	private static final Pattern PAT_IMEI = Pattern
			.compile("(?im)IMO(\\d{15});");

	/* 00381$GPRMC,145954.467,V,8960.0000,N,00000.0000,E,0.00,0.00,260209,,,N*73 */
	private static final Pattern PAT_PACKET = Pattern
			.compile("(?i)(\\d+)\\$GPRMC,(\\d+)\\.({0,1}\\d*),(V|A),(\\d+\\.{0,1}\\d*),N,(\\d+\\.{0,1}\\d*),E,(\\d+\\.{0,1}\\d*),(\\d+\\.{0,1}\\d*),(\\d+\\.{0,1}),\\S*,\\S*,\\S+\\*(\\S+)");

	private String packet;

	private ModUtils utl = new ModUtils();

	public ModGnsMinitrack(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader console, ModConfig conf, TrackPgUtils pgcon)
			throws ParseException, IOException, SQLException {
		this.setDasnType(conf.getModType());
		maxPacketSize = conf.getMaxSize();
		String data = "";
		int cread;
		// Получение от устройства CIO;
		readbytes = 0;
		while ((cread = console.read()) != -1) {
			readbytes = readbytes + 1;
			logger.debug("Byte [" + readbytes + "] : " + (char) cread);
			if (CTRN == cread) {
				logger.debug("CTRN found");
			} else if (CTRL == cread) {
				logger.debug("CTRL found");
				break;
			} else {
				data = data + (char) cread;
				if (readbytes > maxPacketSize) {
					logger.error("Incorrect Size packet data : " + data);
					data = "";
					return;
				}
				if (PAT_CIO.matcher(data).matches()) {
					logger.debug("CIO found : " + data);
				}
			}

		}
		logger.debug("Get IMEI : " + getIMEI);
		oDs.writeUTF(getIMEI);
		data = "";
		while ((cread = console.read()) != -1) {
			readbytes = readbytes + 1;
			if (CTRN == cread) {
				logger.debug("CTRN found");
				// теперь проверим - пока заглушка
				if (PAT_IMEI.matcher(data).matches()) {
					Matcher m = PAT_IMEI.matcher(data);
					if (m.matches()) {
						dasnUid = m.group(1);
					}
					dasnStatus = NavigationStatus.OK;
					logger.debug("UID : " + dasnUid);
				} else {
					dasnStatus = NavigationStatus.ERR;
					logger.error("UID неверен : " + data);
					return;
				}
				if (pgcon.getImeiModule(dasnUid) > 0) {
					logger.debug("UID найден в БД : " + dasnUid);
					dasnStatus = NavigationStatus.OK;
				} else {
					logger.error("UID найден в БД : " + dasnUid);
					dasnStatus = NavigationStatus.ERR;
					return;
				}
			} else if (CTRL == cread) {
				logger.debug("Найден CTRN");
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
			if (CTRN == cread) {
				logger.debug("Найден CTRN");
			} else if (CTRL == cread) {
				if (PAT_PACKET.matcher(data).matches()) {
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
					// запись в БД
					if (dasnStatus == NavigationStatus.OK) {
						pgcon.setDataSensorValues(dataSensor);
						pgcon.addDataSensor();
						logger.debug("Write Database OK");
					} else {
						logger.error("Status data incorrect : " + dasnUid);
					}
					this.clear();
					packet = "";
				} else if (PAT_EXISTS.matcher(data).matches()) {
					logger.debug("Exists data : " + data);
					logger.debug("Send " + getPacket);
					oDs.writeUTF(getPacket);
				} else if (PAT_NOT_FOUND.matcher(data).matches()) {
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

		if (PAT_PACKET.matcher(packet).matches()) {
			Matcher m = PAT_PACKET.matcher(packet);
			if (m.matches()) {
				dasnDatetime = DSF.parse(m.group(9) + m.group(2));
				dasnLatitude = Double.valueOf(utl.getLL(m.group(5)));
				dasnLongitude = Double.valueOf(utl.getLL(m.group(6)));
				dasnCourse = Double.valueOf(m.group(8));
				if (m.group(4).equalsIgnoreCase("V")) {
					dasnStatus = NavigationStatus.OK;
				} else {
					dasnStatus = NavigationStatus.ERR;
				}

				dasnSog = Double.valueOf(utl.getSpeed(m.group(7)));

			}
			logger.debug("Формат пакета верен.");
			dasnStatus = NavigationStatus.OK;
		} else {
			logger.error("Не корректный формат пакета" + packet);
			dasnStatus = NavigationStatus.ERR;
		}

	}

	public float getReadBytes() {
		return readbytes;
	}

}
