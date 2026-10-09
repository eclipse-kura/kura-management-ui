/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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

import java.io.Serializable;

/**
 * The access technologies a modem can be configured to use. The declaration order defines the mode ranking, from the
 * oldest to the newest technology, and it is used to compute the preferred mode out of the allowed ones.
 */
public enum GwtModemMode implements Serializable {

    NONE("netModemModeNone"),
    CS("netModemModeCs"),
    MODE_2G("netModemMode2G"),
    MODE_3G("netModemMode3G"),
    MODE_4G("netModemMode4G"),
    MODE_5G("netModemMode5G"),
    ANY("netModemModeAny");

    private final String messageKey;

    private GwtModemMode(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return this.messageKey;
    }
}
