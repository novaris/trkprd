/*
 * Copyright 2016 Novaris Ltd.
 *
 * This file is part of Novaris Track System (NTS).
 * NTS is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * NTS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NTS. If not, see <http://www.gnu.org/licenses/>.
 *
 */

/*
 * SNMP for Spring
 *
 * http://spring-snmp.sourceforge.net/index.html
 *
 * SNMP for Spring is licensed under the Apache License, Version 2.0 
 *
 * Original codes: https://github.com/jpragma/spring-snmp
 *
 */

package ru.novoscan.trkpd.snmp.spring;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;

import com.jpragma.snmp.SnmpSystemException;
import com.jpragma.snmp.agent.Mib2System;
import com.jpragma.snmp.agent.MibEntry;
import com.jpragma.snmp.asn.AsnNull;
import com.jpragma.snmp.asn.AsnOID;
import com.jpragma.snmp.asn.AsnObject;
import com.jpragma.snmp.asn.ber.BerTlv;
import com.jpragma.snmp.types.AbstractPDU;
import com.jpragma.snmp.types.GetNextRequestPDU;
import com.jpragma.snmp.types.GetRequestPDU;
import com.jpragma.snmp.types.GetResponsePDU;
import com.jpragma.snmp.types.Message;
import com.jpragma.snmp.types.PDU;
import com.jpragma.snmp.types.SetRequestPDU;
import com.jpragma.snmp.types.VarBind;
import com.jpragma.snmp.types.VarBindList;

/**
 * Updated the original Spring for Snmp class to catch SocketException to lock
 * application to avoid duplicated application launch
 * 
 * Also removed setting proxySnmpClient as it requires call protected SnmpClient
 * method
 * 
 * Also excluded one of the two extra bindings while composing a datagram
 * package.
 * 
 * The second binding is excluded in the TkAsnSequence by providing the method
 * to get the List value without binding.
 * 
 * @author Evgeny Kurensky
 */

public class TrackSpringSnmpAgent implements Runnable {

	private static final int PACKET_MAX_SIZE = 484;

	private static final int SNMP_VERSION = 0;

	private int listeningPort;

	private String readOnlyComunity;

	private String readWriteComunity;

	private Mib2System mib2System;

	private String handledOidPrefixes[];

	// Fix: Added parameters to avoid compilation warnings
	private TreeMap<int[], MibEntry> mibEntries;

	private Thread workerThread;

	private DatagramSocket socket;

	private Logger logger = Logger.getLogger(this.getClass());

	public TrackSpringSnmpAgent() {
		listeningPort = 161;
		readOnlyComunity = "public";
		readWriteComunity = "public";
		workerThread = null;
		socket = null;
	}

	// Fix: Added two flags and accessors
	// to control lock socket within application
	private volatile boolean isSocketCreated = false;

	private volatile boolean isSocketException = false;

	public boolean isSocketCreated() {
		return isSocketCreated;
	}

	public boolean isSocketException() {
		return isSocketException;
	}

