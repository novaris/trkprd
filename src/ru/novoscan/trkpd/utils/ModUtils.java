package ru.novoscan.trkpd.utils;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import ru.novoscan.trkpd.resources.ModConstats;

public class ModUtils implements ModConstats {

	static NumberFormat nf = NumberFormat.getInstance(); // Get Instance

	private static final float nmi = (float) 1.852;

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
		int a2 = (Integer.parseInt(packet.substring(val + 5, 3), 16) >> 2) & 0x03FF; // 00000xxx00
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
		return Float.intBitsToFloat((0x000000FF & b1)
				+ ((0x000000FF & b2) << 8) + ((0x000000FF & b3) << 16)
				+ ((0x000000FF & b4) << 24));
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
			crc = ((crc << 8) ^ Crc16Table[(crc >> 8) ^ (buffer[i] & 0xff)]) & 0xffff;
		}
		return crc;
	}

	public synchronized static int getCrc8Egts(byte[] buffer, int offset,
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
}
