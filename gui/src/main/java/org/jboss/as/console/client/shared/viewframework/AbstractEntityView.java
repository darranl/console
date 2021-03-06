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

package org.jboss.as.console.client.shared.viewframework;

import java.util.EnumSet;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.widgets.forms.AddressBinding;
import org.jboss.as.console.client.widgets.forms.FormMetaData;
import org.jboss.as.console.client.widgets.forms.PropertyBinding;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormAdapter;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.ObservableFormItem;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tabs.FakeTabPanel;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;


/**
 * An abstract view class with a full EntityEditor.  This class assembles the editor and reacts to
 * FrameworkView callbacks.
 *
 * @author Stan Silvert
 */
public abstract class AbstractEntityView<T> extends SuspendableViewImpl implements FrameworkView, FormItemObserver {
    protected EntityEditor<T> entityEditor;
    protected EntityDetails<T> entityDetails;
    protected Class<?> beanType;
    protected EnumSet<FrameworkButton> hideButtons;
    protected FormMetaData formMetaData;
    protected AddressBinding address;

    public AbstractEntityView(Class<?> beanType, ApplicationMetaData propertyMetaData) {
        this(beanType, propertyMetaData, EnumSet.noneOf(FrameworkButton.class));
    }

    public AbstractEntityView(Class<?> beanType, ApplicationMetaData propertyMetaData, EnumSet<FrameworkButton> hideButtons) {
        this.beanType = beanType;
        this.hideButtons = hideButtons;
        formMetaData = propertyMetaData.getFormMetaData(beanType);
        address = propertyMetaData.getBeanMetaData(beanType).getAddress();
    }

    /**
     * Get the FormMetaData for the Entity.
     * @return The FormMetaData.
     */
    protected FormMetaData getFormMetaData() {
        return this.formMetaData;
    }

    protected AddressBinding getAddress() {
        return this.address;
    }

    /**
     * Get the EntityToDmrBridge for the Entity.
     * @return The bridge.
     */
    protected abstract EntityToDmrBridge<T> getEntityBridge();

    /**
     * Create the table with the desired columns for the Entity.
     * @return The table.
     */
    protected abstract DefaultCellTable<T> makeEntityTable();

    /**
     * Create the form with fields used for creating a new Entity.
     * @return The "Add" form.
     */
    protected abstract FormAdapter<T> makeAddEntityForm();

    /**
     * Get the plural name of the Entity to be displayed.
     * @return The Entity name.
     */
    protected abstract String getEntityDisplayName();

    /**
     * This is the default view assembly routine. It simly creates a single
     * top level tab and adds the editors contents to it.<p/>
     * Feel free to override this method in order to create more sophisticated
     * view compositions.
     *
     * @return
     */
    @Override
    public Widget createWidget() {

        LayoutPanel layout = new LayoutPanel();

        FakeTabPanel titleBar = new FakeTabPanel(getEntityDisplayName());
        layout.add(titleBar);


        VerticalPanel panel = new VerticalPanel();
        panel.setStyleName("rhs-content-panel");

        ScrollPanel scrollPanel = new ScrollPanel(panel);
        layout.add(scrollPanel);

        entityEditor = makeEntityEditor();
        Widget editorWidget = entityEditor.setIncludeTools(false).asWidget();
        panel.add(editorWidget);

        ToolStrip tools = createToolStrip();

        if(tools.hasButtons())
        {
            layout.add(tools);

            layout.setWidgetTopHeight(titleBar, 0, Style.Unit.PX, 28, Style.Unit.PX);
            layout.setWidgetTopHeight(tools, 28, Style.Unit.PX, 30, Style.Unit.PX);
            layout.setWidgetTopHeight(scrollPanel, 58, Style.Unit.PX, 100, Style.Unit.PCT);
        }
        else
        {
            layout.setWidgetTopHeight(titleBar, 0, Style.Unit.PX, 28, Style.Unit.PX);
            layout.setWidgetTopHeight(scrollPanel, 28, Style.Unit.PX, 100, Style.Unit.PCT);
        }

        return layout;
    }

