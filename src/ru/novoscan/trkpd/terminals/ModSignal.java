package ru.novoscan.trkpd.terminals;

import static ru.novoscan.trkpd.constants.KeyValues.ACC1;
import static ru.novoscan.trkpd.constants.KeyValues.ACC2;
import static ru.novoscan.trkpd.constants.KeyValues.DI1;
import static ru.novoscan.trkpd.constants.KeyValues.DI2;
import static ru.novoscan.trkpd.constants.KeyValues.FU;
import static ru.novoscan.trkpd.constants.KeyValues.GL;
import static ru.novoscan.trkpd.constants.KeyValues.GSM;
import static ru.novoscan.trkpd.constants.KeyValues.LLS1;
import static ru.novoscan.trkpd.constants.KeyValues.LLS2;
import static ru.novoscan.trkpd.constants.KeyValues.LLS3;
import static ru.novoscan.trkpd.constants.KeyValues.PW;
import static ru.novoscan.trkpd.constants.KeyValues.S1;
import static ru.novoscan.trkpd.constants.KeyValues.S2;
import static ru.novoscan.trkpd.utils.ModUtils.getIntU16;
import static ru.novoscan.trkpd.utils.ModUtils.getIntU32;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.constants.KeyValues;
import ru.novoscan.trkpd.domain.Terminal;
import ru.novoscan.trkpd.utils.BitUtil;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.ModUtils;
import ru.novoscan.trkpd.utils.TrackPgUtils;

/**
 * @author kur
 * 
 */
public class ModSignal extends Terminal {

	private static final byte FLEX_PREAMBULE = 0x7E; // "~"

	private int[] navClientId = new int[4];

	private int[] navServerId = new int[4];

	private Integer navSatellitesType;

	// private int navCRC;

	// private int navCheckCRC;

	private Integer navAcc1;

	private Integer navAcc2;

	static Logger logger = Logger.getLogger(ModSignal.class);

	private final Object lock = new Object();

	private int readbytes;

	private float fullreadbytes = 0;

	private int[] packet;

	private int packetDataLength = 32768; // максимальная длина пакета с

	// private String navPhone;

	// private String navPass;

	private int packetCount;

	private int[] packetHeader;

	private int packetSize;

	private int factCRCHeader;

	// private final int HEADER_LENGTH = 7; // Длина заголовка

	// private final int PACKET_LENGTH = 45; // Длина пакета

	private static final int PACKET_HANDSHAKE_LENGTH = 16; // Длина пакета
															// рукопожатия

	private String dataSignature; // Сигнатураprivate int

	private static final int SIGNATURE_LENGTH = 4; // пакета - по умолчанию @NTC

	private static final int CMD_SIGNATURE_LENGTH = 3;

	private static final String SIGNATURE_FLEX = "*>FLEX";

	private static final String SIGNATURE_S = "*>S"; // Сигнатура пакета S2115
														// NTCB

	private static final String SIGNATURE_T = "*>T";

	private static final String SIGNATURE_A = "*>A";

	private static final String SIGNATURE_RESPONCE_S = "*<S"; // Сигнатура
																// пакета S2115

	private static final String SIGNATURE_RESPONCE_T = "*<T";

	private static final String SIGNATURE_RESPONCE_A = "*<A";

	private static final String SIGNATURE_RESPONSE_FLEX = "*<FLEX";

	private static final String SIGNATURE_FLEX_A = "~A";

	private static final String SIGNATURE_FLEX_T = "~T";

	private static final String SIGNATURE_FLEX_C = "~C";

	private static final String SIGNATURE_FLEX_E = "~E";

	private static final String SIGNATURE_FLEX_X = "~X";

	private static final int PACKET_F1 = 0x01;

	private static final int PACKET_F2 = 0x02;

	private static final int PACKET_LENGTH_F2 = 72;

	private static final int PACKET_LENGTH_F5 = 67;

	private static final int PACKET_LENGTH_F6 = 111; // ???

	private static final int PACKET_LENGTH_F15 = 82; // 2 байта заголовок пакета

	private static final int PACKET_LENGTH_F25 = 86; // 2 байта заголовок

	private static final int PACKET_F3 = 0x03;

	private static final int PACKET_F4 = 0x04;

	private static final int PACKET_F5 = 0x05;

	private static final int PACKET_F6 = 0x06;

	private static final int PACKET_F15 = 0x15;

	private static final int PACKET_F25 = 0x25;

	private static final int PACKET_FLEX = 0xB0;

