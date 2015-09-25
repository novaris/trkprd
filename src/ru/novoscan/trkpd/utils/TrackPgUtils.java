package ru.novoscan.trkpd.utils;

import java.math.BigInteger;
import java.sql.Connection;
import java.util.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.sql.PooledConnection;

import org.apache.log4j.Logger;
import org.postgresql.ds.PGConnectionPoolDataSource;

import ru.novoscan.trkpd.resources.ModConstats;

public class TrackPgUtils implements ModConstats {

	private static final Logger logger = Logger.getLogger(TrackPgUtils.class);

	private PooledConnection pc;

	private Connection db; // A connection to the database

	private HashMap<String, String> ds;

	private HashMap<Integer, BigInteger> values;

	private ResultSet resultSet;

	private String[] commandStr;

	private long[] cmdId;

	private static final int maxCommand = 100;

	private ResultSet result;

	private PreparedStatement ps;

	private Timestamp dasnDateTime;

	private String pgDatabaseName;

	private String pgUser;

	private String pgPasswd;

	private String pgHost;
	
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.ms");

	public String getPgHost() {
		return pgHost;
	}

	public void setPgHost(String pgHost) {
		this.pgHost = pgHost;
	}

	private int pgPort;

	/*
	 * String vehicleId; int dasnUid; String dasnDateTime; float dasnLatitude;
	 * float dasnLongitude; int dasnStatus; int dasnSatUsed; int dasnZoneAlarm;
	 * int dasnMacroId; int dasnMacroSrc; float dasnSog; float dasnCource; float
	 * dasnHdop; float dasnHgeo; float dasnHmet; int dasnGpio; int dasnAdc;
	 * float dasnTemp; int8 i_spmt_id;
	 */

	public Date getDasnDateTime() {
		return dasnDateTime;
	}

	public void setDasnDateTime(Date dasnDateTime) {
		this.dasnDateTime = new java.sql.Timestamp(dasnDateTime.getTime()); 
	}

	public TrackPgUtils() {

	}

	public void setConfig(ModConfig config) {
		{
			this.setPgPasswd(config.getPgPasswd());
			this.setPgUser(config.getPgUser());
			this.setPgDatabaseName(config.getPgDatabaseName());
			this.setPgHost(config.getPgHost());
			this.setPgPort(config.getPgPort());
		}

	}

	public void connect() throws ClassNotFoundException, SQLException {
		{
			final PGConnectionPoolDataSource pgds = new PGConnectionPoolDataSource();
			pgds.setLoginTimeout(10);
			pgds.setPassword(getPgPasswd());
			pgds.setDefaultAutoCommit(true);
			pgds.setUser(getPgUser());
			pgds.setDatabaseName(getPgDatabaseName());
			pgds.setServerName(getPgHost());
			pgds.setPortNumber(getPgPort());
			this.pc = pgds.getPooledConnection();
			this.db = pc.getConnection();
		}

	}

	public PooledConnection getPoolConnection() {
		return pc;
	}

	public void setDataSensor(HashMap<String, String> ds, Date navDateTime) {
		this.ds = ds;
		this.dasnDateTime = new java.sql.Timestamp(navDateTime.getTime());
	}

	public void setDataValues(HashMap<Integer, BigInteger> values) {
		this.values = values;
	}

	public void setDataSensorValues(HashMap<String, String> ds, Date date,
			HashMap<Integer, BigInteger> values) {
		this.ds = ds;
		this.values = values;
		this.dasnDateTime = new java.sql.Timestamp(date.getTime());
	}

