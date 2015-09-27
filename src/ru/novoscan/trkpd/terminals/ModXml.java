package ru.novoscan.trkpd.terminals;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import javax.xml.xpath.*;

import org.apache.log4j.Logger;

import ru.novoscan.trkpd.resources.ModConstats;
import ru.novoscan.trkpd.utils.ModConfig;
import ru.novoscan.trkpd.utils.TrackPgUtils;

import org.apache.commons.lang3.StringEscapeUtils;

public class ModXml implements ModConstats {
	static Logger logger = Logger.getLogger(ModXml.class);

	private static final Pattern rquit = Pattern.compile("^(?im)quit$");

	private static final Pattern rpacket = Pattern.compile("<paket>.*</paket>");

	private float fullreadbytes = 0;

	private SimpleDateFormat sdf = new SimpleDateFormat(DATE_SIMPLE_FORMAT);

	private HashMap<String, String> map = new HashMap<String, String>();

	private Date date = new Date();

	// private TRKUtils utl = new TRKUtils();

	public ModXml(DataInputStream iDs, DataOutputStream oDs,
			InputStreamReader unbconsole, ModConfig conf, TrackPgUtils pgcon)
			throws ParseException, IOException {
		String slog = "";
		BufferedReader binstream = new BufferedReader(
				new InputStreamReader(iDs));
		PrintWriter boutstream = new PrintWriter(oDs, true);
		try {
			while (true) {
				slog = binstream.readLine();
				if (slog != null) {
					fullreadbytes = fullreadbytes + slog.length();
					if (rpacket.matcher(slog).matches()) {
						logger.info("Read packet data : " + slog);
						parsePacket(conf, pgcon, new InputSource(
								new StringReader(slog)));
						slog = "";
						logger.debug("Parsing OK.");
						boutstream.println("OK");
						return;
					} else if (rquit.matcher(slog).matches()) {
						logger.debug("Quit");
						return;
					} else {
						logger.error("Incorrect Data Format : " + slog);
						return;
					}
				}
			}
		} catch (XPathExpressionException e) {
			logger.error("Close connection : " + e.getMessage());
			logger.error("Read packet data : " + slog);
			e.printStackTrace();
		} catch (SAXException e) {
			logger.error("Close connection : " + e.getMessage());
			logger.error("Read packet data : " + slog);
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			logger.error("Close connection : " + e.getMessage());
			logger.error("Read packet data : " + slog);
			e.printStackTrace();
		}
	}

	public float getReadBytes() {
		return fullreadbytes;
	}