	public void run() {
		try {
			logger.debug("Binding to UDP port " + listeningPort);
			socket = new DatagramSocket(listeningPort);
		} catch (SocketException e) {
			// Fix: Flag to control lock socket within application
			isSocketException = true;
			throw new SnmpSystemException(
					"Can not create SNMP agent UDP socket. Reason: "
							+ e.getMessage());
		}

		// Fix: Flag to control lock socket within application
		isSocketCreated = true;

		while (!workerThread.isInterrupted()) {
			byte requestPacketBuffer[] = new byte[PACKET_MAX_SIZE];
			final DatagramPacket requestPacket = new DatagramPacket(
					requestPacketBuffer, requestPacketBuffer.length);
			try {
				socket.receive(requestPacket);
			} catch (IOException e) {
				logger.error("Error processing SNMP request. Reason: "
						+ e.getMessage());
				continue;
			}
			final byte data[] = requestPacket.getData();
			Thread procThread = new Thread(new Runnable() {

				public void run() {
					try {
						Message requestMsg;
						Message responseMsg;
						PDU requestPDU;
						// Fix: Added missed ')' character
						logger.debug("Received packet (length=" + data.length
								+ " bytes)");
						BerTlv tlv = new BerTlv();
						tlv.decode(new ByteArrayInputStream(data));
						requestMsg = new Message(tlv);
						responseMsg = null;
						requestPDU = (PDU) requestMsg.getPDU();
						String requestedOID = requestPDU.getVarBindList()
								.getVarBind(0).getObjectName().stringValue();

						if (!hasHandledPrefix(requestedOID)) {

							logger.info("No entry found for oid "
									+ requestedOID);
							GetResponsePDU responsePDU = new GetResponsePDU(
									requestPDU.getRequestId(), 2, 0L,
									requestPDU.getVarBindList());
							responseMsg = new Message(0, requestMsg
									.getComunity(), responsePDU);

							return;
						}

						if (!validateCommunity(requestMsg)) {
							logger.warn("Community ["
									+ requestMsg.getComunity()
									+ "] doesn't match for snmp request");
							return;
						}

						GetResponsePDU responsePDU = null;
						if (requestPDU instanceof GetRequestPDU)
							responsePDU = handleGetRequest((GetRequestPDU) requestPDU);
						else if (requestPDU instanceof GetNextRequestPDU)
							responsePDU = handleGetNextRequest((GetNextRequestPDU) requestPDU);
						else if (requestPDU instanceof SetRequestPDU)
							responsePDU = handleSetRequest((SetRequestPDU) requestPDU);
						else
							responsePDU = createErrorResponse(
									requestPDU.getRequestId(), 5, 0,
									requestPDU.getVarBindList());
						responseMsg = new Message(0, requestMsg.getComunity(),
								responsePDU);

						logger.info("Sending response to client\n"
								+ responseMsg.toString());
						byte responsePacketBuffer[] = responseMsg.toBerTlv()
								.getBytes();
						if (responsePacketBuffer.length > PACKET_MAX_SIZE) {
							responsePDU = new GetResponsePDU(requestPDU
									.getRequestId(), 1, 0L, requestPDU
									.getVarBindList());
							responseMsg = new Message(SNMP_VERSION, requestMsg
									.getComunity(), responsePDU);
							responsePacketBuffer = responseMsg.toBerTlv()
									.getBytes();
						}
						DatagramPacket responsePacket = new DatagramPacket(
								responsePacketBuffer,
								responsePacketBuffer.length, requestPacket
										.getAddress(), requestPacket.getPort());
						socket.send(responsePacket);
					} catch (Exception e) {
						logger.error("Error processing SNMP request. Reason: "
								+ e.getMessage());
					}
				}
			});
			procThread.start();
		}
	}

	private GetResponsePDU handleGetRequest(GetRequestPDU requestPDU) {
		VarBindList requestVarBindList;
		VarBindList responseVarBindList;
		GetResponsePDU responsePDU;
		int curVarBindIndx;
		logger.debug("Processing snmpget");
		requestVarBindList = requestPDU.getVarBindList();
		responseVarBindList = new VarBindList();
		responsePDU = null;
		curVarBindIndx = 0;
		Iterator<?> iter = requestVarBindList.iterator();

		try {
			while (iter.hasNext()) {
				VarBind curVarBind = (VarBind) iter.next();
				AsnOID curOID = curVarBind.getObjectName();
				logger.debug("Processing request for OID: "
						+ curOID.stringValue());
				MibEntry curMibEntry = getMibEntry((int[]) curOID.getValue());
				if (curMibEntry != null) {
					logger.debug("Found: " + curMibEntry.toString());
					Object result = PropertyUtils.getProperty(
							curMibEntry.getHandlerBean(),
							curMibEntry.getHandlerProperty());
					AsnObject resultAsnObject = null;
					if (result == null) {
						logger.debug("Bean returned NULL value");
						resultAsnObject = new AsnNull();
					} else {
						logger.debug("Bean returned type: "
								+ result.getClass().getName() + " value: "
								+ result.toString());
						resultAsnObject = (AsnObject) curMibEntry
								.getRequiredType()
								.getDeclaredConstructor()
								.newInstance();
						resultAsnObject.setValue(result);
					}
					logger.debug("Result has been converted to "
							+ resultAsnObject.getClass().getName());
					VarBind resultVarBind = new VarBind(curOID, resultAsnObject);
					responseVarBindList.add(resultVarBind);
				} else {
					responsePDU = createErrorResponse(
							requestPDU.getRequestId(), 2, curVarBindIndx,
							requestPDU.getVarBindList());
					return responsePDU;
				}
				curVarBindIndx++;
			}

			responsePDU = new GetResponsePDU(requestPDU.getRequestId(),
					responseVarBindList);
		} catch (Exception e) {
			logger.error("Error invoking bean.Reason: " + e.getMessage());
			responsePDU = createErrorResponse(requestPDU.getRequestId(), 5,
					curVarBindIndx, requestPDU.getVarBindList());
		}
		return responsePDU;
	}

