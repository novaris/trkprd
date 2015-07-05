package ru.novoscan.trkpd.resources;

import java.util.TimeZone;

public interface ModConstats {
	public final static int TERM_TYPE_ANY = 0;

	public final static int TERM_TYPE_GELIX = 1;

	public final static int TERM_TYPE_GS = 100;

	public final static int TERM_TYPE_UTP5 = 200;

	public final static int TERM_TYPE_NOVACOM = 300;

	public final static int TERM_TYPE_ST270 = 301;

	public final static int TERM_TYPE_SIGNAL_S21 = 302;

	public final static int TERM_TYPE_MARKER = 303;

	public final static int TERM_TYPE_TRANSKOM_T15 = 304;

	public final static int TERM_TYPE_NAVIXY_M7 = 305;

	public final static int TERM_TYPE_GALILEO_SKY = 306;

	public final static int TERM_TYPE_NAVIS_UM4 = 310;

	public final static int TERM_TYPE_TELTONIKA_FM = 350;

	public final static int TERM_TYPE_AZIMUT = 360;

	public final static int TERM_TYPE_SCOUT = 400;

	public final static int TERM_TYPE_XML = 500;

	public final static int TERM_TYPE_SCOUT_OPEN = 600;

	public final static int TERM_TYPE_MAJAK = 800;

	public final static int TERM_TYPE_EGTS = 900;

	public final static int IMEI_LENGTH = 15;

	//

	public final static String SQL_DATE_FORMAT = "YYYYMMDDHH24:MI:SS.MS";

	public final static TimeZone TIME_ZONE = TimeZone.getTimeZone("UTC");

	public static final String DATE_FORMAT = "yyyyMMddHH:mm:ss";

	public static final String SQL_DATE_SIMPLE_FORMAT = "DDMMYYHH24MISS";

	public static final String DATE_SIMPLE_FORMAT = "ddMMyyHHmmss";

	public final static long TICKS_AT_EPOCH = 621355968000000000L;

	public final static long TICKS_PER_MILLISECOND = 10000;

	public final static int INT32 = 4;

	public final static int INT64 = 8;

	// EGTS Constants
	/* успешно обработано */
	public final static int EGTS_PC_OK = 0;

	/* в процессе обработки (результат обработки ещё не известен) */
	public final static int EGTS_PC_IN_PROGRESS = 1;

	/* неподдерживаемый протокол */
	public final static int EGTS_PC_UNS_PROTOCOL = 128;

	/* ошибка декодирования */
	public final static int EGTS_PC_DECRYPT_ERROR = 129;

	/* обработка запрещена */
	public final static int EGTS_PC_PROC_DENIED = 130;

	/* неверный формат заголовка */
	public final static int EGTS_PC_INC_HEADERFORM = 131;

	/* неверный формат данных */
	public final static int EGTS_PC_INC_DATAFORM = 132;

	/* неподдерживаемый тип */
	public final static int EGTS_PC_UNS_TYPE = 133;

	/* неверное количество параметров */
	public final static int EGTS_PC_NOTEN_PARAMS = 134;

	/* попытка повторной обработки */
	public final static int EGTS_PC_DBL_PROC = 135;

	/* обработка данных от источника запрещена */
	public final static int EGTS_PC_PROC_SRC_DENIED = 136;

	/* ошибка контрольной суммы заголовка */
	public final static int EGTS_PC_HEADERCRC_ERROR = 137;

	/* ошибка контрольной суммы данных */
	public final static int EGTS_PC_DATACRC_ERROR = 138;

	/* некорректная длина данных */
	public final static int EGTS_PC_INVDATALEN = 139;

	/* маршрут не найден */
	public final static int EGTS_PC_ROUTE_NFOUND = 140;

	/* маршрут закрыт */
	public final static int EGTS_PC_ROUTE_CLOSED = 141;

	/* маршрутизация запрещена */
	public final static int EGTS_PC_ROUTE_DENIED = 142;

	/* неверный адрес */
	public final static int EGTS_PC_INVADDR = 143;

	/* превышено количество ретрансляции данных */
	public final static int EGTS_PC_TTLEXPIRED = 144;

	/* нет подтверждения */
	public final static int EGTS_PC_NO_ACK = 145;

	/* объект не найден */
	public final static int EGTS_PC_OBJ_NFOUND = 146;