	public void addDataSensor() throws SQLException {
		String xmlData;
		if (ds.containsKey("dasnXML")) {
			xmlData = ds.get("dasnXML").toString();
		} else if (values.size() > 0) {
			StringBuffer stringBuffer = new StringBuffer();
			for (Entry<Integer, BigInteger> entry : values.entrySet()) {
				stringBuffer.append(entry.getKey()).append("=")
						.append(entry.getValue()).append(";");
			}
			xmlData = stringBuffer.deleteCharAt(stringBuffer.length() - 1)
					.toString();
		} else {
			xmlData = "";
		}
		int type = 500;
		if (ds.containsKey("type")) {
			type = Integer.parseInt(ds.get("type").toString());
		}
		try {
			if (type == 501) {
				logger.debug(new StringBuffer().append("SQL Statment : ")
						.append("SELECT spmd_id").append("      ,spmd_spob_id")
						.append("  FROM sprv_modules")
						.append(" WHERE spmd_uid::varchar = '")
						.append(ds.get("vehicleId")).append("'").toString());
				ps = db.prepareStatement(new StringBuffer()
						.append("SELECT spmd_id ")
						.append("      ,spmd_spob_id)")
						.append("  FROM sprv_modules ")
						.append(" WHERE spmd_uid::varchar = ?").toString());

				if (ds.get("vehicleId") == null) {
					ps.setNull(1, java.sql.Types.VARCHAR);
				} else {
					ps.setString(1, ds.get("vehicleId").toString());
				}
				resultSet = ps.executeQuery();
				double spmdId = 0;
				while (resultSet.next()) {
					spmdId = resultSet.getDouble("spmd_id");
					logger.debug(new StringBuffer().append("Module found : ")
							.append(resultSet.getDouble("spmd_id")).toString());
				}
				if (spmdId == 0) {
					logger.debug(new StringBuffer().append("SQL Statment : ")
							.append("SELECT add_object(0, 0, '")
							.append(ds.get("n")).append("'::varchar")
							.append(",'").append(ds.get("c"))
							.append("'::varchar)").toString());
					ps = db.prepareStatement("SELECT add_object(0, 0, ?::varchar, ?::varchar)");
					if (ds.get("n") == null) {
						ps.setNull(1, java.sql.Types.VARCHAR);
					} else {
						ps.setString(1, ds.get("n").toString());
					}
					if (ds.get("c") == null) {
						ps.setNull(2, java.sql.Types.VARCHAR);
					} else {
						ps.setString(2, ds.get("c").toString());
					}
					resultSet = ps.executeQuery();
					double spobId = 0;
					while (resultSet.next()) {
						spobId = resultSet.getDouble(1);
						logger.debug(new StringBuffer()
								.append("Create Object : ").append(spobId)
								.toString());
					}
					logger.debug(new StringBuffer().append("SQL Statment : ")
							.append("SELECT add_module_combine (")
							.append(ds.get("vehicleId")).append("::float8,'")
							.append(ds.get("n")).append("'::varchar,")
							.append(ds.get("f")).append("'::varchar,")
							.append(",").append(spobId).append("::int8")
							.append(",500::int8").toString());
					ps = db.prepareStatement(new StringBuffer()
							.append("SELECT add_module_combine ")
							.append("(?::float8")// uid
							.append(",?::varchar")// name
							.append(",?::varchar")// imei
							.append(",?::varchar")// numb
							.append(",?::varchar")// desc
							.append(",?::int8")// spob_id
							.append(",?::int8")// spmt_id
							.append(")").toString());
					if (ds.get("vehicleId") == null) {
						ps.setNull(1, java.sql.Types.DOUBLE);
					} else {
						ps.setDouble(1,
								Double.valueOf(ds.get("vehicleId").toString()));
					}
					if (ds.get("n") == null) {
						ps.setNull(2, java.sql.Types.VARCHAR);
					} else {
						ps.setString(2, ds.get("n").toString());
					}
					ps.setString(3, "-");
					if (ds.get("s") == null) {
						ps.setNull(4, java.sql.Types.VARCHAR);
					} else {
						ps.setString(4, ds.get("s").toString());
					}
					if (ds.get("f") == null) {
						ps.setNull(5, java.sql.Types.VARCHAR);
					} else {
						ps.setString(5, ds.get("f").toString());
					}
					// ид объекта
					ps.setDouble(6, spobId);
					ps.setLong(7, 500);
					resultSet = ps.executeQuery();
					resultSet.next();
					logger.debug(new StringBuffer().append("Create Module : ")
							.append(resultSet.getDouble(1)));

				}
			} else {
				logger.debug(new StringBuffer().append("SQL Statment : ")
						.append("SELECT ").append("add_data_sensor('")
						.append(ds.get("vehicleId")).append("'::varchar,")
						.append(ds.get("vehicleId")).append("::int8,'")
						.append(dateFormat.format(dasnDateTime)).append("'::timestamp,")
						.append(ds.get("dasnLatitude")).append("::float8,")
						.append(ds.get("dasnLongitude")).append("::float8,")
						.append(ds.get("dasnStatus")).append("::int4,")
						.append(ds.get("dasnSatUsed")).append("::int4,")
						.append(ds.get("dasnZoneAlarm")).append("::int4,")
						.append(ds.get("dasnMacroId")).append("::int4,")
						.append(ds.get("dasnMacroSrc")).append("::int4,")
						.append(ds.get("dasnSog")).append("::float8,")
						.append(ds.get("dasnCource")).append("::float8,")
						.append(ds.get("dasnHdop")).append("::float8,")
						.append(ds.get("dasnHgeo")).append("::float8,")
						.append(ds.get("dasnHmet")).append("::float8,")
						.append(ds.get("dasnGpio")).append("::int4,")
						.append(ds.get("dasnAdc")).append("::int8,")
						.append(ds.get("dasnTemp")).append("::float8,")
						.append("1::int4,'").append(xmlData).append("'::text,")
						.append("now()::timestamp,")
						.append(ds.get("i_spmt_id")).append(")").toString());
				ps = db.prepareStatement(new StringBuffer().append("SELECT ")
						.append("add_data_sensor").append("(?::varchar")
						// 1 идентификатор блока.
						.append(",?::int8")
						// 2 - идентификатор записи лога
						.append(",?::timestamp") // 3
													// -Дата
													// время
													// с
													// таймзоной
						.append(",?::float8") // 4 - latitude Географическая
												// долгота
						.append(",?::float8") // 5 - longitude Географическая
												// широта
						.append(",?::int4") // 6 - int4 -- Флаг состояний
						.append(",?::int4") // 7 - int4 -- Количество спутников
						.append(",?::int4") // 8 - int4 -- Состояние тревога зон
											// охраны
						.append(",?::int4") // 9 - int4 -- Номер макроса
						.append(",?::int4") // 10 - int4 -- Код источника
						.append(",?::float8") // 11 - float8 -- Скорость в км/ч
						.append(",?::float8") // 12 - float8 -- Курс в градусах
						.append(",?::float8") // 13 - float8 -- Значение HDOP
						.append(",?::float8") // 14 - float8 -- Значение HGEO
						.append(",?::float8") // 15 - float8 -- Значение HMET
						.append(",?::int4") // 16 - int4 -- Состояние IO
						.append(",?::int8") // 17 - int8 -- Состояние аналоговых
											// входов
						.append(",?::float8") // 18 - float8 -- Температура С
						.append(",1::int4") // 19 - int4 -- Тип данных
						.append(",'").append(xmlData).append("'::text") // 20 -
																		// text
																		// Дополнтельные
																		// данные.
						.append(",now()::timestamp") // 21 - timestamp Дата
														// модификации
						.append(",?::int8)") // идентификатор типа блока
						.toString());
				if (ds.get("vehicleId") == null) {
					ps.setNull(1, java.sql.Types.VARCHAR);
				} else {
					ps.setString(1, ds.get("vehicleId").toString());
				}
				if (ds.get("vehicleId") == null) {
					ps.setNull(2, java.sql.Types.INTEGER);
				} else {
					ps.setLong(2, Long.valueOf(ds.get("vehicleId").toString())
							.longValue());
				}
				if (dasnDateTime == null) {
					ps.setNull(3, java.sql.Types.DATE);
				} else {
					ps.setTimestamp(3, dasnDateTime);
				}
				if (ds.get("dasnLatitude") == null) {
					ps.setNull(4, java.sql.Types.FLOAT);
				} else {
					ps.setFloat(4,
							Float.valueOf(ds.get("dasnLatitude").toString())
									.floatValue());
				}
				if (ds.get("dasnLongitude") == null) {
					ps.setNull(5, java.sql.Types.FLOAT);
				} else {
					ps.setFloat(5,
							Float.valueOf(ds.get("dasnLongitude").toString())
									.floatValue());
				}
				if (ds.get("dasnStatus") == null) {
					ps.setNull(6, java.sql.Types.INTEGER);
				} else {
					ps.setInt(6,
							Integer.valueOf((ds.get("dasnStatus").toString()))
									.intValue());
				}
				if (ds.get("dasnSatUsed") == null) {
					ps.setNull(7, java.sql.Types.INTEGER);
				} else {
					ps.setInt(7,
							Integer.valueOf((ds.get("dasnSatUsed").toString()))
									.intValue());
				}
				if (ds.get("dasnZoneAlarm") == null) {
					ps.setNull(8, java.sql.Types.INTEGER);
				} else {
					ps.setInt(
							8,
							Integer.valueOf(
									(ds.get("dasnZoneAlarm").toString()))
									.intValue());
				}
				if (ds.get("dasnMacroId") == null) {
					ps.setNull(9, java.sql.Types.INTEGER);
				} else {
					ps.setInt(9,
							Integer.valueOf((ds.get("dasnMacroId").toString()))
									.intValue());
				}
				if (ds.get("dasnMacroSrc") == null) {
					ps.setNull(10, java.sql.Types.INTEGER);
				} else {
					ps.setInt(
							10,
							Integer.valueOf((ds.get("dasnMacroSrc").toString()))
									.intValue());
				}
				if (ds.get("dasnSog") == null) {
					ps.setNull(11, java.sql.Types.FLOAT);
				} else {
					ps.setFloat(11,
							Float.valueOf((ds.get("dasnSog").toString()))
									.floatValue());
				}
				if (ds.get("dasnCource") == null) {
					ps.setNull(12, java.sql.Types.FLOAT);
				} else {
					ps.setFloat(12,
							Float.valueOf((ds.get("dasnCource").toString()))
									.floatValue());
				}
				if (ds.get("dasnHdop") == null) {
					ps.setNull(13, java.sql.Types.FLOAT);
				} else {
					ps.setFloat(13,
							Float.valueOf((ds.get("dasnHdop").toString()))
									.floatValue());
				}
				if (ds.get("dasnHgeo") == null) {
					ps.setNull(14, java.sql.Types.FLOAT);
				} else {
					ps.setFloat(14,
							Float.valueOf((ds.get("dasnHgeo").toString()))
									.floatValue());
				}
				if (ds.get("dasnHmet") == null) {
					ps.setNull(15, java.sql.Types.FLOAT);
				} else {
					ps.setFloat(15,
							Float.valueOf((ds.get("dasnHmet").toString()))
									.floatValue());
				}
				if (ds.get("dasnGpio") == null) {
					ps.setNull(16, java.sql.Types.INTEGER);
				} else {
					ps.setInt(16,
							Integer.valueOf((ds.get("dasnGpio").toString()))
									.intValue());
				}
				if (ds.get("dasnAdc") == null) {
					ps.setNull(17, java.sql.Types.INTEGER);
				} else {
					ps.setLong(17, Long.valueOf((ds.get("dasnAdc").toString()))
							.longValue());
				}
				if (ds.get("dasnTemp") == null) {
					ps.setNull(18, java.sql.Types.FLOAT);
				} else {
					ps.setFloat(18,
							Float.valueOf((ds.get("dasnTemp").toString()))
									.floatValue());
				}
				if (ds.get("i_spmt_id") == null) {
					ps.setNull(19, java.sql.Types.INTEGER);
				} else {
					ps.setLong(19,
							Long.valueOf((ds.get("i_spmt_id").toString()))
									.longValue());
				}
				resultSet = ps.executeQuery();
			}

		} catch (Exception e) {
			logger.warn(e.getMessage());
			throw new RuntimeException(e);
		}
		ps.close();
	}

