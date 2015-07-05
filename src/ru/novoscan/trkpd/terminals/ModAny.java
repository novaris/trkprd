/**
 * 
 */
package ru.novoscan.trkpd.terminals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.TrackPgUtils;

/**
 * @author kur
 * 
 */
public class ModAny {
	static Logger logger = Logger.getLogger(ModAny.class);

	private static final Pattern rquit = Pattern.compile("(?im)quit.*");

	private final byte rbeg;

	private final byte rend;

	private float readbytes = 0;

	private float fullreadbytes = 0;

	private final int maxPacketSize;

	// private HashMap map = new HashMap();

	public ModAny(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader unbconsole, ModConfig conf, TrackPgUtils pgcon) {
		int cread;
		char data;
		String slog = "";
		this.maxPacketSize = conf.getMaxSize();
		this.rbeg = conf.getBegChar();
		this.rend = conf.getEndChar();
		try {
			while ((cread = unbconsole.read()) != -1) {
				readbytes = readbytes + 1;
				fullreadbytes = fullreadbytes + 1;
				if (readbytes > maxPacketSize) {
					logger.error("Over size " + maxPacketSize + " data : "
							+ slog);
					slog = "";
					// map.clear();
					return;
				} else {
					data = (char) cread;
					logger.debug("Read : " + Integer.toHexString(cread));
					if (rbeg == cread) {
						slog = "";
						// map.clear();
					} else if (rend == cread) {
						logger.info("Read packet data : " + slog);
						slog = "";
						// map.clear();
						readbytes = 0;
					} else {
						slog = slog + Character.toString(data);
						if (rquit.matcher(slog).matches()) {
							return;
						}
					}
				}

			}
			logger.debug("Close reader console");
		} catch (SocketTimeoutException e) {
			logger.error("Close connection : " + e.getMessage());
			logger.error("Read packet data : " + slog);
		} catch (IOException e) {
			logger.warn("IO socket error : " + e.getMessage());
		}
	}

	public float getReadBytes() {
		return fullreadbytes;
	}

}
