/*
 * Copyright (c) [2017-2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Portions Copyright [2021] [OmniFaces and/or its affiliates]
package org.omnifaces.arquillian.container.glassfish.managed;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.omnifaces.arquillian.container.glassfish.CommonGlassFishManager;

/**
 * GlassFish managed container using REST deployments
 *
 * @author <a href="http://community.jboss.org/people/LightGuard">Jason Porter</a>
 * @author <a href="http://community.jboss.org/people/dan.j.allen">Dan Allen</a>
 * @author Vineet Reynolds
 */
public class GlassFishManagedDeployableContainer implements DeployableContainer<GlassFishManagedContainerConfiguration> {

    private GlassFishManagedContainerConfiguration configuration;
    private GlassFishServerControl glassFishServerControl;
    private CommonGlassFishManager<GlassFishManagedContainerConfiguration> glassFishManager;
    private boolean connectedToRunningServer;

    @Override
    public Class<GlassFishManagedContainerConfiguration> getConfigurationClass() {
        return GlassFishManagedContainerConfiguration.class;
    }

    @Override
    public void setup(GlassFishManagedContainerConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration must not be null");
        }

        this.configuration = configuration;
        glassFishServerControl = new GlassFishServerControl(configuration);
        glassFishManager = new CommonGlassFishManager<>(configuration);
    }

    @Override
    public void start() throws LifecycleException {
        if (glassFishManager.isDASRunning()) {
            if (configuration.isAllowConnectingToRunningServer()) {
                // If we are allowed to connect to a running server, then do not issue the 'asadmin start-domain' command.
                connectedToRunningServer = true;
            } else {
                throw new LifecycleException(
                    "The server is already running! " +
                    "Managed containers do not support connecting to running server instances due to the " +
                    "possible harmful effect of connecting to the wrong server. Please stop server before running or " +
                    "change to another type of container.\n" + "To disable this check and allow Arquillian to connect to a running server, " +
                    "set allowConnectingToRunningServer to true in the container configuration");
            }
        }
    }

    @Override
    public void stop() throws LifecycleException {
        if (!connectedToRunningServer) {
            glassFishServerControl.stop();
        }
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Servlet 5.0");
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        try {
            if (connectedToRunningServer || glassFishManager.isDASRunning()) {
            } else {
                glassFishServerControl.start();
            }
            glassFishManager.start();
        } catch (LifecycleException ex) {
            throw new DeploymentException("Cannot start GlassFish", ex);
        }
        return glassFishManager.deploy(archive);
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        glassFishManager.undeploy(archive);
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }
}