	public int getImeiModule(String imei) {
		int spmd = -2;
		if (imei.length() != IMEI_LENGTH)
			return -3;
		try {
			db.prepareStatement(new StringBuffer().append("SELECT spmd_id ")
					.append("  FROM sprv_modules ")
					.append(" WHERE spmd_imei=?").toString());
			ps.setString(1, imei);
			resultSet = ps.executeQuery();
			if (resultSet.first()) {
				spmd = resultSet.getInt(1);
			}
			result.close();
			ps.close();
		} catch (SQLException e) {
			spmd = -1;
		}

		return spmd;
	}

	public void getCommand(String navDeviceID, int modType) throws SQLException {
		cmdId = new long[maxCommand];
		commandStr = new String[maxCommand];
		int cmd_num = 0;
		if ((navDeviceID == null) && (Integer.valueOf(modType) == null)) {
			return;
		}
		logger.debug(new StringBuffer().append("SQL Statment : ")
				.append("SELECT o_qumx ").append("      ,o_command ")
				.append("  FROM get_cmd_next(get_mod_id('").append(navDeviceID)
				.append("'::varchar").append("      ,").append(modType)
				.append("::int8))").toString());
		PreparedStatement ps = db
				.prepareStatement(
						"SELECT o_qumx,o_command FROM get_cmd_next(get_mod_id(?::varchar,?::int8))",
						ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_UPDATABLE);
		ps.setString(1, navDeviceID);
		ps.setInt(2, modType);
		ResultSet command = ps.executeQuery();
		while (command.next() && (cmd_num < maxCommand)) {
			cmdId[cmd_num] = command.getLong(1);
			commandStr[cmd_num] = command.getString(2);
			cmd_num = cmd_num + 1;
		}
		command.close();
		ps.close();
	}

