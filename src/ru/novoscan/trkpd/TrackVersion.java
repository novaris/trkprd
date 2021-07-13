package ru.novoscan.trkpd;

public class TrackVersion {
	public static final String svnRevision="4092";

    public static final String buildTimestamp="2019-09-03T18:15:28Z";

    public static final String projectVersion="1.5.1";
    
    public static final String osInfo="Linux 4.15.0-60-generic amd64";

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
