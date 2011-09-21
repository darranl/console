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
package org.jboss.as.console.client.shared.subsys.ejb.session.model;

import java.util.Set;

import org.jboss.as.console.client.shared.subsys.ejb.model.EJBCommonModel;
import org.jboss.as.console.client.widgets.forms.Binding;


/**
 * @author David Bosschaert
 */
public interface SessionBeans extends EJBCommonModel {
    @Override
    @Binding(detypedName = "default-slsb-instance-pool")
    String getDefaultPool();
    @Override
    void setDefaultPool(String name);

    // TODO can we move these down? Do we only need to override the ones with an @Binding?
    @Override
    Set<String> getAvailablePools();
    @Override
    void setAvailablePools(Set<String> keys);
}