	private GetResponsePDU handleGetNextRequest(GetNextRequestPDU requestPDU) {
		VarBindList requestVarBindList;
		VarBindList responseVarBindList;
		GetResponsePDU responsePDU;
		int curVarBindIndx;
		logger.debug("Processing snmpgetnext");
		requestVarBindList = requestPDU.getVarBindList();
		responseVarBindList = new VarBindList();
		responsePDU = null;
		curVarBindIndx = 0;
		Iterator<?> iter = requestVarBindList.iterator();

		try {
			while (iter.hasNext()) {
				VarBind curVarBind = (VarBind) iter.next();
				AsnOID curOID = curVarBind.getObjectName();
				logger.debug("Processing request for OID: "
						+ curOID.stringValue());
				int nextOIDDigits[] = nextOID((int[]) (int[]) curOID.getValue());
				logger.debug("Lexicographical successor is "
						+ (nextOIDDigits != null ? Arrays
								.toString(nextOIDDigits) : "NULL"));
				MibEntry curMibEntry = null;
				if (nextOIDDigits != null)
					curMibEntry = getMibEntry(nextOIDDigits);
				if (curMibEntry != null) {
					logger.debug("Found: " + curMibEntry.toString());
					Object result = PropertyUtils.getProperty(
							curMibEntry.getHandlerBean(),
							curMibEntry.getHandlerProperty());
					AsnObject resultAsnObject = null;
					if (result == null) {
						logger.debug("Bean returned NULL value");
						resultAsnObject = new AsnNull();
					} else {
						logger.debug("Bean returned type: "
								+ result.getClass().getName() + " value: "
								+ result.toString());
						resultAsnObject = (AsnObject) curMibEntry
								.getRequiredType()
								.getDeclaredConstructor()
								.newInstance();
						resultAsnObject.setValue(result);
					}
					logger.debug("Result has been converted to "
							+ resultAsnObject.getClass().getName());
					// Fix: Exclude extra binding for the ASN_SEQUENCE
					if (resultAsnObject instanceof TrackAsnSequence) {
						responseVarBindList = new VarBindList(
								((TrackAsnSequence) resultAsnObject).getList());
					} else {
						VarBind nextResultVarBind = new VarBind(new AsnOID(
								nextOIDDigits), resultAsnObject);
						responseVarBindList.add(nextResultVarBind);
					}
				} else {
					responsePDU = createErrorResponse(
							requestPDU.getRequestId(), 2, curVarBindIndx,
							requestPDU.getVarBindList());
					return responsePDU;
				}
				curVarBindIndx++;
			}

			responsePDU = new GetResponsePDU(requestPDU.getRequestId(),
					responseVarBindList);
		} catch (Exception e) {
			logger.error("Error invoking bean.Reason: " + e.getMessage());
			responsePDU = createErrorResponse(requestPDU.getRequestId(), 5,
					curVarBindIndx, requestPDU.getVarBindList());
		}
		return responsePDU;
	}

