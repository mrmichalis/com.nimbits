/*
 * Copyright (c) 2010 Nimbits Inc.
 *
 * http://www.nimbits.com
 *
 *
 * Licensed under the GNU GENERAL PUBLIC LICENSE, Version 3.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/gpl.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the license is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, eitherexpress or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.nimbits.client.ui.panels;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Orientation;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.*;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.*;
import com.extjs.gxt.ui.client.widget.layout.FillLayout;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.TableData;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.nimbits.client.enums.FilterType;
import com.nimbits.client.enums.Parameters;
import com.nimbits.client.enums.point.PointType;
import com.nimbits.client.exception.NimbitsException;
import com.nimbits.client.model.entity.Entity;
import com.nimbits.client.model.point.Point;
import com.nimbits.client.service.entity.EntityService;
import com.nimbits.client.service.entity.EntityServiceAsync;
import com.nimbits.client.ui.controls.ProtectionLevelOptions;
import com.nimbits.client.ui.helper.FeedbackHelper;
import com.nimbits.client.ui.icons.Icons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PointPanel extends LayoutContainer {

    private ProtectionLevelOptions protectionLevelOptions;
    private final CheckBox he = new CheckBox();
    private final CheckBox idleOn = new CheckBox();
    private final CheckBox deltaOn = new CheckBox();


    private final CheckBox le = new CheckBox();
    private final Collection<PointUpdatedListener> pointUpdatedListeners = new ArrayList<PointUpdatedListener>(1);
    private final NumberField compression = new NumberField();
    private final NumberField expires = new NumberField();
    private final NumberField high = new NumberField();
    private final NumberField idleSeconds = new NumberField();

    private final NumberField deltaSeconds = new NumberField();
    private final NumberField deltaAlert = new NumberField();

    private final NumberField low = new NumberField();
    private final CheckBox inferLocationCheckbox = new CheckBox();
    private final SeparatorToolItem separatorToolItem = new SeparatorToolItem();
    private final TextArea description = new TextArea();
    private final TextField<String> unit = new TextField<String>();
    private final Point entity;
    private final FormData formdata;
    private boolean readOnlyForm;

    private ComboBox<TypeOption> hysteresisType;


    public PointPanel(final Entity entity)   {
        this.entity = (Point) entity;
        readOnlyForm = this.entity.isReadOnly() || ! this.entity.getPointType().equals(PointType.basic);
        protectionLevelOptions = new ProtectionLevelOptions(entity);
        formdata = new FormData("-20");
        loadForm();


    }
    private ComboBox<TypeOption> hysteresisTypeCombo(final FilterType selectedValue) {
        final ComboBox<TypeOption> combo = new ComboBox<TypeOption>();

        final List<TypeOption> ops = new ArrayList<TypeOption>(FilterType.values().length);

        for (final FilterType type : FilterType.values()){
            ops.add(new TypeOption(type));
        }



        final ListStore<TypeOption> store = new ListStore<TypeOption>();

        store.add(ops);

        combo.setFieldLabel("Filter type");
        combo.setReadOnly(readOnlyForm);
        combo.setDisplayField(Parameters.name.getText());
        combo.setValueField(Parameters.value.getText());
        combo.setTriggerAction(ComboBox.TriggerAction.ALL);
        combo.setStore(store);
        combo.setForceSelection(true);
        final TypeOption selected = combo.getStore().findModel(Parameters.value.getText(), selectedValue.getCode());
        combo.setValue(selected);

        return combo;

    }

    private void loadForm()  {



        //  setSize("475", "475");
        setLayout(new FillLayout(Orientation.VERTICAL));
        //setLayout(new FitLayout());

        final com.google.gwt.user.client.ui.VerticalPanel verticalPanel = new com.google.gwt.user.client.ui.VerticalPanel();

        add(verticalPanel);

        verticalPanel.setBorderWidth(0);
        final ToolBar mainToolBar = mainToolBar();
        verticalPanel.add(mainToolBar);

        final TabPanel tabPanel = new TabPanel();

        final TabItem tabAlerts = new TabItem("Alerts");

        final TabItem tabGeneral = new TabItem("General");
        // tabGeneral.setHeight("425");
        tabPanel.setBodyBorder(false);

        verticalPanel.add(tabPanel);
        tabPanel.setSize("600", "600");

        tabPanel.add(tabGeneral);

        tabPanel.add(tabAlerts);


        tabAlerts.add(alertForm());
        tabGeneral.add(generalForm());

        add(verticalPanel);
        doLayout();


    }

    private ToolBar mainToolBar() {
        final ToolBar toolBar = new ToolBar();
        //toolBar.setHeight("");
        final Button buttonSave = saveButtonInit();



        separatorToolItem.setWidth("25px");

        toolBar.add(buttonSave);



        return toolBar;
    }

    private Button saveButtonInit() {
        final Button buttonSave = new Button("Save");
        buttonSave.setEnabled(!entity.isReadOnly());
        buttonSave.setIcon(AbstractImagePrototype.create(Icons.INSTANCE.SaveAll()));
        buttonSave.setEnabled(! this.readOnlyForm);

        buttonSave.addSelectionListener(new SelectionListener<ButtonEvent>() {
            public void componentSelected(ButtonEvent ce) {

                savePoint();

            }
        });

        return buttonSave;
    }


    private void savePoint()  {
        final MessageBox box = MessageBox.wait("Progress",
                "Saving your data, please wait...", "Saving...");
        box.show();

        if (! readOnlyForm) {
            //General
            final Point point = entity;


            point.setDescription(description.getValue());
            point.setProtectionLevel(protectionLevelOptions.getProtectionLevel());

            point.setFilterValue(compression.getValue().doubleValue());
            point.setFilterType(hysteresisType.getValue().type);
            point.setExpire(expires.getValue().intValue());
            point.setUnit(unit.getValue());

            point.setInferLocation(this.inferLocationCheckbox.getValue());
            //Alerts

            point.setHighAlarm(high.getValue().doubleValue());
            point.setLowAlarm(low.getValue().doubleValue());
            point.setHighAlarmOn(he.getValue());
            point.setLowAlarmOn(le.getValue());

            //idlealarm
            point.setIdleAlarmOn(idleOn.getValue());
            point.setIdleSeconds(idleSeconds.getValue().intValue());
            point.setIdleAlarmSent(false);
//
            point.setDeltaAlarm(deltaAlert.getValue().doubleValue());
            point.setDeltaAlarmOn(deltaOn.getValue());
            point.setDeltaSeconds(deltaSeconds.getValue().intValue());
            // point.setSendAlertsAsJson(sendAlertAsJson.getValue());



            final EntityServiceAsync service = GWT.create(EntityService.class);
            // PointServiceAsync service = GWT.create(PointService.class);
            service.addUpdateEntity(point, new AsyncCallback<Entity>() {
                @Override
                public void onFailure(final Throwable caught) {
                    box.close();

                    FeedbackHelper.showError(caught);


                }

                @Override
                public void onSuccess(final Entity result) {

                    try {
                        notifyPointUpdatedListener();
                        MessageBox.alert("Success", "Point Updated", null);
                    } catch (NimbitsException e) {
                        FeedbackHelper.showError(e);
                    }
                    box.close();
                }
            });
        }
    }

    private void notifyPointUpdatedListener() throws NimbitsException {
        for (final PointUpdatedListener pointUpdatedListener : pointUpdatedListeners) {
            pointUpdatedListener.onPointUpdated(entity);
        }
    }



    private FormPanel alertForm() {
        final FormPanel simple = form();



        final Html h = new Html();


        h.setHtml("<P>Enter values that will trigger an high or low alert if the value of this point goes above or below a value and/or" +
                " the number of minutes this point can go without receiving a new value before it goes into an idle alert state. </p>" +
                "<BR><P>Right click on this point and select \"subscribe\" to configure how you'd like to be alerted to changes in this point's alert state. " +
                "Other users who subscribe to this point will also receive alerts based on their settings.</P><BR><BR>");


        simple.add(h);

        final Point point = entity;

        high.setFieldLabel("High Value");
        high.setValue(point.getHighAlarm());
        high.setAllowBlank(false);
        high.setReadOnly(this.readOnlyForm);
        simple.add(high, formdata);


        he.setBoxLabel("High alert enabled");
        he.setLabelSeparator("");
        he.setValue(point.isHighAlarmOn());
        he.setReadOnly(this.readOnlyForm);
        // he.setFieldLabel("Enabled");
        simple.add(he, formdata);


        low.setFieldLabel("Low Value");
        low.setAllowBlank(false);
        low.setValue(point.getLowAlarm());
        low.setReadOnly(this.readOnlyForm);
        simple.add(low, formdata);


        le.setBoxLabel("Low alert enabled");
        le.setLabelSeparator("");
        le.setValue(point.isLowAlarmOn());
        le.setReadOnly(this.readOnlyForm);
        simple.add(le, formdata);

        idleOn.setBoxLabel("Idle alert enabled");
        idleOn.setLabelSeparator("");
        idleOn.setReadOnly(this.readOnlyForm);
        idleOn.setValue(point.isIdleAlarmOn());

        idleSeconds.setFieldLabel("Idle Seconds");
        idleSeconds.setValue(point.getIdleSeconds());
        idleSeconds.setReadOnly(this.readOnlyForm);

        simple.add(idleSeconds, formdata);
        simple.add(idleOn, formdata);


        deltaOn.setBoxLabel("Delta alert enabled");
        deltaOn.setLabelSeparator("");
        deltaOn.setReadOnly(this.readOnlyForm);
        deltaOn.setValue(point.isDeltaAlarmOn());

        deltaSeconds.setFieldLabel("Delta Seconds");
        deltaSeconds.setValue(point.getDeltaSeconds());
        deltaSeconds.setReadOnly(this.readOnlyForm);

        deltaAlert.setFieldLabel("Delta Alert Value");
        deltaAlert.setValue(point.getDeltaAlarm());
        deltaAlert.setReadOnly(this.readOnlyForm);

        simple.add(deltaAlert, formdata);
        simple.add(deltaSeconds, formdata);
        simple.add(deltaOn, formdata);





        if (point.isIdleAlarmOn()) {
            final Html h2 = new Html();

            String s = "<P>Based on the current settings, this point is currently ";
            if (point.getIdleAlarmSent()) {
                s += "idle.";
            } else {
                s += "not idle.";
            }
            //  s += " Last recorded timestamp was: " + point.getLastRecordedTimestamp() + "</p>";
            h2.setHtml(s);

            simple.add(h2, formdata);
        }

        return simple;
    }

    private static FormPanel form() {
        final FormPanel simple = new FormPanel();
        simple.setHeaderVisible(false);
        simple.setFrame(false);
        simple.setBorders(false);
        simple.setBodyBorder(false);
        simple.setSize("400", "500");
        return simple;
    }

    //general
    private FormPanel generalForm( ) {

        final FormPanel simple = form();
        simple.setReadOnly(this.readOnlyForm);
        final VerticalPanel verticalPanel = new VerticalPanel();
        verticalPanel.setSpacing(10);
        //verticalPanel.setHeight(80);

        final TableData tdVerticalPanel = new TableData();
        tdVerticalPanel.setHorizontalAlign(HorizontalAlignment.CENTER);
        tdVerticalPanel.setMargin(5);
        final Point point = entity;
        compression.setFieldLabel("Compression Filter");
        compression.setReadOnly(this.readOnlyForm);
        hysteresisType = hysteresisTypeCombo(point.getFilterType());

        hysteresisType.setReadOnly(this.readOnlyForm);
        compression.setValue(point.getFilterValue());
        compression.setAllowBlank(false);
        compression.setReadOnly(this.readOnlyForm);
        simple.add(compression, formdata);
        simple.add(hysteresisType, formdata);

        inferLocationCheckbox.setFieldLabel("");
        //inferLocationCheckbox.setTitle();
        inferLocationCheckbox.setBoxLabel("Infer GPS Location");
        inferLocationCheckbox.setLabelSeparator("");
        inferLocationCheckbox.setValue(point.inferLocation());
        inferLocationCheckbox.setReadOnly(this.readOnlyForm);
        simple.add(inferLocationCheckbox);
        expires.setFieldLabel("Expires (days)");
        expires.setValue(point.getExpire());
        expires.setAllowBlank(false);
        expires.setReadOnly(this.readOnlyForm);
        simple.add(expires, formdata);


        unit.setFieldLabel("Unit of Measure");
        unit.setValue(point.getUnit());
        unit.setAllowBlank(true);
        unit.setReadOnly(this.readOnlyForm);
        simple.add(unit, formdata);


        simple.add(protectionLevelOptions, formdata);


        description.setPreventScrollbars(true);
        description.setValue(entity.getDescription());
        description.setFieldLabel("Description");
        description.setReadOnly(this.readOnlyForm);
        simple.add(description, formdata);
        description.setSize("400", "100");

        final Html h = new Html("<p>Use filter types to ignore new values that are +/- the previously recorded value or above/below the floor or ceiling setting. This is useful for " +
                "filtering out noise such as small changes in a value or the same value repeated many times when you only want to record significant changes.</p>");
        simple.add(h, formdata);
        return simple;
    }


    public interface PointUpdatedListener {
        void onPointUpdated(Entity entity) throws NimbitsException;
    }

    public void addPointUpdatedListeners(PointUpdatedListener listener) {
        pointUpdatedListeners.add(listener);
    }
    private static class TypeOption extends BaseModelData {
        private static final long serialVersionUID = -4464630285165637035L;
        private FilterType type;


        public TypeOption(final FilterType value) {
            this.type = value;
            set(Parameters.value.getText(), value.getCode());
            set(Parameters.name.getText(), value.getText());
        }

        public FilterType getMethod() {
            return type;
        }
    }
}