    /**
     * Creates a default widget without the top most tab.
     * It can be easily embedded into a TablayoutPanel for instance.
     *
     * @return
     */
    public Widget createEmbeddableWidget() {

        LayoutPanel layout = new LayoutPanel();
        layout.setStyleName("fill-layout");

        VerticalPanel panel = new VerticalPanel();
        panel.setStyleName("rhs-content-panel");

        ScrollPanel scrollPanel = new ScrollPanel(panel);
        layout.add(scrollPanel);

        entityEditor = makeEntityEditor();
        Widget editorWidget = entityEditor.setIncludeTools(false).asWidget();
        panel.add(editorWidget);

        ToolStrip tools = createToolStrip();

        if(tools.hasButtons())
        {
            layout.add(tools);

            layout.setWidgetTopHeight(tools, 0, Style.Unit.PX, 28, Style.Unit.PX);
            layout.setWidgetTopHeight(scrollPanel, 28, Style.Unit.PX, 100, Style.Unit.PCT);
        }
        else
        {
            layout.setWidgetTopHeight(scrollPanel, 0, Style.Unit.PX, 100, Style.Unit.PCT);
        }

        return layout;
    }

    protected ToolStrip createToolStrip() {
        return entityEditor.createTools();
    }

    /**
     * Create the EntityEditor with the following pieces:
     * - A title obtained from getPluralEntityName()
     * - A table obtained from makeEntityTable()
     * - An AddEntityWindow with a form from makeAddEntityForm()
     * - An EntityDetails with a form from makeEditEntityDetailsForm()
     *
     * @return The EntityEditor
     */
    protected EntityEditor<T> makeEntityEditor() {
        entityDetails = new EntityDetails<T>(getEntityDisplayName(),
                                                               makeEditEntityDetailsForm(),
                                                               getEntityBridge(),
                                                               address,
                                                               hideButtons);
        String title = Console.CONSTANTS.common_label_add() + " " + getEntityDisplayName();
        EntityPopupWindow<T> window = new AddEntityWindow<T>(title,
                                                             makeAddEntityForm(),
                                                             getAddress(),
                                                             getEntityBridge());
        DefaultCellTable<T> table = makeEntityTable();
        return new EntityEditor<T>(getEntityDisplayName(), window, table, entityDetails, hideButtons);
    }

    /**
     * Creates an details for for the Entity.  This method will add all the Entity's Attributes to
     * the form in a 2-column format.  If you desire a different layout or you want to exclude
     * some attributes, you should override this method.
     *
     * @return The details form.
     */
    protected FormAdapter<T> makeEditEntityDetailsForm() {
        if (getFormMetaData().hasTabs()) {
            TabbedFormLayoutPanel tabPanel = new TabbedFormLayoutPanel(beanType, getFormMetaData(), this);
            return tabPanel;
        } else {
            return makeSimpleForm();
        }
    }
    
    private FormAdapter<T> makeSimpleForm() {
        Form<T> form = new Form(beanType);
        form.setNumColumns(2);
        FormMetaData attributes = getFormMetaData();

        // add base items to form
        FormItem[][] items = new FormItem[attributes.getBaseAttributes().size()][];
        int i=0;
        for (PropertyBinding attrib : attributes.getBaseAttributes()) {
            items[i++] = attrib.getFormItemForEdit(this);
        }
        form.setFields(items);

        // add grouped items to form
        for (String subgroup : attributes.getGroupNames()) {
            FormItem[][] groupItems = new FormItem[attributes.getGroupedAttribtes(subgroup).size()][];
            int j=0;
            for (PropertyBinding attrib : attributes.getGroupedAttribtes(subgroup)) {
                groupItems[j++] = attrib.getFormItemForEdit(this);
            }
            form.setFieldsInGroup(subgroup, groupItems);
        }

        return form;
    }

    /**
     * Called when the user requests details to be edited.
     * @param isEnabled
     */
    @Override
    public void setEditingEnabled(boolean isEnabled) {
        entityEditor.setEditingEnabled(isEnabled);
    }

    @Override
    public void initialLoad() {
        getEntityBridge().loadEntities(null);
    }

    /**
     * Called whenever there is a change to any Entity
     */
    @Override
    public void refresh() {
        EntityToDmrBridge entityBridge = getEntityBridge();
        T lastEntityEdited = null;
        if (entityBridge.getNameOfLastEdited() != null) {
            // Look up by name.
            lastEntityEdited = (T)entityBridge.findEntity(entityBridge.getNameOfLastEdited());
        }

        entityEditor.updateEntityList(entityBridge.getEntityList(), lastEntityEdited);
    }

    @Override
    public void itemAction(Action action, ObservableFormItem item) {
    }

}
