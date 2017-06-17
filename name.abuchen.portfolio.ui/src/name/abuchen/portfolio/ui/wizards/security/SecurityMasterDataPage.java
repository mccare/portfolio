package name.abuchen.portfolio.ui.wizards.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.OnlineState;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.online.sync.PortfolioReportNet;
import name.abuchen.portfolio.online.sync.PortfolioReportNet.OnlineItem;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.ui.util.BindingHelper.CurrencyUnitToStringConverter;
import name.abuchen.portfolio.ui.util.BindingHelper.StringToCurrencyUnitConverter;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.FormDataFactory;

public class SecurityMasterDataPage extends AbstractPage
{
    private final EditSecurityModel model;
    private final BindingHelper bindings;

    private Color defaultBackground;

    private Button buttonSync;
    private Control isin;
    private Control tickerSymbol;
    private Control wkn;

    protected SecurityMasterDataPage(EditSecurityModel model, BindingHelper bindings)
    {
        this.model = model;
        this.bindings = bindings;

        setTitle(Messages.EditWizardMasterDataTitle);
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);

        FormLayout layout = new FormLayout();
        layout.marginWidth = 5;
        layout.marginHeight = 5;
        container.setLayout(layout);

        Group grpIdentificationCodes = new Group(container, SWT.NONE);
        grpIdentificationCodes.setText("Identification Codes");
        grpIdentificationCodes.setLayout(new RowLayout(SWT.HORIZONTAL));
        addIdentificationCodesControls(grpIdentificationCodes);

        Group grpCurrency = new Group(container, SWT.NONE);
        grpCurrency.setText(Messages.ColumnCurrency);
        grpCurrency.setLayout(new RowLayout(SWT.VERTICAL));
        addCurrencyControls(grpCurrency);

        Group grpOthers = new Group(container, SWT.NONE);
        grpOthers.setText("Sonstige");
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(grpOthers);
        addOthersControls(grpOthers);