	// private static final int FLEX_PROTOCOL_VERSION_1 = 0x0A;

	// private static final int FLEX_PROTOCOL_VERSION_2 = 0x1A;

	// private static final int FLEX_STRUCT_VERSION_1 = 0x0A;

	// private static final int FLEX_STRUCT_VERSION_2 = 0x1A;

	private static final int[] FLEX_FIELDS_SIZES = { 4, 2, 4, 1, 1, 1, 1, 1, 4,
			4, 4, 4, 4, 2, 4, 4, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1,
			4, 4, 2, 2, 4, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 2, 4, 2,
			1, 4, 2, 2, 2, 2, 2, 1, 1, 1, 2, 4, 2, 1,
			/* FLEX 2.0 */
			8, 2, 1, 16, 4, 2, 4, 37, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 3,
			3, 3, 6, 12, 24, 48, 1, 1, 1, 1, 4, 4, 1, 4, 2, 6, 2, 6, 2, 2, 2, 2,
			2, 2, 2, 2, 1, 2, 2, 2, 1 };

	private static final int FLEX_HEADER_LENGTH = 2;

	private final int PACKET_COUNT_T = 1;

	private String signature = "";

	private byte[] flexBitField = new byte[16];

	private int flexProtocolVersion;

	private int flexStructVersion;

	private int flexBitFieldSize;

	private int flexDataSize;

	private int[] dataSignatureArray = new int[SIGNATURE_LENGTH];

	private DataInputStream iDsLocal;

	private int dataBlockCount;

	private int dataIndex;

	private int formatType;

	private int dataIndexBytes[] = new int[4];

	private int packetDataSeek;

	private Float navPowerReserv;

	private Integer navDigit1;

	private Integer navDigit2;

	private String navDateTimeFixed;

	private final TrackPgUtils pgcon;

	private static final SimpleDateFormat DSF = new SimpleDateFormat(
			DATE_SIMPLE_FORMAT);

	private Double navLls1;

	private Double navLls2;

	private Double navLls3;

	private String uid;

	private int eventIndex;

	private byte[] indexBytes;

	private byte[] flexPacketData;

	private int flexPacketCount;

	private Integer navStatus1;

	private Integer navStatus2;

	private Integer navGsm;

	private Integer canFuel;

	private Date navDatetime;

	public ModSignal(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader unbconsole, ModConfig conf, TrackPgUtils pgcon)
			throws ParseException, IOException, SQLException {
		this.setDasnType(conf.getModType());
		this.pgcon = pgcon;
		iDsLocal = iDs;
		logger.debug("Чтение потока...");
		packetHeader = new int[PACKET_HANDSHAKE_LENGTH]; // пакет с нулевого
															// считается
		packet = new int[packetDataLength]; // пакет с нулевого считается
		fullreadbytes = 0;
		dasnStatus = NavigationStatus.OK;

		while (true) {
			synchronized (lock) {
				packetHeader[0] = readByte();
				if (packetHeader[0] == FLEX_PREAMBULE) {

					flexPacketCount = 0;
					packet[0] = packetHeader[0];
					packet[1] = readByte();
					switch (readFlexSignature()) {
					case SIGNATURE_FLEX_A:
						logger.debug("Пакет ~А");
						readFlexA();
						sendResponceFlexA(oDs);
						parseFlexData();
						break;
					case SIGNATURE_FLEX_T:
						logger.debug("Пакет ~T");
						readFlexT();
						sendResponceFlexT(oDs);
						parseFlexData();
						break;
					case SIGNATURE_FLEX_C:
						logger.debug("Пакет ~C");
						readFlexC();
						sendResponceFlexC(oDs);
						parseFlexData();
						break;
					case SIGNATURE_FLEX_X:
						logger.debug("Пакет ~X");
						readFlexX();
						sendResponceFlexX(oDs);
						parseFlexData();
						break;
					case SIGNATURE_FLEX_E:
						logger.debug("Пакет ~E");
						readFlexE();
						sendResponceFlexE(oDs);
						parseFlexData();
						break;
					default:
						throw new IllegalArgumentException(
								"Неверная сигнатура FLEX: " + signature);
					}
					readbytes = 0;
				} else if (SIGNATURE_FLEX.equals(signature)
						&& packetHeader[0] == 0x7F) {
					logger.debug("Пакет ping");
					readbytes = 0;
				} else {
					String cmd;
					for (int i = 1; i < PACKET_HANDSHAKE_LENGTH; i++) {
						packetHeader[i] = readByte();
					}
					// Обработаем заголовок
					parseHeader();
					if (packetSize >= CMD_SIGNATURE_LENGTH) {
						// Считаем данные
						for (int i = 0; i < packetSize; i++) {
							packet[i] = readByte();
						}
						// Определим тип команд
						readSignature();
						switch (signature) {
						case SIGNATURE_S:
							parseIMEI();
							if (uid.length() == 0) {
								// Ошибка не определён IMEI
								logger.error("Не определён IMEI");
								throw new ParseException(
										"Неверные данные пакета HANDSHAKE : IMEI не определён",
										0);
							}
							cmd = SIGNATURE_RESPONCE_S;
							break;
						case SIGNATURE_T:
							// Телеметрические записи
							parseDataTypeT();
							cmd = SIGNATURE_RESPONCE_T + (char) PACKET_COUNT_T;
							break;
						case SIGNATURE_A:
							parseDataTypeA();
							cmd = SIGNATURE_RESPONCE_A + (char) dataBlockCount;
							break;
						case SIGNATURE_FLEX:
							parseFlexProtocol();
							cmd = SIGNATURE_RESPONSE_FLEX + (char) PACKET_FLEX
									+ (char) flexProtocolVersion
									+ (char) flexStructVersion;
							break;
						default:
							throw new IllegalArgumentException(
									"Неверная сигнатура F: " + signature);
						}
						/*
						 * Формат ответа: dataSignature +navServerID
						 * +navDeviceID +длина_сигнатуры_запроса
						 * +КонтрольнаяСуммаОтвета
						 * +КонтрольнаяСумма15байтЗаголовка +Ответ
						 */
						sendResponce(oDs, cmd);
						readbytes = 0;
					}

				}
			}
		}
	}

