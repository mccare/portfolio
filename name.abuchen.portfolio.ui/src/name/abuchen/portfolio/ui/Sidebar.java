package name.abuchen.portfolio.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

public final class Sidebar extends Composite
{
    public static final class Entry
    {
        private Sidebar bar;
        private Entry parent;
        private List<Entry> children = new ArrayList<Entry>();

        private Item item;

        private Action action;

        private Entry(Sidebar sidebar)
        {
            bar = sidebar;
        }

        public Entry(Sidebar sidebar, String label)
        {
            bar = sidebar;
            bar.root.children.add(this);
            parent = bar.root;
            item = bar.createItem(this, label, 0);
        }

        public Entry(Entry parent, String label)
        {
            this.bar = parent.bar;
            parent.children.add(this);
            this.parent = parent;
            item = bar.createItem(this, label, parent.item.indent + STEP);
        }

        public Entry(Entry parent, Action action)
        {
            this(parent, action.getText());
            setAction(action);
        }

        public void setAction(Action action)
        {
            this.action = action;

            if (action.getImageDescriptor() != null)
                item.setImage(action.getImageDescriptor().createImage(true));
        }

        public void setContextMenu(IMenuListener listener)
        {
            item.addContextMenu(listener);
        }

        private Entry findPeer(int direction)
        {
            if (parent == null)
                return null;

            int index = parent.children.indexOf(this);

            if (direction == SWT.ARROW_DOWN && parent.children.size() > index + 1)
                return parent.children.get(index + 1);
            else if (direction == SWT.ARROW_UP && index > 0)
                return parent.children.get(index - 1);
            else
                return null;
        }

        private Entry findNeighbor(int direction)
        {
            if (direction == SWT.ARROW_DOWN)
            {
                if (!children.isEmpty())
                    return children.get(0);

                Entry e = this;
                while (e != null)
                {
                    Entry peer = e.findPeer(SWT.ARROW_DOWN);
                    if (peer != null)
                        return peer;
                    else
                        e = e.parent;
                }
            }
            else if (direction == SWT.ARROW_UP)
            {
                if (parent == null)
                    return null;

                Entry peer = findPeer(SWT.ARROW_UP);
                if (peer == null)
                    return parent;

                while (true)
                {
                    if (peer.children.isEmpty())
                        return peer;
                    peer = peer.children.get(peer.children.size() - 1);
                }
            }

            return null;
        }

        public boolean isSelectable()
        {
            return item != null && item.indent > 0 && action != null;
        }

        public void setLabel(String label)
        {
            item.text = label;
            item.redraw();
        }

        public void dispose()
        {
            Entry down = findNeighbor(SWT.ARROW_DOWN);

            if (down != null)
            {
                Entry up = findNeighbor(SWT.ARROW_UP);
                if (up != bar.root)
                {
                    FormData data = (FormData) down.item.getLayoutData();
                    data.top = new FormAttachment(up.item, down.item.indent == 0 ? 20 : 0);
                }
            }

            parent.children.remove(this);

            if (bar.selection == this)
                bar.selection = bar.root;

            item.dispose();
            bar.layout();
        }

        public void select()
        {
            bar.select(this);
        }
    }

    private static final int STEP = 15;

    private Color backgroundColor;
    private Color selectedColor;
    private Color lighterSelectedColor;
    private Color sectionColor;

    private Font regularFont;
    private Font boldFont;
    private Font sectionFont;

    private Entry root = null;
    private Entry selection = null;

    public Sidebar(Composite parent, int style)
    {
        super(parent, style);

        root = selection = new Entry(this);

        setLayout(new FormLayout());

        createColorsAndFonts(parent);
        registerListeners();
    }

    public void select(Entry entry)
    {
        Entry oldSelection = selection;
        selection = entry;

        if (oldSelection != null && oldSelection.item != null)
            oldSelection.item.redraw();

        if (selection.item != null)
            selection.item.redraw();

        this.setFocus();

        entry.action.run();
    }

    //
    // listener implementations
    //

