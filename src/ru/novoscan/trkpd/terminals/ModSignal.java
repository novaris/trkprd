package ru.novoscan.trkpd.terminals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.resources.ModConstats;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.ModUtils;
import ru.novoscan.trkpd.utils.TrackPgUtils;

/**
 * @author kur
 * 
 */
public class ModSignal implements ModConstats {
	private static int[] navDeviceID = new int[4];

	private static int[] navServerID = new int[4];

	// private int navPacketSize;

	// private int navPacketType;

	// private int navSoftVersion;

	// private int navNavOnOff;

	private int navDeviceStatus;

	private String navDateTime;

	private float navLatitude;

	private float navLongitude;

	// private float navHeight;

	private float navSpeed;

	private float navCource;

	// private float navHdop;

	private int navSatellitesCount;

	private String navSatellitesType = "UNDEF";

	// private int navCRC;

	// private int navCheckCRC;

	private int navAcc1;

	private int navAcc2;

	private float navPower;

	// private int navStatus;

	private float navTemp;

	private long navGPIO;

	private long navADC;

	static Logger logger = Logger.getLogger(ModSignal.class);

	private int readbytes;

	private float fullreadbytes = 0;

	// private static int maxPacketSize = 18; // длина пакета авторизации

	private HashMap<String, String> map = new HashMap<String, String>();

	private static int[] packet;

	private static int packetDataLength = 32768; // максимальная длина пакета с
													// данными

	private String navIMEI = "";

	// private String navPhone;

	// private String navPass;

	private int packetCount;

	private int[] packetHeader;

	private int packetSize;

	private int factCRCHeader;

	String signature = "";

	// private final int HEADER_LENGTH = 7; // Длина заголовка

	// private final int PACKET_LENGTH = 45; // Длина пакета

	private final int PACKET_HANDSHAKE_LENGTH = 16; // Длина пакета рукопожатия

	private String dataSignature; // Сигнатураprivate int

	private final int SIGNATURE_LENGTH = 4; // пакета - по умолчанию @NTC

	private final int CMD_SIGNATURE_LENGTH = 3;

	private final String SIGNATURE_S = "*>S"; // Сигнатура пакета S2115 NTCB

	private final String SIGNATURE_T = "*>T";

	private final String SIGNATURE_A = "*>A";

	private final int PACKET_F1 = 0x01;

	private final int PACKET_F2 = 0x02;

	private final int PACKET_LENGTH_F2 = 72;

	private final int PACKET_LENGTH_F5 = 67;

	private final int PACKET_F3 = 0x03;

	private final int PACKET_F4 = 0x04;

	private final int PACKET_F5 = 0x05;

	private final int PACKET_F15 = 0x15;

	private final int PACKET_F25 = 0x25;

	private String cmdSignature = "";

	private int[] dataSignatureArray = new int[SIGNATURE_LENGTH];

	private int[] cmdSignatureArray = new int[CMD_SIGNATURE_LENGTH];

	private DataInputStream iDsLocal;

	private int dataBlockCount;

	private int dataIndex;

	private int formatType;

	private int dataIndexBytes[] = new int[4];

	private int packetDataSeek;

	private float navPowerReserv;

	private int navDigit1;

	private int navDigit2;

	private String navDateTimeFixed;

	private final ModConfig conf;

	private final TrackPgUtils pgcon;

	private SimpleDateFormat sdf = new SimpleDateFormat(DATE_SIMPLE_FORMAT);