	/* событие не найдено */
	public final static int EGTS_PC_EVNT_NFOUND = 147;

	/* сервис не найден */
	public final static int EGTS_PC_SRVC_NFOUND = 148;

	/* сервис запрещен */
	public final static int EGTS_PC_SRVC_DENIED = 149;

	/* неизвестный тип сервиса */
	public final static int EGTS_PC_SRVC_UNKN = 150;

	/* авторизация запрещена */
	public final static int EGTS_PC_AUTH_DENIED = 151;

	/* объект уже существует */
	public final static int EGTS_PC_ALREADY_EXISTS = 152;

	/* идентификатор не найден */
	public final static int EGTS_PC_ID_NFOUND = 153;

	/* неправильная дата и время */
	public final static int EGTS_PC_INC_DATETIME = 154;

	/* ошибка ввода вывода */
	public final static int EGTS_PC_IO_ERROR = 155;

	/* недостаточно ресурсов */
	public final static int EGTS_PC_NO_RES_AVAIL = 156;

	/* внутренний сбой модуля */
	public final static int EGTS_PC_MODULE_FAULT = 157;

	/* внутренний сбой модуля */
	public final static int EGTS_PC_MODULE_PWR_FLT = 158;

	/* сбой в работе микроконтроллера модуля */
	public final static int EGTS_PC_MODULE_PROC_FLT = 159;

	/* сбой в работе программы модуля */
	public final static int EGTS_PC_MODULE_SW_FLT = 160;

	/* сбой в работе внутреннего ПО модуля */
	public final static int EGTS_PC_MODULE_FW_FLT = 161;

	/* сбой в работе блока ввода/вывода модуля */
	public final static int EGTS_PC_MODULE_IO_FLT = 162;

	/* сбой в работе внутренней памяти модуля */
	public final static int EGTS_PC_MODULE_MEM_FLT = 163;

	/* тест не пройден */
	public final static int EGTS_PC_TEST_FAILED = 164;

	/*
	 * Время ожидания подтверждения пакета на Транспортном Уровне, отсчитываемое
	 * с момента его отправки стороной, сгенерировавшей пакет, секунды 0...255
	 */
	public int TL_RESPONSE_TO = 5;

	/*
	 * Количество повторных попыток отправки неподтвержденного пакета стороной,
	 * сгенерировавшей пакет. Отсчитывается после истечения времени параметра
	 * TL_RESPONSE_TO при отсутствии пакета подтверждения 0...255
	 */
	public int TL_RESEND_ATTEMPTS = 3;

	/*
	 * Время в секундах, по истечении которого осуществляется повторная попытка
	 * установления канала связи после его разрыва 0...255
	 */
	public int TL_RECONNECT_TO = 30;

	/* EGTS Transport Packet Types definition */
	public int EGTS_UNDEFINE = -1;

	public int EGTS_PT_RESPONSE = 0;

	public int EGTS_PT_APPDATA = 1;

	public int EGTS_PT_SIGNED_APPDATA = 2;

	public int EGTS_PT_UNKNOWN = 0xFFFF;

	public int EGTS_PROTOCOL_VERSION = 0x01;

	public int EGTS_PT_PREF = 0x00;

	public int EGTS_PT_NOTSKID = 0x00;

	public int EGTS_PT_NOTROUTE = 0x00;

	public int EGTS_PT_NOTCRYPT = 0x00;

	public int EGTS_PT_NOTCOMPRES = 0x00;

	public int EGTS_PT_NOTENCODING = 0x00;

	/* Приоритеты */
	public int EGTS_PT_PRIORITY_LOW = 0x03;

	public int EGTS_PT_PRIORITY_HIGH = 0x01;

	public int EGTS_PT_PRIORITY_NORMAL = 0x02;

	public int EGTS_PT_PRIORITY_MAX = 0x00;

	public long EGTS_UNIX_TIME_DIFF = 0x4B3D3B00;

	public int EGTS_TRANSPORT_LAYER_MIN_HEADER_LEN = 11;

	public int EGTS_TRANSPORT_LAYER_MAX_HEADER_LEN = 16;

	public int EGTS_TRANSPORT_LAYER_ROUTE_REQ = 0x20;

	public int EGTS_TRANSPORT_LAYER_MAX_TP_ADDRESS = 0xFFFF;

	public int EGTS_TRANSPORT_LAYER_MIN_TP_ADDRESS = 0x0;

