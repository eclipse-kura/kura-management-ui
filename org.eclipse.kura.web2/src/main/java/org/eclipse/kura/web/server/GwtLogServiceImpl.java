/*******************************************************************************
 * Copyright (c) 2021, 2026 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.server;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.log.LogEntry;
import org.eclipse.kura.log.LogProvider;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtLogEntry;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtLogService;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtLogServiceImpl extends OsgiRemoteServiceServlet implements GwtLogService {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(GwtLogServiceImpl.class);

    private static final LogEntriesCache LOG_CACHE = new LogEntriesCache();
    private static final List<String> REGISTERED_LOG_PROVIDERS = new LinkedList<>();

    @Override
    public List<String> initLogProviders(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        loadLogProviders();

        return REGISTERED_LOG_PROVIDERS;
    }

    @Override
    public List<GwtLogEntry> readLogs(int fromId) throws GwtKuraException {
        return LOG_CACHE.getLogs(fromId);
    }

    private void loadLogProviders() {
        try {
            List<String> availableLogProviders = new ArrayList<>();

            final String matchEverything = "(objectClass=*)";
            List<ServiceReference<LogProvider>> logProviderRefs = (List<ServiceReference<LogProvider>>) ServiceLocator
                    .getInstance().getServiceReferences(LogProvider.class, matchEverything);

            for (ServiceReference<LogProvider> logProviderRef : logProviderRefs) {
                String pid = (String) logProviderRef.getProperty(ConfigurationService.KURA_SERVICE_PID);
                LogProvider service = FrameworkUtil.getBundle(LogProvider.class).getBundleContext()
                        .getService(logProviderRef);

                availableLogProviders.add(pid);

                if (pid != null && service != null && !REGISTERED_LOG_PROVIDERS.contains(pid)) {

                    service.registerLogListener((LogEntry entry) -> {
                        GwtLogEntry gwtEntry = new GwtLogEntry();
                        gwtEntry.setProperties(entry.getProperties());
                        gwtEntry.setSourceLogProviderPid(pid);
                        gwtEntry.setTimestamp(getFormattedTimestamp(entry));

                        GwtLogServiceImpl.LOG_CACHE.add(gwtEntry);
                    });

                    REGISTERED_LOG_PROVIDERS.add(pid);
                    logger.info("LogProvider {} loaded.", pid);
                }
            }

            for (String pid : REGISTERED_LOG_PROVIDERS) {
                if (!availableLogProviders.contains(pid)) {
                    REGISTERED_LOG_PROVIDERS.remove(pid);
                    logger.info("LogProvider {} no more available.", pid);
                }
            }

            Optional<String> defaultLogManager = getDefaultLogManager();
            if (defaultLogManager.isPresent() && !REGISTERED_LOG_PROVIDERS.isEmpty()
                    && !REGISTERED_LOG_PROVIDERS.get(0).equals(defaultLogManager.get())) {
                String logManager = defaultLogManager.get();
                if (REGISTERED_LOG_PROVIDERS.contains(logManager)) {
                    REGISTERED_LOG_PROVIDERS.remove(logManager);
                    REGISTERED_LOG_PROVIDERS.add(0, logManager);
                }
            }
        } catch (GwtKuraException e) {
            logger.error("Error loading log providers.");
        }
    }

    private String getFormattedTimestamp(LogEntry entry) {
        String time = "UNDEFINED";
        try {
            return Instant.ofEpochSecond(entry.getTimestamp()).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    .toString();
        } catch (Exception ex) {
            return time;
        }
    }

    private Optional<String> getDefaultLogManager() {
        Optional<String> defaultLogManager = Optional.empty();
        try {
            SystemService systemService = ServiceLocator.getInstance().getService(SystemService.class);
            if (systemService != null) {
                defaultLogManager = systemService.getDefaultLogManager();
            }
        } catch (GwtKuraException e) {
            logger.error("Error retrieving default LogManager name", e);
        }
        return defaultLogManager;
    }

    private static final class LogEntriesCache {

        private static final LinkedList<GwtLogEntry> ENTRIES = new LinkedList<>();
        private static final int MAX_CACHE_SIZE = 1000;
        private static int nextEntryId = 0;

        public void add(GwtLogEntry newEntry) {
            synchronized (ENTRIES) {
                if (ENTRIES.size() >= MAX_CACHE_SIZE) {
                    ENTRIES.removeFirst();
                }
                manageIdIntOverflow();
                newEntry.setId(nextEntryId++);
                ENTRIES.add(newEntry);
            }
        }

        public List<GwtLogEntry> getLogs(int fromId) {
            List<GwtLogEntry> result = new LinkedList<>();
            synchronized (ENTRIES) {
                ENTRIES.forEach(entry -> {
                    if (entry.getId() > fromId) {
                        result.add(entry);
                    }
                });
            }
            return result;
        }

        /*
         * Very unlikely to happen, but if it will then entries are reindexed
         */
        private static void manageIdIntOverflow() {
            if (nextEntryId >= Integer.MAX_VALUE) {
                logger.info("ID overflow for cached UI log entries. Reindexing.");

                for (int i = 0; i < ENTRIES.size(); i++) {
                    ENTRIES.get(i).setId(i);
                }
                nextEntryId = ENTRIES.size();
            }
        }
    }
}
