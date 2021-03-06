/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.jboss.as.console.client.domain.model.impl;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import org.jboss.as.console.client.domain.hosts.CurrentServerConfigurations;
import org.jboss.as.console.client.domain.model.Host;
import org.jboss.as.console.client.domain.model.HostInformationStore;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.domain.model.ServerInstance;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.dispatch.DispatchAsync;
import org.jboss.as.console.client.shared.dispatch.impl.DMRAction;
import org.jboss.as.console.client.shared.dispatch.impl.DMRResponse;
import org.jboss.as.console.client.shared.jvm.Jvm;
import org.jboss.as.console.client.shared.model.ModelAdapter;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.widgets.forms.EntityAdapter;
import org.jboss.as.console.client.widgets.forms.PropertyBinding;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.dmr.client.ModelDescriptionConstants;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 3/18/11
 */
public class HostInfoStoreImpl implements HostInformationStore {

    private DispatchAsync dispatcher;
    private BeanFactory factory;
    private ApplicationMetaData propertyMetaData;
    private CurrentServerConfigurations currentConfigs;
    private EntityAdapter<Server> serverAdapter;
    private EntityAdapter<Jvm> jvmAdapter;
    private EntityAdapter<PropertyRecord> propertyAdapter;

    @Inject
    public HostInfoStoreImpl(DispatchAsync dispatcher, BeanFactory factory, ApplicationMetaData propertyMeta,
                             CurrentServerConfigurations currentConfigs) {
        this.dispatcher = dispatcher;
        this.factory = factory;
        this.propertyMetaData = propertyMeta;
        this.currentConfigs =currentConfigs;
        serverAdapter = new EntityAdapter<Server>(Server.class, propertyMeta);
        jvmAdapter = new EntityAdapter<Jvm>(Jvm.class, propertyMeta);
        propertyAdapter = new EntityAdapter<PropertyRecord>(PropertyRecord.class, propertyMeta);
    }

    @Override
    public void getHosts(final AsyncCallback<List<Host>> callback) {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_CHILDREN_NAMES_OPERATION);
        operation.get(CHILD_TYPE).set("host");
        operation.get(ADDRESS).setEmptyList();

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = ModelNode.fromBase64(result.getResponseText());
                List<ModelNode> payload = response.get("result").asList();

                List<Host> records = new LinkedList<Host>();
                for(int i=0; i<payload.size(); i++)
                {
                    Host record = factory.host().as();
                    record.setName(payload.get(i).asString());
                    records.add(record);
                }

