package ru.novoscan.trkpd;

import java.io.IOException;

import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.TrackPgUtils;

public interface TrackServer {
	void setConfig(ModConfig config);
	ModConfig getConfig();
	void setPgConnect(TrackPgUtils pgConnect);
	void run() throws IOException; 
}