        FormDataFactory.startingWith(grpIdentificationCodes).left(new FormAttachment(0, 0))
                        .right(new FormAttachment(100, 0)) //
                        .thenBelow(grpCurrency).left(new FormAttachment(0, 0)).right(new FormAttachment(100, 0)) //
                        .thenBelow(grpOthers).right(new FormAttachment(100, 0));
    }

    private void addIdentificationCodesControls(Group grpIdentificationCodes)
    {
        Composite left = new Composite(grpIdentificationCodes, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(left);

        isin = bindings.bindISINInput(left, Messages.ColumnISIN, "isin"); //$NON-NLS-1$
        tickerSymbol = bindings.bindStringInput(left, Messages.ColumnTicker, "tickerSymbol", SWT.NONE, 12).getControl(); //$NON-NLS-1$
        wkn = bindings.bindStringInput(left, Messages.ColumnWKN, "wkn", SWT.NONE, 12).getControl(); //$NON-NLS-1$

        this.defaultBackground = isin.getBackground();

        Composite right = new Composite(grpIdentificationCodes, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(right);
        buttonSync = new Button(right, SWT.PUSH);
        buttonSync.setImage(Images.SYNC.image());
        buttonSync.setText("Sync");
        buttonSync.setToolTipText("Check if we can complete identification codes.");
        buttonSync.setFont(right.getFont());
        buttonSync.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                runOnlineSync();
            }
        });
    }

    private void addCurrencyControls(Group grpCurrency)
    {
        ComboViewer currencyCode = new ComboViewer(grpCurrency, SWT.READ_ONLY);
        currencyCode.setContentProvider(ArrayContentProvider.getInstance());
        currencyCode.setLabelProvider(new LabelProvider());

        List<CurrencyUnit> currencies = new ArrayList<>();
        currencies.add(CurrencyUnit.EMPTY);
        currencies.addAll(CurrencyUnit.getAvailableCurrencyUnits().stream().sorted().collect(Collectors.toList()));
        currencyCode.setInput(currencies);

        UpdateValueStrategy targetToModel = new UpdateValueStrategy();
        targetToModel.setConverter(new CurrencyUnitToStringConverter());

        UpdateValueStrategy modelToTarget = new UpdateValueStrategy();
        modelToTarget.setConverter(new StringToCurrencyUnitConverter());

        bindings.getBindingContext().bindValue(ViewersObservables.observeSingleSelection(currencyCode), //
                        BeanProperties.value("currencyCode").observe(model), targetToModel, modelToTarget); //$NON-NLS-1$

        if (model.getSecurity().hasTransactions(model.getClient()))
        {
            currencyCode.getCombo().setEnabled(false);

            Composite info = new Composite(grpCurrency, SWT.NONE);
            info.setLayout(new RowLayout());

            Label l = new Label(info, SWT.NONE);
            l.setImage(Images.INFO.image());

            l = new Label(info, SWT.NONE);
            l.setText(Messages.MsgInfoChangingCurrencyNotPossible);
        }
    }

    private void addOthersControls(Group grpOthers)
    {
        Control control = bindings.bindBooleanInput(grpOthers, Messages.ColumnRetired, "retired"); //$NON-NLS-1$
        Image image = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION)
                        .getImage();
        ControlDecoration deco = new ControlDecoration(control, SWT.TOP | SWT.LEFT);
        deco.setDescriptionText(Messages.MsgInfoRetiredSecurities);
        deco.setImage(image);
        deco.show();

        bindings.bindStringInput(grpOthers, Messages.ColumnNote, "note"); //$NON-NLS-1$

        // FIXME remove debug field
        bindings.bindStringInput(grpOthers, "Status", "onlineStateString").getControl().setEnabled(false);
    }

    protected void runOnlineSync()
    {
        Security security = new Security();
        model.setAttributes(security);

        buttonSync.setEnabled(false);

        new Job(security.getName())
        {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                try
                {
                    Optional<OnlineItem> onlineItem = security.getOnlineId() != null
                                    ? new PortfolioReportNet().getUpdatedValues(security)
                                    : new PortfolioReportNet().findMatch(security);

                    if (onlineItem.isPresent())
                    {
                        OnlineItem o = onlineItem.get();
                        Display.getDefault().asyncExec(() -> {

                            model.setOnlineId(o.getId());

                            updateProperty(null, OnlineState.Property.NAME, model.getName(), o.getName(),
                                            model::syncName);
                            updateProperty(isin, OnlineState.Property.ISIN, model.getIsin(), o.getIsin(),
                                            model::syncIsin);
                            updateProperty(tickerSymbol, OnlineState.Property.TICKER, model.getTickerSymbol(),
                                            o.getTicker(), model::syncTickerSymbol);
                            updateProperty(wkn, OnlineState.Property.WKN, model.getWkn(), o.getWkn(), model::syncWkn);

                            model.triggerOnlineStateChange();
                        });
                    }

                }
                catch (IOException e)
                {
                    PortfolioPlugin.log(e);
                }
                finally
                {
                    Display.getDefault().asyncExec(() -> buttonSync.setEnabled(true));
                }

                return Status.OK_STATUS;
            }

        }.schedule();
    }

    private void updateProperty(Control control, OnlineState.Property property, String oldValue, String newValue,
                    Consumer<String> setter)
    {
        if (control != null)
            control.setBackground(this.defaultBackground);

        // if newValue (= remote value) is empty, do not set existing values
        if (newValue == null || newValue.isEmpty())
        {
            model.getOnlineState().setState(property, oldValue == null || oldValue.isEmpty() ? OnlineState.State.SYNCED
                            : OnlineState.State.CUSTOM);
            return;
        }

        // if new value is identical to old value, set the status to sync
        if (newValue.equals(oldValue))
        {
            model.getOnlineState().setState(property, OnlineState.State.SYNCED);
            return;
        }

        OnlineState.State state = model.getOnlineState().getState(property);

        switch (state)
        {
            case BLANK:
            case SYNCED:
                setter.accept(newValue);
                model.getOnlineState().setState(property, OnlineState.State.SYNCED);

                if (control != null)
                    control.setBackground(Colors.WARNING);

                break;
            case CUSTOM:
            case EDITED:
                model.getOnlineState().setState(property, OnlineState.State.CUSTOM);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }
}
