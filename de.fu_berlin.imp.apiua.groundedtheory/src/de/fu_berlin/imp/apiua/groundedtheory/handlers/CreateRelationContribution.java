package de.fu_berlin.imp.apiua.groundedtheory.handlers;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.apache.log4j.Logger;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.bkahlert.nebula.dialogs.RenameDialog;
import com.bkahlert.nebula.utils.Pair;
import com.bkahlert.nebula.utils.Triple;
import com.bkahlert.nebula.utils.selection.SelectionUtils;

import de.fu_berlin.imp.apiua.core.model.URI;
import de.fu_berlin.imp.apiua.core.services.ILabelProviderService;
import de.fu_berlin.imp.apiua.groundedtheory.model.ICode;
import de.fu_berlin.imp.apiua.groundedtheory.model.ICodeInstance;
import de.fu_berlin.imp.apiua.groundedtheory.model.IRelation;
import de.fu_berlin.imp.apiua.groundedtheory.preferences.SUAGTPreferenceUtil;
import de.fu_berlin.imp.apiua.groundedtheory.services.ICodeService;
import de.fu_berlin.imp.apiua.groundedtheory.ui.ImageManager;

public class CreateRelationContribution extends ContributionItem {

	private static final Logger LOGGER = Logger
			.getLogger(CreateRelationContribution.class);

	private ILabelProviderService labelProviderService;
	private ICodeService codeService;

	public CreateRelationContribution() {
		this.init();
	}

	public CreateRelationContribution(String id) {
		super(id);
		this.init();
	}

	private void init() {
		this.labelProviderService = (ILabelProviderService) PlatformUI
				.getWorkbench().getService(ILabelProviderService.class);
		this.codeService = (ICodeService) PlatformUI.getWorkbench().getService(
				ICodeService.class);
	}

