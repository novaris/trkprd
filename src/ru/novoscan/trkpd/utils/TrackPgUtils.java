package ru.novoscan.trkpd.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.postgresql.ds.PGSimpleDataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import ru.novoscan.trkpd.domain.DataSensor;
import ru.novoscan.trkpd.resources.ModConstats;

public class TrackPgUtils implements ModConstats {

	private static final Logger logger = Logger.getLogger(TrackPgUtils.class);

	private String[] commandStr;

	private long[] cmdId;

	private static final int maxCommand = 100;

	private String pgDatabaseName;

	private String pgUser;

	private String pgPasswd;

	private String pgHost;

	private final HikariDataSource pgds;

	private final SimpleDateFormat dateFormatFull = new SimpleDateFormat(
			DATE_FORMAT_FULL);

	public String getPgHost() {
		return pgHost;
	}

	public void setPgHost(String pgHost) {
		this.pgHost = pgHost;
	}

	private int pgPort;

	private DataSensor dataSensor;

	private final ModConfig config;

	private String valuesData;

	/*
	 * String vehicleId; int dasnUid; String dasnDateTime; float dasnLatitude;
	 * float dasnLongitude; int dasnStatus; int dasnSatUsed; int dasnZoneAlarm;
	 * int dasnMacroId; int dasnMacroSrc; float dasnSog; float dasnCource; float
	 * dasnHdop; float dasnHgeo; float dasnHmet; int dasnGpio; int dasnAdc;
	 * float dasnTemp; int8 i_spmt_id;
	 */

	public TrackPgUtils(ModConfig config) throws SQLException {
		this.config = config;
		this.setPgPasswd(config.getPgPasswd());
		this.setPgUser(config.getPgUser());
		this.setPgDatabaseName(config.getPgDatabaseName());
		this.setPgHost(config.getPgHost());
		this.setPgPort(config.getPgPort());

		HikariConfig dsConfig = new HikariConfig();
		dsConfig.setDataSourceClassName(PGSimpleDataSource.class.getName());
		dsConfig.setUsername(getPgUser());
		dsConfig.setPassword(getPgPasswd());
		dsConfig.addDataSourceProperty("databaseName",
				config.getPgDatabaseName());
		dsConfig.addDataSourceProperty("serverName", config.getPgHost());
		dsConfig.addDataSourceProperty("portNumber", config.getPgPort());
		// config.setJdbcUrl("jdbc:pgsql://localhost:5432/dbtrack");
		dsConfig.setConnectionTimeout(10000);
		dsConfig.setMaximumPoolSize(config.getPgMaxConn());
		dsConfig.setAutoCommit(false);

		pgds = new HikariDataSource(dsConfig);
	}

