package name.abuchen.portfolio.ui.handlers;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPart;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.wizards.datatransfer.ImportExtractedItemsWizard;

public class ImportPDFHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return MenuHelper.isClientPartActive(part);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell)
    {
        Client client = MenuHelper.getActiveClient(part);
        if (client == null)
            return;

        try
        {
            FileDialog fileDialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
            fileDialog.setText(Messages.PDFImportWizardAssistant);
            fileDialog.setFilterNames(new String[] { Messages.PDFImportFilterName });
            fileDialog.setFilterExtensions(new String[] { "*.pdf" }); //$NON-NLS-1$
            fileDialog.open();

            String[] filenames = fileDialog.getFileNames();

            if (filenames.length == 0)
                return;

            List<Extractor.InputFile> files = new ArrayList<>();
            for (String filename : filenames)
                files.add(new PDFInputFile(new File(fileDialog.getFilterPath(), filename)));

            IPreferenceStore preferences = ((PortfolioPart) part.getObject()).getPreferenceStore();

            IRunnableWithProgress operation = monitor -> {
                monitor.beginTask("Text auslesen...", files.size());

                for (Extractor.InputFile inputFile : files)
                {
                    monitor.setTaskName(inputFile.getName());

                    try
                    {
                        ((PDFInputFile) inputFile).parse();
                    }
                    catch (IOException e)
                    {
                        PortfolioPlugin.log(e);
                    }

                    monitor.worked(1);
                }

                Display.getDefault().asyncExec(() -> openWizard(client, files, preferences));
            };
            new ProgressMonitorDialog(shell).run(true, true, operation);

        }
        catch (IllegalArgumentException | InvocationTargetException | InterruptedException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(shell, Messages.LabelError, e.getMessage());
        }
    }

    protected void openWizard(Client client, List<Extractor.InputFile> files, IPreferenceStore preferences)
    {
        try
        {
            Dialog wizwardDialog = new WizardDialog(Display.getDefault().getActiveShell(),
                            new ImportExtractedItemsWizard(client, null, preferences, files));
            wizwardDialog.open();
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError, e.getMessage());
        }
    }
}
