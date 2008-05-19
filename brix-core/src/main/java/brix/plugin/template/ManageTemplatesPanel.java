package brix.plugin.template;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.WindowClosedCallback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import brix.BrixRequestCycle;
import brix.auth.Action;
import brix.auth.Action.Context;
import brix.plugin.template.auth.CreateTemplateAction;
import brix.plugin.template.auth.DeleteTemplateAction;
import brix.plugin.template.auth.RestoreTemplateAction;
import brix.web.admin.AdminPanel;
import brix.web.admin.navigation.NavigationAwarePanel;
import brix.workspace.Workspace;
import brix.workspace.WorkspaceModel;

public class ManageTemplatesPanel extends NavigationAwarePanel<Workspace>
{

    public ManageTemplatesPanel(String id, IModel<Workspace> model)
    {
        super(id, model);
        setOutputMarkupId(true);

        IModel<List<Workspace>> templatesModel = new LoadableDetachableModel<List<Workspace>>()
        {
            @Override
            protected List<Workspace> load()
            {
                List<Workspace> list = TemplatePlugin.get().getTemplates();
                return BrixRequestCycle.Locator.getBrix().filterVisibleWorkspaces(list,
                    Context.ADMINISTRATION);
            }
        };

        Form<Void> modalWindowForm = new Form<Void>("modalWindowForm");
        add(modalWindowForm);

        final ModalWindow modalWindow = new ModalWindow("modalWindow");
        modalWindow.setInitialWidth(64);
        modalWindow.setWidthUnit("em");
        modalWindow.setUseInitialHeight(false);
        modalWindow.setResizable(false);
        modalWindow.setTitle(new ResourceModel("selectItems"));
        modalWindowForm.add(modalWindow);


        add(new ListView<Workspace>("templates", templatesModel)
        {
            @Override
            protected IModel<Workspace> getListItemModel(IModel<List<Workspace>> listViewModel,
                    int index)
            {
                return new WorkspaceModel(listViewModel.getObject().get(index));
            }

            @Override
            protected void populateItem(final ListItem<Workspace> item)
            {
                TemplatePlugin plugin = TemplatePlugin.get();
                final String name = plugin.getUserVisibleName(item.getModelObject(), false);

                item.add(new Label<String>("label", name));
                item.add(new Link<Void>("browse")
                {
                    @Override
                    public void onClick()
                    {
                        AdminPanel panel = findParent(AdminPanel.class);
                        panel.setWorkspace(item.getModelObject().getId(), name);
                    }
                });

                item.add(new AjaxLink<Void>("restoreItems")
                {
                    @Override
                    public void onClick(AjaxRequestTarget target)
                    {
                        String templateId = item.getModelObject().getId();
                        String targetId = ManageTemplatesPanel.this.getModelObject().getId();
                        Panel<Void> panel = new RestoreItemsPanel(modalWindow.getContentId(),
                            templateId, targetId);
                        modalWindow.setContent(panel);
                        modalWindow.show(target);
                    }

                    @Override
                    public boolean isVisible()
                    {
                        Workspace target = ManageTemplatesPanel.this.getModelObject();
                        Action action = new RestoreTemplateAction(Context.ADMINISTRATION, item
                            .getModelObject(), target);
                        return BrixRequestCycle.Locator.getBrix().getAuthorizationStrategy()
                            .isActionAuthorized(action);
                    }
                });

                item.add(new Link<Void>("delete")
                {
                    @Override
                    public void onClick()
                    {
                        Workspace template = item.getModelObject();
                        template.delete();
                    }

                    @Override
                    public boolean isVisible()
                    {
                        Action action = new DeleteTemplateAction(Context.ADMINISTRATION, item
                            .getModelObject());
                        return BrixRequestCycle.Locator.getBrix().getAuthorizationStrategy()
                            .isActionAuthorized(action);
                    }
                });
            }
        });

        Form<Object> form = new Form<Object>("form")
        {
            @Override
            public boolean isVisible()
            {
                Workspace current = ManageTemplatesPanel.this.getModelObject();
                Action action = new CreateTemplateAction(Context.ADMINISTRATION, current);
                return BrixRequestCycle.Locator.getBrix().getAuthorizationStrategy()
                    .isActionAuthorized(action);
            }
        };

        TextField<String> templateName = new TextField<String>("templateName",
            new PropertyModel<String>(this, "templateName"));
        form.add(templateName);

        templateName.setRequired(true);
        templateName.add(new UniqueTemplateNameValidator());

        final FeedbackPanel feedback;

        add(feedback = new FeedbackPanel("feedback"));
        feedback.setOutputMarkupId(true);

        form.add(new AjaxButton<Void>("submit")
        {
            @Override
            public void onSubmit(AjaxRequestTarget target, Form< ? > form)
            {
                String workspaceId = ManageTemplatesPanel.this.getModelObject().getId();
                CreateTemplatePanel panel = new CreateTemplatePanel(modalWindow.getContentId(),
                    workspaceId, ManageTemplatesPanel.this.templateName);
                modalWindow.setContent(panel);
                modalWindow.setWindowClosedCallback(new WindowClosedCallback()
                {
                    public void onClose(AjaxRequestTarget target)
                    {
                        target.addComponent(ManageTemplatesPanel.this);
                    }
                });
                modalWindow.show(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form< ? > form)
            {
                target.addComponent(feedback);
            }
        });

        add(form);

    }

    private String templateName;

    private class UniqueTemplateNameValidator implements IValidator
    {
        public void validate(IValidatable validatable)
        {
            String name = (String)validatable.getValue();
            if (TemplatePlugin.get().templateExists(name))
            {
                validatable.error(new ValidationError()
                    .addMessageKey("UniqueTemplateNameValidator"));
            }
        }
    }


}