	private GetResponsePDU handleSetRequest(SetRequestPDU requestPDU) {
		VarBindList requestVarBindList;
		VarBindList responseVarBindList;
		GetResponsePDU responsePDU;
		int curVarBindIndx;
		logger.debug("Processing snmpset");
		requestVarBindList = requestPDU.getVarBindList();
		responseVarBindList = new VarBindList();
		responsePDU = null;
		curVarBindIndx = 0;
		Iterator<?> iter = requestVarBindList.iterator();

		try {
			while (iter.hasNext()) {

				VarBind curVarBind = (VarBind) iter.next();
				AsnOID curOID = curVarBind.getObjectName();
				AsnObject curValue = curVarBind.getObjectSyntax();
				logger.debug("Processing request for OID: "
						+ curOID.stringValue() + " value: "
						+ curValue.toString());
				MibEntry curMibEntry = getMibEntry((int[]) (int[]) curOID
						.getValue());
				if (curMibEntry != null) {
					if (curMibEntry.isWritable()) {
						logger.debug("Found: " + curMibEntry.toString());
						BeanUtils.setProperty(curMibEntry.getHandlerBean(),
								curMibEntry.getHandlerProperty(),
								curValue.getValue());
					} else {
						responsePDU = createErrorResponse(
								requestPDU.getRequestId(), 4 /* readOnly */,
								curVarBindIndx, requestPDU.getVarBindList());
						return responsePDU;
					}
				} else {
					responsePDU = createErrorResponse(
							requestPDU.getRequestId(), 2, curVarBindIndx,
							requestPDU.getVarBindList());
					return responsePDU;
				}
				curVarBindIndx++;
			}
			responsePDU = new GetResponsePDU(requestPDU.getRequestId(),
					responseVarBindList);
		} catch (Exception e) {
			logger.error("Error invoking bean.Reason: " + e.getMessage());
			responsePDU = createErrorResponse(requestPDU.getRequestId(), 5,
					curVarBindIndx, requestPDU.getVarBindList());
		}
		return responsePDU;
	}

	private GetResponsePDU createErrorResponse(long requestId, int errorStatus,
			int errorIndex, VarBindList varBindList) {
		GetResponsePDU responsePDU = new GetResponsePDU(requestId, errorStatus,
				errorIndex, varBindList != null ? varBindList
						: new VarBindList());
		return responsePDU;
	}

	private boolean validateCommunity(Message requestMsg) {
		AbstractPDU pdu = requestMsg.getPDU();
		boolean validCommunity = false;
		if ((pdu instanceof GetRequestPDU)
				|| (pdu instanceof GetNextRequestPDU)) {
			if (requestMsg.getComunity().equals(getReadOnlyComunity()))
				validCommunity = true;
		} else if ((pdu instanceof SetRequestPDU)
				&& requestMsg.getComunity().equals(getReadWriteComunity()))
			validCommunity = true;
		return validCommunity;
	}

	private boolean hasHandledPrefix(String oid) {
		if (handledOidPrefixes == null)
			return false;
		for (int i = 0; i < handledOidPrefixes.length; i++)
			if (oid.startsWith(handledOidPrefixes[i]))
				return true;
		return false;
	}

