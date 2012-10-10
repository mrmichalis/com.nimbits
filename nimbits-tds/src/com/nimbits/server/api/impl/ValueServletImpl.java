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

package com.nimbits.server.api.impl;

import com.nimbits.client.common.Utils;
import com.nimbits.client.constants.Const;
import com.nimbits.client.constants.UserMessages;
import com.nimbits.client.constants.Words;
import com.nimbits.client.enums.*;
import com.nimbits.client.exception.NimbitsException;
import com.nimbits.client.model.entity.Entity;
import com.nimbits.client.model.entity.EntityName;
import com.nimbits.client.model.location.Location;
import com.nimbits.client.model.location.LocationFactory;
import com.nimbits.client.model.point.Point;
import com.nimbits.client.model.user.User;
import com.nimbits.client.model.value.Value;
import com.nimbits.client.model.value.ValueData;
import com.nimbits.client.model.value.impl.ValueFactory;
import com.nimbits.client.model.value.impl.ValueModel;
import com.nimbits.server.api.ApiServlet;
import com.nimbits.server.gson.GsonFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

@Transactional
@Service("value")
public class ValueServletImpl extends ApiServlet implements org.springframework.web.HttpRequestHandler {
    final private static Logger log = Logger.getLogger(ValueServletImpl.class.getName());