	private void parsePacket(ModConfig conf, TrackPgUtils pgcon,
			InputSource xmldata) throws SAXException, IOException,
			ParserConfigurationException, XPathExpressionException,
			ParseException {
		// разбор пакета
		logger.debug("Parse Packets");
		//
		DocumentBuilderFactory factoryDOM = DocumentBuilderFactory
				.newInstance();
		factoryDOM.setNamespaceAware(true); // never forget this!
		DocumentBuilder builder = factoryDOM.newDocumentBuilder();
		Document doc = builder.parse(xmldata);
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		XPathExpression expr = xpath.compile("/paket/pid/*");
		Object result = expr.evaluate(doc, XPathConstants.NODESET);
		NodeList nodes = (NodeList) result;
		Integer pid = null;
		for (int i = 0; i < nodes.getLength(); i++) {
			/*
			 * для 500 типа <paket> <pid> <id>"Ид данных пакета"</id>
			 * <uid>"Ид блока"</uid> <dt>"DDMMYYHH24MISS"<dt>
			 * <lat>"Широта"</lat> <lon>"Долгота"</lon>
			 * <stat>"Статус блока"</stat> <sat>"Количество спутников"</sat>
			 * <alarm>"Тревога"</alarm> <course>"Курс в градусах"</course>
			 * <sog>"Скорость в км.ч"</sog>
			 * <hdop>"Снижение точности в горизонтальной плоскости"</hdop>
			 * <hgeo>"Высота над уровем моря"</hgeo>
			 * <gpio>"Данные датчиков"</gpio> <adc>"Напряжение питания"</adc>
			 * <temp>"Температура"</temp> <type>"Тип блока"</type> </pid>
			 * </paket>
			 * 
			 * Для 501 типа <paket> <pid> <id>номер записи в пакете</id> <uid>ид
			 * терминала</uid> <ur>реальный ид терминала</ur> <p>протокол
			 * терминала</p> <d>дата регистрации объекта</d>" <c>комментарий
			 * объекта</c> <s>номер тел SIM терминала</s> <n>гос номер
			 * объекта</n> <g>ид группы объекта</g> <f>версия ПО терминала</f>
			 * <type>501</type> </pid> <pid> .... </pid> </paket>
			 */
			String nodeName = nodes.item(i).getNodeName();
			String nodeValue = nodes.item(i).getTextContent();
			logger.debug("Node : " + nodeName + " Value : "
					+ StringEscapeUtils.unescapeHtml4(nodeValue));
			if (nodeName == "id") {
				if (pid != null) {
					// запись в БД предыдущего пакета
					map.put("i_spmt_id", Integer.toString(conf.getModType()));
					map.put("dasnMacroId", null);
					map.put("dasnMacroSrc", null);
					pgcon.setDataSensor(map, date);
					try {
						pgcon.addDataSensor();
						logger.debug("Write Database OK");
					} catch (SQLException e) {
						logger.warn("Error Writing Database : "
								+ e.getMessage());
					}
					map.clear();
				}
				logger.debug("Packet ID : " + nodeValue);
				pid = Integer.parseInt(StringEscapeUtils
						.unescapeHtml4(nodeValue));
			} else if (nodeName == "uid") {
				map.put("vehicleId", StringEscapeUtils.unescapeHtml4(nodeValue));
				map.put("dasnUid", StringEscapeUtils.unescapeHtml4(nodeValue));
			} else if (nodeName == "dt") {
				date = sdf.parse(StringEscapeUtils.unescapeHtml4(nodeValue));
			} else if (nodeName == "lat") {
				map.put("dasnLatitude",
						StringEscapeUtils.unescapeHtml4(nodeValue));
			} else if (nodeName == "lon") {
				map.put("dasnLongitude",
						StringEscapeUtils.unescapeHtml4(nodeValue));
			} else if (nodeName == "stat") {
				map.put("dasnStatus",
						StringEscapeUtils.unescapeHtml4(nodeValue));
			} else if (nodeName == "sat") {
				map.put("dasnSatUsed",
						StringEscapeUtils.unescapeHtml4(nodeValue));
			} else if (nodeName == "alarm") {
				map.put("dasnZoneAlarm",
						StringEscapeUtils.unescapeHtml4(nodeValue));
			} else if (nodeName == "course") {
				map.put("dasnCource",
						StringEscapeUtils.unescapeHtml4(nodeValue));
			} else if (nodeName == "sog") {
				map.put("dasnSog", StringEscapeUtils.unescapeHtml4(nodeValue));
			} else if (nodeName == "hdop") {
				map.put("dasnHdop", StringEscapeUtils.unescapeHtml4(nodeValue));
			} else if (nodeName == "hget") {
				map.put("dasnHgeo", StringEscapeUtils.unescapeHtml4(nodeValue));
			} else if (nodeName == "gpio") {
				map.put("dasnGpio", StringEscapeUtils.unescapeHtml4(nodeValue));
			} else if (nodeName == "adc") {
				map.put("dasnAdc", StringEscapeUtils.unescapeHtml4(nodeValue));
			} else if (nodeName == "temp") {
				map.put("dasnTemp", StringEscapeUtils.unescapeHtml4(nodeValue));
			} else {
				map.put(nodeName, StringEscapeUtils.unescapeHtml4(nodeValue));
			}

		}
		if (pid != null) {
			// запись последнего пакета
			logger.debug("Packet ID : " + pid);
			map.put("i_spmt_id", Integer.toString(conf.getModType()));
			map.put("dasnMacroId", null);
			map.put("dasnMacroSrc", null);
			pgcon.setDataSensor(map, date);
			try {
				pgcon.addDataSensor();
				logger.debug("Write Database OK");
			} catch (SQLException e) {
				logger.warn("Error Writing Database : " + e.getMessage());
			}
			map.clear();
		}
	}

}
