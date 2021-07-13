package ru.novoscan.trkpd.terminals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.domain.Terminal;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.ModUtils;
import ru.novoscan.trkpd.utils.TrackPgUtils;

/**
 * @author kur
 * 
 */
public class ModSignal extends Terminal {

	private static int[] navClientId = new int[4];

	private static int[] navServerId = new int[4];

	private int navSatellitesType = -1;

	// private int navCRC;

	// private int navCheckCRC;

	private int navAcc1;

	private int navAcc2;

	static Logger logger = Logger.getLogger(ModSignal.class);

	private int readbytes;

	private float fullreadbytes = 0;

	private static int[] packet;

	private static int packetDataLength = 32768; // максимальная длина пакета с

	// private String navPhone;

	// private String navPass;

	private int packetCount;

	private int[] packetHeader;

	private int packetSize;

	private int factCRCHeader;

	// private final int HEADER_LENGTH = 7; // Длина заголовка

	// private final int PACKET_LENGTH = 45; // Длина пакета

	private final int PACKET_HANDSHAKE_LENGTH = 16; // Длина пакета рукопожатия

	private String dataSignature; // Сигнатураprivate int

	private final int SIGNATURE_LENGTH = 4; // пакета - по умолчанию @NTC

	private final int CMD_SIGNATURE_LENGTH = 3;

	private final String SIGNATURE_S = "*>S"; // Сигнатура пакета S2115 NTCB

	private final String SIGNATURE_T = "*>T";

	private final String SIGNATURE_A = "*>A";

	private final String SIGNATURE_RESPONCE_S = "*<S"; // Сигнатура пакета S2115
	
	private final String SIGNATURE_RESPONCE_T = "*<T";

	private final String SIGNATURE_RESPONCE_A = "*<A";

	private final int PACKET_F1 = 0x01;

	private final int PACKET_F2 = 0x02;

	private final int PACKET_LENGTH_F2 = 72;

	private final int PACKET_LENGTH_F5 = 67;

	private final int PACKET_LENGTH_F6 = 111; // ???
	
	private static final int PACKET_LENGTH_F15 = 82; // 2 байта заголовок пакета

	private static final int PACKET_LENGTH_F25 = 86; // 2 байта заголовок

	private final int PACKET_F3 = 0x03;

	private final int PACKET_F4 = 0x04;

	private final int PACKET_F5 = 0x05;

	private final int PACKET_F6 = 0x06;

	private final int PACKET_F15 = 0x15;

	private final int PACKET_F25 = 0x25;
	
	private final int PACKET_COUNT_T = 1;

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

	private final TrackPgUtils pgcon;

	private SimpleDateFormat DSF = new SimpleDateFormat(DATE_SIMPLE_FORMAT);

	private Double navLls1;

	private Double navLls2;

	private Double navLls3;

	private String uid;