	public int EGTS_MAX_PACKET_LENGTH = 0xFFFF;

	public int EGTS_SERVICE_LAYER_MIN_RECORD_HEADER_LEN = 7;

	public int EGTS_SERVICE_LAYER_MIN_SUBRECORD_LEN = 3;

	public int EGTS_SR_TERM_IDENTITY_MIN_LEN = 5;

	public int EGTS_SERVICE_LAYER_MIN_SUBRECORD_HEADER_LEN = 3;

	public int EGTS_SERVICE_RECORD_FLAG_SSOD = 0x80;

	public int EGTS_SERVICE_RECORD_FLAG_RSOD = 0x40;

	public int EGTS_SERVICE_RECORD_FLAG_GRP = 0x20;

	public int EGTS_SERVICE_RECORD_FLAG_TMFE = 0x04;

	public int EGTS_SERVICE_RECORD_FLAG_EVFE = 0x02;

	public int EGTS_SERVICE_RECORD_FLAG_OBFE = 0x01;

	public int EGTS_SERVICE_RECORD_RPP_MASK = 0x18;

	public int EGTS_PC_ERROR_MASK = 0x80;

	public int EGTS_MAX_COUNT = 0xFFFF;

	public int EGTS_MIN_COUNT = 0x01;

	public int[] CRC8Table = { 0x00, 0x31, 0x62, 0x53, 0xC4, 0xF5, 0xA6, 0x97,
			0xB9, 0x88, 0xDB, 0xEA, 0x7D, 0x4C, 0x1F, 0x2E, 0x43, 0x72, 0x21,
			0x10, 0x87, 0xB6, 0xE5, 0xD4, 0xFA, 0xCB, 0x98, 0xA9, 0x3E, 0x0F,
			0x5C, 0x6D, 0x86, 0xB7, 0xE4, 0xD5, 0x42, 0x73, 0x20, 0x11, 0x3F,
			0x0E, 0x5D, 0x6C, 0xFB, 0xCA, 0x99, 0xA8, 0xC5, 0xF4, 0xA7, 0x96,
			0x01, 0x30, 0x63, 0x52, 0x7C, 0x4D, 0x1E, 0x2F, 0xB8, 0x89, 0xDA,
			0xEB, 0x3D, 0x0C, 0x5F, 0x6E, 0xF9, 0xC8, 0x9B, 0xAA, 0x84, 0xB5,
			0xE6, 0xD7, 0x40, 0x71, 0x22, 0x13, 0x7E, 0x4F, 0x1C, 0x2D, 0xBA,
			0x8B, 0xD8, 0xE9, 0xC7, 0xF6, 0xA5, 0x94, 0x03, 0x32, 0x61, 0x50,
			0xBB, 0x8A, 0xD9, 0xE8, 0x7F, 0x4E, 0x1D, 0x2C, 0x02, 0x33, 0x60,
			0x51, 0xC6, 0xF7, 0xA4, 0x95, 0xF8, 0xC9, 0x9A, 0xAB, 0x3C, 0x0D,
			0x5E, 0x6F, 0x41, 0x70, 0x23, 0x12, 0x85, 0xB4, 0xE7, 0xD6, 0x7A,
			0x4B, 0x18, 0x29, 0xBE, 0x8F, 0xDC, 0xED, 0xC3, 0xF2, 0xA1, 0x90,
			0x07, 0x36, 0x65, 0x54, 0x39, 0x08, 0x5B, 0x6A, 0xFD, 0xCC, 0x9F,
			0xAE, 0x80, 0xB1, 0xE2, 0xD3, 0x44, 0x75, 0x26, 0x17, 0xFC, 0xCD,
			0x9E, 0xAF, 0x38, 0x09, 0x5A, 0x6B, 0x45, 0x74, 0x27, 0x16, 0x81,
			0xB0, 0xE3, 0xD2, 0xBF, 0x8E, 0xDD, 0xEC, 0x7B, 0x4A, 0x19, 0x28,
			0x06, 0x37, 0x64, 0x55, 0xC2, 0xF3, 0xA0, 0x91, 0x47, 0x76, 0x25,
			0x14, 0x83, 0xB2, 0xE1, 0xD0, 0xFE, 0xCF, 0x9C, 0xAD, 0x3A, 0x0B,
			0x58, 0x69, 0x04, 0x35, 0x66, 0x57, 0xC0, 0xF1, 0xA2, 0x93, 0xBD,
			0x8C, 0xDF, 0xEE, 0x79, 0x48, 0x1B, 0x2A, 0xC1, 0xF0, 0xA3, 0x92,
			0x05, 0x34, 0x67, 0x56, 0x78, 0x49, 0x1A, 0x2B, 0xBC, 0x8D, 0xDE,
			0xEF, 0x82, 0xB3, 0xE0, 0xD1, 0x46, 0x77, 0x24, 0x15, 0x3B, 0x0A,
			0x59, 0x68, 0xFF, 0xCE, 0x9D, 0xAC };