	// FLEX 1.0
	private void sendResponceFlexT(DataOutputStream oDs) throws IOException {
		int len = 6;
		for (int i = 0; i < len; i++) {
			oDs.write(packet[i]);
			logger.debug("Write byte[" + i + "] : "
					+ Integer.toHexString(packet[i]));
		}
		int crc = ModUtils.getCrc8(packet, 0, len);
		oDs.write(crc);
		logger.debug("Write byte[" + len + "] : " + Integer.toHexString(crc));
	}

	private void sendResponceFlexC(DataOutputStream oDs) throws IOException {
		int len = 2;
		for (int i = 0; i < len; i++) {
			oDs.write(packet[i]);
			logger.debug("Write byte[" + i + "] : "
					+ Integer.toHexString(packet[i]));
		}
		int crc = ModUtils.getCrc8(packet, 0, len);
		oDs.write(crc);
		logger.debug("Write byte[" + len + "] : " + Integer.toHexString(crc));
	}

	private void sendResponceFlexA(DataOutputStream oDs) throws IOException {
		int len = 3;
		for (int i = 0; i < len; i++) {
			oDs.write(packet[i]);
			logger.debug("Write byte[" + i + "] : "
					+ Integer.toHexString(packet[i]));
		}
		int crc = ModUtils.getCrc8(packet, 0, len);
		oDs.write(crc);
		logger.debug("Write byte[" + len + "] : " + Integer.toHexString(crc));

	}

	private void readFlexT() throws IOException, ParseException {
		flexPacketCount = 1;
		indexBytes = new byte[4];
		for (int i = 0; i < 4; i++) {
			indexBytes[i] = readByte();
		}
		flexPacketData = new byte[flexDataSize];
		for (int i = 0; i < flexDataSize; i++) {
			flexPacketData[i] = readByte();
		}
		int flexCrc8 = readByte() & 0xFF;

		int flexRealCrc8 = ModUtils.getCrc8(packet, 0, 2 + 4 + flexDataSize)
				& 0xFF;
		if (flexRealCrc8 != flexCrc8) {
			logger.error("Неверная контрольная сумма: " + flexRealCrc8
					+ " ожидается " + flexCrc8);
		}

		eventIndex = getIntU32(indexBytes, 0);
		logger.debug("Индекс принятой телеметрической записи : " + eventIndex
				+ ". Размер пакета данных: " + flexDataSize
				+ ". Контрольная сумма: " + flexCrc8);
	}

