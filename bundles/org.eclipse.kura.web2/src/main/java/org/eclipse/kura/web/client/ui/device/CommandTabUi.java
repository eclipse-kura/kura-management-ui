/*******************************************************************************
 * Copyright (c) 2011, 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.ui.device;

import java.util.Optional;

import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtDeviceService;
import org.eclipse.kura.web.shared.service.GwtDeviceServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.TextBox;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.Widget;

public class CommandTabUi extends Composite {

    private static CommandTabUiUiBinder uiBinder = GWT.create(CommandTabUiUiBinder.class);

    private static final Messages MSGS = GWT.create(Messages.class);
    private static final String SERVLET_URL = Console.ADMIN_ROOT + '/' + GWT.getModuleName() + "/file/command";

    @SuppressWarnings("unused")
    private GwtSession session;

    interface CommandTabUiUiBinder extends UiBinder<Widget, CommandTabUi> {
    }

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtDeviceServiceAsync gwtDeviceService = GWT.create(GwtDeviceService.class);

    @UiField
    TextBox formExecute;
    @UiField
    Input formPassword;
    @UiField
    Button reset;
    @UiField
    Button execute;
    @UiField
    FormPanel commandForm;
    @UiField
    FileUpload docPath;
    @UiField
    PanelBody resultPanel;
    @UiField
    Hidden xsrfTokenField;

    String command;
    String password;
    SafeHtmlBuilder safeHtml = new SafeHtmlBuilder();

    public CommandTabUi() {
        initWidget(uiBinder.createAndBindUi(this));

        this.formExecute.clear();
        this.formExecute.setFocus(true);
        this.formExecute.setName("command");
        this.formExecute.addKeyDownHandler(event -> {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                CommandTabUi.this.formPassword.setFocus(true);
            }
        });

        this.xsrfTokenField.setID("xsrfToken");
        this.xsrfTokenField.setName("xsrfToken");
        this.xsrfTokenField.setValue("");

        this.formPassword.setText(null);
        this.formPassword.setName("password");
        this.formPassword.addKeyDownHandler(event -> {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                CommandTabUi.this.execute.setFocus(true);
            }
        });

        this.docPath.setName("file");

        display(MSGS.deviceCommandNoOutput());

        this.reset.setText(MSGS.reset());
        this.reset.addClickHandler(event -> {
            this.commandForm.reset();
            this.formExecute.setFocus(true);
            display(MSGS.deviceCommandNoOutput());
        });

        this.execute.setText(MSGS.deviceCommandExecute());

        this.execute.addClickHandler(
                event -> RequestQueue.submit(c -> this.gwtXSRFService.generateSecurityToken(c.callback(token -> {
                    this.xsrfTokenField.setValue(token.getToken());
                    this.commandForm.submit();
                    this.formExecute.setFocus(true);
                }))));

        this.commandForm.setEncoding(FormPanel.ENCODING_MULTIPART);
        this.commandForm.setMethod(FormPanel.METHOD_POST);
        this.commandForm.setAction(SERVLET_URL);

        this.commandForm.addSubmitCompleteHandler(event -> {
            final String result = event.getResults();

            if (result == null || result.contains("HTTP ERROR")) {
                display(MSGS.fileUploadFailure());
            } else {
                display(result);
            }
        });

        this.docPath.getElement().setAttribute("accept", ".sh,.zip");

    }

    public void display(String string) {
        this.resultPanel.clear();
        this.resultPanel.add(new HTML(new SafeHtmlBuilder().appendEscapedLines(string).toSafeHtml()));
    }

    public void setSession(GwtSession currentSession) {
        this.session = currentSession;
    }

}