                callback.onSuccess(records);
            }

        });
    }

    @Override
    public void getServerConfigurations(String host, final AsyncCallback<List<Server>> callback) {

        assert host!=null : "Host parameter is null!";

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(CHILD_TYPE).set("server-config");
        operation.get(ADDRESS).setEmptyList();
        operation.get(ADDRESS).add("host", host);
        operation.get(INCLUDE_RUNTIME).set(true);

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(DMRResponse result) {

                ModelNode response = ModelNode.fromBase64(result.getResponseText());
                List<ModelNode> payload = response.get("result").asList();

                List<Server> records = new LinkedList<Server>();
                for(ModelNode item : payload)
                {
                    ModelNode model = item.asProperty().getValue();
                    Server server = serverAdapter.fromDMR(model);
                    server.setStarted(model.get("status").asString().equals("STARTED"));
                    records.add(server);
                }

                currentConfigs.setServerConfigs(records);

                callback.onSuccess(records);
            }

        });
    }

    public void getVirtualMachines(String host, final AsyncCallback<List<String>> callback) {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_CHILDREN_NAMES_OPERATION);
        operation.get(CHILD_TYPE).set("jvm");
        operation.get(ADDRESS).setEmptyList();
        operation.get(ADDRESS).add("host", host);

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(DMRResponse result) {

                ModelNode response = ModelNode.fromBase64(result.getResponseText());
                List<ModelNode> payload = response.get("result").asList();

                List<String> records = new ArrayList<String>(payload.size());

                for(ModelNode jvm : payload)
                    records.add(jvm.asString());

                callback.onSuccess(records);
            }

        });
    }

    @Override
    public void getServerInstances(final String host, final AsyncCallback<List<ServerInstance>> callback) {

        final List<ServerInstance> instanceList = new LinkedList<ServerInstance>();

        getServerConfigurations(host, new SimpleCallback<List<Server>>() {
            @Override
            public void onSuccess(final List<Server> serverConfigs) {

                for(final Server serverConfig : serverConfigs)
                {
                    ServerInstance instance = factory.serverInstance().as();
                    instance.setName(serverConfig.getName());
                    instance.setServer(serverConfig.getName());
                    instance.setGroup(serverConfig.getGroup());
                    instance.setRunning(serverConfig.isStarted());

                    instanceList.add(instance);
                }

                callback.onSuccess(instanceList);

                /*for(final Server handle : serverNames)
                {
                    final ModelNode operation = new ModelNode();
                    operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
                    operation.get(ADDRESS).setEmptyList();
                    operation.get(ADDRESS).add("host", host);
                    operation.get(ADDRESS).add("server-config", handle.getName());
                    operation.get("name").set("status");

                    dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
                        @Override
                        public void onSuccess(DMRResponse result) {

                            ModelNode statusResponse = ModelNode.fromBase64(result.getResponseText());
                            ModelNode payload = statusResponse.get("result");

                            ServerInstance instance = factory.serverInstance().as();
                            instance.setName(handle.getName());
                            instance.setRunning(payload.asString().equals("STARTED"));
                            instance.setServer(handle.getName());
                            instance.setGroup(handle.getGroup());

                            instanceList.add(instance);

                            if(instanceList.size() == serverNames.size())
                                callback.onSuccess(instanceList);
                        }
                    });

                } */
            }
        });
    }

    @Override
    public void startServer(final String host, final String configName, boolean startIt, final AsyncCallback<Boolean> callback) {
        final String actualOp = startIt ? "start" : "stop";

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(actualOp);
        operation.get(ADDRESS).add("host", host);
        operation.get(ADDRESS).add("server-config", configName);

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = ModelNode.fromBase64(result.getResponseText());
                if(response.get("outcome").asString().equals("success"))
                {
                    callback.onSuccess(Boolean.TRUE);
                }
                else
                {
                    callback.onSuccess(Boolean.FALSE);
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                callback.onSuccess(Boolean.FALSE);
                Log.error("Failed to "+actualOp + " server " +configName);
            }
        });

    }

    @Override
    public void reloadServer(String host, final String configName, final AsyncCallback<Boolean> callback) {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("reload");
        operation.get(ADDRESS).add("host", host);
        operation.get(ADDRESS).add("server", configName);

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = ModelNode.fromBase64(result.getResponseText());
                if(response.get("outcome").asString().equals("success"))
                {
                    callback.onSuccess(Boolean.TRUE);
                }
                else
                {
                    callback.onSuccess(Boolean.FALSE);
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                callback.onSuccess(Boolean.FALSE);
            }
        });

    }

    @Override
    public void createServerConfig(String host, Server record, final AsyncCallback<Boolean> callback) {
        final ModelNode serverConfig = new ModelNode();
        serverConfig.get(OP).set(ModelDescriptionConstants.ADD);
        serverConfig.get(ADDRESS).add("host", host);
        serverConfig.get(ADDRESS).add(ModelDescriptionConstants.SERVER_CONFIG, record.getName());

        serverConfig.get("name").set(record.getName());
        serverConfig.get("group").set(record.getGroup());
        serverConfig.get("auto-start").set(record.isAutoStart());

        // TODO: can be null?
        if(record.getJvm()!=null)
            serverConfig.get("jvm").set(record.getJvm().getName());
        else
            Log.warn("JVM null for server "+record.getName());

        serverConfig.get("socket-binding-group").set(record.getSocketBinding());
        serverConfig.get("socket-binding-port-offset").set(record.getPortOffset());


        //System.out.println(serverConfig.toJSONString());

        dispatcher.execute(new DMRAction(serverConfig), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                Log.error("Failed to create server config: " + caught);
                callback.onSuccess(Boolean.FALSE);
            }

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = ModelNode.fromBase64(result.getResponseText());
                String outcome = response.get("outcome").asString();

                Boolean wasSuccessful = outcome.equals("success") ? Boolean.TRUE : Boolean.FALSE;
                callback.onSuccess(wasSuccessful);
            }
        });
    }

    @Override
    public void saveServerConfig(String host, String name, Map<String, Object> changedValues, final AsyncCallback<Boolean> callback) {
        ModelNode proto = new ModelNode();
        proto.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        proto.get(ADDRESS).add("host", host);
        proto.get(ADDRESS).add(ModelDescriptionConstants.SERVER_CONFIG, name);

        List<PropertyBinding> bindings = propertyMetaData.getBindingsForType(Server.class);
        ModelNode operation  = ModelAdapter.detypedFromChangeset(proto, changedValues, bindings);


        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = ModelNode.fromBase64(result.getResponseText());
                callback.onSuccess(response.get(OUTCOME).asString().equals(SUCCESS));
            }
        });
    }

    @Override
    public void deleteServerConfig(String host, Server record, final AsyncCallback<Boolean> callback) {
        final ModelNode serverConfig = new ModelNode();
        serverConfig.get(OP).set(ModelDescriptionConstants.REMOVE);
        serverConfig.get(ADDRESS).add("host", host);
        serverConfig.get(ADDRESS).add(ModelDescriptionConstants.SERVER_CONFIG, record.getName());


        dispatcher.execute(new DMRAction(serverConfig), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                Log.error("Failed to create server config: " + caught);
                callback.onSuccess(Boolean.FALSE);
            }

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = ModelNode.fromBase64(result.getResponseText());
                String outcome = response.get("outcome").asString();

                Boolean wasSuccessful = outcome.equals("success") ? Boolean.TRUE : Boolean.FALSE;
                callback.onSuccess(wasSuccessful);
            }
        });
    }

    @Override
    public void loadJVMConfiguration(String host, Server server, final AsyncCallback<Jvm> callback) {

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(ADDRESS).add("host", host);
        operation.get(ADDRESS).add(ModelDescriptionConstants.SERVER_CONFIG, server.getName());
        operation.get(CHILD_TYPE).set("jvm");

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode result = ModelNode.fromBase64(dmrResponse.getResponseText());

                List<Property> jvms = result.get(RESULT).asPropertyList();
                if(!jvms.isEmpty())
                {
                    // select first entry
                    Property property = jvms.get(0);
                    Jvm jvm = jvmAdapter.fromDMR(property.getValue().asObject());
                    jvm.setName(property.getName());

                    callback.onSuccess(jvm);
                }
                else
                {
                    callback.onSuccess(null);
                }

            }
        });

    }

    @Override
    public void loadProperties(String host, Server server, final AsyncCallback<List<PropertyRecord>> callback) {

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(ADDRESS).add("host", host);
        operation.get(ADDRESS).add(ModelDescriptionConstants.SERVER_CONFIG, server.getName());
        operation.get(CHILD_TYPE).set("system-property");

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode result = ModelNode.fromBase64(dmrResponse.getResponseText());
                List<Property> properties = result.get(RESULT).asPropertyList();
                List<PropertyRecord> records = new ArrayList<PropertyRecord>(properties.size());

                for(Property prop : properties)
                {
                    PropertyRecord record = factory.property().as();
                    record.setKey(prop.getName());
                    ModelNode payload = prop.getValue().asObject();
                    record.setValue(payload.get("value").asString());
                    record.setBootTime(payload.get("boot-time").asBoolean());

                    records.add(record);
                }

                callback.onSuccess(records);
            }
        });
    }
}