	private void readFlexC() throws IOException, ParseException {
		flexPacketCount = 1;
		flexPacketData = new byte[flexDataSize];
		for (int i = 0; i < flexDataSize; i++) {
			flexPacketData[i] = readByte();
		}
		int flexCrc8 = readByte() & 0xFF;

		int flexRealCrc8 = ModUtils.getCrc8(packet, 0,
				FLEX_HEADER_LENGTH + flexDataSize) & 0xFF;
		if (flexRealCrc8 != flexCrc8) {
			logger.error("Неверная контрольная сумма: " + flexRealCrc8
					+ " ожидается " + flexCrc8);
		}

		logger.debug("Размер пакета данных: " + flexDataSize
				+ ". Контрольная сумма: " + flexCrc8);
	}

	private void readFlexA() throws IOException, ParseException {
		flexPacketCount = readByte() & 0xFF;
		int dataSize = flexDataSize * flexPacketCount;
		flexPacketData = new byte[dataSize];
		for (int i = 0; i < flexPacketData.length; i++) {
			flexPacketData[i] = readByte();
		}
		int flexCrc8 = readByte() & 0xFF;

		int flexRealCrc8 = ModUtils.getCrc8(packet, 0,
				FLEX_HEADER_LENGTH + 1 + dataSize) & 0xFF;
		if (flexRealCrc8 != flexCrc8) {
			logger.error("Неверная контрольная сумма: " + flexRealCrc8
					+ " ожидается " + flexCrc8);
		}

		logger.debug("Количество телеметрических записей : " + flexPacketCount
				+ ". Размер пакета данных: " + flexDataSize
				+ ". Индекс пакета: " + eventIndex + ". Контрольная сумма: "
				+ flexCrc8);

	}

	// FLEX 2.0
	private void sendResponceFlexE(DataOutputStream oDs) throws IOException {
		sendResponceFlexA(oDs);
	}

	private void readFlexE() throws IOException, ParseException {
		readFlexA();
	}

	private void sendResponceFlexX(DataOutputStream oDs) throws IOException {
		sendResponceFlexT(oDs);
	}

	private void readFlexX() throws IOException, ParseException {
		readFlexT();
	}

	private void parseFlexProtocol() throws ParseException {
		int k = SIGNATURE_FLEX.length();
		int flexVersion = packet[k++] & 0XFF; // 6
		flexProtocolVersion = packet[k++]; // 7
		flexStructVersion = packet[k++]; // 8
		flexBitFieldSize = packet[k++]; // 9
		if (flexBitFieldSize > 122) {
			logger.error(
					"Размер конфигурационных данныз превышает максимальный размер 122 : "
							+ flexBitFieldSize);
			throw new ParseException(SIGNATURE_FLEX, 0);
		}
		if (flexVersion != PACKET_FLEX) {
			logger.error("Версия протокола не соответствует ожиданию : "
					+ flexVersion);
			throw new ParseException(SIGNATURE_FLEX, 0);
		}

		int lastFlexFieldBit = (int) Math.ceil(flexBitFieldSize / 8D);
		for (int i = 0; i < lastFlexFieldBit; i++) {
			flexBitField[i] = (byte) (packet[k++] & 0XFF);
		}

		flexDataSize = 0;
		for (int i = 0; i < flexBitFieldSize; i++) {
			if (checkFlexBitfield(i)) {
				flexDataSize += FLEX_FIELDS_SIZES[i];
			}
		}
		logger.debug("Сигнатура протокола : " + signature
				+ ". Версия протокола: " + flexProtocolVersion
				+ ". Версия структуры данных: " + flexStructVersion
				+ ". Размер конфигурационного поля: " + flexBitFieldSize
				+ ". Размер пакета данных: " + flexDataSize);

	}

