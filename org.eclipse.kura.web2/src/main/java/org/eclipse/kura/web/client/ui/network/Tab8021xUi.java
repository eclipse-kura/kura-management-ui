/*******************************************************************************
 * Copyright (c) 2023, 2025 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *  Sterwen-Technology
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.network;

import java.util.Optional;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.util.HelpButton;
import org.eclipse.kura.web.client.util.request.RequestQueue;
import org.eclipse.kura.web.shared.model.Gwt8021xConfig;
import org.eclipse.kura.web.shared.model.Gwt8021xEap;
import org.eclipse.kura.web.shared.model.Gwt8021xInnerAuth;
import org.eclipse.kura.web.shared.model.GwtKeystoreEntry;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtCertificatesService;
import org.eclipse.kura.web.shared.service.GwtCertificatesServiceAsync;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Anchor;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.DropDown;
import org.gwtbootstrap3.client.ui.DropDownHeader;
import org.gwtbootstrap3.client.ui.DropDownMenu;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.base.TextBoxBase;
import org.gwtbootstrap3.client.ui.constants.Toggle;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

public class Tab8021xUi extends Composite implements NetworkTab {

    protected static final Logger logger = Logger.getLogger(Tab8021xUi.class.getSimpleName());

    private static final String KURA_SERVICE_PID_OSGI_FILTER = "(objectClass=org.eclipse.kura.security.keystore.KeystoreService)";

    private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtCertificatesServiceAsync gwtCertificatesService = GWT.create(GwtCertificatesService.class);

    private static Tab8021xUiUiBinder uiBinder = GWT.create(Tab8021xUiUiBinder.class);

    private static final Messages MSGS = GWT.create(Messages.class);

    interface Tab8021xUiUiBinder extends UiBinder<Widget, Tab8021xUi> {
    }

    private final NetworkTabsUi netTabs;

    Gwt8021xConfig activeConfig;
    GwtSession currentSession;

    private final TabIp4Ui tcp4Tab;
    private final TabIp6Ui tcp6Tab;

    private boolean dirty;

    // Labels
    @UiField
    FormLabel labelEap;

    @UiField
    FormLabel labelInnerAuth;

    @UiField
    FormLabel labelUsername;

    @UiField
    FormLabel labelPassword;

    @UiField
    FormLabel labelKeystorePid;

    @UiField
    FormLabel labelCaCertName;

    @UiField
    FormLabel labelPublicPrivateKeyPairName;

    @UiField
    Form form;

    // Fields
    @UiField
    Button buttonTestPassword;

    @UiField
    ListBox eap;

    @UiField
    ListBox innerAuth;

    @UiField
    TextBox username;

    @UiField
    Input password;

    @UiField
    TextBox keystorePid;

    @UiField
    DropDown keystorePidDropdown;

    @UiField
    Anchor keystorePidAnchor;

    @UiField
    ListBox caCertName;

    @UiField
    ListBox publicPrivateKeyPairName;

    // Help
    @UiField
    HelpButton helpEap;

    @UiField
    HelpButton helpInnerAuth;

    @UiField
    HelpButton helpUsername;

    @UiField
    HelpButton helpPassword;

    @UiField
    HelpButton helpKeystorePid;

    @UiField
    HelpButton helpCaCertName;

    @UiField
    HelpButton helpPublicPrivateKeyPairName;

    @UiField
    PanelHeader helpTitle;

    @UiField
    ScrollPanel helpText;

    @UiField
    FormGroup formgroupIdentityUsername;

    @UiField
    FormGroup formgroupPassword;

    @UiField
    FormGroup identityKeystorePid;

    @UiField
    FormGroup identityCaCertName;

    @UiField
    FormGroup identityPublicPrivateKeyPairName;

    public Tab8021xUi(GwtSession currentSession, NetworkTabsUi tabs, TabIp4Ui tcp4, TabIp6Ui tcp6) {
        initWidget(uiBinder.createAndBindUi(this));

        this.currentSession = currentSession;
        this.netTabs = tabs;
        this.tcp4Tab = tcp4;
        this.tcp6Tab = tcp6;
        this.helpTitle.setText(MSGS.netHelpTitle());

        initLabels();
        initHelpButtons();
        initTextBoxes();
        initListBoxes();
        initDropdownBoxes();

        this.buttonTestPassword.setVisible(false);

        this.tcp4Tab.status.addChangeHandler(event -> update());

        this.tcp6Tab.status.addChangeHandler(event -> update());
    }

    private void initLabels() {
        this.labelEap.setText(MSGS.net8021xEap());
        this.labelInnerAuth.setText(MSGS.net8021xInnerAuth());
        this.labelUsername.setText(MSGS.net8021xUsername());
        this.labelPassword.setText(MSGS.net8021xPassword());
        this.labelKeystorePid.setText(MSGS.net8021xKeystorePid());
        this.labelCaCertName.setText(MSGS.net8021xCaCert());
        this.labelPublicPrivateKeyPairName.setText(MSGS.net8021xPublicPrivateKeyPair());
    }

    private void initHelpButtons() {
        this.helpEap.setHelpText(MSGS.net8021xEapHelp());
        this.helpInnerAuth.setHelpText(MSGS.net8021xInnerAuthHelp());
        this.helpUsername.setHelpText(MSGS.net8021xUsernameHelp());
        this.helpPassword.setHelpText(MSGS.net8021xPasswordHelp());
        this.helpKeystorePid.setHelpText(MSGS.net8021xKeystorePidHelp());
        this.helpCaCertName.setHelpText(MSGS.net8021xCaCertHelp());
        this.helpPublicPrivateKeyPairName.setHelpText(MSGS.net8021xPublicPrivateKeyPairHelp());
    }

    private void initListBoxes() {
        initEapListBox();
        initInnerAuthListBox();
        initCaCertNameListBox();
        initPrivateKeyNameListBox();
    }

    private void initTextBoxes() {
        initUsernameTextBox();
        initPasswordTextBox();
        initKeystorePidTextBox();
    }

    private void initDropdownBoxes() {

        this.keystorePidAnchor.setText(MSGS.selectAvailableTargets());
        this.keystorePidAnchor.setDataToggle(Toggle.DROPDOWN);

        this.keystorePidDropdown.add(this.keystorePidAnchor);

        final DropDownMenu dropDownMenu = new DropDownMenu();
        dropDownMenu.addStyleName("drop-down");

        DropDownHeader dropDownHeader = new DropDownHeader();
        dropDownHeader.setVisible(true);
        dropDownMenu.add(dropDownHeader);

        this.keystorePidDropdown.add(dropDownMenu);

        RequestQueue.submit(context -> this.gwtXSRFService
                .generateSecurityToken(context.callback(token -> Tab8021xUi.this.gwtComponentService
                        .findComponentConfigurations(token, KURA_SERVICE_PID_OSGI_FILTER, context.callback(data -> {
                            if (data.isEmpty()) {
                                dropDownHeader.setText(MSGS.noTargetsAvailable());
                            } else {
                                dropDownHeader.setText(MSGS.targetsAvailable());
                                data.forEach(targetEntry -> {
                                    AnchorListItem listItem = createListItem(this.keystorePid,
                                            targetEntry.getComponentId());
                                    dropDownMenu.add(listItem);
                                });
                            }
                            dropDownHeader.setVisible(true);
                        })))));
    }

    private AnchorListItem createListItem(final TextBoxBase textBox, String targetServicePid) {
        AnchorListItem listItem = new AnchorListItem();
        listItem.setText("(kura.service.pid=" + targetServicePid + ")");
        listItem.addClickHandler(event -> {
            Anchor eventGenerator = (Anchor) event.getSource();
            textBox.setText(eventGenerator.getText());
            textBox.setValue(targetServicePid, true);
            setDirty(true);

        });
        return listItem;
    }

    private void initEapListBox() {
        for (Gwt8021xEap eapValue : Gwt8021xEap.values()) {
            this.eap.addItem(eapValue.name());
        }

        this.eap.addMouseOverHandler(event -> {
            if (this.eap.isEnabled()) {
                setHelpText(MSGS.net8021xEapHelp());
            }
        });

        this.eap.addMouseOutHandler(event -> resetHelpText());

        this.eap.addChangeHandler(event -> {
            setDirty(true);
            refreshForm();
            resetValidations();
        });
    }

    private void initInnerAuthListBox() {
        for (Gwt8021xInnerAuth auth : Gwt8021xInnerAuth.values()) {
            this.innerAuth.addItem(auth.name());
        }

        this.innerAuth.addMouseOverHandler(event -> setHelpText(MSGS.net8021xInnerAuthHelp()));

        this.innerAuth.addMouseOutHandler(event -> resetHelpText());
    }

    private void initUsernameTextBox() {
        this.username.addMouseOverHandler(event -> {
            if (this.username.isEnabled()) {
                setHelpText(MSGS.net8021xUsernameHelp());
            }
        });

        this.username.addBlurHandler(e -> this.username.validate());
        this.username.setAllowBlank(true);
        this.username.addMouseOutHandler(event -> resetHelpText());

        this.username.addChangeHandler(event -> {
            setDirty(true);

            if (this.username.getValue().isEmpty()) {
                this.formgroupIdentityUsername.setValidationState(ValidationState.ERROR);
            } else {
                this.formgroupIdentityUsername.setValidationState(ValidationState.NONE);
            }

        });
    }

    private void initPasswordTextBox() {
        this.password.addMouseOverHandler(event -> {
            if (this.password.isEnabled()) {
                setHelpText(MSGS.net8021xPasswordHelp());
            }
        });

        this.password.addBlurHandler(e -> this.password.validate());
        this.password.setAllowBlank(true);
        this.password.addMouseOutHandler(event -> resetHelpText());

        this.password.addChangeHandler(event -> {
            setDirty(true);

            if (!this.password.validate() && this.password.isEnabled()) {
                this.formgroupPassword.setValidationState(ValidationState.ERROR);
            } else {
                this.formgroupPassword.setValidationState(ValidationState.NONE);
            }

        });
    }

    private void initKeystorePidTextBox() {
        this.keystorePid.addMouseOverHandler(event -> {
            if (this.keystorePid.isEnabled()) {
                setHelpText(MSGS.net8021xKeystorePidHelp());
            }
        });

        this.keystorePid.addBlurHandler(e -> this.keystorePid.validate());
        this.keystorePid.setAllowBlank(false);
        this.keystorePid.addMouseOutHandler(event -> resetHelpText());

        this.keystorePid.addValueChangeHandler(event -> {
            setDirty(true);

            logger.info("Keystore PID value changed to: " + this.keystorePid.getValue());
            logger.info("Keystore PID text changed to: " + this.keystorePid.getText());
            logger.info("Keystore PID enabled: " + this.keystorePid.isEnabled());
            logger.info("Identity Keystore PID visible: " + this.identityKeystorePid.isVisible());

            if (this.keystorePid.getValue().isEmpty() && this.keystorePid.isEnabled()) {
                this.identityKeystorePid.setValidationState(ValidationState.ERROR);
                logger.info("Keystore PID is empty");
            } else {
                logger.info("Keystore PID is valid");
                this.identityKeystorePid.setValidationState(ValidationState.NONE);
                loadcaCertNameList(this.keystorePid.getValue(), Optional.empty());
                loadPrivateKeyNameList(this.keystorePid.getValue(), Optional.empty());
            }

        });

        this.keystorePid.setReadOnly(true);
    }

    private void initCaCertNameListBox() {
        this.caCertName.addMouseOverHandler(event -> {
            if (this.caCertName.isEnabled()) {
                setHelpText(MSGS.net8021xCaCertHelp());
            }
        });

        this.caCertName.addItem("");

        this.caCertName.addMouseOutHandler(event -> resetHelpText());

        this.caCertName.addChangeHandler(event -> setDirty(true));
    }

    private void loadcaCertNameList(String keystorePidValue, Optional<String> selectedValue) {
        this.caCertName.clear();
        this.caCertName.addItem("");
        RequestQueue
                .submit(context -> this.gwtCertificatesService.listKeystoreEntriesByKeystorePidAndKind(keystorePidValue,
                        GwtKeystoreEntry.Kind.TRUSTED_CERT, context.callback(data -> {
                            if (!data.isEmpty()) {
                                for (int i = 0; i < data.size(); ++i) {
                                    GwtKeystoreEntry certName = data.get(i);
                                    this.caCertName.addItem(certName.getAlias());
                                    if (selectedValue.isPresent() && certName.getAlias().equals(selectedValue.get())) {
                                        this.caCertName.setSelectedIndex(i + 1); // +1 because of the empty item
                                    }

                                }
                            }
                        })));
    }

    private void initPrivateKeyNameListBox() {
        this.publicPrivateKeyPairName.addMouseOverHandler(event -> {
            if (this.publicPrivateKeyPairName.isEnabled()) {
                setHelpText(MSGS.net8021xPublicPrivateKeyPairHelp());
            }
        });

        this.publicPrivateKeyPairName.addMouseOutHandler(event -> resetHelpText());
        this.publicPrivateKeyPairName.addChangeHandler(event -> setDirty(true));
    }

    private void loadPrivateKeyNameList(String keystorePidValue, Optional<String> selectedValue) {
        this.publicPrivateKeyPairName.clear();
        RequestQueue
                .submit(context -> this.gwtCertificatesService.listKeystoreEntriesByKeystorePidAndKind(keystorePidValue,
                        GwtKeystoreEntry.Kind.KEY_PAIR, context.callback(data -> {
                            if (!data.isEmpty()) {
                                for (int i = 0; i < data.size(); ++i) {
                                    GwtKeystoreEntry keyPair = data.get(i);
                                    this.publicPrivateKeyPairName.addItem(keyPair.getAlias());
                                    if (selectedValue.isPresent() && keyPair.getAlias().equals(selectedValue.get())) {
                                        this.publicPrivateKeyPairName.setSelectedIndex(i);
                                    }

                                }
                            }
                        })));
    }

    private void update() {
        setValues();
        refreshForm();
        this.netTabs.updateTabs();
    }

    private void resetValidations() {
        this.formgroupIdentityUsername.setValidationState(ValidationState.NONE);
        this.formgroupPassword.setValidationState(ValidationState.NONE);
        this.identityKeystorePid.setValidationState(ValidationState.NONE);
        this.identityCaCertName.setValidationState(ValidationState.NONE);
        this.identityPublicPrivateKeyPairName.setValidationState(ValidationState.NONE);
    }

    private void refreshForm() {
        this.eap.setEnabled(true);
        this.innerAuth.setEnabled(true);
        this.username.setEnabled(true);
        this.password.setEnabled(true);
        this.keystorePid.setEnabled(true);
        this.caCertName.setEnabled(true);
        this.publicPrivateKeyPairName.setEnabled(true);

        switch (Gwt8021xEap.valueOf(this.eap.getSelectedValue())) {
        case PEAP:
        case TTLS:
            this.innerAuth.setEnabled(false);
            setInnerAuthTo(Gwt8021xInnerAuth.MSCHAPV2);
            this.identityKeystorePid.setVisible(false);
            this.identityCaCertName.setVisible(false);
            this.identityPublicPrivateKeyPairName.setVisible(false);
            break;
        case TLS:
            this.innerAuth.setEnabled(false);
            setInnerAuthTo(Gwt8021xInnerAuth.NONE);
            this.identityKeystorePid.setVisible(true);
            this.identityCaCertName.setVisible(true);
            this.identityPublicPrivateKeyPairName.setVisible(true);
            break;
        default:
            break;
        }

    }

    private void reset() {
        for (int i = 0; i < this.eap.getItemCount(); i++) {
            if (this.eap.getSelectedItemText().equals(Gwt8021xEap.TTLS.name())) {
                this.eap.setSelectedIndex(i);
                break;
            }
        }

        for (int i = 0; i < this.innerAuth.getItemCount(); i++) {
            if (this.innerAuth.getSelectedItemText().equals(Gwt8021xInnerAuth.MSCHAPV2.name())) {
                this.innerAuth.setSelectedIndex(i);
                break;
            }
        }

        this.username.setValue("");
        this.password.setValue("");

        this.keystorePid.setValue("");
        this.caCertName.setSelectedIndex(0);
        this.publicPrivateKeyPairName.setSelectedIndex(0);
        update();
    }

    private void setValues() {

        for (int i = 0; i < this.eap.getItemCount(); i++) {
            if (this.eap.getValue(i).equals(this.activeConfig.getEapEnum().name())) {
                this.eap.setSelectedIndex(i);
                break;
            }
        }

        for (int i = 0; i < this.innerAuth.getItemCount(); i++) {
            if (this.innerAuth.getValue(i).equals(this.activeConfig.getInnerAuthEnum().name())) {
                this.innerAuth.setSelectedIndex(i);
                break;
            }
        }

        this.username.setValue(this.activeConfig.getUsername());
        this.password.setValue(this.activeConfig.getPassword());

        this.keystorePid.setValue(this.activeConfig.getKeystorePid());

        loadcaCertNameList(this.activeConfig.getKeystorePid(), Optional.ofNullable(this.activeConfig.getCaCertName()));
        loadPrivateKeyNameList(this.activeConfig.getKeystorePid(),
                Optional.ofNullable(this.activeConfig.getPublicPrivateKeyPairName()));
    }

    @Override
    public void clear() {
        // Not needed
    }

    @Override
    public void refresh() {
        if (isDirty()) {
            setDirty(false);
            resetValidations();

            if (this.activeConfig == null) {
                reset();
            } else {
                update();
            }
        }
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public boolean isValid() {
        boolean isTLS = Gwt8021xEap.valueOf(this.eap.getSelectedValue()) == Gwt8021xEap.TLS;
        boolean isPEAP = Gwt8021xEap.valueOf(this.eap.getSelectedValue()) == Gwt8021xEap.PEAP;
        boolean isTTLS = Gwt8021xEap.valueOf(this.eap.getSelectedValue()) == Gwt8021xEap.TTLS;

        boolean result = true;

        if (isTLS) {

            if (isNonEmptyString(this.username)) {
                this.formgroupIdentityUsername.setValidationState(ValidationState.ERROR);
                result = false;
            }

            if (isNonEmptyString(this.keystorePid)) {
                this.identityKeystorePid.setValidationState(ValidationState.ERROR);
                result = false;
            }

            if (isNonEmptyString(this.publicPrivateKeyPairName.getSelectedItemText())) {
                this.identityPublicPrivateKeyPairName.setValidationState(ValidationState.ERROR);
                result = false;
            }
        }

        if (isPEAP || isTTLS) {
            if (isNonEmptyString(this.username)) {
                this.formgroupIdentityUsername.setValidationState(ValidationState.ERROR);
                result = false;
            }

            if (this.password.getValue() == null || this.password.getValue().trim().isEmpty()) {
                this.formgroupPassword.setValidationState(ValidationState.ERROR);
                result = false;
            }

            if (isNonEmptyString(this.keystorePid) && !isNonEmptyString(this.caCertName.getSelectedItemText())) {
                this.identityKeystorePid.setValidationState(ValidationState.ERROR);
                result = false;
            }
        }

        return result;
    }

    private boolean isNonEmptyString(TextBox value) {
        return value.getValue() == null || value.getValue().trim().isEmpty();
    }

    private boolean isNonEmptyString(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
        if (this.netTabs.getButtons() != null) {
            this.netTabs.getButtons().setButtonsDirty(dirty);
        }
    }

    @Override
    public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf) {
        Gwt8021xConfig updated8021xConfig = new Gwt8021xConfig();

        updated8021xConfig.setIdentity(this.username.getText());

        updated8021xConfig.setPassword(this.password.getText());

        if (!this.eap.getSelectedValue().isEmpty() && this.eap.getSelectedValue() != null) {
            updated8021xConfig.setEap(Gwt8021xEap.valueOf(this.eap.getSelectedValue()));
        }

        if (!this.innerAuth.getSelectedValue().isEmpty() && this.innerAuth.getSelectedValue() != null) {
            updated8021xConfig.setInnerAuthEnum(Gwt8021xInnerAuth.valueOf(this.innerAuth.getSelectedValue()));
        }

        updated8021xConfig.setKeystorePid(this.keystorePid.getValue());
        updated8021xConfig.setCaCertName(this.caCertName.getSelectedValue());
        updated8021xConfig.setPublicPrivateKeyPairName(this.publicPrivateKeyPairName.getSelectedValue());

        updatedNetIf.setEnterpriseConfig(updated8021xConfig);
    }

    @Override
    public void setNetInterface(GwtNetInterfaceConfig config) {
        setDirty(true);
        this.activeConfig = config.get8021xConfig();
    }

    private void setInnerAuthTo(Gwt8021xInnerAuth auth) {
        for (int i = 0; i < this.innerAuth.getItemCount(); i++) {
            if (this.innerAuth.getItemText(i).equals(auth.name())) {
                this.innerAuth.setSelectedIndex(i);
                break;
            }
        }
    }

    private void setHelpText(String message) {
        this.helpText.clear();
        this.helpText.add(new Span(message));
    }

    private void resetHelpText() {
        this.helpText.clear();
        this.helpText.add(new Span(MSGS.netHelpDefaultHint()));
    }

}