	public ModSignal(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader unbconsole, ModConfig conf, TrackPgUtils pgcon)
			throws ParseException, IOException {
		this.setDasnType(conf.getModType());
		this.pgcon = pgcon;
		iDsLocal = iDs;
		logger.debug("Чтение потока...");
		packetHeader = new int[PACKET_HANDSHAKE_LENGTH]; // пакет с нулевого
															// считается
		packet = new int[packetDataLength]; // пакет с нулевого считается
		fullreadbytes = 0;

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
					if (uid.length() == 0) {
						// Ошибка не определён IMEI
						logger.error("Не определён IMEI");
						throw new RuntimeException(
								"Неверные данные пакета HANDSHAKE : IMEI не определён");
					}
					cmd = SIGNATURE_RESPONCE_S;
				} else if (cmdSignature.equals(SIGNATURE_T)) {
					// Телеметрические записи
					parseDataTypeT();
					cmd = SIGNATURE_RESPONCE_T + (char) PACKET_COUNT_T;
				} else if (cmdSignature.equals(SIGNATURE_A)) {
					// Архивные данные
					parseDataTypeA();
					cmd = SIGNATURE_RESPONCE_A + (char) dataBlockCount;
				}
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
			 * Integer.toString(navSatellitesCount)); map.put("dasnZoneAlarm",
			 * null); map.put("dasnMacroId", null); map.put("dasnMacroSrc",
			 * null); map.put("dasnSog", String.valueOf(navSpeed));
			 * map.put("dasnCource", String.valueOf(navCource));
			 * map.put("dasnHdop", null); map.put("dasnHgeo", null);
			 * map.put("dasnHmet", null); map.put("dasnGpio", null);
			 * map.put("dasnAdc", String.valueOf(navPower)); map.put("dasnTemp",
			 * String.valueOf(navTemp)); map.put("i_spmt_id",
			 * Integer.toString(conf.getModType())); // запись в БД
			 * pgcon.setDataSensor(map); try { pgcon.addDataSensor();
			 * logger.debug("Write Database OK"); } catch (SQLException e) {
			 * logger.warn("Error Writing Database : " + e.getMessage()); }
			 * map.clear();
			 */
			// sendCRC(oDs, navCRC);
		}

	}

	private void parseDataTypeT() throws IOException, ParseException {
		logger.debug("Он-лайн пакет.");
		setPacketCount();
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
			writeData();
			break;
		case PACKET_F5:
			parsePacketF5();
			writeData();
			break;
		case PACKET_F6:
			parsePacketF6();
			writeData();
			break;
		case PACKET_F15:
			parsePacketF15();
			writeData();
			break;
		case PACKET_F25:
			parsePacketF25();
			writeData();
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
			logger.debug("Смещение : " + packetDataSeek);
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
			case PACKET_F6:
				parsePacketF6();
				packetDataSeek = packetDataSeek + PACKET_LENGTH_F6;
				writeData();
				break;
			case PACKET_F15:
				parsePacketF15();
				packetDataSeek = packetDataSeek + PACKET_LENGTH_F15;
				writeData();
				break;
			case PACKET_F25:
				parsePacketF25();
				packetDataSeek = packetDataSeek + PACKET_LENGTH_F25;
				writeData();
				break;
			default:
				logger.error("Неверный тип пакета : " + getFormatType());
				throw new RuntimeException("Тип пакета не поддерживается : "
						+ getFormatType());
			}
			dataBlockCount++;
		}
	}

	private void setPacketCount() {
		if (cmdSignature.equals(SIGNATURE_A)) {
			this.packetCount = packet[CMD_SIGNATURE_LENGTH];
			this.packetDataSeek = CMD_SIGNATURE_LENGTH + 1;
		} else if (cmdSignature.equals(SIGNATURE_T)) {
			this.packetCount = PACKET_COUNT_T;
			this.packetDataSeek = CMD_SIGNATURE_LENGTH;
		}
		logger.debug("Количество пакетов данных : "
				+ Integer.toHexString(packetCount));
	}

	private void setFormatType() {
		logger.debug("Порядок байта типа формата : " + packetDataSeek);
		if (cmdSignature.equals(SIGNATURE_A)) {
			formatType = packet[packetDataSeek];
		} else if (cmdSignature.equals(SIGNATURE_T)) {
			formatType = packet[packetDataSeek];
		}
		logger.debug("Тип пакета : " + Integer.toHexString(formatType));
	}

	private void parsePacketF25() throws ParseException {
		logger.debug("Парсим F25");
		parsePacketF5();
		// k = 55-58 Текущий пробег
		// k = 59-62 Последний отрезок пути
		// k = 63-64 Общее количество секунд на последнем отрезке пути
		// k = 65-66 Количество секунд на последнем отрезке пути
		// k = 70-71 Частота датчика уровня топлива 1
		int k = 70;
		navLls1 = Double
				.valueOf(ModUtils.getIntU16(packet, packetDataSeek + k));
		// k = 72 Температура, измеренная датчиком 1
		// k = 73-74 Уровень топлива, измеренный датчиком 1
		// k = 75-76 Частота датчика уровня топлива 2
		k = 75;
		navLls2 = Double
				.valueOf(ModUtils.getIntU16(packet, packetDataSeek + k));
		// k = 77 Температура, измеренная датчиком 2
		// k = 78-79 Уровень топлива, измеренный датчиком 2
		// k = 80-81 Частота датчика уровня топлива 3
		k = 80;
		navLls3 = Double
				.valueOf(ModUtils.getIntU16(packet, packetDataSeek + k));
		// k = 81 Температура, измеренная датчиком 3
		// k = 82-83 Уровень топлива, измеренный датчиком 3
		// k = 84 Температура с цифрового датчика 1
		// k = 85 Температура с цифрового датчика 2
		// k = 86 Температура с цифрового датчика 3
		// k = 87 Температура с цифрового датчика 4
	}

	private void parsePacketF15() {
		// TODO Auto-generated method stub
		logger.debug("Парсим F15");
		for (int i = 0; i < 4; i++) {
			dataIndexBytes[i] = packet[i + 1];
		}
		dataIndex = ModUtils.getIntU32(dataIndexBytes);
	}

	private void parsePacketF5() throws ParseException {
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
		dataIndex = ModUtils.getIntU16(packet, packetDataSeek + k); // 5-6 ид
																	// типа
																	// события
		logger.debug("Ид события : " + dataIndex);
		k = 7;
		navDateTimeFixed = ModUtils.getDateTimeSignal(packet, packetDataSeek
				+ k); // 8-13 Дата фиксации события
		logger.debug("Время события : " + navDateTimeFixed);
		k = 13; // Статус охраны
		k = 14; // Состояние функциональных модулей
		k = 15; // Уровень GSM
		k = 16; // Спутники 2-7 бит
		dasnSatUsed = (long) (packet[packetDataSeek + k] >> 2 & 0xff); // 38
		k = 17; // Датчики цифровых входов 0 бит зажигание

		k = 18; // Наряжение на основном питании
		dasnAdc = (long) (ModUtils.getIntU16(packet, packetDataSeek + k) / 1000.0); // Питание
																					// в
																					// миливольтах
																					// 21-22
		logger.debug("Питание в вольтах : " + dasnAdc);
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
		navAcc2 = navAcc1 + (navAcc2 << 16);
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
		dasnGpio = (long) (navDigit1 + (navDigit2 << 8));
		// Спутники - 2-7 бит (0-1 байт выхода 0 - ВЫКЛ 1 - ВКЛ)
		// Состояние навигации
		k = 34;
		navSatellitesType = packet[packetDataSeek + k] >> 2 & 0xff; // 38
		logger.debug("Тип определения местоположения : " + navSatellitesType);
		// navNavOnOff = packet[packetDataSeek+k] & 0x01;
		if (((packet[packetDataSeek + k] >> 1) & 0x01) == 0) {
			dasnStatus = DATA_STATUS.ERR; // координаты невалидны!
		} else {
			dasnStatus = DATA_STATUS.OK;
		}
		k = 35;
		dasnDatetime = DSF.parse(ModUtils.getDateTimeSignal(packet,
				packetDataSeek + k)); // 35-40
		// Дата
		// данных
		logger.debug("Время координат : " + dasnDatetime);
		k = 41;
		dasnLatitude = (double) ModUtils
				.convRadianToDegree(Float.intBitsToFloat(ModUtils.getIntU32(
						packet, packetDataSeek + k)));
		logger.debug("Широта : " + dasnLatitude);
		k = 45;
		dasnLongitude = (double) ModUtils
				.convRadianToDegree(Float.intBitsToFloat(ModUtils.getIntU32(
						packet, packetDataSeek + k)));
		logger.debug("Долгота : " + dasnLongitude);
		k = 49;
		dasnSog = Double
				.valueOf(Float.intBitsToFloat(ModUtils.getIntU32(packet, packetDataSeek + k)));
		logger.debug("Скорость : " + dasnSog);
		k = 53;
		dasnCourse = Double.valueOf(ModUtils.getIntU16(packet, packetDataSeek
				+ k));
		logger.debug("Курс : " + dasnCourse);
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

	private void parsePacketF2() throws ParseException {
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
		dasnAdc = (long) (ModUtils.getIntU16(packet, packetDataSeek + k) / 1000.0); // Питание
																					// в
																					// миливольтах
																					// 21-22
		logger.debug("Питание в вольтах : " + dasnAdc);
		k = 23;
		navPowerReserv = (float) (ModUtils
				.getIntU16(packet, packetDataSeek + k) / 1000.0); // Питание в
																	// миливольтах
																	// 23-24
		logger.debug("Резервное питание в вольтах : " + navPowerReserv);
		k = 25;
		dasnTemp = Double.valueOf(((packet[packetDataSeek + k]) << 8)
				+ (packet[packetDataSeek + k + 1])); // Температура в градусах
		logger.debug("Температура градусы : " + dasnTemp);
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
		navAcc2 = navAcc1 + (navAcc2 << 16);
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
		dasnGpio = (long) (navDigit1 + (navDigit2 << 8)); // Спутники - 2-7 бит
															// (0-1 байт
		// выхода 0 - ВЫКЛ 1 - ВКЛ)
		// Состояние навигации
		k = 39;
		navSatellitesType = packet[packetDataSeek + k] >> 2 & 0xff; // 38
		logger.debug("Тип определения местоположения : " + navSatellitesType);
		dasnSatUsed = 0L;
		// navNavOnOff = packet[packetDataSeek+k] & 0x01;
		logger.debug("Валидность координат : " + dasnStatus);
		if (((packet[packetDataSeek + k] >> 1) & 0x01) == 1) {
			dasnStatus = DATA_STATUS.ERR;
		} else {
			dasnStatus = DATA_STATUS.OK;
		}
		k = 40;
		dasnDatetime = DSF.parse(ModUtils.getDateTimeSignal(packet,
				packetDataSeek + k)); // 40-45
		// Дата
		// данных
		logger.debug("Время координат : " + dasnDatetime);
		k = 46;
		dasnLatitude = Double
				.valueOf(ModUtils.convRadianToDegree(Float
						.intBitsToFloat(ModUtils.getIntU32(packet,
								packetDataSeek + k))));
		logger.debug("Широта : " + dasnLatitude);
		k = 50;
		dasnLongitude = Double
				.valueOf(ModUtils.convRadianToDegree(Float
						.intBitsToFloat(ModUtils.getIntU32(packet,
								packetDataSeek + k))));
		logger.debug("Долгота : " + dasnLongitude);
		k = 54;
		dasnSog = Double
				.valueOf(Float.intBitsToFloat(ModUtils.getIntU32(packet, packetDataSeek + k)));
		logger.debug("Скорость : " + dasnSog);
		k = 58;
		dasnCourse = Double.valueOf(ModUtils.getIntU16(packet, packetDataSeek
				+ k));
		logger.debug("Курс : " + dasnCourse);
	}

	private void parsePacketF1() {
		// TODO Auto-generated method stub
		for (int i = 0; i < 4; i++) {
			dataIndexBytes[i] = packet[i + 2];
		}
		dataIndex = ModUtils.getIntU32(dataIndexBytes);
	}

	private void parsePacketF6() throws ParseException {
		logger.debug("Парсим F6");
		parsePacketF5();
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
		for (int i = 0; i < navClientId.length; i++) {
			packet[k] = navClientId[i];
			k++;
		}
		for (int i = 0; i < navServerId.length; i++) {
			packet[k] = navServerId[i];
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
		uid = String.valueOf(ModUtils.getIntByte(navClientId));
		logger.debug("UID блока : " + uid);
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
			navServerId[i] = packetHeader[k];
			k++;
		}
		logger.debug("Ид сервера : " + ModUtils.getIntByte(navServerId));
		// Идентификатор отправителя
		for (int i = 0; i < 4; i++) {
			navClientId[i] = packetHeader[k];
			k++;
		}
		logger.debug("Ид терминала : " + ModUtils.getIntByte(navClientId));
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
		this.dasnUid = uid;
		// Сохраним в БД данные
		dasnValues.put("GL", String.valueOf(navSatellitesType));
		dasnValues.put("ACC1", String.valueOf(navAcc1));
		dasnValues.put("ACC2", String.valueOf(navAcc2));
		dasnValues.put("DI1", String.valueOf(navDigit1));
		dasnValues.put("DI2", String.valueOf(navDigit2));
		dasnValues.put("PW", String.valueOf(navPowerReserv));
		dasnValues.put("LLS1", String.valueOf(navLls1));
		dasnValues.put("LLS2", String.valueOf(navLls2));
		dasnValues.put("LLS3", String.valueOf(navLls3));

		dataSensor.setDasnDatetime(dasnDatetime);
		dataSensor.setDasnUid(dasnUid);
		dataSensor.setDasnTemp(dasnTemp);
		dataSensor.setDasnAdc(dasnAdc);
		dataSensor.setDasnLatitude(dasnLatitude);
		dataSensor.setDasnLongitude(dasnLongitude);
		dataSensor.setDasnSog(dasnSog);
		dataSensor.setDasnGpio(dasnGpio);
		dataSensor.setDasnSatUsed(dasnSatUsed);
		dataSensor.setDasnCourse(dasnCourse);
		dataSensor.setDasnValues(dasnValues);
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
