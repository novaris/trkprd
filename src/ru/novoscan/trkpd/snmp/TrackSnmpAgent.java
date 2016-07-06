/*
 * Copyright 2016 Novaris Ltd.
 *
 * This file is part of Novaris Track System (NTS).
 * NTS is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * NCC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NTS. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package ru.novoscan.trkpd.snmp;

import ru.novoscan.trkpd.snmp.spring.TrackSpringSnmpAgent;
//import com.jpragma.snmp.SnmpClient;
import ru.novoscan.trkpd.snmp.spring.TrackAsnInteger;

import java.util.HashSet;
import java.util.Set;

import com.jpragma.snmp.agent.Mib2System;
import com.jpragma.snmp.agent.MibEntry;
import com.jpragma.snmp.asn.AsnOctetString;

/**
 *
 * @author Vladimir Koldakov
 */
public class TrackSnmpAgent {
    TrackSpringSnmpAgent snmpAgent;
    TrackSnmpInfo trackSnmpInfo;
    int snmpPort;

    public TrackSnmpAgent(int snmpPort, String snmpCommunity, TrackSnmpInfo trackSnmpInfo) {
        // Create an instance of TrackSnmpInfo object
        this.trackSnmpInfo = trackSnmpInfo;
        this.snmpPort   = snmpPort;
        // Create the agent
        snmpAgent = new TrackSpringSnmpAgent();
        snmpAgent.setListeningPort(snmpPort);
        // Set communities (SNMP passwords)
        snmpAgent.setReadOnlyComunity(snmpCommunity);
        snmpAgent.setReadWriteComunity("private");
        // We handle mib2-system and enterprises.46962 subtrees
        snmpAgent.setHandledOidPrefixes(new String[]{"1.3.6.1.2.1", "1.3.6.1.4.1.46962"});
        // Create MIB2-SYSTEM object
        Mib2System mib2System = new Mib2System();
        mib2System.setSysObjectId("1.3.6.1.4.1.46962.1");
        mib2System.setSysContact("support@novaris.ru");
        mib2System.setSysDescr("Novaris Monitoring System");
        mib2System.setSysLocation("IM, room 1");
        snmpAgent.setMib2System(mib2System);
        // Create MIB entries for TrackSnmpAgent
        Set<MibEntry> mibEntries = new HashSet<MibEntry>();
        MibEntry entry = null;

        // Application name
        entry = new MibEntry("1.3.6.1.4.1.46962.1.1.0", trackSnmpInfo, "appName", true, AsnOctetString.class);
        mibEntries.add(entry);

        entry = new MibEntry("1.3.6.1.4.1.46962.1.1.1", trackSnmpInfo, "svnVersion", true, TrackAsnInteger.class);
        mibEntries.add(entry);

        entry = new MibEntry("1.3.6.1.4.1.46962.1.1.2", trackSnmpInfo, "buildNum", true, TrackAsnInteger.class);
        mibEntries.add(entry);

        entry = new MibEntry("1.3.6.1.4.1.46962.1.1.3", trackSnmpInfo, "buildDate", true, AsnOctetString.class);
        mibEntries.add(entry);

        entry = new MibEntry("1.3.6.1.4.1.46962.1.1.4", trackSnmpInfo, "buildUser", true, AsnOctetString.class);
        mibEntries.add(entry);

        entry = new MibEntry("1.3.6.1.4.1.46962.1.1.5", trackSnmpInfo, "buildHost", true, AsnOctetString.class);
        mibEntries.add(entry);

        entry = new MibEntry("1.3.6.1.4.1.46962.1.1.6", trackSnmpInfo, "buildOs", true, AsnOctetString.class);
        mibEntries.add(entry);

        // Duration of the application run
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.0", trackSnmpInfo, "timeTics", true, AsnOctetString.class);
        mibEntries.add(entry);

        // Total number of tasks received
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.1", trackSnmpInfo, "totalTasksReceived", TrackAsnInteger.class);
        mibEntries.add(entry);
        // Total calls originated
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.2", trackSnmpInfo, "totalTasksOriginated", TrackAsnInteger.class);
        mibEntries.add(entry);
        // Total calls completed
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.3", trackSnmpInfo, "totalTasksCompleted", TrackAsnInteger.class);
        mibEntries.add(entry);
        // Number of GetTaskNext executors
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.4", trackSnmpInfo, "taskNextExecutors", TrackAsnInteger.class);
        mibEntries.add(entry);
        // Current count of active calls
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.5", trackSnmpInfo, "activeCalls", TrackAsnInteger.class);
        mibEntries.add(entry);
        // Maximal number of active calls
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.6", trackSnmpInfo, "maxActiveCalls", TrackAsnInteger.class);
        mibEntries.add(entry);
        // Current count of Event executors
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.7", trackSnmpInfo, "eventExecutors", TrackAsnInteger.class);
        mibEntries.add(entry);
        // Maximal number of Event executors
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.8", trackSnmpInfo, "maxEventExecutors", TrackAsnInteger.class);
        mibEntries.add(entry);
        // Current count of Bgjob Event executors
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.9", trackSnmpInfo, "bgjobEventExecutors", TrackAsnInteger.class);
        mibEntries.add(entry);
        // Maximal number of Bgjob Event executors
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.10", trackSnmpInfo, "maxBgjobEventExecutors", TrackAsnInteger.class);
        mibEntries.add(entry);
        // Current count of Queue Copleted Tasks executors
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.11", trackSnmpInfo, "queCopletedTasksExecutors", TrackAsnInteger.class);
        mibEntries.add(entry);
        // Number of 'normal' tasks received last hour
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.12", trackSnmpInfo, "taskNextOk", TrackAsnInteger.class);
        mibEntries.add(entry);
        // Number of incomplete tasks received last hour
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.13", trackSnmpInfo, "taskNextIncomplete", TrackAsnInteger.class);
        mibEntries.add(entry);
        // Number of originated calls last hour
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.14", trackSnmpInfo, "originateCall", TrackAsnInteger.class);
        mibEntries.add(entry);
        // Number of completed calls last hour
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.15", trackSnmpInfo, "completeCall", TrackAsnInteger.class);
        mibEntries.add(entry);
        // Number of sql exceptions last hour
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.16", trackSnmpInfo, "sqlException", TrackAsnInteger.class);
        mibEntries.add(entry);
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.17", trackSnmpInfo, "averageNextTaskRequestDuration", TrackAsnInteger.class);
        mibEntries.add(entry);
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.18", trackSnmpInfo, "minNextTaskRequestDuration", TrackAsnInteger.class);
        mibEntries.add(entry);
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.19", trackSnmpInfo, "maxNextTaskRequestDuration", TrackAsnInteger.class);
        mibEntries.add(entry);
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.20", trackSnmpInfo, "averageOriginateCallDuration", TrackAsnInteger.class);
        mibEntries.add(entry);
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.21", trackSnmpInfo, "minOriginateCallDuration", TrackAsnInteger.class);
        mibEntries.add(entry);
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.22", trackSnmpInfo, "maxOriginateCallDuration", TrackAsnInteger.class);
        mibEntries.add(entry);
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.23", trackSnmpInfo, "countQueCompletedCalls", TrackAsnInteger.class);
        mibEntries.add(entry);
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.24", trackSnmpInfo, "maxQueCompletedCallsDuration", TrackAsnInteger.class);
        mibEntries.add(entry);
        entry = new MibEntry("1.3.6.1.4.1.46962.1.2.25", trackSnmpInfo, "averageQueCompletedCallsDuration", TrackAsnInteger.class);
        mibEntries.add(entry);


        // Application configuration
        entry = new MibEntry("1.3.6.1.4.1.46962.1.3.0", trackSnmpInfo, "eventsFilter", AsnOctetString.class);
        mibEntries.add(entry);
        entry = new MibEntry("1.3.6.1.4.1.46962.1.3.1", trackSnmpInfo, "readTaskParallel", TrackAsnInteger.class);
        mibEntries.add(entry);
        entry = new MibEntry("1.3.6.1.4.1.46962.1.3.2", trackSnmpInfo, "limitActiveCalls", TrackAsnInteger.class);
        mibEntries.add(entry);
        entry = new MibEntry("1.3.6.1.4.1.46962.1.3.3", trackSnmpInfo, "waitEmptyQueue", TrackAsnInteger.class);
        mibEntries.add(entry);
        entry = new MibEntry("1.3.6.1.4.1.46962.1.3.4", trackSnmpInfo, "fsConfInterval", TrackAsnInteger.class);
        mibEntries.add(entry);
        entry = new MibEntry("1.3.6.1.4.1.46962.1.3.5", trackSnmpInfo, "soundsInterval", TrackAsnInteger.class);
        mibEntries.add(entry);
        entry = new MibEntry("1.3.6.1.4.1.46962.1.3.6", trackSnmpInfo, "minPoolSize", TrackAsnInteger.class);
        mibEntries.add(entry);
        entry = new MibEntry("1.3.6.1.4.1.46962.1.3.7", trackSnmpInfo, "maxPoolSize", TrackAsnInteger.class);
        mibEntries.add(entry);

//        entry = new MibEntry("1.3.6.1.4.1.46962.1.4.0", nccSnmpInfo, "executorTaskReceived", NccAsnSequence.class);
//        mibEntries.add(entry);


        // PoolDataSource values
        entry = new MibEntry("1.3.6.1.4.1.46962.1.5.0", trackSnmpInfo, "pdsInitialPoolSize", TrackAsnInteger.class);
        mibEntries.add(entry);
        entry = new MibEntry("1.3.6.1.4.1.46962.1.5.2", trackSnmpInfo, "pdsMaxPoolSize", TrackAsnInteger.class);
        mibEntries.add(entry);

        // System: number of processors
        entry = new MibEntry("1.3.6.1.4.1.46962.1.9.0", trackSnmpInfo, "availableProcessors", TrackAsnInteger.class);
        mibEntries.add(entry);
        // System: free memory
        entry = new MibEntry("1.3.6.1.4.1.46962.1.9.1", trackSnmpInfo, "freeMemory", TrackAsnInteger.class);
        mibEntries.add(entry);
        // System: max memory
        entry = new MibEntry("1.3.6.1.4.1.46962.1.9.2", trackSnmpInfo, "maxMemory", TrackAsnInteger.class);
        mibEntries.add(entry);
        // System: total memory
        entry = new MibEntry("1.3.6.1.4.1.46962.1.9.3", trackSnmpInfo, "totalMemory", TrackAsnInteger.class);
        mibEntries.add(entry);
        // System: number of threads
        entry = new MibEntry("1.3.6.1.4.1.46962.1.9.4", trackSnmpInfo, "numberOfThreads", TrackAsnInteger.class);
        mibEntries.add(entry);

        snmpAgent.setMibEntries(mibEntries);

        // Start the agent
        snmpAgent.start();
    }

    public void stop() {
        snmpAgent.stop();
    }

    public boolean isSocketCreated() {
        return snmpAgent.isSocketCreated();
    }

    public boolean isSocketException() {
        return snmpAgent.isSocketException();
    }
}
