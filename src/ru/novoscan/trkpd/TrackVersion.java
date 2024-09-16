package ru.novoscan.trkpd;

public class TrackVersion {
	public static final String svnRevision="4191";

    public static final String buildTimestamp="2024-09-10T18:51:16Z";

    public static final String projectVersion="1.6.0";
    
    public static final String osInfo="Linux 6.10.7-arch1-1 amd64";

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