	public int[] Crc16Table = { 0x0000, 0x1021, 0x2042, 0x3063, 0x4084, 0x50A5,
			0x60C6, 0x70E7, 0x8108, 0x9129, 0xA14A, 0xB16B, 0xC18C, 0xD1AD,
			0xE1CE, 0xF1EF, 0x1231, 0x0210, 0x3273, 0x2252, 0x52B5, 0x4294,
			0x72F7, 0x62D6, 0x9339, 0x8318, 0xB37B, 0xA35A, 0xD3BD, 0xC39C,
			0xF3FF, 0xE3DE, 0x2462, 0x3443, 0x0420, 0x1401, 0x64E6, 0x74C7,
			0x44A4, 0x5485, 0xA56A, 0xB54B, 0x8528, 0x9509, 0xE5EE, 0xF5CF,
			0xC5AC, 0xD58D, 0x3653, 0x2672, 0x1611, 0x0630, 0x76D7, 0x66F6,
			0x5695, 0x46B4, 0xB75B, 0xA77A, 0x9719, 0x8738, 0xF7DF, 0xE7FE,
			0xD79D, 0xC7BC, 0x48C4, 0x58E5, 0x6886, 0x78A7, 0x0840, 0x1861,
			0x2802, 0x3823, 0xC9CC, 0xD9ED, 0xE98E, 0xF9AF, 0x8948, 0x9969,
			0xA90A, 0xB92B, 0x5AF5, 0x4AD4, 0x7AB7, 0x6A96, 0x1A71, 0x0A50,
			0x3A33, 0x2A12, 0xDBFD, 0xCBDC, 0xFBBF, 0xEB9E, 0x9B79, 0x8B58,
			0xBB3B, 0xAB1A, 0x6CA6, 0x7C87, 0x4CE4, 0x5CC5, 0x2C22, 0x3C03,
			0x0C60, 0x1C41, 0xEDAE, 0xFD8F, 0xCDEC, 0xDDCD, 0xAD2A, 0xBD0B,
			0x8D68, 0x9D49, 0x7E97, 0x6EB6, 0x5ED5, 0x4EF4, 0x3E13, 0x2E32,
			0x1E51, 0x0E70, 0xFF9F, 0xEFBE, 0xDFDD, 0xCFFC, 0xBF1B, 0xAF3A,
			0x9F59, 0x8F78, 0x9188, 0x81A9, 0xB1CA, 0xA1EB, 0xD10C, 0xC12D,
			0xF14E, 0xE16F, 0x1080, 0x00A1, 0x30C2, 0x20E3, 0x5004, 0x4025,
			0x7046, 0x6067, 0x83B9, 0x9398, 0xA3FB, 0xB3DA, 0xC33D, 0xD31C,
			0xE37F, 0xF35E, 0x02B1, 0x1290, 0x22F3, 0x32D2, 0x4235, 0x5214,
			0x6277, 0x7256, 0xB5EA, 0xA5CB, 0x95A8, 0x8589, 0xF56E, 0xE54F,
			0xD52C, 0xC50D, 0x34E2, 0x24C3, 0x14A0, 0x0481, 0x7466, 0x6447,
			0x5424, 0x4405, 0xA7DB, 0xB7FA, 0x8799, 0x97B8, 0xE75F, 0xF77E,
			0xC71D, 0xD73C, 0x26D3, 0x36F2, 0x0691, 0x16B0, 0x6657, 0x7676,
			0x4615, 0x5634, 0xD94C, 0xC96D, 0xF90E, 0xE92F, 0x99C8, 0x89E9,
			0xB98A, 0xA9AB, 0x5844, 0x4865, 0x7806, 0x6827, 0x18C0, 0x08E1,
			0x3882, 0x28A3, 0xCB7D, 0xDB5C, 0xEB3F, 0xFB1E, 0x8BF9, 0x9BD8,
			0xABBB, 0xBB9A, 0x4A75, 0x5A54, 0x6A37, 0x7A16, 0x0AF1, 0x1AD0,
			0x2AB3, 0x3A92, 0xFD2E, 0xED0F, 0xDD6C, 0xCD4D, 0xBDAA, 0xAD8B,
			0x9DE8, 0x8DC9, 0x7C26, 0x6C07, 0x5C64, 0x4C45, 0x3CA2, 0x2C83,
			0x1CE0, 0x0CC1, 0xEF1F, 0xFF3E, 0xCF5D, 0xDF7C, 0xAF9B, 0xBFBA,
			0x8FD9, 0x9FF8, 0x6E17, 0x7E36, 0x4E55, 0x5E74, 0x2E93, 0x3EB2,
			0x0ED1, 0x1EF0 };

