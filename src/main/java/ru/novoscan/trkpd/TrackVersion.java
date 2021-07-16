package ru.novoscan.trkpd;

public class TrackVersion {
	public static final String svnRevision="-1";

    public static final String buildTimestamp="2021-07-14T08:23:41Z";

    public static final String projectVersion="1.5.2";
    
    public static final String osInfo="Linux 5.12.14-arch1-1 amd64";

	public static String getSvnVersion() {
		return svnRevision;
	}

	public static String getBuildDate() {
		return buildTimestamp;
	}

	public static String getVersion() {
		return projectVersion;
	}
	
	public static String getBuildOs() {
        return osInfo;
    }

}
