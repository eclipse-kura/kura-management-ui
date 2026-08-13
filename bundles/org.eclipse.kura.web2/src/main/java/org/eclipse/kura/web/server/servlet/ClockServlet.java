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
package org.eclipse.kura.web.server.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneId;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Read-only time source for the Status page's live clock. No XSRF token is
 * required by design: the endpoint has no side effects and is polled at
 * 0.5 Hz; session authentication is enforced by the whiteboard context this
 * servlet is registered under, not by this class.
 */
public class ClockServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long epoch = System.currentTimeMillis();
        ZoneId zone = ZoneId.systemDefault();
        int offsetMillis = zone.getRules().getOffset(Instant.ofEpochMilli(epoch)).getTotalSeconds() * 1000;

        JsonObject json = new JsonObject();
        json.addProperty("epoch", epoch);
        json.addProperty("offsetMillis", offsetMillis);
        json.addProperty("zoneId", zone.getId());

        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");
        resp.setHeader("Cache-Control", "no-store");

        try (PrintWriter writer = resp.getWriter()) {
            new Gson().toJson(json, writer);
        }
    }
}
