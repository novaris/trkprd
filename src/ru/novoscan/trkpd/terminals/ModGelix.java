package ru.novoscan.trkpd.terminals;

/**
 * 
 */

/**
 * @author Kurensky A. Evgeny
 *
 * Формат принимаемыд данных:
 * HTTP: 
 * get id=2608&amp;len=43&amp;data=000227EFD909514FA3479E04A4EBF47A9504004000428E03473DC169003D4000000000000000000000000077000227F0D90D514FA3479E03A4EBF46E8504704000421A03472EC169003D4000000000000000000000000472000227F1DA3B514FA3479E05A4EBF40F8504004001437903472DC169003EC000000000000000000000000502000227F2DA97514FA3479E13A4EBF3F385048040164B50034766C1690040C00000000000000000000000050D000227F3DA9B514FA3479E1CA4EBF3CD850470400E4B6403477BC169003DC000000000000000000000000507000227F4DAA2514FA3479E08A4EBF442850480401B441F03477EC169003C4000000000000000000000000509000227F5DAAF514FA3479DBCA4EBF5C585047040074454034742C169003DC00000000000000000000000050C000227F6DAF8514FA3479DF3A4EBF4AC85048040204B4D034754C169003E400000000000000000000000050B000227F7DB08514FA3479E22A4EBF3F085047040044399034787C169003DC000000000000000000000000508000227F8DB10514FA3479E30A4EBF3B785048040234B52034790C169003E4000000000000000000000000007
 */
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.novoscan.trkpd.domain.Terminal;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.TrackPgUtils;

public class ModGelix extends Terminal {
	public static final Pattern pattern = Pattern
			.compile("(?i)^get id=(\\d+)(&amp;|&)len=(\\d+)(&amp;|&)data=(.*)$");

	@SuppressWarnings("unused")
	private static String packet = "";

	private static int keylen;

	private static String data;

	@SuppressWarnings("unused")
	private static int blokid;

	private float readbytes = 0;

	public ModGelix(DataInputStream iDs, DataOutputStream oDs,
			BufferedReader console, ModConfig conf, TrackPgUtils pgcon)
			throws IOException {
		this.setDasnType(conf.getModType());
		String request;
		request = console.readLine();
		if (request != null) {
			Matcher m = pattern.matcher(request);
			readbytes = request.length();
			if (m.matches()) {
				// HTTPGelix module
				blokid = Integer.parseInt(m.group(1));
				keylen = Integer.parseInt(m.group(3));
				data = m.group(5);
				getPacket(1); // Бывает более 1 пакета?
			} else {
				// TCPGelix module

			}
		}
	}

	public void getPacket(int index) {
		packet = data.substring(index * keylen, keylen);
	}

	public boolean wrongDataLength() {
		if ((data.length() == 0) || (data.length() % keylen != 0)) {
			return true;
		} else {
			return false;
		}
	}

	public float getReadBytes() {
		return readbytes;
	}
}

class HTTPGelix {

}

class TCPGelix {

}