	public ModSignal(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader unbconsole, ModConfig conf, TrackPgUtils pgcon)
			throws ParseException {
		// int cread;
		this.conf = conf;
		this.pgcon = pgcon;

		iDsLocal = iDs;
		logger.debug("Read streems..");
		packetHeader = new int[PACKET_HANDSHAKE_LENGTH]; // пакет с нулевого
															// считается
		packet = new int[packetDataLength]; // пакет с нулевого считается
		fullreadbytes = 0;
		try {
			while (true) {
				for (int i = 0; i < PACKET_HANDSHAKE_LENGTH; i++) {
					packetHeader[i] = readByte();
				}
				// Обработаем заголовок
				parseHeader();
				if (packetSize >= CMD_SIGNATURE_LENGTH) {
					String cmd = "";
					// Считаем данные
					for (int i = 0; i < packetSize; i++) {
						packet[i] = readByte();
					}
					// Определим тип команд
					readCMDSignature();
					if (cmdSignature.equals(SIGNATURE_S)) {
						// Пакет первичной инициализации
						parseIMEI();
						if (navIMEI.length() == 0) {
							// Ошибка не определён IMEI
							logger.error("Не определён IMEI");
							throw new RuntimeException(
									"Неверные данные пакета HANDSHAKE : IMEI не определён");
						}
						cmd = "*<S";
					} else if (cmdSignature.equals(SIGNATURE_T)) {
						// Телеметрические записи
						parseDataTypeT();
						cmd = "*<T" + (char) dataIndex;
					} else if (cmdSignature.equals(SIGNATURE_A)) {
						// Архивные данные
						parseDataTypeA();
						cmd = "*<A" + (char) dataBlockCount;
					}
					// ответим
					/*
					 * Формат ответа: dataSignature +navServerID +navDeviceID
					 * +длина_сигнатуры_запроса +КонтрольнаяСуммаОтвета
					 * +КонтрольнаяСумма15байтЗаголовка +Ответ
					 */
					sendResponce(oDs, cmd);
					readbytes = 0;
				}
				/*
				 * // Сохраним в БД данные map.put("vehicleId",
				 * String.valueOf(navDeviceID)); map.put("dasnUid",
				 * String.valueOf(navDeviceID)); map.put("dasnLatitude",
				 * String.valueOf(navLatitude)); map.put("dasnLongitude",
				 * String.valueOf(navLongitude)); map.put("dasnStatus",
				 * Integer.toString(navDeviceStatus)); map.put("dasnSatUsed",
				 * Integer.toString(navSatellitesCount));
				 * map.put("dasnZoneAlarm", null); map.put("dasnMacroId", null);
				 * map.put("dasnMacroSrc", null); map.put("dasnSog",
				 * String.valueOf(navSpeed)); map.put("dasnCource",
				 * String.valueOf(navCource)); map.put("dasnHdop", null);
				 * map.put("dasnHgeo", null); map.put("dasnHmet", null);
				 * map.put("dasnGpio", null); map.put("dasnAdc",
				 * String.valueOf(navPower)); map.put("dasnTemp",
				 * String.valueOf(navTemp)); map.put("i_spmt_id",
				 * Integer.toString(conf.getModType())); // запись в БД
				 * pgcon.setDataSensor(map); try { pgcon.addDataSensor();
				 * logger.debug("Write Database OK"); } catch (SQLException e) {
				 * logger.warn("Error Writing Database : " + e.getMessage()); }
				 * map.clear();
				 */
				// sendCRC(oDs, navCRC);
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

	private void parseDataTypeT() throws IOException {
		logger.debug("Он-лайн пакет.");
		setFormatType();
		switch (getFormatType()) {
		case PACKET_F1:
			parsePacketF1();
			break;
		case PACKET_F2:
			parsePacketF2();
			break;
		case PACKET_F3:
			parsePacketF3();
			break;
		case PACKET_F4:
			parsePacketF4();
			break;
		case PACKET_F5:
			parsePacketF5();
			break;
		case PACKET_F15:
			parsePacketF15();
			break;
		case PACKET_F25:
			parsePacketF25();
			break;
		default:
			logger.error("Неверный тип пакета : " + getFormatType());
			throw new RuntimeException("Тип пакета не поддерживается : "
					+ getFormatType());
		}
	}

	private void parseDataTypeA() throws IOException, ParseException {
		logger.debug("Пакеты из энергозависимой памяти.");
		dataBlockCount = 0;
		setPacketCount();
		for (int i = 0; i < packetCount; i++) {
			setFormatType();
			switch (getFormatType()) {
			case PACKET_F1:
				parsePacketF1();
				break;
			case PACKET_F2:
				parsePacketF2();
				packetDataSeek = packetDataSeek + PACKET_LENGTH_F2;
				writeData();
				break;
			case PACKET_F3:
				parsePacketF3();
				break;
			case PACKET_F4:
				parsePacketF4();
				break;
			case PACKET_F5:
				parsePacketF5();
				packetDataSeek = packetDataSeek + PACKET_LENGTH_F5;
				writeData();
				break;
			case PACKET_F15:
				parsePacketF15();
				break;
			case PACKET_F25:
				parsePacketF25();
				break;
			default:
				logger.error("Неверный тип пакета : " + getFormatType());
				throw new RuntimeException("Тип пакета не поддерживается : "
						+ getFormatType());
			}
			dataBlockCount = dataBlockCount + 1;
		}
	}

	private void setPacketCount() {
		if (cmdSignature.equals(SIGNATURE_A)) {
			this.packetCount = packet[CMD_SIGNATURE_LENGTH];
			this.packetDataSeek = CMD_SIGNATURE_LENGTH + 1;
		} else if (cmdSignature.equals(SIGNATURE_T)) {
			this.packetCount = 1;
			this.packetDataSeek = CMD_SIGNATURE_LENGTH;
		}
		logger.debug("Количество пакетов данных : "
				+ Integer.toHexString(packetCount));
	}

	private void setFormatType() {
		if (cmdSignature.equals(SIGNATURE_A)) {
			formatType = packet[packetDataSeek];
		} else if (cmdSignature.equals(SIGNATURE_T)) {
			formatType = packet[packetDataSeek];
		}
		logger.debug("Тип пакета : " + Integer.toHexString(formatType));
	}

	private void parsePacketF25() {
		// TODO Auto-generated method stub
		logger.debug("Парсим F25");
		for (int i = 0; i < 4; i++) {
			dataIndexBytes[i] = packet[i + 1];
		}
		dataIndex = ModUtils.getIntU32(dataIndexBytes);

	}

	private void parsePacketF15() {
		// TODO Auto-generated method stub
		logger.debug("Парсим F15");
		for (int i = 0; i < 4; i++) {
			dataIndexBytes[i] = packet[i + 1];
		}
		dataIndex = ModUtils.getIntU32(dataIndexBytes);
	}

	private void parsePacketF5() {
		logger.debug("Парсим F5");
		// navPacketSize = PACKET_LENGTH_F5;
		// navPacketType = PACKET_F5;
		// navSoftVersion = 2115;
		// Пропускаем 1 байт - тип определили выше.
		int k = 1; // 0
		int id = ModUtils.getIntU32(packet, packetDataSeek + k); // 1-4 ид
																	// записи
		logger.debug("Ид записи : " + id);
		k = 5;
		int iventId = ModUtils.getIntU16(packet, packetDataSeek + k); // 5-6 ид
																		// типа
																		// события
		logger.debug("Ид события : " + iventId);
		k = 7;
		navDateTimeFixed = ModUtils.getDateTimeSignal(packet, packetDataSeek
				+ k); // 8-13 Дата фиксации события
		logger.debug("Время события : " + navDateTimeFixed);
		k = 13; // Статус охраны
		k = 14; // Состояние функциональных модулей
		k = 15; // Уровень GSM
		k = 16; // Спутники 2-7 бит
		navSatellitesCount = packet[packetDataSeek + k] >> 2 & 0xff; // 38
		k = 17; // Датчики цифровых входов 0 бит зажигание

		k = 18; // Наряжение на основном питании
		navPower = (float) (ModUtils.getIntU16(packet, packetDataSeek + k) / 1000.0); // Питание
																						// в
																						// миливольтах
																						// 21-22
		logger.debug("Питание в вольтах : " + navPower);
		k = 20;
		navPowerReserv = (float) (ModUtils
				.getIntU16(packet, packetDataSeek + k) / 1000.0); // Питание в
																	// миливольтах
																	// 23-24
		logger.debug("Резервное питание в вольтах : " + navPowerReserv);
		// k = 25;
		// navTemp =(((packet[packetDataSeek+k]) << 8) +
		// (packet[packetDataSeek+k+1])); // Температура в градусах
		// logger.debug("Температура градусы : " + navTemp);
		k = 22;
		navAcc1 = ModUtils.getIntU16(packet, packetDataSeek + k); // Значение на
																	// аналоговом
																	// входе 1
																	// 27-28
		logger.debug("Значение ACC1 : " + ((float) navAcc1 / 1000.0));
		k = 24;
		navAcc2 = ModUtils.getIntU16(packet, packetDataSeek + k); // Значение на
																	// аналоговом
																	// входе 2
																	// 29-30
		logger.debug("Значение ACC2 : " + ((float) navAcc2) / 1000.0);
		navADC = navAcc1 + (navAcc2 << 16);
		k = 26;
		navDigit1 = ModUtils.getIntU32(packet, packetDataSeek + k); // Цифровой
																	// вход
																	// (Количество
																	// импульсов
																	// подсчитанное
																	// на момент
																	// события)
																	// вход 1
																	// 31-34
		k = 30;
		navDigit2 = ModUtils.getIntU32(packet, packetDataSeek + k); // Цифровой
																	// вход
																	// (Количество
																	// импульсов
																	// подсчитанное
																	// на момент
																	// события)
																	// вход 1
																	// 35-38
		navGPIO = navDigit1 + (navDigit2 << 8);
		// Спутники - 2-7 бит (0-1 байт выхода 0 - ВЫКЛ 1 - ВКЛ)
		// Состояние навигации
		k = 34;
		int navType = packet[packetDataSeek + k] >> 2 & 0xff; // 38
		if (navType == 1) {
			navSatellitesType = "GPS";
		} else if (navType == 2) {
			navSatellitesType = "GLONASS";
		} else if (navType == 3) {
			navSatellitesType = "GLONASS/GPS";
		}
		logger.debug("Тип определения местоположения : " + navSatellitesType);
		// navNavOnOff = packet[packetDataSeek+k] & 0x01;
		navDeviceStatus = (packet[packetDataSeek + k] >> 1) & 0x01;
		logger.debug("Валидность координат : " + navDeviceStatus);
		if (navDeviceStatus == 0) {
			navSatellitesCount = 0; // координаты невалидны!
		}
		k = 35;
		navDateTime = ModUtils.getDateTimeSignal(packet, packetDataSeek + k); // 35-40
																				// Дата
																				// данных
		logger.debug("Время координат : " + navDateTime);
		k = 41;
		navLatitude = ModUtils.convRadianToDegree(Float.intBitsToFloat(ModUtils
				.getIntU32(packet, packetDataSeek + k)));
		logger.debug("Широта : " + navLatitude);
		k = 45;
		navLongitude = ModUtils
				.convRadianToDegree(Float.intBitsToFloat(ModUtils.getIntU32(
						packet, packetDataSeek + k)));
		logger.debug("Долгота : " + navLongitude);
		k = 49;
		navSpeed = Float.intBitsToFloat(ModUtils.getIntU32(packet,
				packetDataSeek + k));
		logger.debug("Скорость : " + navSpeed);
		k = 53;
		navCource = ModUtils.getIntU16(packet, packetDataSeek + k);
		logger.debug("Курс : " + navCource);
	}

	private void parsePacketF4() {
		// TODO Auto-generated method stub
		logger.debug("Парсим F4");
		for (int i = 0; i < 4; i++) {
			dataIndexBytes[i] = packet[i + 1];
		}
		dataIndex = ModUtils.getIntU32(dataIndexBytes);

	}

	private void parsePacketF3() {
		// TODO Auto-generated method stub
		logger.debug("Парсим F4");
		for (int i = 0; i < 4; i++) {
			dataIndexBytes[i] = packet[i + 2];
		}
		dataIndex = ModUtils.getIntU32(dataIndexBytes);

	}

	private void parsePacketF2() {
		// Обработка формата пакета F2
		logger.debug("Парсим F2");
		// navPacketSize = PACKET_LENGTH_F2;
		// navPacketType = PACKET_F2;
		// navSoftVersion = 2110;
		// Пропускаем 2 байта - тип определили выше.
		int k = 1; // 0-1
		// int id = TRKUtils.getIntU32(packet,packetDataSeek+k); // 2-5 ид
		// записи
		k = 6;
		// int iventId = TRKUtils.getIntU16(packet,packetDataSeek+k); // 6-7 ид
		// типа события
		k = 8;
		navDateTimeFixed = ModUtils.getDateTimeSignal(packet, packetDataSeek
				+ k); // 8-13 Дата фиксации события
		logger.debug("Время события : " + navDateTimeFixed);
		k = 14; // Статус охраны
		k = 15; // Состояние функциональных модулей
		k = 16; // Уровень GSM
		k = 17; // Состояние выходов 17-18
		k = 19; // Датчики цифровых входов 19-20
		k = 21; // Наряжение на основном питании
		navPower = (float) (ModUtils.getIntU16(packet, packetDataSeek + k) / 1000.0); // Питание
																						// в
																						// миливольтах
																						// 21-22
		logger.debug("Питание в вольтах : " + navPower);
		k = 23;
		navPowerReserv = (float) (ModUtils
				.getIntU16(packet, packetDataSeek + k) / 1000.0); // Питание в
																	// миливольтах
																	// 23-24
		logger.debug("Резервное питание в вольтах : " + navPowerReserv);
		k = 25;
		navTemp = (((packet[packetDataSeek + k]) << 8) + (packet[packetDataSeek
				+ k + 1])); // Температура в градусах
		logger.debug("Температура градусы : " + navTemp);
		k = 27;
		navAcc1 = ModUtils.getIntU16(packet, packetDataSeek + k); // Значение на
																	// аналоговом
																	// входе 1
																	// 27-28
		logger.debug("Значение ACC1 : " + ((float) navAcc1 / 1000.0));
		k = 29;
		navAcc2 = ModUtils.getIntU16(packet, packetDataSeek + k); // Значение на
																	// аналоговом
																	// входе 2
																	// 29-30
		logger.debug("Значение ACC2 : " + ((float) navAcc2 / 1000.0));
		navADC = navAcc1 + (navAcc2 << 16);
		k = 31;
		navDigit1 = ModUtils.getIntU32(packet, packetDataSeek + k); // Цифровой
																	// вход
																	// (Количество
																	// импульсов
																	// подсчитанное
																	// на момент
																	// события)
																	// вход 1
																	// 31-34
		k = 35;
		navDigit2 = ModUtils.getIntU32(packet, packetDataSeek + k); // Цифровой
																	// вход
																	// (Количество
																	// импульсов
																	// подсчитанное
																	// на момент
																	// события)
																	// вход 1
																	// 35-38
		navGPIO = navDigit1 + (navDigit2 << 8); // Спутники - 2-7 бит (0-1 байт
												// выхода 0 - ВЫКЛ 1 - ВКЛ)
		// Состояние навигации
		k = 39;
		navSatellitesCount = packet[packetDataSeek + k] >> 2 & 0xff; // 38

		if (navSatellitesCount == 1) {
			navSatellitesType = "GPS";
		} else if (navSatellitesCount == 2) {
			navSatellitesType = "GLONASS";
		} else if (navSatellitesCount == 3) {
			navSatellitesType = "GLONASS/GPS";
		}
		logger.debug("Тип определения местоположения : " + navSatellitesType);
		navSatellitesCount = 0;
		// navNavOnOff = packet[packetDataSeek+k] & 0x01;
		navDeviceStatus = (packet[packetDataSeek + k] >> 1) & 0x01;
		logger.debug("Валидность координат : " + navDeviceStatus);
		if (navDeviceStatus == 1) {
			navSatellitesCount = 4;
		}
		k = 40;
		navDateTime = ModUtils.getDateTimeSignal(packet, packetDataSeek + k); // 40-45
																				// Дата
																				// данных
		logger.debug("Время координат : " + navDateTime);
		k = 46;
		navLatitude = ModUtils.convRadianToDegree(Float.intBitsToFloat(ModUtils
				.getIntU32(packet, packetDataSeek + k)));
		logger.debug("Широта : " + navLatitude);
		k = 50;
		navLongitude = ModUtils
				.convRadianToDegree(Float.intBitsToFloat(ModUtils.getIntU32(
						packet, packetDataSeek + k)));
		logger.debug("Долгота : " + navLongitude);
		k = 54;
		navSpeed = Float.intBitsToFloat(ModUtils.getIntU32(packet,
				packetDataSeek + k));
		logger.debug("Скорость : " + navSpeed);
		k = 58;
		navCource = ModUtils.getIntU16(packet, packetDataSeek + k);
		logger.debug("Курс : " + navCource);
	}

	private void parsePacketF1() {
		// TODO Auto-generated method stub
		for (int i = 0; i < 4; i++) {
			dataIndexBytes[i] = packet[i + 2];
		}
		dataIndex = ModUtils.getIntU32(dataIndexBytes);
	}

	private int getFormatType() {
		return formatType;
	}

	private void sendResponce(DataOutputStream oDs, String cmd)
			throws IOException {
		int k = 0;
		for (int i = 0; i < SIGNATURE_LENGTH; i++) {
			packet[k] = dataSignatureArray[i];
			k++;
		}
		for (int i = 0; i < navDeviceID.length; i++) {
			packet[k] = navDeviceID[i];
			k++;
		}
		for (int i = 0; i < navServerID.length; i++) {
			packet[k] = navServerID[i];
			k++;
		}
		int cmdLength = cmd.length();
		packet[k] = (byte) cmdLength & 0xff;
		k++;
		cmdLength >>= 8;
		packet[k] = (byte) cmdLength & 0xff;
		k++;
		packet[k] = ModUtils.getSumXor(cmd, cmd.length());
		k++;
		packet[k] = ModUtils.getSumXor(packet, 15);
		k++;
		for (int i = 0; i < cmd.length(); i++) {
			packet[k] = cmd.charAt(i);
			k++;
		}
		for (int i = 0; i < k; i++) {
			oDs.write(packet[i]);
			logger.debug("Write byte[" + i + "] : "
					+ Integer.toHexString(packet[i]));
		}
		// oDs.flush();

	}

	private void parseIMEI() {
		navIMEI = "";
		for (int i = 4; i < packetSize; i++) { // первый символ ;
			navIMEI = navIMEI + (char) packet[i];
		}
		logger.debug("IMEI блока : " + navIMEI);
	}

	private void parseHeader() {
		// Разбор заголовка - 16 Байт.
		dataSignature = "";
		int k = 0;
		for (int i = 0; i < SIGNATURE_LENGTH; i++) {
			dataSignature = dataSignature + (char) packetHeader[i];
			dataSignatureArray[i] = packetHeader[i];
			k++;
		}
		logger.debug("Сигнатура пакетов данных : " + dataSignature);
		// throw new RuntimeException("Ошибка сигнатуры HANDSHAKE : " +
		// signature);
		// Идентификатор получателя
		for (int i = 0; i < 4; i++) {
			navServerID[i] = packetHeader[k];
			k++;
		}
		logger.debug("Ид сервера : " + ModUtils.getIntByte(navServerID));
		// Идентификатор отправителя
		for (int i = 0; i < 4; i++) {
			navDeviceID[i] = packetHeader[k];
			k++;
		}
		logger.debug("Ид терминала : " + ModUtils.getIntByte(navDeviceID));
		packetSize = (int) (packetHeader[12] & 0xff)
				+ (int) ((packetHeader[13] & 0xff) << 8);
		logger.debug("Размер пакета данных : " + packetSize);
		logger.debug("Контрольная сумма данных : " + packetHeader[14]);
		logger.debug("Контрольная сумма заголовка : " + packetHeader[15]);
		factCRCHeader = ModUtils.getSumXor(packetHeader, 15);
		logger.debug("Контрольная сумма заголовка (фактическая) : "
				+ factCRCHeader);
	}

	private void readCMDSignature() throws IOException {
		cmdSignature = "";
		for (int i = 0; i < CMD_SIGNATURE_LENGTH; i++) {
			cmdSignature = cmdSignature + (char) packet[i];
			cmdSignatureArray[i] = packet[i];
		}
		logger.debug("Сигнатура команды : " + cmdSignature);
	}

	private int readByte() throws IOException {
		byte bread = iDsLocal.readByte();
		packet[readbytes] = bread;
		logger.debug("packet[" + readbytes + "] : "
				+ Integer.toHexString(packet[readbytes]));
		readbytes++;
		fullreadbytes++;
		return bread;
	}

	public float getReadBytes() {
		return fullreadbytes;
	}

	private void writeData() throws ParseException {
		// Сохраним в БД данные
		map.put("vehicleId", String.valueOf(ModUtils.getIntByte(navDeviceID)));
		map.put("dasnUid", String.valueOf(ModUtils.getIntByte(navDeviceID)));
		map.put("dasnLatitude", String.valueOf(navLatitude));
		map.put("dasnLongitude", String.valueOf(navLongitude));
		map.put("dasnStatus", Integer.toString(navDeviceStatus));
		map.put("dasnSatUsed", Integer.toString(navSatellitesCount));
		map.put("dasnZoneAlarm", null);
		map.put("dasnMacroId", null);
		map.put("dasnMacroSrc", null);
		map.put("dasnSog", String.valueOf(navSpeed));
		map.put("dasnCource", String.valueOf(navCource));
		map.put("dasnHdop", null);
		map.put("dasnHgeo", null);
		map.put("dasnHmet", null);
		map.put("dasnGpio", String.valueOf(navGPIO));
		map.put("dasnAdc", String.valueOf(navADC));
		map.put("dasnTemp", String.valueOf((int) navTemp));
		map.put("i_spmt_id", Integer.toString(this.conf.getModType())); // запись
																		// в
		// БД
		map.put("dasnXML", "<xml><gl>" + navSatellitesType + "</gl><pw1>"
				+ navPower + "</pw1><dt>" + navDateTimeFixed + "</dt><pw>"
				+ navPowerReserv + "</pw></xml>");
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
