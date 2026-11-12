/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.server.net2.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.junit.Test;

public class NetworkConfigurationServiceAdapterTest {

    private static final String NETWORK_CONFIGURATION_SERVICE_PID =
            "org.eclipse.kura.net.admin.NetworkConfigurationService";

    private ComponentConfiguration networkConfiguration;
    private String requestedPid;
    private NetworkConfigurationServiceAdapter adapter;
    private Exception occurredException;

    @Test
    public void missingNetworkConfigurationIsReportedAsServiceNotEnabled() {
        givenNoNetworkConfiguration();

        whenAdapterIsCreated();

        thenNetworkConfigurationIsRequested();
        thenServiceNotEnabledIsReported();
    }

    @Test
    public void networkConfigurationWithoutPropertiesIsReportedAsServiceNotEnabled() {
        givenNetworkConfigurationWithProperties(null);

        whenAdapterIsCreated();

        thenServiceNotEnabledIsReported();
    }

    @Test
    public void configuredInterfaceNamesAreReadFromTheNetworkConfiguration() {
        givenNetworkConfigurationWithInterfaces("lo, eth0,wlan0");

        whenAdapterIsCreated();

        thenNoExceptionOccurred();
        thenConfiguredInterfaceNamesAre("lo", "eth0", "wlan0");
    }

    /*
     * Given
     */

    private void givenNoNetworkConfiguration() {
        this.networkConfiguration = null;
    }

    private void givenNetworkConfigurationWithProperties(Map<String, Object> properties) {
        this.networkConfiguration = new ComponentConfigurationImpl(NETWORK_CONFIGURATION_SERVICE_PID, null, properties);
    }

    private void givenNetworkConfigurationWithInterfaces(String interfaces) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("net.interfaces", interfaces);
        givenNetworkConfigurationWithProperties(properties);
    }

    /*
     * When
     */

    private void whenAdapterIsCreated() {
        try {
            this.adapter = new NetworkConfigurationServiceAdapter(newConfigurationService());
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    /*
     * Then
     */

    private void thenNetworkConfigurationIsRequested() {
        assertEquals(NETWORK_CONFIGURATION_SERVICE_PID, this.requestedPid);
    }

    private void thenServiceNotEnabledIsReported() {
        assertNull(this.adapter);
        assertNotNull(this.occurredException);
        assertEquals(GwtKuraException.class, this.occurredException.getClass());
        assertEquals(GwtKuraErrorCode.SERVICE_NOT_ENABLED, ((GwtKuraException) this.occurredException).getCode());
    }

    private void thenNoExceptionOccurred() {
        assertNull(this.occurredException);
    }

    private void thenConfiguredInterfaceNamesAre(String... expectedNames) {
        List<String> names = this.adapter.getConfiguredNetworkInterfaceNames();
        assertEquals(Arrays.asList(expectedNames), names);
    }

    /*
     * Utilities
     */

    private ConfigurationService newConfigurationService() {
        return (ConfigurationService) Proxy.newProxyInstance(
                ConfigurationService.class.getClassLoader(),
                new Class<?>[] {ConfigurationService.class},
                (proxy, method, args) -> {
                    if ("getComponentConfiguration".equals(method.getName())) {
                        this.requestedPid = (String) args[0];
                        return this.networkConfiguration;
                    }

                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