	public void addDataSensor() throws SQLException {

		if ((dataSensor != null) && (dataSensor.getDasnValues().size() > 0)) {
			StringBuffer stringBuffer = new StringBuffer();
			for (Entry<String, String> entry : dataSensor.getDasnValues()
					.entrySet()) {
				stringBuffer.append(entry.getKey()).append("=")
						.append(entry.getValue()).append(";");
			}
			valuesData = stringBuffer.deleteCharAt(stringBuffer.length() - 1)
					.toString();
		} else {
			valuesData = null;
		}
		if (dataSensor != null) {

			try (Connection db = pgds.getConnection()) {
				logger.debug(new StringBuffer().append("SQL Statment : ")
						.append("SELECT ").append("add_data_sensor('")
						.append(dataSensor.getDasnUid()).append("'::varchar,")
						.append(dataSensor.getDasnUid()).append("::int8,'")
						.append(dateFormatFull
								.format(dataSensor.getDasnDatetime()))
						.append("'::timestamp,")
						.append(dataSensor.getDasnLatitude())
						.append("::float8,")
						.append(dataSensor.getDasnLongitude())
						.append("::float8,").append(dataSensor.getDasnStatus())
						.append("::int8,").append(dataSensor.getDasnSatUsed())
						.append("::int8,").append(dataSensor.getDasnZoneAlarm())
						.append("::int8,").append(dataSensor.getDasnMacroId())
						.append("::int8,").append(dataSensor.getDasnMacroSrc())
						.append("::int8,").append(dataSensor.getDasnSog())
						.append("::float8,").append(dataSensor.getDasnCourse())
						.append("::float8,").append(dataSensor.getDasnHdop())
						.append("::float8,").append(dataSensor.getDasnHgeo())
						.append("::float8,").append(dataSensor.getDasnHmet())
						.append("::float8,").append(dataSensor.getDasnGpio())
						.append("::int8,").append(dataSensor.getDasnAdc())
						.append("::int8,").append(dataSensor.getDasnTemp())
						.append("::float8,").append(dataSensor.getDasnType())
						.append("::int8,'").append(valuesData)
						.append("'::text,").append("now()::timestamp,")
						.append(dataSensor.getDasnType()).append("::int8)")
						.toString());
				try (PreparedStatement ps = db
						.prepareStatement(new StringBuffer().append("SELECT ")
								.append("add_data_sensor").append("(?::varchar")
								// 1 идентификатор блока.
								.append(",?::int8")
								// 2 - идентификатор записи лога
								.append(",?::timestamp") // 3
															// -Дата
															// время
															// с
															// таймзоной
								.append(",?::float8") // 4 - latitude
														// Географическая
														// долгота
								.append(",?::float8") // 5 - longitude
														// Географическая
														// широта
								.append(",?::int8") // 6 - int4 -- Флаг
													// состояний
								.append(",?::int8") // 7 - int4 -- Количество
													// спутников
								.append(",?::int8") // 8 - int4 -- Состояние
													// тревога зон
													// охраны
								.append(",?::int8") // 9 - int4 -- Номер макроса
								.append(",?::int8") // 10 - int4 -- Код
													// источника
								.append(",?::float8") // 11 - float8 -- Скорость
														// в км/ч
								.append(",?::float8") // 12 - float8 -- Курс в
														// градусах
								.append(",?::float8") // 13 - float8 -- Значение
														// HDOP
								.append(",?::float8") // 14 - float8 -- Значение
														// HGEO
								.append(",?::float8") // 15 - float8 -- Значение
														// HMET
								.append(",?::int8") // 16 - int4 -- Состояние IO
								.append(",?::int8") // 17 - int8 -- Состояние
													// аналоговых
													// входов
								.append(",?::float8") // 18 - float8 --
														// Температура С
								.append(",?::int8") // 19 - int4 -- Тип данных
								.append(",?::text") // 20 -
													// text
													// Дополнтельные
													// данные.
								.append(",now()::timestamp") // 21 - timestamp
																// Дата
																// модификации
								.append(",?::int8)") // идентификатор типа
														// терминала
								.toString())) {
					if (dataSensor.isValid()) {
						ps.setString(1,
								String.valueOf(dataSensor.getDasnUid()));
						ps.setLong(2, Long.parseLong(dataSensor.getDasnUid()));
						ps.setTimestamp(3, dataSensor.getDasnTimestamp());
						ps.setDouble(4, dataSensor.getDasnLatitude());
						ps.setDouble(5, dataSensor.getDasnLongitude());
						if (dataSensor.getDasnStatus() == null) {
							ps.setNull(6, java.sql.Types.INTEGER);
						} else {
							ps.setLong(6, dataSensor.getDasnStatus());
						}
						if (dataSensor.getDasnSatUsed() == null) {
							ps.setNull(7, java.sql.Types.INTEGER);
						} else {
							ps.setLong(7, dataSensor.getDasnSatUsed());
						}
						if (dataSensor.getDasnZoneAlarm() == null) {
							ps.setNull(8, java.sql.Types.INTEGER);
						} else {
							ps.setLong(8, dataSensor.getDasnZoneAlarm());
						}
						if (dataSensor.getDasnMacroId() == null) {
							ps.setNull(9, java.sql.Types.INTEGER);
						} else {
							ps.setLong(9, dataSensor.getDasnMacroId());
						}
						if (dataSensor.getDasnMacroSrc() == null) {
							ps.setNull(10, java.sql.Types.INTEGER);
						} else {
							ps.setLong(10, dataSensor.getDasnMacroSrc());
						}
						if (dataSensor.getDasnSog() == null) {
							ps.setNull(11, java.sql.Types.DOUBLE);
						} else {
							ps.setDouble(11, dataSensor.getDasnSog());
						}
						if (dataSensor.getDasnCourse() == null) {
							ps.setNull(12, java.sql.Types.DOUBLE);
						} else {
							ps.setDouble(12, dataSensor.getDasnCourse());
						}
						if (dataSensor.getDasnHdop() == null) {
							ps.setNull(13, java.sql.Types.DOUBLE);
						} else {
							ps.setDouble(13, dataSensor.getDasnHdop());
						}
						if (dataSensor.getDasnHgeo() == null) {
							ps.setNull(14, java.sql.Types.DOUBLE);
						} else {
							ps.setDouble(14, dataSensor.getDasnHgeo());
						}
						if (dataSensor.getDasnHmet() == null) {
							ps.setNull(15, java.sql.Types.DOUBLE);
						} else {
							ps.setDouble(15, dataSensor.getDasnHmet());
						}
						if (dataSensor.getDasnGpio() == null) {
							ps.setNull(16, java.sql.Types.INTEGER);
						} else {
							ps.setLong(16, dataSensor.getDasnGpio());
						}
						if (dataSensor.getDasnAdc() == null) {
							ps.setNull(17, java.sql.Types.INTEGER);
						} else {
							ps.setLong(17, dataSensor.getDasnAdc());
						}
						if (dataSensor.getDasnTemp() == null) {
							ps.setNull(18, java.sql.Types.DOUBLE);
						} else {
							ps.setDouble(18, dataSensor.getDasnTemp());
						}
						ps.setLong(19, dataSensor.getDasnType());
						if (valuesData == null) {
							ps.setNull(20, java.sql.Types.LONGVARCHAR);
						} else {
							ps.setString(20, valuesData);
						}
						ps.setLong(21, config.getModType());
					}
					ResultSet resultSet = ps.executeQuery();
					resultSet.close();
				}

				db.commit();

			}
		} else {
			logger.error("Нет данных для записи в БД!");
		}

	}