	@Override
	public void fill(Menu menu, int index) {
		ISelection selection = SelectionUtils.getSelection();

		List<ICode> codes = SelectionUtils.getAdaptableObjects(selection,
				ICode.class);
		List<URI> uris = SelectionUtils.getAdaptableObjects(selection,
				URI.class);

		List<Pair<URI, URI>> creatableRelations = new LinkedList<>();
		List<Pair<URI, IRelation>> groundableRelations = new LinkedList<>();
		List<Triple<URI, URI, URI>> creatableGroundedRelations = new LinkedList<>();

		if (codes.size() >= 2) {
			for (ICode code1 : codes) {
				for (ICode code2 : codes) {
					if (code1.equals(code2)) {
						continue;
					}
					creatableRelations.add(new Pair<>(code1.getUri(), code2
							.getUri()));
				}
			}
		}

		List<ICodeInstance> codeInstances = new LinkedList<>();
		if (uris.size() == 1) {
			URI uri = uris.get(0);
			codeInstances = this.codeService.getAllInstances(uri);
			for (ICodeInstance codeInstance1 : codeInstances) {
				for (ICodeInstance codeInstance2 : codeInstances) {
					URI fromUri = codeInstance1.getCode().getUri();
					URI toUri = codeInstance2.getCode().getUri();
					if (fromUri.equals(toUri)) {
						continue;
					}

					creatableGroundedRelations.add(new Triple<>(uri, fromUri,
							toUri));

					for (IRelation relation : this.codeService
							.getAllRelations()) {
						if (relation.getFrom().equals(fromUri)
								&& relation.getTo().equals(toUri)) {
							groundableRelations.add(new Pair<URI, IRelation>(
									uri, relation));
						}
					}
				}
			}
		}

		if (codeInstances.size() == 0 && creatableRelations.size() == 0
				&& groundableRelations.size() == 0
				&& creatableGroundedRelations.size() == 0) {
			return;
		}

		MenuItem createRelationItem = new MenuItem(menu, SWT.CASCADE, index);
		createRelationItem.setText("Create Relation");
		createRelationItem.setImage(ImageManager.RELATION);
		final Menu createRelationSubMenu = new Menu(createRelationItem);
		createRelationItem.setMenu(createRelationSubMenu);

		if (true) {
			this.createSpacer(createRelationSubMenu, "Recent:");
			Pair<Boolean, URI> lastCreatedRelation = new SUAGTPreferenceUtil()
					.getLastCreatedRelation();
			if (lastCreatedRelation != null) {
				URI uri1 = lastCreatedRelation.getSecond();
				String name1 = this.labelProviderService
						.getText(lastCreatedRelation.getSecond());

				for (ICodeInstance codeInstance : codeInstances) {
					URI uri2 = codeInstance.getCode().getUri();
					String name2 = this.labelProviderService
							.getText(codeInstance.getCode().getUri());

					URI from;
					URI to;
					String fromName;
					String toName;
					if (lastCreatedRelation.getFirst()) {
						from = uri2;
						to = uri1;
						fromName = name2;
						toName = name1;
					} else {
						from = uri1;
						to = uri2;
						fromName = name1;
						toName = name2;
					}
					MenuItem menuItem = new MenuItem(createRelationSubMenu,
							SWT.PUSH);
					menuItem.setText(fromName + " → " + toName);
					menuItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							IRelation relation = CreateRelationContribution.this
									.createRelation(menu.getShell(), from, to);
							CreateRelationContribution.this.groundRelation(
									null, codeInstance.getId(), relation);
						}
					});
				}
			}
		}

		if (creatableRelations.size() > 0) {
			this.createSpacer(createRelationSubMenu, "New Relation:");

			for (Pair<URI, URI> creatableRelation : creatableRelations) {
				MenuItem menuItem = new MenuItem(createRelationSubMenu,
						SWT.PUSH);

				String fromName = this.labelProviderService
						.getText(creatableRelation.getFirst());
				String toName = this.labelProviderService
						.getText(creatableRelation.getSecond());

				menuItem.setText(fromName + " → " + toName);
				menuItem.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						CreateRelationContribution.this.createRelation(
								menu.getShell(), creatableRelation.getFirst(),
								creatableRelation.getSecond());
					}
				});
			}
		}

		if (creatableGroundedRelations.size() > 0) {
			this.createSpacer(createRelationSubMenu, "New Grounded Relation:");

			URI lastFrom = null;
			Menu currentMenu = null;
			for (Triple<URI, URI, URI> creatableGroundedRelation : creatableGroundedRelations) {

				String fromName = this.labelProviderService
						.getText(creatableGroundedRelation.getSecond());
				String toName = this.labelProviderService
						.getText(creatableGroundedRelation.getThird());

				if (!ObjectUtils.equals(lastFrom,
						creatableGroundedRelation.getSecond())) {
					lastFrom = creatableGroundedRelation.getSecond();
					MenuItem fromMenuItem = new MenuItem(createRelationSubMenu,
							SWT.CASCADE);
					fromMenuItem.setText(fromName);
					currentMenu = new Menu(fromMenuItem);
					fromMenuItem.setMenu(currentMenu);
				}

				MenuItem menuItem = new MenuItem(currentMenu, SWT.PUSH);
				menuItem.setText(toName);
				menuItem.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						IRelation relation = CreateRelationContribution.this
								.createRelation(menu.getShell(),
										creatableGroundedRelation.getSecond(),
										creatableGroundedRelation.getThird());
						if (relation != null) {
							CreateRelationContribution.this.groundRelation(
									menu.getShell(),
									creatableGroundedRelation.getFirst(),
									relation);
						}
					}
				});

				if (this.codeService.isExplicitlyGrounded(
						creatableGroundedRelation.getFirst(),
						creatableGroundedRelation.getSecond(),
						creatableGroundedRelation.getThird())) {
					menuItem.setText(menuItem.getText() + " (already grounded)");
					menuItem.setEnabled(false);
				} else if (this.codeService.getExplicitRelations(
						creatableGroundedRelation.getSecond(),
						creatableGroundedRelation.getThird()).size() > 0) {
					menuItem.setText(menuItem.getText() + " (already exists)");
					menuItem.setEnabled(false);
				}
			}
		}

		if (groundableRelations.size() > 0) {
			this.createSpacer(createRelationSubMenu, "Ground Relation:");

			for (Pair<URI, IRelation> groundableRelation : groundableRelations) {
				MenuItem menuItem = new MenuItem(createRelationSubMenu,
						SWT.PUSH);
				String fromName = this.labelProviderService
						.getText(groundableRelation.getSecond().getFrom());
				String toName = this.labelProviderService
						.getText(groundableRelation.getSecond().getTo());

				menuItem.setText(fromName + " → "
						+ groundableRelation.getSecond().getName() + " → "
						+ toName);
				menuItem.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						CreateRelationContribution.this.groundRelation(
								menu.getShell(), groundableRelation.getFirst(),
								groundableRelation.getSecond());
					}
				});

				if (this.codeService.isExplicitlyGrounded(groundableRelation.getFirst(),
						groundableRelation.getSecond())) {
					menuItem.setText(menuItem.getText() + " (already grounded)");
					menuItem.setEnabled(false);
				}
			}
		}

		if (codeInstances.size() > 0) {
			this.fillFromMenu(codeInstances, createRelationSubMenu);
			this.fillToMenu(codeInstances, createRelationSubMenu);
		}
	}

	private void createSpacer(Menu menu, String text) {
		if (menu.getItemCount() > 0) {
			new MenuItem(menu, SWT.SEPARATOR);
		}
		MenuItem item = new MenuItem(menu, SWT.NONE);
		item.setEnabled(false);
		item.setText(text);
	}

	private void fillFromMenu(List<ICodeInstance> codeInstances, Menu menu) {
		if (codeInstances.size() == 0) {
			return;
		}

		this.createSpacer(menu, "From Here:");

		for (ICodeInstance codeInstance : codeInstances) {
			MenuItem menuItem = new MenuItem(menu, SWT.CASCADE);
			menuItem.setText(this.labelProviderService.getText(codeInstance
					.getCode().getUri()));
			final Menu subMenu = new Menu(menuItem);
			menuItem.setMenu(subMenu);
			this.addFullMenu(codeInstance.getCode().getUri(), null,
					codeInstance.getId(), subMenu);
		}
	}

	private void fillToMenu(List<ICodeInstance> codeInstances, Menu menu) {
		if (codeInstances.size() == 0) {
			return;
		}

		this.createSpacer(menu, "To Here:");

		for (ICodeInstance codeInstance : codeInstances) {
			MenuItem menuItem = new MenuItem(menu, SWT.CASCADE);
			menuItem.setText(this.labelProviderService.getText(codeInstance
					.getCode().getUri()));
			final Menu subMenu = new Menu(menuItem);
			menuItem.setMenu(subMenu);
			this.addFullMenu(null, codeInstance.getCode().getUri(),
					codeInstance.getId(), subMenu);
		}
	}

	private void addFullMenu(URI from, URI to, URI phenomenon, Menu menu) {
		for (ICode code : this.codeService.getTopLevelCodes()) {
			this.addFullMenu(from, to, phenomenon, menu, code);
		}
	}

	private void addFullMenu(URI from, URI to, URI phenomenon, Menu menu,
			ICode parent) {
		String text = this.labelProviderService.getText(parent.getUri());
		Image image = this.labelProviderService.getImage(parent.getUri());
		SelectionListener selectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				new SUAGTPreferenceUtil().setLastCreatedRelation(from != null,
						parent.getUri());
				IRelation relation = CreateRelationContribution.this
						.createRelation(null,
								from != null ? from : parent.getUri(),
								to != null ? to : parent.getUri());
				if (phenomenon != null) {
					CreateRelationContribution.this.groundRelation(null,
							phenomenon, relation);
				}
			}
		};
		List<ICode> children = this.codeService.getChildren(parent);

		MenuItem menuItem = new MenuItem(menu,
				children.size() > 0 ? SWT.CASCADE : SWT.NONE);
		menuItem.setText(text);
		menuItem.setImage(image);
		menuItem.addSelectionListener(selectionListener);

		if (children.size() > 0) {
			final Menu subMenu = new Menu(menuItem);
			menuItem.setMenu(subMenu);

			MenuItem subMenuItem = new MenuItem(subMenu, SWT.NONE);
			subMenuItem.setText(text);
			subMenuItem.setImage(image);
			subMenuItem.addSelectionListener(selectionListener);

			new MenuItem(subMenu, SWT.SEPARATOR);
			for (ICode code : children) {
				this.addFullMenu(from, to, phenomenon, subMenu, code);
			}
		}
	}

	private IRelation createRelation(Shell shell, URI from, URI to) {
		SUAGTPreferenceUtil gt = new SUAGTPreferenceUtil();
		RenameDialog renameDialog = new RenameDialog(shell,
				gt.getLastCreatedRelationName());
		renameDialog.create();
		if (renameDialog.open() == Window.OK) {
			final String relationName = renameDialog.getCaption();
			gt.setLastCreatedRelationName(relationName);
			try {
				IRelation relation = CreateRelationContribution.this.codeService
						.createRelation(from, to, relationName);
				return relation;
			} catch (Exception e) {
				LOGGER.error("Error creating relation " + from + " → " + to);
			}
		}
		return null;
	}

	private void groundRelation(Shell shell, URI uri, IRelation relation) {
		try {
			CreateRelationContribution.this.codeService.createRelationInstance(
					uri, relation);
		} catch (Exception e) {
			LOGGER.error("Error grounding relation " + uri + " → " + relation);
		}
	}

}