	public long[] getCommandId() {
		return cmdId;
	}

	public String getCommandString(int cmdId) {
		return commandStr[cmdId];
	}

	public void setCommandStatus(long cmdId, String cmdStatus, String cmdInfo)
			throws SQLException {
		if (cmdId > 0) {

			logger.debug(new StringBuffer().append("SQL Statment : ")
					.append("SELECT set_cmd_status(").append(cmdId)
					.append("::int8,").append(cmdStatus).append("::varchar")
					.append(",now()::timestamp").append(cmdInfo)
					.append("::text)").toString());
			try {
				ps = db.prepareStatement(
						"SELECT set_cmd_status(?::int8,?::varchar,now()::timestamp,?::text)",
						ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_UPDATABLE);
				ps.setLong(1, cmdId);
				ps.setString(2, cmdStatus);
				ps.setString(3, cmdInfo);
				result = ps.executeQuery();
				logger.debug("SQL Statment executed");
				result.close();
				ps.close();
			} finally {
				if (result != null)
					result.close();
				if (ps != null)
					ps.close();
			}
		} else {
			logger.error(new StringBuffer().append(
					"Command not getting. Describe : ").append(cmdInfo));
		}

	}

	public String getPgDatabaseName() {
		return pgDatabaseName;
	}

	public void setPgDatabaseName(String pgDatabaseName) {
		this.pgDatabaseName = pgDatabaseName;
	}

	public String getPgUser() {
		return pgUser;
	}

	public void setPgUser(String pgUser) {
		this.pgUser = pgUser;
	}

	public String getPgPasswd() {
		return pgPasswd;
	}

	public void setPgPasswd(String pgPasswd) {
		this.pgPasswd = pgPasswd;
	}

	public int getPgPort() {
		return pgPort;
	}

	public void setPgPort(int pgPort) {
		this.pgPort = pgPort;
	}

}