    @Override
    public void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        if (isPost(req)) {

            doPost(req, resp);
        }
        else {
            doGet(req, resp);
        }

    }


    @Override
    protected  void doPost(final HttpServletRequest req, final HttpServletResponse resp)   {
        try {
            doInit(req, resp, ExportType.plain);

            log.info("recording post");

            if (user != null && ! user.isRestricted()) {

                final EntityName pointName = commonFactory.createName(getParam(Parameters.point), EntityType.point);
                final List<Entity> points =  entityService.getEntityByName(user, pointName, EntityType.point);

                if (points.isEmpty()) {
                    throw new NimbitsException(new NimbitsException(UserMessages.ERROR_POINT_NOT_FOUND));

                } else {
                    final Value v;
                    final Point point = (Point) points.get(0);
                    if (Utils.isEmptyString(getParam(Parameters.json))) {
                        v = createValueFromRequest(point.inferLocation());
                    } else {
                        final Value vx = GsonFactory.getInstance().fromJson(getParam(Parameters.json), ValueModel.class);
                        Location l = vx.getLocation();
//                    log.info(point.getName().getValue() + " " + point.inferLocation());
                        if (point.inferLocation() && vx.getLocation().isEmpty()) {
                            l = location;
                        }
//                    log.info(location.toString());
                        v = ValueFactory.createValueModel(l, vx.getDoubleValue(), vx.getTimestamp(),
                                vx.getNote(), vx.getData(), AlertType.OK);
                    }


                    final Value result = valueService.recordValue(user, point, v);

                    //reportLocation(point, location);

                    final PrintWriter out = resp.getWriter();
                    final String j = GsonFactory.getInstance().toJson(result);
                    out.print(j);

                }
                resp.setStatus(Const.HTTP_STATUS_OK);
            }
            else {
                resp.setStatus(Const.HTTP_STATUS_UNAUTHORISED);
            }
        } catch (NimbitsException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.addHeader("ERROR", e.getMessage());

        } catch (IOException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.addHeader("ERROR", e.getMessage());
        }
    }



    @Override
    public void doGet(final HttpServletRequest req, final HttpServletResponse resp)   {
        try {
            doInit(req, resp, ExportType.plain);
            final PrintWriter out = resp.getWriter();
            Value nv = null;
            final String format = getParam(Parameters.format)==null ? Words.WORD_DOUBLE : getParam(Parameters.format);

            if (format.equals(Parameters.json.getText()) && !Utils.isEmptyString(getParam(Parameters.json))) {
                nv = GsonFactory.getInstance().fromJson(getParam(Parameters.json), ValueModel.class);
            } else if (format.equals(Words.WORD_DOUBLE) && !Utils.isEmptyString(getParam(Parameters.value))) {

                nv = createValueFromRequest(false);

            }
            out.print(processRequest(getParam(Parameters.point), getParam(Parameters.uuid), format, nv, user));
            out.close();
            resp.setStatus(Const.HTTP_STATUS_OK);
        } catch (NimbitsException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.addHeader("ERROR", e.getMessage());

        } catch (IOException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.addHeader("ERROR", e.getMessage());
        }
    }

    private static Value createValueFromRequest(boolean inferLocation) {
        Value nv;
        final double latitude = getDoubleFromParam(getParam(Parameters.lat));
        final double longitude = getDoubleFromParam(getParam(Parameters.lng));
        final double value = getDoubleFromParam(getParam(Parameters.value));
        final String data =  getParam(Parameters.data);
        final ValueData vd = ValueFactory.createValueData(data);
        final Date timestamp = getParam(Parameters.timestamp) != null ? new Date(Long.parseLong(getParam(Parameters.timestamp))) : new Date();
        Location location1 = LocationFactory.createLocation(latitude, longitude);
        if (inferLocation && location1.isEmpty()) {
            location1 = location;
        }


        nv  = ValueFactory.createValueModel(location1, value, timestamp, getParam(Parameters.note), vd, AlertType.OK);
        return nv;
    }

    private static double getDoubleFromParam(final String valueStr) {
        double retVal;
        try {
            retVal = valueStr != null ? Double.valueOf(valueStr) : 0;
        } catch (NumberFormatException e) {
            retVal = 0;
        }
        return retVal;
    }

    protected String processRequest(
            final String pointNameParam,
            final String uuid,
            final String format,
            final Value nv,
            final User u) throws NimbitsException {

        final List<Entity> result;
        if (!Utils.isEmptyString(uuid)) {
            result = entityService.getEntityByKey(u, uuid, EntityType.point);
        }
        else if (!Utils.isEmptyString(pointNameParam)) {
            final EntityName pointName =  commonFactory.createName(pointNameParam, EntityType.point);

            result = entityService.getEntityByName(u, pointName, EntityType.point);
        }
        else {
            throw new NimbitsException(UserMessages.ERROR_POINT_NOT_FOUND);
        }

        if (result.isEmpty()) {
            throw new NimbitsException(UserMessages.ERROR_POINT_NOT_FOUND);
        }
        else {

            final Entity p = result.get(0);
            if ((u == null || u.isRestricted()) && !p.getProtectionLevel().equals(ProtectionLevel.everyone)) {
                throw new NimbitsException(UserMessages.RESPONSE_PROTECTED_POINT);
            }
            Value value = null;
            if (nv != null && u != null && !u.isRestricted()) {
                // record the value, but not if this is a public
                // request
                final Value newValue = ValueFactory.createValueModel(
                        nv.getLocation(), nv.getDoubleValue(),
                        nv.getTimestamp(),nv.getNote(),  nv.getData(), AlertType.OK);


                value = valueService.recordValue(u, p, newValue);
                if (nv.getLocation().isEmpty()) {
                    //reportLocation(p, location);
                }
                else {
                  //  reportLocation(p,nv.getLocation());
                }
            } else {
                List<Value> values = valueService.getCurrentValue(p);
                if (! values.isEmpty()) {
                    value = values.get(0);
                }
            }
            String r =  value != null ? format.equals(Parameters.json.getText()) ? GsonFactory.getInstance().toJson(value) : String.valueOf(value.getDoubleValue()) : "";

            if (containsParam(Parameters.client) && getParam(Parameters.client).equals(ClientType.arduino.getCode())) {
                r = Const.CONST_ARDUINO_DATA_SEPARATOR + r + Const.CONST_ARDUINO_DATA_SEPARATOR;
            }

            return r;

        }

    }



}