	private void parseDataTypeT()
			throws IOException, ParseException, SQLException {
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
			throw new RuntimeException(
					"Тип пакета не поддерживается : " + getFormatType());
		}
	}

	private void parseDataTypeA()
			throws IOException, ParseException, SQLException {
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
				throw new RuntimeException(
						"Тип пакета не поддерживается : " + getFormatType());
			}
			dataBlockCount++;
		}
	}

	private void setPacketCount() {
		switch (signature) {
		case SIGNATURE_A: {
			this.packetCount = packet[CMD_SIGNATURE_LENGTH];
			this.packetDataSeek = CMD_SIGNATURE_LENGTH + 1;
			break;
		}
		case SIGNATURE_T: {
			this.packetCount = PACKET_COUNT_T;
			this.packetDataSeek = CMD_SIGNATURE_LENGTH;
			break;
		}
		default:
		}
		logger.debug("Количество пакетов данных : "
				+ Integer.toHexString(packetCount));
	}

	private void setFormatType() {
		logger.debug("Порядок байта типа формата : " + packetDataSeek);
		if (signature.equals(SIGNATURE_A)) {
			formatType = packet[packetDataSeek];
		} else if (signature.equals(SIGNATURE_T)) {
			formatType = packet[packetDataSeek];
		} else if (signature.equals(SIGNATURE_FLEX)) {
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
		navLls1 = Double.valueOf(getIntU16(packet, packetDataSeek + k));
		// k = 72 Температура, измеренная датчиком 1
		// k = 73-74 Уровень топлива, измеренный датчиком 1
		// k = 75-76 Частота датчика уровня топлива 2
		k = 75;
		navLls2 = Double.valueOf(getIntU16(packet, packetDataSeek + k));
		// k = 77 Температура, измеренная датчиком 2
		// k = 78-79 Уровень топлива, измеренный датчиком 2
		// k = 80-81 Частота датчика уровня топлива 3
		k = 80;
		navLls3 = Double.valueOf(getIntU16(packet, packetDataSeek + k));
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
		dataIndex = getIntU32(dataIndexBytes);
	}

	private void parsePacketF5() throws ParseException {
		logger.debug("Парсим F5");
		// navPacketSize = PACKET_LENGTH_F5;
		// navPacketType = PACKET_F5;
		// navSoftVersion = 2115;
		// Пропускаем 1 байт - тип определили выше.
		int k = 1; // 0
		int id = getIntU32(packet, packetDataSeek + k); // 1-4 ид
														// записи
		logger.debug("Ид записи : " + id);
		k = 5;
		dataIndex = getIntU16(packet, packetDataSeek + k); // 5-6 ид
															// типа
															// события
		logger.debug("Ид события : " + dataIndex);
		k = 7;
		navDateTimeFixed = ModUtils.getDateTimeSignal(packet,
				packetDataSeek + k); // 8-13 Дата фиксации события
		logger.debug("Время события : " + navDateTimeFixed);
		k = 13; // Статус охраны
		k = 14; // Состояние функциональных модулей
		k = 15; // Уровень GSM
		k = 16; // Спутники 2-7 бит
		dasnSatUsed = (long) (packet[packetDataSeek + k] >> 2 & 0xff); // 38
		k = 17; // Датчики цифровых входов 0 бит зажигание

		k = 18; // Наряжение на основном питании
		dasnAdc = getIntU16(packet, packetDataSeek + k) / 1000L; // Питание
																	// в
																	// миливольтах
																	// 21-22
		logger.debug("Питание в вольтах : " + dasnAdc);
		k = 20;
		navPowerReserv = getIntU16(packet, packetDataSeek + k) / 1000.0F; // Питание
																			// в
																			// миливольтах
																			// 23-24
		logger.debug("Резервное питание в вольтах : " + navPowerReserv);
		// k = 25;
		// navTemp =(((packet[packetDataSeek+k]) << 8) +
		// (packet[packetDataSeek+k+1])); // Температура в градусах
		// logger.debug("Температура градусы : " + navTemp);
		k = 22;
		navAcc1 = getIntU16(packet, packetDataSeek + k); // Значение на
															// аналоговом
															// входе 1
															// 27-28
		logger.debug("Значение ACC1 : " + (navAcc1 / 1000.0));
		k = 24;
		navAcc2 = getIntU16(packet, packetDataSeek + k); // Значение на
															// аналоговом
															// входе 2
															// 29-30
		logger.debug("Значение ACC2 : " + (navAcc2) / 1000.0);
		navAcc2 = navAcc1 + (navAcc2 << 16);
		k = 26;
		navDigit1 = getIntU32(packet, packetDataSeek + k); // Цифровой
															// вход
															// (Количество
															// импульсов
															// подсчитанное
															// на момент
															// события)
															// вход 1
															// 31-34
		k = 30;
		navDigit2 = getIntU32(packet, packetDataSeek + k); // Цифровой
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
		dasnStatus = ((packet[packetDataSeek + k] >> 1) & 0x01) == 0
				? NavigationStatus.ERR
				: NavigationStatus.OK;
		logger.debug("Валидность координат : " + dasnStatus);
		k = 35;
		dasnDatetime = DSF
				.parse(ModUtils.getDateTimeSignal(packet, packetDataSeek + k)); // 35-40
		// Дата
		// данных
		logger.debug("Время координат : " + dasnDatetime);
		k = 41;
		dasnLatitude = Double.valueOf(ModUtils.convRadianToDegree(
				Float.intBitsToFloat(getIntU32(packet, packetDataSeek + k))));
		logger.debug("Широта : " + dasnLatitude);
		k = 45;
		dasnLongitude = Double.valueOf(ModUtils.convRadianToDegree(
				Float.intBitsToFloat(getIntU32(packet, packetDataSeek + k))));
		logger.debug("Долгота : " + dasnLongitude);
		k = 49;
		dasnSog = Double.valueOf(
				Float.intBitsToFloat(getIntU32(packet, packetDataSeek + k)));
		logger.debug("Скорость : " + dasnSog);
		k = 53;
		dasnCourse = Double.valueOf(getIntU16(packet, packetDataSeek + k));
		logger.debug("Курс : " + dasnCourse);
	}

	private void parsePacketF4() {
		// TODO Auto-generated method stub
		logger.debug("Парсим F4");
		for (int i = 0; i < 4; i++) {
			dataIndexBytes[i] = packet[i + 1];
		}
		dataIndex = getIntU32(dataIndexBytes);

	}

	private void parsePacketF3() {
		// TODO Auto-generated method stub
		logger.debug("Парсим F4");
		for (int i = 0; i < 4; i++) {
			dataIndexBytes[i] = packet[i + 2];
		}
		dataIndex = getIntU32(dataIndexBytes);

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
		navDateTimeFixed = ModUtils.getDateTimeSignal(packet,
				packetDataSeek + k); // 8-13 Дата фиксации события
		logger.debug("Время события : " + navDateTimeFixed);
		k = 14; // Статус охраны
		k = 15; // Состояние функциональных модулей
		k = 16; // Уровень GSM
		k = 17; // Состояние выходов 17-18
		k = 19; // Датчики цифровых входов 19-20
		k = 21; // Наряжение на основном питании
		dasnAdc = getIntU16(packet, packetDataSeek + k) / 1000L; // Питание
																	// в
																	// миливольтах
																	// 21-22
		logger.debug("Питание в вольтах : " + dasnAdc);
		k = 23;
		navPowerReserv = getIntU16(packet, packetDataSeek + k) / 1000.0F; // Питание
																			// в
																			// миливольтах
																			// 23-24
		logger.debug("Резервное питание в вольтах : " + navPowerReserv);
		k = 25;
		dasnTemp = Double.valueOf(((packet[packetDataSeek + k]) << 8)
				+ (packet[packetDataSeek + k + 1])); // Температура в градусах
		logger.debug("Температура градусы : " + dasnTemp);
		k = 27;
		navAcc1 = getIntU16(packet, packetDataSeek + k); // Значение на
															// аналоговом
															// входе 1
															// 27-28
		logger.debug("Значение ACC1 : " + (navAcc1 / 1000.0));
		k = 29;
		navAcc2 = getIntU16(packet, packetDataSeek + k); // Значение на
															// аналоговом
															// входе 2
															// 29-30
		logger.debug("Значение ACC2 : " + (navAcc2 / 1000.0));
		navAcc2 = navAcc1 + (navAcc2 << 16);
		k = 31;
		navDigit1 = getIntU32(packet, packetDataSeek + k); // Цифровой
															// вход
															// (Количество
															// импульсов
															// подсчитанное
															// на момент
															// события)
															// вход 1
															// 31-34
		k = 35;
		navDigit2 = getIntU32(packet, packetDataSeek + k); // Цифровой
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
		dasnStatus = ((packet[packetDataSeek + k] >> 1) & 0x01) == 0
				? NavigationStatus.ERR
				: NavigationStatus.OK;
		logger.debug("Валидность координат : " + dasnStatus);
		k = 40;
		dasnDatetime = DSF
				.parse(ModUtils.getDateTimeSignal(packet, packetDataSeek + k)); // 40-45
		// Дата
		// данных
		logger.debug("Время координат : " + dasnDatetime);
		k = 46;
		dasnLatitude = Double.valueOf(ModUtils.convRadianToDegree(
				Float.intBitsToFloat(getIntU32(packet, packetDataSeek + k))));
		logger.debug("Широта : " + dasnLatitude);
		k = 50;
		dasnLongitude = Double.valueOf(ModUtils.convRadianToDegree(
				Float.intBitsToFloat(getIntU32(packet, packetDataSeek + k))));
		logger.debug("Долгота : " + dasnLongitude);
		k = 54;
		dasnSog = Double.valueOf(
				Float.intBitsToFloat(getIntU32(packet, packetDataSeek + k)));
		logger.debug("Скорость : " + dasnSog);
		k = 58;
		dasnCourse = Double.valueOf(getIntU16(packet, packetDataSeek + k));
		logger.debug("Курс : " + dasnCourse);
	}

	private void parsePacketF1() {
		for (int i = 0; i < 4; i++) {
			dataIndexBytes[i] = packet[i + 2];
		}
		dataIndex = getIntU32(dataIndexBytes);
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
		parseIMEI();
		logger.debug("Ид терминала : " + uid);
		packetSize = (int) (packetHeader[12] & 0xff)
				+ (int) ((packetHeader[13] & 0xff) << 8);
		logger.debug("Размер пакета данных : " + packetSize);
		logger.debug("Контрольная сумма данных : " + packetHeader[14]);
		logger.debug("Контрольная сумма заголовка : " + packetHeader[15]);
		factCRCHeader = ModUtils.getSumXor(packetHeader, 15);
		logger.debug(
				"Контрольная сумма заголовка (фактическая) : " + factCRCHeader);

	}

	private void readSignature() {
		signature = "";
		for (int i = 0; i < SIGNATURE_FLEX.length(); i++) {
			signature = signature + (char) packet[i];
		}
		if (!signature.equals(SIGNATURE_FLEX)) {
			signature = signature.substring(0, CMD_SIGNATURE_LENGTH);
		}

		logger.debug("Сигнатура команды : " + signature);
	}

	private String readFlexSignature() {
		String signatureData = "" + (char) packet[0] + (char) packet[1];
		logger.debug("Сигнатура команды : " + signatureData);
		return signatureData;
	}

	private void parseFlexData() throws SQLException {
		int seek = 0;
		int offset = TimeZone.getDefault().getRawOffset();
		for (int k = 0; k < flexPacketCount; k++) {
			for (int i = 0; i < flexBitFieldSize; i++) {
				if (!checkFlexBitfield(i)) {
					continue;
				}
				switch (i) {
				case 0:
					// Сквозной номер записи в энергонезависимой памяти Little
					// Endian U32
					break;
				case 1:
					// Код события U16
					break;
				case 2:
					// Время события U32
					dasnDatetime = new Date((getIntU32(flexPacketData, seek) & 0xffffffffL) * 1000L - offset);
					logger.debug("Дата : " + dasnDatetime);
					break;
				case 3:
					// Статус устройства U8
					navStatus1 = flexPacketData[seek] & 0xFF;
					break;
				case 4:
					// Статус функциональных модулей 1 U8
					dasnGpio = Long.valueOf(flexPacketData[seek] & 0xFF);
					break;
				case 5:
					// Статус функциональных модулей 2 U8
					navStatus2 = flexPacketData[seek] & 0xFF;
					break;
				case 6:
					// Уровень GSM U8
					navGsm = flexPacketData[seek] & 0xFF;
					break;
				case 7:
					// Состояние навигационного датчика GPS/ГЛОНАСС U8
					dasnStatus = ModUtils.check(flexPacketData[seek], 1)
							? dasnStatus = NavigationStatus.OK
							: NavigationStatus.ERR;
					logger.debug("Валидность координат : " + dasnStatus);

					dasnSatUsed = Long
							.valueOf(flexPacketData[seek] >> 2 & 0xff);
					logger.debug("Количество спутников : " + dasnSatUsed);
					break;
				case 8:
					// Время последних валидных координат U32
					navDatetime =new Date((getIntU32(flexPacketData, seek) & 0xffffffffL) * 1000L - offset);
					logger.debug("Дата координат : " + navDatetime);
					break;
				case 9:
					// Последняя валидная широта I32
					dasnLatitude = Double.valueOf(
							getIntU32(flexPacketData, seek) / 600000.0D);
					logger.debug("Последняя валидная широта : " + dasnLatitude);
					break;
				case 10:
					// Последняя валидная долгота I32
					dasnLongitude = Double.valueOf(
							getIntU32(flexPacketData, seek) / 600000.0D);
					logger.debug(
							"Последняя валидная долгота : " + dasnLongitude);
					break;
				case 11:
					// Последняя валидная высота дм I32
					dasnHgeo = getIntU32(flexPacketData, seek) * 0.1;
					logger.debug("Последняя валидная высота : " + dasnHgeo);
					break;
				case 12:
					// Скорость
					dasnSog = Double.valueOf(Float
							.intBitsToFloat(getIntU32(flexPacketData, seek)));
					logger.debug("Скорость : " + dasnSog);
					break;
				case 13:
					dasnCourse = Double
							.valueOf(getIntU16(flexPacketData, seek));
					logger.debug("Курс : " + dasnCourse);
					break;
				case 14:
				case 15:
					break;
				case 18:
					// Напряжение на основном источнике питания
					dasnAdc = getIntU16(flexPacketData, seek) / 1000L;
					logger.debug("Напряжение : " + dasnAdc);
					break;
				case 19:
					// Напряжение на резервном
					navPowerReserv = getIntU16(flexPacketData, seek) / 1000.0f;
					logger.debug("Напряжение на резервном : " + navPowerReserv);
					break;
				case 20:
					// Напряжение на аналоговом входе 1 (Ain1)
					navAcc1 = getIntU16(flexPacketData, seek);
					logger.debug("Напряжение на (Ain1) : " + navAcc1);
					break;
				case 21:
					// Напряжение на аналоговом входе 2 (Ain2)
					navAcc2 = getIntU16(flexPacketData, seek);
					logger.debug("Напряжение на (Ain2) : " + navAcc2);
					break;
				case 22:
					// Ain3
					break;
				case 23:
					// Ain4
					break;
				case 24:
					// Ain5
					break;
				case 25:
					// Ain6
					break;
				case 26:
					// Ain7
					break;
				case 27:
					// Ain8
					break;
				case 28:
					navDigit1 = flexPacketData[seek] & 0xFF;
					logger.debug("Дискретные 1 : " + navDigit1);
					break;
				case 29:
					navDigit2 = flexPacketData[seek] & 0xFF;
					logger.debug("Дискретные 2 : " + navDigit2);
					break;
				case 30:
					dasnGpio = Long.valueOf(flexPacketData[seek]);
					logger.debug("Дискретные 3 : " + dasnGpio);
					break;
				case 31:
					// Выхода 1
					break;
				case 36:
					// Моточасы, подсчитанные во время срабатывания датчика
					// работы генератора
					break;
				case 44:
					// t1
					dasnTemp = Double.valueOf(flexPacketData[seek]);
					logger.debug("Температура : " + dasnTemp);
					break;
				case 45:
					// t2
					break;
				case 46:
					// t3
					break;
				case 47:
					// t4
					break;
				case 48:
					// t5
					break;
				case 49:
					// t6
					break;
				case 50:
					// t7
					break;
				case 51:
					// t8
					break;
				case 52:
					// CAN Уровень топлива в баке
					canFuel = getIntU16(flexPacketData, seek);
					if (canFuel == 32767) {
						canFuel = null;
					}
					break;
				case 68:
					break;
				// FLEX 2.0
				case 69:
					break;
				case 70:
					dasnHdop = flexPacketData[seek] * 0.1;
					break;
				default:
				}
				seek = seek + FLEX_FIELDS_SIZES[i];
			}
			writeData();
		}
	}

	private boolean checkFlexBitfield(int index) {
		int byteIndex = Math.floorDiv(index, 8);
		int bitIndex = Math.floorMod(index, 8);
		return BitUtil.check(flexBitField[byteIndex], 7 - bitIndex);
	}

	private byte readByte() throws IOException {
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

	private void writeData() throws SQLException {
		this.dasnUid = uid;
		// Сохраним в БД данные
		putIfPresent(GL, navSatellitesType);
		putIfPresent(ACC1, navAcc1);
		putIfPresent(ACC2, navAcc2);
		putIfPresent(DI1, navDigit1);
		putIfPresent(DI2, navDigit2);
		putIfPresent(PW, navPowerReserv);
		putIfPresent(LLS1, navLls1);
		putIfPresent(LLS2, navLls2);
		putIfPresent(LLS3, navLls3);
		putIfPresent(GSM, navGsm);
		putIfPresent(S1, navStatus1);
		putIfPresent(S2, navStatus2);
		putIfPresent(FU, canFuel);

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
		dataSensor.setDasnHgeo(dasnHgeo == null ? 0 : dasnHgeo);
		dataSensor.setDasnHdop(dasnHdop == null ? 0 : dasnHdop);
		dataSensor.setDasnValues(dasnValues);
		dataSensor.setDasnStatus(dasnStatus.getCode());
		pgcon.setDataSensorValues(dataSensor);
		pgcon.addDataSensor();
		this.clear();
	}

	private <T> void putIfPresent(KeyValues key, T value) {
		if (value == null) {
			return;
		}
		dasnValues.put(key.name(), String.valueOf(value));
		if (!value.getClass().isPrimitive()) {
			value = null;
		}
	}

}