	private void addSystemMibEntries() {
		// Fix: Added parameters to avoid compilation warnings
		Map<int[], MibEntry> systemMibEntryMap = new HashMap<int[], MibEntry>();
		if (mib2System != null) {
			MibEntry entry = new MibEntry("1.3.6.1.2.1.1.1.0", mib2System,
					"sysDescr", com.jpragma.snmp.asn.AsnOctetString.class);
			systemMibEntryMap.put(entry.getOidDigits(), entry);
			entry = new MibEntry("1.3.6.1.2.1.1.2.0", mib2System,
					"sysObjectId", com.jpragma.snmp.asn.AsnOID.class);
			systemMibEntryMap.put(entry.getOidDigits(), entry);
			entry = new MibEntry("1.3.6.1.2.1.1.3.0", mib2System, "sysUpTime",
					com.jpragma.snmp.asn.SmiTimeTicks.class);
			systemMibEntryMap.put(entry.getOidDigits(), entry);
			entry = new MibEntry("1.3.6.1.2.1.1.4.0", mib2System, "sysContact",
					com.jpragma.snmp.asn.AsnOctetString.class);
			systemMibEntryMap.put(entry.getOidDigits(), entry);
			entry = new MibEntry("1.3.6.1.2.1.1.5.0", mib2System, "sysName",
					com.jpragma.snmp.asn.AsnOctetString.class);
			systemMibEntryMap.put(entry.getOidDigits(), entry);
			entry = new MibEntry("1.3.6.1.2.1.1.6.0", mib2System,
					"sysLocation", com.jpragma.snmp.asn.AsnOctetString.class);
			systemMibEntryMap.put(entry.getOidDigits(), entry);
			entry = new MibEntry("1.3.6.1.2.1.1.7.0", mib2System,
					"sysServices", com.jpragma.snmp.asn.AsnInteger.class);
			systemMibEntryMap.put(entry.getOidDigits(), entry);
		}
		// Fix: Added parameters and replace comparator to avoid compilation
		// warnings
		if (mibEntries == null) {
			mibEntries = new TreeMap<int[], MibEntry>(new TrackOidComparator());
		}
		mibEntries.putAll(systemMibEntryMap);
	}

	private MibEntry getMibEntry(int oidDigits[]) {
		return (MibEntry) mibEntries.get(oidDigits);
	}

	private int[] nextOID(int oidDigits[]) {
		Iterator<?> keysIterator = mibEntries.keySet().iterator();
		// Fix: Added parameters to avoid compilation warnings
		Comparator<int[]> comparator = (TrackOidComparator) mibEntries
				.comparator();
		while (keysIterator.hasNext()) {
			int curKey[] = (int[]) (int[]) keysIterator.next();
			if (comparator.compare(curKey, oidDigits) > 0)
				return curKey;
		}
		return null;
	}

	public void setMibEntries(Set<MibEntry> mibEntrySet) {
		// Fix: Added parameters to avoid compilation warnings
		Map<int[], MibEntry> mibEntryMap = new HashMap<int[], MibEntry>();
		MibEntry mibEntry;
		for (Iterator<?> iter = mibEntrySet.iterator(); iter.hasNext(); mibEntryMap
				.put(mibEntry.getOidDigits(), mibEntry))
			mibEntry = (MibEntry) iter.next();

		// Fix: Added parameters to avoid compilation warnings
		if (mibEntries == null)
			mibEntries = new TreeMap<int[], MibEntry>(new TrackOidComparator());
		mibEntries.putAll(mibEntryMap);
	}

	public int getListeningPort() {
		return listeningPort;
	}

	public void setListeningPort(int listeningPort) {
		this.listeningPort = listeningPort;
	}

	public String getReadOnlyComunity() {
		return readOnlyComunity;
	}

	public void setReadOnlyComunity(String readOnlyComunity) {
		this.readOnlyComunity = readOnlyComunity;
	}

	public String getReadWriteComunity() {
		return readWriteComunity;
	}

	public void setReadWriteComunity(String readWriteComunity) {
		this.readWriteComunity = readWriteComunity;
	}

	public Mib2System getMib2System() {
		return mib2System;
	}

	public void setMib2System(Mib2System mib2System) {
		this.mib2System = mib2System;
		addSystemMibEntries();
	}

	public String[] getHandledOidPrefixes() {
		return handledOidPrefixes;
	}

	public void setHandledOidPrefixes(String handledOidPrefixes[]) {
		this.handledOidPrefixes = handledOidPrefixes;
	}

	public void start() {
		if (workerThread != null && workerThread.isAlive()) {
			return;
		} else {
			workerThread = new Thread(this, "SNMP Agent");
			workerThread.setDaemon(true);
			workerThread.start();
			return;
		}
	}

	public void stop() {
		if (workerThread != null) {
			workerThread.interrupt();
			if (socket != null)
				socket.close();
		}
	}

}
