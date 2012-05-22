/*
 * Copyright (c) 2010 Tonic Solutions LLC.
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

package com.nimbits.server.api;

import com.nimbits.client.enums.Action;
import com.nimbits.client.enums.EntityType;
import com.nimbits.client.enums.Parameters;
import com.nimbits.client.enums.ProtectionLevel;
import com.nimbits.client.exception.NimbitsException;

import com.nimbits.client.model.entity.Entity;
import com.nimbits.client.model.entity.EntityModel;
import com.nimbits.client.model.entity.EntityModelFactory;
import com.nimbits.client.model.instance.Instance;
import com.nimbits.client.model.instance.InstanceModel;

import com.nimbits.server.com.nimbits.server.transactions.dao.entity.EntityJPATransactions;
import com.nimbits.server.com.nimbits.server.transactions.dao.instance.InstanceTransactions;
import com.nimbits.server.com.nimbits.server.transactions.dao.search.SearchLogTransactions;
import com.nimbits.server.gson.GsonFactory;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by Benjamin Sautner
 * User: BSautner
 * Date: 12/15/11
 * Time: 1:47 PM
 */
public class EntityDescriptionServletImpl extends HttpServlet {

    private InstanceTransactions instanceTransactions;
    private EntityJPATransactions entityTransactions;

    @Resource(name="instanceDao")
    public void setInstanceTransactions(InstanceTransactions transactions) {
        this.instanceTransactions = transactions;
    }

    @Resource(name="entityDao")
    public void setEntityTransactions(EntityJPATransactions transactions) {
        this.entityTransactions = transactions;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    }

    @Override
    public void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        try {
            processGet(request, response);
        } catch (NimbitsException ignored) {    //todo logging

        }
    }

    private void processGet(HttpServletRequest request, HttpServletResponse response) throws IOException, NimbitsException {
        final String serverJson = request.getParameter(Parameters.server.getText());
        final String json = request.getParameter(Parameters.entity.getText());
        final String action = request.getParameter(Parameters.action.getText());

        final PrintWriter out = response.getWriter();


        out.println("Getting Post Data");
        out.println(action);
        if (StringUtils.isNotEmpty(json) && StringUtils.isNotEmpty(action)) {
            final Instance server = GsonFactory.getInstance().fromJson(serverJson, InstanceModel.class);
            final Instance currentServer = instanceTransactions.readInstance(server.getBaseUrl());

            final Entity entityDescription;

                final Entity entity = GsonFactory.getInstance().fromJson(json, EntityModel.class);
                final String desc = StringUtils.isEmpty(entity.getDescription()) ? entity.getName().getValue() : entity.getDescription();
               entity.setDescription(desc);
               entityDescription =
                        EntityModelFactory.createEntity(
                                 entity
                        ); //TODO - needs the server info




            if (action.equals(Action.update.name()) && StringUtils.isNotEmpty(serverJson)) {

                if (currentServer != null && entityDescription != null) {

                    if (  entity.getProtectionLevel().equals(ProtectionLevel.everyone) && sharedType(entity.getEntityType())) {

                        final Entity retObj = entityTransactions.addUpdateEntity(entityDescription);

                        out.println("Reponse:");
                        String r = GsonFactory.getInstance().toJson(retObj);
                        out.println(r);
                    } else {
                        out.println("deleting : " + entityDescription.getKey());
                        entityTransactions.deleteEntityByUUID(entityDescription.getKey());
                    }


                }
            } else if (action.equals(Action.delete.name()) && entityDescription != null) {
                entityTransactions.deleteEntityByUUID(entityDescription.getKey());


            }
        }
        out.close();
    }

    private boolean sharedType(EntityType type) {
        return type.equals(EntityType.point) ||  type.equals(EntityType.category) || type.equals(EntityType.file);

    }
}
