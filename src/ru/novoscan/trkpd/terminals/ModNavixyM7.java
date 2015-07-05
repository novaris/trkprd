package ru.novoscan.trkpd.terminals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.resources.ModConstats;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.TrackPgUtils;

public class ModNavixyM7 implements ModConstats {
	static Logger logger = Logger.getLogger(ModNavixyM7.class);

	private static String navDeviceID;

	private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

	private SimpleDateFormat sdf = new SimpleDateFormat(DATE_SIMPLE_FORMAT);

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

	private HashMap<String, String> map = new HashMap<String, String>();

	private static int keepAliveLength = 8;

	int[] keepAlive = new int[keepAliveLength];

	int[] packet = new int[maxPacketSize];

	private DataOutputStream oDs;

	private DataInputStream iDs;

	private String navDateTime;

	private String navLatitude;

	private String navLongitude;

	private String navSpeed;

	private String navCource;

	private String navSatellitesCount;

	private float navPower;

	private String navDeviceStatus;

	private int packetCount;

	private String navGsm;

	private String navHdop;

	private static long deviceId;

	private static int aliveId;

	private static ModConfig conf;

	private static TrackPgUtils pgcon;

	public ModNavixyM7(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader console, ModConfig conf, TrackPgUtils pgcon)
			throws ParseException {
		ModNavixyM7.conf = conf;
		ModNavixyM7.pgcon = pgcon;
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		maxPacketSize = conf.getMaxSize();
		this.oDs = oDs;
		this.iDs = iDs;
		logger.debug("Read streems..");
		int i = 0;
		packetCount = 0;
		try {
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
						deviceId = (keepAlive[7] & 0xff); // long тип
						deviceId = keepAlive[4] + ((keepAlive[5] & 0xff) << 8)
								+ ((keepAlive[6] & 0xff) << 16)
								+ (deviceId << 24);
						logger.debug("Device Id : " + deviceId);
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
		} catch (SocketTimeoutException e) {
			logger.error("Close connection : " + e.getMessage());
			logger.debug("Количество принятых пакетов : " + packetCount);
		} catch (IOException e3) {
			logger.warn("IO socket error : " + e3.getMessage());
			logger.debug("Количество принятых пакетов : " + packetCount);
		} catch (RuntimeException e4) {
			logger.warn("Packet parse error : " + e4.getMessage());
			logger.debug("Количество принятых пакетов : " + packetCount);
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
			navDeviceID = m.group(1);
			navDateTime = date + m.group(3);
			navLongitude = m.group(4);
			navLatitude = m.group(5);
			navSpeed = m.group(6);
			navCource = m.group(7);
			navHdop = m.group(8);
			navSatellitesCount = m.group(9);
			navPower = Float.valueOf(m.group(11));
			navDeviceStatus = m.group(12);
			navGsm = m.group(13);
			writeData();
			packetCount++;
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
		map.put("vehicleId", String.valueOf(navDeviceID));
		map.put("dasnUid", String.valueOf(navDeviceID));
		map.put("dasnLatitude", String.valueOf(navLatitude));
		map.put("dasnLongitude", String.valueOf(navLongitude));
		map.put("dasnStatus", navDeviceStatus);
		map.put("dasnSatUsed", navSatellitesCount);
		map.put("dasnZoneAlarm", null);
		map.put("dasnMacroId", null);
		map.put("dasnMacroSrc", null);
		map.put("dasnSog", String.valueOf(navSpeed));
		map.put("dasnCource", navCource);
		map.put("dasnHdop", navHdop);
		map.put("dasnHgeo", null);
		map.put("dasnHmet", null);
		map.put("dasnGpio", null);
		map.put("dasnAdc", String.valueOf((int) Math.round(navPower)));
		map.put("i_spmt_id", Integer.toString(ModNavixyM7.conf.getModType())); // запись
																				// в
		// БД
		map.put("dasnXML", "<xml><pw>" + navPower + "</pw><gsm>" + navGsm
				+ "</gsm></xml>");
		pgcon.setDataSensor(map, sdf.parse(navDateTime));
		try {
			pgcon.addDataSensor();
			logger.debug("Write Database OK");
		} catch (SQLException e) {
			logger.warn("Error Writing Database : " + e.getMessage());
		}
		map.clear();

	}

}
