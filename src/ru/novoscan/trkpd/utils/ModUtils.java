package ru.novoscan.trkpd.utils;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import ru.novoscan.trkpd.resources.ModConstats;

public class ModUtils implements ModConstats {

	public static final double KNOTS_TO_KPH_RATIO = 0.539957;

	public static final double KNOTS_TO_MPH_RATIO = 0.868976;

	public static final double KNOTS_TO_MPS_RATIO = 1.94384;

	public static final double KNOTS_TO_CPS_RATIO = 0.0194384449;

	public static final double METERS_TO_FEET_RATIO = 0.3048;

	public static final double METERS_TO_MILE_RATIO = 1609.34;

	public static final long MILLISECONDS_TO_HOURS_RATIO = 3600000;

	public static final long MILLISECONDS_TO_MINUTES_RATIO = 60000;

	static NumberFormat nf = NumberFormat.getInstance(); // Get Instance

	private static final float nmi = (float) 1.852;

	private static final int[] table = { 0x0000, 0xC0C1, 0xC181, 0x0140, 0xC301,
			0x03C0, 0x0280, 0xC241, 0xC601, 0x06C0, 0x0780, 0xC741, 0x0500,
			0xC5C1, 0xC481, 0x0440, 0xCC01, 0x0CC0, 0x0D80, 0xCD41, 0x0F00,
			0xCFC1, 0xCE81, 0x0E40, 0x0A00, 0xCAC1, 0xCB81, 0x0B40, 0xC901,
			0x09C0, 0x0880, 0xC841, 0xD801, 0x18C0, 0x1980, 0xD941, 0x1B00,
			0xDBC1, 0xDA81, 0x1A40, 0x1E00, 0xDEC1, 0xDF81, 0x1F40, 0xDD01,
			0x1DC0, 0x1C80, 0xDC41, 0x1400, 0xD4C1, 0xD581, 0x1540, 0xD701,
			0x17C0, 0x1680, 0xD641, 0xD201, 0x12C0, 0x1380, 0xD341, 0x1100,
			0xD1C1, 0xD081, 0x1040, 0xF001, 0x30C0, 0x3180, 0xF141, 0x3300,
			0xF3C1, 0xF281, 0x3240, 0x3600, 0xF6C1, 0xF781, 0x3740, 0xF501,
			0x35C0, 0x3480, 0xF441, 0x3C00, 0xFCC1, 0xFD81, 0x3D40, 0xFF01,
			0x3FC0, 0x3E80, 0xFE41, 0xFA01, 0x3AC0, 0x3B80, 0xFB41, 0x3900,
			0xF9C1, 0xF881, 0x3840, 0x2800, 0xE8C1, 0xE981, 0x2940, 0xEB01,
			0x2BC0, 0x2A80, 0xEA41, 0xEE01, 0x2EC0, 0x2F80, 0xEF41, 0x2D00,
			0xEDC1, 0xEC81, 0x2C40, 0xE401, 0x24C0, 0x2580, 0xE541, 0x2700,
			0xE7C1, 0xE681, 0x2640, 0x2200, 0xE2C1, 0xE381, 0x2340, 0xE101,
			0x21C0, 0x2080, 0xE041, 0xA001, 0x60C0, 0x6180, 0xA141, 0x6300,
			0xA3C1, 0xA281, 0x6240, 0x6600, 0xA6C1, 0xA781, 0x6740, 0xA501,
			0x65C0, 0x6480, 0xA441, 0x6C00, 0xACC1, 0xAD81, 0x6D40, 0xAF01,
			0x6FC0, 0x6E80, 0xAE41, 0xAA01, 0x6AC0, 0x6B80, 0xAB41, 0x6900,
			0xA9C1, 0xA881, 0x6840, 0x7800, 0xB8C1, 0xB981, 0x7940, 0xBB01,
			0x7BC0, 0x7A80, 0xBA41, 0xBE01, 0x7EC0, 0x7F80, 0xBF41, 0x7D00,
			0xBDC1, 0xBC81, 0x7C40, 0xB401, 0x74C0, 0x7580, 0xB541, 0x7700,
			0xB7C1, 0xB681, 0x7640, 0x7200, 0xB2C1, 0xB381, 0x7340, 0xB101,
			0x71C0, 0x7080, 0xB041, 0x5000, 0x90C1, 0x9181, 0x5140, 0x9301,
			0x53C0, 0x5280, 0x9241, 0x9601, 0x56C0, 0x5780, 0x9741, 0x5500,
			0x95C1, 0x9481, 0x5440, 0x9C01, 0x5CC0, 0x5D80, 0x9D41, 0x5F00,
			0x9FC1, 0x9E81, 0x5E40, 0x5A00, 0x9AC1, 0x9B81, 0x5B40, 0x9901,
			0x59C0, 0x5880, 0x9841, 0x8801, 0x48C0, 0x4980, 0x8941, 0x4B00,
			0x8BC1, 0x8A81, 0x4A40, 0x4E00, 0x8EC1, 0x8F81, 0x4F40, 0x8D01,
			0x4DC0, 0x4C80, 0x8C41, 0x4400, 0x84C1, 0x8581, 0x4540, 0x8701,
			0x47C0, 0x4680, 0x8641, 0x8201, 0x42C0, 0x4380, 0x8341, 0x4100,
			0x81C1, 0x8081, 0x4040, };

