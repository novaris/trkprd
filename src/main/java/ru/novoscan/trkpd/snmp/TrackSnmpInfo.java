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

import ru.novoscan.trkpd.snmp.spring.TrackAsnSequence;
import ru.novoscan.trkpd.TrackVersion;
import ru.novoscan.trkpd.utils.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.net.InetAddress;
import java.net.UnknownHostException;

import  org.postgresql.ds.PGPoolingDataSource;

import com.jpragma.snmp.types.VarBind;

/**
 *
 * @author Vladimir Koldakov
 */
public class TrackSnmpInfo {
    private String appName;
    private ModConfig config;

    PGPoolingDataSource pds;

    // Application start time
    private long start_time;

    // Total numbers
    private TreeMap<Integer,Long> executorTasksReceived;
    private long totalTasksReceived;
    private long totalTasksOriginated;
    private long totalTasksCompleted;
    // Current active executors
    private int taskNextExecutors;
    private int eventExecutors;
    private int bgjobEventExecutors;
    private int queCopletedTasksExecutors;
    // Current value
    private int activeCalls;
    // Maximal values
    private int maxActiveCalls;
    private int maxEventExecutors;
    private int maxBgjobEventExecutors;
    private long maxNextTaskRequestDuration;
    private long minNextTaskRequestDuration;
    private long maxOriginateCallDuration;
    private long minOriginateCallDuration;
    private long maxQueCompletedCallsDuration;
    private long countQueCompletedCalls;
    private long sumQueCompletedCallsDuration;

    private static class HourInfo {
        public HourInfo() {
            nextCallOk         = 0;
            nextCallIncomplete = 0;
            originateCall      = 0;
            completeCall       = 0;
            sqlException       = 0;
        }

        public long nextCallOk;
        public long nextCallIncomplete;
        public long originateCall;
        public long completeCall;
        public long sqlException;
        public long countNextTaskRequestDuration;
        public long sumNextTaskRequestDuration;
        public long countOriginateCallDuration;
        public long sumOriginateCallDuration;
    }

    private final HourInfo[] hoursInfo = new HourInfo[60];

    private int hourIndex = 0;

    private Timer timer = new Timer(true);

    public TrackSnmpInfo( String appName, ModConfig config ) {
        this.appName = appName;
        this.config  = config;

        totalTasksReceived        = 0;
        totalTasksOriginated      = 0;
        totalTasksCompleted       = 0;
        taskNextExecutors         = 0;
        activeCalls               = 0;
        maxActiveCalls            = 0;
        queCopletedTasksExecutors = 0;
        eventExecutors            = 0;
        maxEventExecutors         = 0;
        bgjobEventExecutors       = 0;
        maxBgjobEventExecutors    = 0;
        maxNextTaskRequestDuration = 0;
        minNextTaskRequestDuration = Long.MAX_VALUE;
        maxOriginateCallDuration   = 0;
        minOriginateCallDuration   = Long.MAX_VALUE;
        executorTasksReceived      = new TreeMap<Integer,Long>();
        countQueCompletedCalls = 0;
        sumQueCompletedCallsDuration   = 0;
        maxQueCompletedCallsDuration   = 0;

        // switch hourIndex every an hour and resets statistic for that hour
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                synchronized (TrackSnmpInfo.this) {
                    ++hourIndex;
                    if (hourIndex >= hoursInfo.length) {
                        hourIndex = 0;
                    }
                    hoursInfo[hourIndex] = null;
                }
            }
        }, 1000 * 60 * 1, 1000 * 60 * 1);
    }

    private HourInfo getHourInfo() {
        HourInfo hourInfo = hoursInfo[hourIndex];
        if (hourInfo == null) {
            hoursInfo[hourIndex] = hourInfo = new HourInfo();
        }
        return hourInfo;
    }

    /**
     * 
     * @return Application name
     */
    public String getAppName() {
      return appName;
    }

    public String getSvnVersion() {
        return TrackVersion.getSvnVersion();
    }

    public String getBuildDate() {
        return TrackVersion.getBuildDate();
    }


    public void setStartTime(long time_in_millis) {
        start_time = time_in_millis;
    }

    // Duration of the application run
    public String getTimeTics() {
        long duration = System.currentTimeMillis() - start_time;
        long days     = duration / ( 24 * 60 * 60 * 1000);
        duration -= days * 24 * 60 * 60 * 1000;
        long hours    = duration / ( 60 * 60 * 1000);
        duration -= hours * 60 * 60 * 1000;
        long minutes  = duration / ( 60 * 1000);
        duration -= minutes * 60 * 1000;
        long seconds  = duration / 1000;
        long mseconds = duration % 1000;
        StringBuilder sb = new StringBuilder();
        if(days>0) {
            sb.append(days);
            sb.append(" days ");
        }
        if(hours>0) {
            sb.append(hours);
            sb.append(" hours ");
        }
        if(minutes>0) {
            sb.append(minutes);
            sb.append(" minutes ");
        }
        if(seconds>0 || mseconds>0) {
            sb.append(seconds);
            if(mseconds>0) {
                sb.append(".");
                if(mseconds<10) {
                    sb.append("00");
                } else if(mseconds<100) {
                    sb.append("0");
                }
                sb.append(mseconds);
            }
            sb.append(" seconds ");
        }
        return sb.toString().trim();
    }

    public synchronized void setTaskNextExecutors(boolean increase) {
        if(increase) {
            taskNextExecutors++;
        } else {
            taskNextExecutors--;
        }
    }

    public synchronized void setTaskNext(boolean ok, int executor) {
        totalTasksReceived++;
        Long l = executorTasksReceived.get(executor);
        executorTasksReceived.put(executor,(l==null) ? 1L : l+1);
        HourInfo hourInfo = getHourInfo();
        if( ok ) {
            hourInfo.nextCallOk++;
        } else {
            hourInfo.nextCallIncomplete++;
        }
    }