    private void registerListeners()
    {
        addDisposeListener(new DisposeListener()
        {
            public void widgetDisposed(DisposeEvent e)
            {
                Sidebar.this.widgetDisposed(e);
            }
        });

        addPaintListener(new PaintListener()
        {
            public void paintControl(PaintEvent e)
            {
                Sidebar.this.paintControl(e);
            }
        });

        addKeyListener(new KeyListener()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {}

            @Override
            public void keyPressed(KeyEvent e)
            {
                Sidebar.this.keyPressed(e);
            }
        });

        addTraverseListener(new TraverseListener()
        {
            @Override
            public void keyTraversed(TraverseEvent e)
            {
                Sidebar.this.keyTraversed(e);
            }
        });
    }

    private void widgetDisposed(DisposeEvent e)
    {
        backgroundColor.dispose();
        selectedColor.dispose();
        lighterSelectedColor.dispose();
        sectionColor.dispose();

        regularFont.dispose();
        boldFont.dispose();
        sectionFont.dispose();
    }

    private void paintControl(PaintEvent e)
    {
        GC gc = e.gc;
        gc.setForeground(backgroundColor);
        gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        Rectangle r = getClientArea();
        gc.fillGradientRectangle(0, 0, r.width, r.height, false);
    }

    private void keyPressed(KeyEvent e)
    {
        if (e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN)
        {
            Entry entry = selection.findNeighbor(e.keyCode);
            while (entry != null && !entry.isSelectable())
                entry = entry.findNeighbor(e.keyCode);

            if (entry != null)
                select(entry);
        }
    }

    private void keyTraversed(TraverseEvent e)
    {
        if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS)
        {
            e.doit = true;
        }
    }

    //
    // item implementation
    //

    private void createColorsAndFonts(Composite parent)
    {
        backgroundColor = new Color(null, 233, 241, 248);
        selectedColor = new Color(null, 115, 158, 227);
        lighterSelectedColor = new Color(null, 189, 208, 241);
        sectionColor = new Color(null, 149, 165, 180);

        FontData fontData = parent.getFont().getFontData()[0];
        regularFont = new Font(Display.getDefault(), fontData);
        fontData.setStyle(SWT.BOLD);
        boldFont = new Font(Display.getDefault(), fontData);

        if (!Platform.OS_MACOSX.equals(Platform.getOS()))
            fontData.setHeight(fontData.getHeight() - 1);

        sectionFont = new Font(Display.getDefault(), fontData);
    }

    private Item createItem(Entry entry, String label, int indent)
    {
        Item l = new Item(this, entry);
        l.setText(label);
        l.setIndent(indent);

        FormData data = new FormData();
        data.left = new FormAttachment(0);
        data.right = new FormAttachment(100);

        Entry up = entry.findNeighbor(SWT.ARROW_UP);
        if (up == root)
            data.top = new FormAttachment(0, 5);
        else
            data.top = new FormAttachment(up.item, indent == 0 ? 20 : 0);
        l.setLayoutData(data);

        Entry down = entry.findNeighbor(SWT.ARROW_DOWN);
        if (down != null)
        {
            data = (FormData) down.item.getLayoutData();
            data.top = new FormAttachment(l, down.item.indent == 0 ? 20 : 0);
        }

        return l;
    }

    private void action(Entry entry)
    {
        if (entry.action != null)
            entry.action.run();
    }

    //
    // item widget
    //

    private class Item extends Canvas
    {
        private static final int MARGIN_X = 5;
        private static final int MARGIN_Y = 3;

        private final Entry entry;

        private int indent;
        private String text;
        private Image image;

        private Menu contextMenu;

        public Item(Composite parent, Entry entry)
        {
            super(parent, SWT.NO_BACKGROUND | SWT.NO_FOCUS);
            this.entry = entry;

            addDisposeListener(new DisposeListener()
            {
                public void widgetDisposed(DisposeEvent e)
                {
                    Item.this.widgetDisposed(e);
                }
            });

            addPaintListener(new PaintListener()
            {
                public void paintControl(PaintEvent e)
                {
                    Item.this.paintControl(e);
                }
            });

            addKeyListener(new KeyListener()
            {
                @Override
                public void keyReleased(KeyEvent e)
                {}

                @Override
                public void keyPressed(KeyEvent e)
                {
                    if (e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN)
                        Sidebar.this.keyPressed(e);
                }
            });

            addTraverseListener(new TraverseListener()
            {
                @Override
                public void keyTraversed(TraverseEvent e)
                {
                    Sidebar.this.keyTraversed(e);
                }
            });

            addMouseListener(new MouseAdapter()
            {
                public void mouseDown(MouseEvent event)
                {
                    if (event.button == 1)
                    {
                        if (indent > 0)
                        {
                            Sidebar.this.select(Item.this.entry);
                        }
                        else if (indent == 0 && Item.this.entry.action != null)
                        {
                            boolean doIt = true;

                            if (image != null)
                            {
                                Rectangle clientArea = getClientArea();
                                Rectangle imgBounds = image.getBounds();

                                doIt = (event.x >= clientArea.width - imgBounds.width - MARGIN_X)
                                                && (event.x <= clientArea.width - MARGIN_X);
                            }

                            if (doIt)
                                Sidebar.this.action(Item.this.entry);
                        }
                    }
                }
            });
        }

        public void addContextMenu(IMenuListener listener)
        {
            if (contextMenu != null)
                contextMenu.dispose();

            MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
            menuMgr.setRemoveAllWhenShown(true);
            menuMgr.addMenuListener(listener);

            contextMenu = menuMgr.createContextMenu(this);
            setMenu(contextMenu);
        }

        private void widgetDisposed(DisposeEvent e)
        {
            if (image != null)
                image.dispose();

            if (contextMenu != null)
                contextMenu.dispose();
        }

        public void setIndent(int indent)
        {
            this.indent = indent;
        }

        public void setText(String text)
        {
            this.text = text;
        }

        public void setImage(Image image)
        {
            this.image = image;
        }

        public Point computeSize(int wHint, int hHint, boolean changed)
        {
            int width = 0, height = 0;
            if (text != null)
            {
                GC gc = new GC(this);
                Point extent = gc.stringExtent(text);
                gc.dispose();
                width += extent.x;
                height = Math.max(height, extent.y);
            }
            if (image != null)
            {
                width += image.getBounds().width + 5;
            }
            return new Point(width + 2 + (2 * MARGIN_X) + indent, height + 2 + (2 * MARGIN_Y));
        }

        private void paintControl(PaintEvent e)
        {
            GC gc = e.gc;

            Color oldBackground = gc.getBackground();
            Color oldForeground = gc.getForeground();

            Rectangle bounds = getClientArea();

            if (this == Sidebar.this.selection.item)
            {
                gc.setForeground(lighterSelectedColor);
                gc.setBackground(selectedColor);
                gc.fillGradientRectangle(bounds.x, bounds.y, bounds.width, bounds.height, true);

                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
                gc.setFont(boldFont);
            }
            else
            {
                gc.setForeground(backgroundColor);
                gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
                gc.fillGradientRectangle(bounds.x, bounds.y, bounds.width, bounds.height, false);

                if (indent > 0)
                {
                    gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
                    gc.setFont(regularFont);
                }
                else
                {
                    gc.setForeground(sectionColor);
                    gc.setFont(sectionFont);
                }
            }

            int x = bounds.x + MARGIN_X + indent;
            if (image != null)
            {
                Rectangle imgBounds = image.getBounds();
                if (indent == 0)
                {
                    gc.drawImage(image, bounds.width - imgBounds.width - MARGIN_X,
                                    (bounds.height - imgBounds.height) / 2);
                }
                else
                {
                    gc.drawImage(image, x, bounds.y + MARGIN_Y);
                    x += imgBounds.width + 5;
                }
            }

            gc.drawText(text, x, bounds.y + MARGIN_Y, true);

            gc.setBackground(oldBackground);
            gc.setForeground(oldForeground);
        }
    }
}