	private static final char[] kDigits = { '0', '1', '2', '3', '4', '5', '6',
			'7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	private static final float rad = (float) (180.0 / Math.PI);

	public ModUtils() {
	}

	public int fractal(int val) {
		int ret = val & 0x3FFF;
		if ((val & 0x4000) == 0) {
			ret = ret / 10;
		}
		if ((val & 0x8000) == 0) {
			ret = ret * (-1);
		}
		return ret;
	}

	public static char[] bytesToHex(byte[] raw) {
		int length = raw.length;
		char[] hex = new char[length * 2];
		for (int i = 0; i < length; i++) {
			int value = (raw[i] + 256) % 256;
			int highIndex = value >> 4;
			int lowIndex = value & 0x0f;
			hex[i * 2 + 0] = kDigits[highIndex];
			hex[i * 2 + 1] = kDigits[lowIndex];
		}
		return hex;
	}

	// Формирование времени в формате HH24MISS
	public String cTime(int val) {
		String ret;
		// Выполнить проверку входящего значения на 24*3600
		if (val < 86400 && val >= 0) {
			nf.setMinimumIntegerDigits(2); // The minimum Digits required is 2
			nf.setMaximumIntegerDigits(2); // The maximum Digits required is 2
			int hh = val / 3600;
			int mm = (val - hh * 3600) / 60;
			int ss = val - hh * 3600 - mm * 60;
			ret = nf.format(hh) + nf.format(mm) + nf.format(ss);

		} else {
			throw new InternalError("Time [0-86399] :" + val);
		}
		return ret;
	}

	// Формирование даты в формате DDMMYY
	public String cDate(int val) {
		nf.setMinimumIntegerDigits(2); // The minimum Digits required is 2
		nf.setMaximumIntegerDigits(2); // The maximum Digits required is 2
		int dd = val & 0x1F;
		int mm = (val >> 5) & 0x0F;
		int yy = (val >> 9) & 0x1F;
		String ret = nf.format(dd) + nf.format(mm) + nf.format(yy);
		return ret;
	}

	// Формирование даты в формате DDMMYY с лидирующим нулём

	public String formatDate(int val) {
		nf.setMinimumIntegerDigits(6); // The minimum Digits required is 6
		nf.setMaximumIntegerDigits(6); // The maximum Digits required is 6
		nf.setMinimumFractionDigits(0);
		nf.setGroupingUsed(false);
		String ret = nf.format(val);
		return ret;
	}

	// Преобразование форматов координат GG.MMMMMM
	public static float getGGMM(String val) {
		float fval = Float.valueOf(val).floatValue();
		float ll = (int) (fval / 1000000);
		ll = ll + (fval - (ll * 1000000)) / 600000;
		return ll;
	}

	// Преобразование форматов координат GGMM.MMMM
	public String getLL(String val) {
		float fval = Float.valueOf(val).floatValue();
		float ll = (int) (fval / 100);
		ll = ll + (fval - (ll * 100)) / 60;
		return String.valueOf(ll).toString();
	}

	// Преобразование скорости в узлах в час в км в час
	public String getSpeed(String val) {
		float fval = Float.valueOf(val).floatValue();
		float ll = fval * nmi;
		return String.valueOf(ll).toString();
	}

	/*
	 * SOURCE: 0xE0000000 coverts given coordinate in GGG.DDDDDDDD format source
	 * format: GGGMMSS.DDDD or GGMMSS.DDDD MM*60 + SS.DDDD -- X 3600" -- 1 grad.
	 */
	public int getLL(int offset, String packet) {
		String dotc = packet.substring(offset, 1);
		if (!dotc.equalsIgnoreCase("A")) {
			return 0;
		}
		int dot = (Integer.parseInt(dotc, 16) >> 1) - 1;
		int pos = Integer.parseInt(packet.substring(offset, 8), 8) & 0x1FFFFFFF; // GGGMMDDDD
		// System.out.print("dot=" + dot + " pos=" + pos + "<br>");
		float pos1 = (float) (pos / (Math.pow(10, (dot + 2))));
		int gg = (int) pos1;
		float mm = (pos1 - gg) * 5 / 3; // GG
		pos = (int) (gg + mm);
		return pos;
	}

	public String get5b(int val, String packet) {
		/*
		 * For Gelix if (keylen < 70) return "0000000000";
		 */
		int a0 = (Integer.parseInt(packet.substring(val, 3), 16) >> 2) & 0x03FF; // xxx0000000
		int a1 = (Integer.parseInt(packet.substring(val + 2, 3), 16)) & 0x03FF; // 00xxx00000
		int a2 = (Integer.parseInt(packet.substring(val + 5, 3), 16) >> 2)
				& 0x03FF; // 00000xxx00
		int a3 = (Integer.parseInt(packet.substring(val + 5, 3), 16)) & 0x03FF; // 0000000xxx
		NumberFormat nfl = NumberFormat.getInstance(); // Get Instance
		nfl.setMinimumIntegerDigits(3); // The minimum Digits required is 3
		nfl.setMaximumIntegerDigits(3); // The maximum Digits required is 3
		return nfl.format(a0) + nfl.format(a1) + nfl.format(a2)
				+ nfl.format(a3);
	}

	public String get4b(int val, String packet) {
		return Integer.toString(Integer.parseInt(packet.substring(val, 8), 16));
	}

	public String get2b(int val, String packet) {
		return Integer.toString(Integer.parseInt(packet.substring(val, 4), 16));
	}

	public String get1b(int val, String packet) {
		return Integer.toString(Integer.parseInt(packet.substring(val, 2), 16));
	}

	public int unsigned4Bytes(int b1, int b2, int b3, int b4) {
		return (0x000000FF & b1) + ((0x000000FF & b2) << 8)
				+ ((0x000000FF & b3) << 16) + ((0x000000FF & b4) << 24);
	}

	public float unsignedFloat4Bytes(int b1, int b2, int b3, int b4) {
		return Float.intBitsToFloat((0x000000FF & b1) + ((0x000000FF & b2) << 8)
				+ ((0x000000FF & b3) << 16) + ((0x000000FF & b4) << 24));
	}

	/*
	 * Функция подсчёта контрольной суммы для массива с помощью XOR
	 */

	public static int getSumXor(int[] b, int b_length) {
		int sumXor = 0;
		for (int i = 0; i < b_length; i++) {
			sumXor = (b[i] ^ sumXor);
		}
		return sumXor;
	}

	/*
	 * Получение строки с конца
	 */

	public String getLastnCharacters(String inputString, int subStringLength) {
		int length = inputString.length();
		if (length <= subStringLength) {
			return inputString;
		}
		int startIndex = length - subStringLength;
		return inputString.substring(startIndex);
	}

	/*
	 * 
	 */
	public float getNmi() {
		return nmi;
	}

	public static int getSumXor(String str, int length) {
		int ret;
		if (str.length() < length) {
			ret = -1;
		} else {
			int[] intArray = new int[length];
			for (int i = 0; i < length; i++) {
				intArray[i] = (int) str.charAt(i);
			}
			ret = getSumXor(intArray, length);
		}
		return ret;
	}

	public static int getIntByte(int[] dataBytes) {
		int data = 0;
		for (int i = 0; i < dataBytes.length; i++) {
			data = data + (int) ((dataBytes[i] & 0xff) << (8 * i));
		}
		return data;
	}

	public static int getIntU32(int[] u32) {
		int data = 0;
		for (int i = 0; i < 4; i++) {
			data = data + ((u32[i] & 0xff) << (8 * i));
		}
		return data;
	}

	public static int getIntU32(int[] u32, int seek) {
		int data = 0;
		for (int i = 0; i < 4; i++) {
			data = data + ((u32[seek + i] & 0xff) << (8 * i));
		}
		return data;
	}

	public static int getIntU32(byte[] u32, int seek) {
		int data = 0;
		for (int i = 0; i < 4; i++) {
			data = data + ((u32[seek + i] & 0xff) << (8 * i));
		}
		return data;
	}

	public static int getIntU32L(int[] u32, int seek) {
		int data = 0;
		for (int i = 0; i < 4; i++) {
			data = data + ((u32[seek + i] & 0xff) << (8 * (3 - i)));
		}
		return data;
	}

	public static int getIntU32L(byte[] u32, int seek) {
		int data = 0;
		for (int i = 0; i < 4; i++) {
			data = data + ((u32[seek + i] & 0xff) << (8 * (3 - i)));
		}
		return data;
	}

	public static int getIntU16(int[] u16, int seek) {
		int data = 0;
		for (int i = 0; i < 2; i++) {
			data = data + ((u16[seek + i] & 0xff) << (8 * i));
		}
		return data;
	}

	public static int getIntU16(byte[] u16, int seek) {
		int data = 0;
		for (int i = 0; i < 2; i++) {
			data = data + ((u16[seek + i] & 0xff) << (8 * i));
		}
		return data;
	}

	public static int getIntU16L(int[] u16, int seek) {
		int data = 0;
		for (int i = 0; i < 2; i++) {
			data = data + ((u16[seek + i] & 0xff) << (8 * (1 - i)));
		}
		return data;
	}

	public static int getIntU16L(byte[] u16, int seek) {
		int data = 0;
		for (int i = 0; i < 2; i++) {
			data = data + ((u16[seek + i] & 0xff) << (8 * (1 - i)));
		}
		return data;
	}

	public static String getDateTimeSignal(int[] b, int seek) {
		// DDMMYYHH24MISS
		nf.setMinimumIntegerDigits(2); // The minimum Digits required is 2
		nf.setMaximumIntegerDigits(2); // The maximum Digits required is 2
		return nf.format(b[seek + 3]) + nf.format(b[seek + 4] + 1)
				+ nf.format(b[seek + 5]) + nf.format(b[seek])
				+ nf.format(b[seek + 1]) + nf.format(b[seek + 2]);
	}

	public static float convRadianToDegree(float radian) {
		return radian * rad;
	}

	public static int getCrc16(int[] buffer) {
		return getCrc16(buffer, 0, buffer.length, 0xA001, 0);
	}

	public synchronized static int getCrc16(int[] buffer, int offset,
			int bufLen, int polynom, int preset) {
		preset &= 0xFFFF;
		polynom &= 0xFFFF;
		int crc = preset;
		for (int i = 0; i < bufLen; i++) {
			int data = buffer[i + offset] & 0xFF;
			crc ^= data;
			for (int j = 0; j < 8; j++) {
				if ((crc & 0x0001) != 0) {
					crc = (crc >> 1) ^ polynom;
				} else {
					crc = crc >> 1;
				}
			}
		}
		return crc & 0xFFFF;
	}

	public synchronized static int getCrc16(byte[] buffer, int offset,
			int lenght) {
		int crc = 0xffff;
		for (int i = offset; i < lenght + offset; i++) {
			crc = ((crc << 8) ^ Crc16Table[(crc >> 8) ^ (buffer[i] & 0xff)])
					& 0xffff;
		}
		return crc;
	}

	public synchronized static String getCRC16(String data) {

		byte[] bytes = data.getBytes();
		int crc = 0x0000;
		for (byte b : bytes) {
			crc = (crc >>> 8) ^ table[(crc ^ b) & 0xff];
		}

		return Integer.toHexString(crc);

	}

	public synchronized static int getCrc8Egts(byte[] buffer, int offset,
			int bufLen) {
		int crc = 0xff;
		for (int i = offset; i < (bufLen + offset); i++) {
			crc = CRC8Table[crc ^ (buffer[i] & 0xff)];
		}
		return crc;
	}

	public synchronized static int getCrc8Egts(int[] buffer, int offset,
			int bufLen) {
		int crc = 0xff;
		for (int i = offset; i < (bufLen + offset); i++) {
			crc = CRC8Table[crc ^ (buffer[i] & 0xff)];
		}
		return crc;
	}

	public synchronized static int getCrc8(int[] buffer, int offset,
			int bufLen) {
		int crc = 0xff;
		for (int i = offset; i < (bufLen + offset); i++) {
			crc = CRC8Table[crc ^ (buffer[i] & 0xff)];
		}
		return crc;
	}

	public static float getDegreeFromInt(int data) {
		float degree = (float) (data / 10000000.0);

		return degree;

	}

	public String getDate(Date date) {
		SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
		formatter.setTimeZone(TIME_ZONE);
		return formatter.format(date);
	}

	public static int getCrc16(String packet) {
		return getCrc16(packet.getBytes(), 0, packet.length());
	}

	public static boolean check(long number, int index) {
		return (number & (1 << index)) != 0;
	}

	public static int from(int number, int from) {
		return number >> from;
	}
}