	/* EGTS Service Layer Types definition */
	public int EGTS_UNKNOWN_SERVICE = 0;

	public int EGTS_AUTH_SERVICE = 1;

	public int EGTS_TELEDATA_SERVICE = 2;

	public int EGTS_COMMANDS_SERVICE = 4;

	public int EGTS_FIRMWARE_SERVICE = 9;

	public int EGTS_ECALL_SERVICE = 10;

	public int EGTS_LOGGER_SERVICE = 16;

	public int EGTS_NIP_SERVICE = 17;

	public int EGTS_SMS_SERVICE = 18;

	public int EGTS_TACHOGRAPH_SERVICE = 19;

	public int EGTS_INSURANCE_SERVICE = 20;

	public int EGTS_REM_VEH_DIAG_SERVICE = 21;

	public int EGTS_TOLL_ROADS_SERVICE = 22;

	/* EGTS Protocol Service Records Types definition */
	public int EGTS_SR_RECORD_RESPONSE = 0x00;

	public int EGTS_SR_RECORD_UNKNOWN = 0xFFFF;

	/* AUTH_SERVICE */
	public int EGTS_SR_TERM_IDENTITY = 1;

	public int EGTS_SR_MODULE_DATA = 2;

	public int EGTS_SR_VEHICLE_DATA = 3;

	public int EGTS_SR_AUTH_PARAMS = 4;

	public int EGTS_SR_AUTH_INFO = 5;

	public int EGTS_SR_SERVICE_INFO = 6;

	public int EGTS_SR_RESULT_CODE = 7;

	/* FIRMWARE_SERVICE */
	public int EGTS_SR_FW_SERVICE_PART_DATA = 33;

	public int EGTS_SR_FW_SERVICE_FULL_DATA = 34;

	/* COMMANDS SERVICE */
	public int EGTS_SR_COMMANDS_COMMAND_DATA = 51;

	/* ECALL SERVICE */
	public int EGTS_SR_ECALL_ACCEL_DATA = 20;

	public int EGTS_SR_ECALL_RAW_MSD_DATA = 40;

	public int EGTS_SR_ECALL_MSD_DATA = 50;

	public int EGTS_SR_ECALL_TRACK_DATA = 62;

	/* REMOTE VECHICLE DIAGNOSTIC SERVICE */
	public int EGTS_SR_VEH_DIAG_DATA = 43;

	public int EGTS_SR_OBD2_DIAG_DATA = 44;

	/* MULTIMEDIA SERVICE */
	public int EGTS_SR_MM_SERVICE_PART_DATA = 33;

	public int EGTS_SR_MM_SERVICE_FULL_DATA = 34;

	/* LOGGER SERVICE */
	public int EGTS_SR_LOGGER_DATA = 56;

	/* SMS_SERVICE */
	public int EGTS_SR_WRAPPED_DATA = 57;

	/* TACHOGRAPH SERVICE */
	public int EGTS_SR_TACHO_DATA = 59;

	public int EGTS_SR_CARD_ID = 60;

	/* TOLL ROAD SERVICE */
	public int EGTS_SR_ROAD_GRAPH = 63;

	public int EGTS_SR_TOLL_ROAD_TELEDATA = 64;

	public int EGTS_TP_HEADER_CRC_LEN = 1;

	public int EGTS_TP_APP_CRC_LEN = 2;

}