	public int getImeiModule(String imei) {
		int spmd = -2;
		if (imei.length() != IMEI_LENGTH)
			return -3;
		try (Connection db = pgds.getConnection();
				PreparedStatement ps = db.prepareStatement(
						"SELECT spmd_id  FROM sprv_modules WHERE spmd_imei=?")) {
			ps.setString(1, imei);
			ResultSet resultSet = ps.executeQuery();
			if (resultSet.first()) {
				spmd = resultSet.getInt(1);
			}
			resultSet.close();
			db.commit();
		} catch (SQLException e) {
			logger.error("Ошибка поиска модуля по IMEI : " + e.getMessage());
			spmd = -1;
		}

		return spmd;
	}

	public void getCommand(String navDeviceID, int modType)
			throws SQLException {
		try (Connection db = pgds.getConnection()) {
			cmdId = new long[maxCommand];
			commandStr = new String[maxCommand];
			int cmd_num = 0;
			if ((navDeviceID == null) && (Integer.valueOf(modType) == null)) {
				return;
			}
			logger.debug(
					"SQL Statement : SELECT o_qumx, o_command FROM get_cmd_next(get_mod_id('"
							+ navDeviceID + "'::varchar, " + modType
							+ "::int8))");
			try (PreparedStatement ps = db.prepareStatement(
					"SELECT o_qumx,o_command FROM get_cmd_next(get_mod_id(?::varchar,?::int8))",
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_UPDATABLE)) {
				ps.setString(1, navDeviceID);
				ps.setInt(2, modType);
				ResultSet command = ps.executeQuery();
				while (command.next() && (cmd_num < maxCommand)) {
					cmdId[cmd_num] = command.getLong(1);
					commandStr[cmd_num] = command.getString(2);
					cmd_num = cmd_num + 1;
				}
				command.close();
				db.commit();
			}
		}
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
			try (Connection db = pgds.getConnection();
					PreparedStatement ps = db.prepareStatement(
							"SELECT set_cmd_status(?::int8,?::varchar,now()::timestamp,?::text)",
							ResultSet.TYPE_SCROLL_INSENSITIVE,
							ResultSet.CONCUR_UPDATABLE)) {
				logger.debug("SQL Statment : SELECT set_cmd_status(" + cmdId
						+ "::int8," + cmdStatus
						+ "::varchar, now()::timestamp, " + cmdInfo
						+ "::text)");
				ps.setLong(1, cmdId);
				ps.setString(2, cmdStatus);
				ps.setString(3, cmdInfo);
				ResultSet result = ps.executeQuery();
				logger.debug("SQL Statment executed");
				result.close();
			}
		} else {
			logger.error(new StringBuffer()
					.append("Command not getting. Describe : ")
					.append(cmdInfo));
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

	public DataSource getPgds() {
		return this.pgds;
	}

	public void setDataSensorValues(DataSensor dataSensor) {
		this.dataSensor = dataSensor;
	}

}
