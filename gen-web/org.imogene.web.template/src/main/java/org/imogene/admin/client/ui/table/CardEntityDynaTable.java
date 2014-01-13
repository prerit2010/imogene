package org.imogene.admin.client.ui.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.imogene.admin.client.AdminRenderer;
import org.imogene.admin.client.event.create.CreateCardEntityEvent;
import org.imogene.admin.client.event.list.ListCardEntityEvent;
import org.imogene.admin.client.event.view.ViewCardEntityEvent;
import org.imogene.admin.client.i18n.AdminNLS;
import org.imogene.admin.client.ui.filter.CardEntityFilterPanel;
import org.imogene.admin.shared.AdminRequestFactory;
import org.imogene.admin.shared.request.CardEntityRequest;
import org.imogene.web.client.event.SelectionChangedInTableEvent;
import org.imogene.web.client.i18n.BaseNLS;
import org.imogene.web.client.ui.table.ImogBeanDataProvider;
import org.imogene.web.client.ui.table.ImogColumn;
import org.imogene.web.client.ui.table.ImogDynaTable;
import org.imogene.web.client.ui.table.filter.ImogFilterPanel;
import org.imogene.web.client.util.ProfileUtil;
import org.imogene.web.shared.proxy.CardEntityProxy;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PushButton;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.ServerFailure;

/**
 * Composite that displays the list of CardEntity entries
 * 
 * @author MEDES-IMPS
 */
public class CardEntityDynaTable extends ImogDynaTable<CardEntityProxy> {

	private List<HandlerRegistration> registrations = new ArrayList<HandlerRegistration>();

	private PushButton deleteButton;

	public CardEntityDynaTable(AdminRequestFactory requestFactory, ImogBeanDataProvider<CardEntityProxy> provider,
			boolean checkBoxesVisible) {
		super(requestFactory, provider, checkBoxesVisible);
	}

	public ImogFilterPanel getFilterPanel() {
		ImogFilterPanel filterPanel = new CardEntityFilterPanel();
		super.configureFilterPanel(filterPanel);
		return filterPanel;
	}

	/**
	 * 
	 */
	@Override
	protected void setColumns() {

		if (ProfileUtil.isAdmin()) {
			Column<CardEntityProxy, String> nameColumn = new NameColumn();
			nameColumn.setSortable(true);
			table.addColumn(nameColumn, AdminNLS.constants().cardEntity_name());
		}

	}

	@Override
	protected GwtEvent<?> getViewEvent(CardEntityProxy value) {
		return new ViewCardEntityEvent(value.getId());
	}

	@Override
	protected String getDefaultSortProperty() {
		return "modified";
	}

	@Override
	protected boolean getDefaultSortPropertyOrder() {
		return false;
	}

	/**
	 * Creates the Create action command for the entity
	 * 
	 * @return the create command
	 */
	public Command getCreateCommand() {

		if (ProfileUtil.isAdmin()) {
			Command command = new Command() {
				public void execute() {
					requestFactory.getEventBus().fireEvent(new CreateCardEntityEvent());
				}
			};
			return command;
		} else
			return null;
	}

	/**
	 * Creates the Delete action command for the entity
	 * 
	 * @return the delete command
	 */
	public PushButton getDeleteButton() {

		if (ProfileUtil.isAdmin()) {
			deleteButton = new PushButton(BaseNLS.constants().button_delete());
			deleteButton.setStyleName(imogResources.imogStyle().imogButton());
			deleteButton.addStyleName("Dynatable-Button");
			deleteButton.setVisible(false);
			return deleteButton;
		}

		return null;
	}

	/**
	 * Creates the Handlers linked to the delete button
	 */
	private void setDeleteButtonHandlers() {

		if (ProfileUtil.isAdmin()) {

			// Click handler
			registrations.add(deleteButton.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {

					Set<CardEntityProxy> selectedEntities = selectionModel.getSelectedSet();

					int count = selectedEntities.size();
					if (count > 0) {

						AdminRenderer renderer = AdminRenderer.get();

						StringBuffer msg = new StringBuffer();
						msg.append(BaseNLS.constants().confirmation_delete_several1() + " "
								+ AdminNLS.constants().cardEntity_name() + " "
								+ BaseNLS.constants().confirmation_delete_several2() + ": ");
						int i = 0;
						for (CardEntityProxy entity : selectedEntities) {
							if (count == 1 || i == count - 1)
								msg.append("'" + renderer.getDisplayValue(entity) + "' ?");
							else
								msg.append("'" + renderer.getDisplayValue(entity) + "', ");
							i = i + 1;
						}

						boolean toDelete = Window.confirm(msg.toString());
						if (toDelete) {

							Request<Void> deleteRequest = getCardEntityRequest().delete(selectedEntities);
							deleteRequest.fire(new Receiver<Void>() {
								@Override
								public void onSuccess(Void response) {
									// Window.alert("The selected CardEntity entries have been deleted");
									requestFactory.getEventBus().fireEvent(new ListCardEntityEvent());
								}

								@Override
								public void onFailure(ServerFailure error) {
									Window.alert("Error deleting the CardEntity entries");
									super.onFailure(error);
								}
							});
						}
					}

				}
			}));

			// Selection changed handler
			registrations.add(requestFactory.getEventBus().addHandler(SelectionChangedInTableEvent.TYPE,
					new SelectionChangedInTableEvent.Handler() {
						@Override
						public void noticeSelectionChange(int selectedItems) {
							if (selectedItems > 0)
								deleteButton.setVisible(true);
							else
								deleteButton.setVisible(false);
						}
					}));
		}
	}

	/**
	 * Creates the action command that enables to export the CardEntity entries in a csv file
	 * 
	 * @return the command
	 */
	public Command getCsvExportButton() {
		return null;
	}

	private CardEntityRequest getCardEntityRequest() {
		AdminRequestFactory AdminRequestFactory = (AdminRequestFactory) requestFactory;
		return AdminRequestFactory.cardEntityRequest();
	}

	@Override
	protected void onUnload() {
		for (HandlerRegistration r : registrations)
			r.removeHandler();
		registrations.clear();
		super.onUnload();
	}

	@Override
	protected void onLoad() {
		setDeleteButtonHandlers();
		super.onLoad();
	}

	/**
	 * --------------------- * Internal classes * ----------------------
	 */

	/**
	 * Column for field Name
	 * 
	 * @author MEDES-IMPS
	 */
	private class NameColumn extends ImogColumn<CardEntityProxy, String> {

		public NameColumn() {
			super(new TextCell());
		}

		@Override
		public String getValue(CardEntityProxy object) {
			String value = null;
			if (object != null) {
				if (object.getName() == null)
					value = "";
				else
					value = object.getName();
			}
			return value;
		}

		public String getPropertyName() {
			return "name";
		}
	}

}