/*
    public synchronized void setTaskNextOk(int executor) {
        HourInfo hourInfo = getHourInfo();
        hourInfo.nextCallOk++;
    }

    public synchronized void setTaskNextIncomplete(int executor) {
        HourInfo hourInfo = getHourInfo();
        hourInfo.nextCallIncomplete++;
    }
*/
    public synchronized void setActiveCalls(boolean increase) {
        HourInfo hourInfo = getHourInfo();
        if(increase) {
            totalTasksOriginated++;
            if( ++activeCalls > maxActiveCalls ) {
                maxActiveCalls = activeCalls;
            }
            hourInfo.originateCall++;
        } else {
            totalTasksCompleted++;
            activeCalls--;
            hourInfo.completeCall++;
        }
    }

    public synchronized void setSqlException(int executor) {
        HourInfo hourInfo = getHourInfo();
        hourInfo.sqlException++;
    }

    public synchronized void setEventExecutors(boolean increase) {
        if(increase) {
            if( ++eventExecutors > maxEventExecutors ) {
                maxEventExecutors = eventExecutors;
            }
        } else {
            eventExecutors--;
        }
    }

    public synchronized void setBgjobEventExecutors(boolean increase) {
        if(increase) {
            if( ++bgjobEventExecutors > maxBgjobEventExecutors ) {
                maxBgjobEventExecutors = bgjobEventExecutors;
            }
        } else {
            bgjobEventExecutors--;
        }
    }

    public synchronized void setQueCopletedTasksExecutors(boolean increase) {
        if(increase) {
            queCopletedTasksExecutors++;
        } else {
            queCopletedTasksExecutors--;
        }
    }

    public synchronized void setNextTaskRequestDuration(long duration) {
        HourInfo hourInfo = getHourInfo();
        hourInfo.countNextTaskRequestDuration++;
        hourInfo.sumNextTaskRequestDuration += duration;
        if(duration>maxNextTaskRequestDuration) {
            maxNextTaskRequestDuration = duration;
        }
        if(duration<minNextTaskRequestDuration) {
            minNextTaskRequestDuration = duration;
        }
    }

    public synchronized void setQueCompletedCallsDuration(long duration) {
        countQueCompletedCalls++;
        sumQueCompletedCallsDuration += duration;
        if(duration>maxQueCompletedCallsDuration) {
            maxQueCompletedCallsDuration = duration;
        }
    }

    public synchronized void setOriginateCallDuration(long duration) {
        HourInfo hourInfo = getHourInfo();
        hourInfo.countOriginateCallDuration++;
        hourInfo.sumOriginateCallDuration += duration;
        if(duration>maxOriginateCallDuration) {
            maxOriginateCallDuration = duration;
        }
        if(duration<minOriginateCallDuration) {
            minOriginateCallDuration = duration;
        }
    }

    // Configuration options
    public String getConfigFile() {
        return config.getConfigFile();
    }

    public InetAddress getHost() throws UnknownHostException {
        return config.getHost();
    }

    public int getPort() {
        return config.getPort();
    }

    public String getModName() {
        return config.getModName();
    }

    public int getModType() {
        return config.getModType();
    }

    public long getReadInterval() {
        return config.getReadInterval();
    }

    public int getMinPoolSize() {
        return config.getPgInitConn();
    }

    public long getMaxPoolSize() {
        return config.getPgMaxConn();
    }

    /**
     * 
     * @return Total number of tasks received
     */
    public synchronized long getTotalTasksReceived() {
        return totalTasksReceived;
    }

    /**
     * 
     * @return Total calls originated
     */
    public synchronized long getTotalTasksOriginated() {
        return totalTasksOriginated;
    }

    /**
     * 
     * @return Total calls completed
     */
    public synchronized long getTotalTasksCompleted() {
        return totalTasksCompleted;
    }

    /**
     * 
     * @return Number of GetTaskNext executors
     */
    public synchronized long getTaskNextExecutors() {
        return taskNextExecutors;
    }

    /**
     * 
     * @return Current count of active calls
     */
    public synchronized long getActiveCalls() {
        return activeCalls;
    }

    /**
     * 
     * @return Maximal number of active calls
     */
    public synchronized long getMaxActiveCalls() {
        return maxActiveCalls;
    }

    /**
     * 
     * @return Current count of Event executors
     */
    public synchronized long getEventExecutors() {
        return eventExecutors;
    }

    /**
     * 
     * @return Maximal number of Event executors
     */
    public synchronized long getMaxEventExecutors() {
        return maxEventExecutors;
    }

    /**
     * 
     * @return Current count of Bgjob Event executors
     */
    public synchronized long getBgjobEventExecutors() {
        return bgjobEventExecutors;
    }

    /**
     * 
     * @return Maximal number of Bgjob Event executors
     */
    public synchronized long getMaxBgjobEventExecutors() {
        return maxBgjobEventExecutors;
    }

    /**
     * 
     * @return Current count of Queue Copleted Tasks executors
     */
    public synchronized long getQueCopletedTasksExecutors() {
        return queCopletedTasksExecutors;
    }

    /**
     * 
     * @return Number of 'normal' tasks received last hour
     */
    public synchronized long getTaskNextOk() {
        long nextCallOk = 0;
        for (int i = 0; i < hoursInfo.length; ++i) {
            HourInfo hourInfo = hoursInfo[i];
            if (hourInfo == null) {
                continue;
            }
            nextCallOk += hourInfo.nextCallOk;
        }
        return nextCallOk;
    }

    /**
     * 
     * @return Number of incomplete tasks received last hour
     */
    public synchronized long getTaskNextIncomplete() {
        long nextCallIncomplete = 0;
        for (int i = 0; i < hoursInfo.length; ++i) {
            HourInfo hourInfo = hoursInfo[i];
            if (hourInfo == null) {
                continue;
            }
            nextCallIncomplete += hourInfo.nextCallIncomplete;
        }
        return nextCallIncomplete;
    }

    /**
     * 
     * @return Number of originated calls last hour
     */
    public synchronized long getOriginateCall() {
        long originateCall = 0;
        for (int i = 0; i < hoursInfo.length; ++i) {
            HourInfo hourInfo = hoursInfo[i];
            if (hourInfo == null) {
                continue;
            }
            originateCall += hourInfo.originateCall;
        }
        return originateCall;
    }

    /**
     * 
     * @return Number of completed calls last hour
     */
    public synchronized long getCompleteCall() {
        long completeCall = 0;
        for (int i = 0; i < hoursInfo.length; ++i) {
            HourInfo hourInfo = hoursInfo[i];
            if (hourInfo == null) {
                continue;
            }
            completeCall += hourInfo.completeCall;
        }
        return completeCall;
    }

    /**
     * 
     * @return Number of sql exceptions last hour
     */
    public synchronized long getSqlException() {
        long sqlException = 0;
        for (int i = 0; i < hoursInfo.length; ++i) {
            HourInfo hourInfo = hoursInfo[i];
            if (hourInfo == null) {
                continue;
            }
            sqlException += hourInfo.sqlException;
        }
        return sqlException;
    }

    public synchronized long getMaxNextTaskRequestDuration() {
        return maxNextTaskRequestDuration;
    }

    public synchronized long getMinNextTaskRequestDuration() {
        return (minNextTaskRequestDuration == Long.MAX_VALUE)
                ? 0
                : minNextTaskRequestDuration;
    }

    public synchronized long getAverageNextTaskRequestDuration() {
        long duration = 0;
        long count = 0;
        for (int i = 0; i < hoursInfo.length; ++i) {
            HourInfo hourInfo = hoursInfo[i];
            if (hourInfo == null) {
                continue;
            }
            duration += hourInfo.sumNextTaskRequestDuration;
            count    += hourInfo.countNextTaskRequestDuration;
        }
        return (count==0) ? 0 : (duration/count);
    }

    public synchronized long getMaxOriginateCallDuration() {
        return maxOriginateCallDuration;
    }

    public synchronized long getMinOriginateCallDuration() {
        return (minOriginateCallDuration == Long.MAX_VALUE)
                ? 0
                : minOriginateCallDuration;
    }

    public synchronized long getAverageOriginateCallDuration() {
        long duration = 0;
        long count = 0;
        for (int i = 0; i < hoursInfo.length; ++i) {
            HourInfo hourInfo = hoursInfo[i];
            if (hourInfo == null) {
                continue;
            }
            duration += hourInfo.sumOriginateCallDuration;
            count    += hourInfo.countOriginateCallDuration;
        }
        return (count==0) ? 0 : (duration/count);
    }

    public synchronized long getCountQueCompletedCalls() {
        return countQueCompletedCalls;
    }

    public synchronized long getMaxQueCompletedCallsDuration() {
        return maxQueCompletedCallsDuration;
    }

    public synchronized long getAverageQueCompletedCallsDuration() {
        return (countQueCompletedCalls==0)
                ? 0
                : (sumQueCompletedCallsDuration/countQueCompletedCalls);
    }

    /**
     * 
     * @return Number of completed calls last hour
     */
    public synchronized List<VarBind> getExecutorTaskReceived() {
        int from = 1;
        List<Long> list = new ArrayList<Long>();

        for(int i=1;i<=executorTasksReceived.size();i++) {
            Long l = executorTasksReceived.get(i);
            list.add( (l==null) ? 0 : l );
        }
        if(list.size()==0) {
            list.add(0L);
            from = 0;
        }
        return TrackAsnSequence.toVarBindList("1.3.6.1.4.1.46962.1.4.0", from, list);
    }

    // PoolDataSource values
    public void setPds(PGPoolingDataSource pds) {
        this.pds = pds;
    }

    public long getPdsInitialPoolSize() {
        try {
            return pds.getInitialConnections();
        } catch(NullPointerException e) {}
        return 0L;
    }


    public long getPdsMaxPoolSize() {
        try {
            return pds.getMaxConnections();
        } catch(NullPointerException e) {}
        return 0L;
    }


    /**
     * 
     * @return System: number of processors
     */
    public int getAvailableProcessors() {
      return Runtime.getRuntime().availableProcessors();
    }

    /**
     * 
     * @return System: free memory
     */
    public long getFreeMemory() {
      return Runtime.getRuntime().freeMemory();
    }
 
    /**
     * 
     * @return System: max memory
     */
    public long getMaxMemory() {
      return Runtime.getRuntime().maxMemory();
    }

    /**
     * 
     * @return System: total memory
     */
    public long getTotalMemory() {
      return Runtime.getRuntime().totalMemory();
    }

    /**
     * 
     * @return System: number of threads
     */
    public int getNumberOfThreads() {
      return Thread.activeCount();
    }
}
