/*******************************************************************************
 * Copyright (c) 2020, 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.shared.model;

import com.google.gwt.user.client.rpc.IsSerializable;
import java.io.Serializable;
import java.util.Set;

public class GwtLoginInfo implements IsSerializable, Serializable {

    private static final long serialVersionUID = 4501114031863790538L;

    private String preLoginbannerContent;
    private Set<String> enabledAuthMethods;
    private Integer certAuthPort;

    public GwtLoginInfo() {}

    public GwtLoginInfo(
            final String preLoginbannerContent, final Set<String> enabledAuthMethods, final Integer clientAuthPort) {
        this.preLoginbannerContent = preLoginbannerContent;
        this.enabledAuthMethods = enabledAuthMethods;
        this.certAuthPort = clientAuthPort;
    }

    public String getPreLoginBannerContent() {
        return this.preLoginbannerContent;
    }

    public Set<String> getEnabledAuthMethods() {
        return this.enabledAuthMethods;
    }

    public Integer getCertAuthPort() {
        return this.certAuthPort;
    }
}
