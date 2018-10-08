package edu.asu.jmars.lmanager;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.basic.BasicMenuUI;
import javax.swing.plaf.metal.MetalComboBoxIcon;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.MultiFactory;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.Util;

public class MainPanel extends JLayeredPane {
	private static final boolean DARK_DRAG = Config.get("lmanager.darkdrag",false);
	private static final Color darkColor = UIManager.getColor("ToggleButton.select");
	private final Color lightBlue = UIManager.getColor("TabbedPane.selected");
	
	Row dragRow;
	int dragPos;
	int dragSrc;
	int dragDst;
	public List<Row> rows = new ArrayList<Row>();
	JMenuBar addBtn;
	JMenuBar editBar;
	JMenu editBtn;
	JButton addBtn1;
	JCheckBoxMenuItem tooltip, tooltipsItem;
	
	public JScrollPane rowScrollPane;
	public JLayeredPane rowsPanel = new JLayeredPane() {
		public void layout() {
			if (rows.isEmpty())
				return;

			Insets insets = rowsPanel.getInsets();
			int h = ((Row) rows.get(0)).getPreferredSize().height;
			int w = rowsPanel.getWidth() - insets.left - insets.right;

			for (int i = 0; i < rows.size(); i++) {
				Row r = (Row) rows.get(i);
				//Can be used to alternate row colors
//				if(i%2 == 0){
//					Color lightGray = new Color(186,186, 186);
//					r.setColor(lightGray);
//				} else{
//					Color standard = UIManager.getColor("Panel.background");
//					r.setColor(standard);
//				}
					
				int y;
				if (i == dragSrc)
					y = dragPos;
				else if (i > dragSrc && i <= dragDst)
					y = h * (i - 1);
				else if (i < dragSrc && i >= dragDst)
					y = h * (i + 1);
				else
					y = h * i;
				r.setSize(w, h);
				r.setLocation(insets.left, insets.top + y);
			}
		}

		public Dimension getPreferredSize() {
			int width = 0;
			int height = 0;
			Dimension size = new Dimension(width, height);

			if (rows.size() == 0)
				return size;
			int h = ((Row) rows.get(0)).getPreferredSize().height;
			height = rows.size() * h;
			width = 1;  // make it expand to take whatever space is available
			size.setSize(width, height);
			return size;
		}

	};

	public void delete(int selectedIdx) {
		// Remove the row... the row indices are reversed relative to everything else.
		Row r = (Row) rows.remove(rows.size() - 1 - selectedIdx);
		rowsPanel.remove(r);
		rowsPanel.repaint();
		if (rowScrollPane != null) {
			rowScrollPane.revalidate();
			rowScrollPane.repaint();
		}	
	}
	
	void buildEditMenu() {
		JMenuItem options = new JMenuItem(actOptions);
		options.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
		JMenuItem docked = new JMenuItem(actDocked);
		docked.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK));
		JMenuItem rename = new JMenuItem(actRename);
		JMenuItem delete = new JMenuItem(actDelete);
		delete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, KeyEvent.SHIFT_DOWN_MASK));
		tooltip = new JCheckBoxMenuItem(actTooltip);
		editBtn.add(options);
		editBtn.add(docked);
		editBtn.add(rename);
		editBtn.add(delete);
		editBtn.add(tooltip);

		editBtn.setUI(new BasicMenuUI() {
			protected void installDefaults() {
				super.installDefaults();
				selectionBackground = UIManager.getColor("Button.select");
			}
		});
		editBtn.setBorder(UIManager.getBorder("Button.border"));
		editBtn.setIcon(new MetalComboBoxIcon());

		editBar.removeAll();
		editBar.add(editBtn);
	}

	//
	//  OLD ADD LAYER METHOD
	//
	public void rebuildAddMenu() {
		JMenu menu = new JMenu("Add new layer");
		MultiFactory.addAllToMenu(menu, LViewFactory.factoryList2);

		menu.setUI(new BasicMenuUI() {
			protected void installDefaults() {
				super.installDefaults();
				selectionBackground = UIManager.getColor("Button.select");
				if(selectionBackground==null){
					selectionBackground = UIManager.getColor("TabbedPane.highlight");
					selectionForeground = Color.BLACK;
				}
			}
		});
		menu.setBorder(UIManager.getBorder("Button.border"));
		menu.setIcon(new MetalComboBoxIcon());

		if (Config.get("lmanager.flatten", false)) {
			menu.removeAll();
			Iterator i = LViewFactory.factoryList.iterator();
			while (i.hasNext())
				menu.add(new JMenuItem(i.next().toString()));
		}

		addBtn.removeAll();
		addBtn.add(menu);
	}

//Depending on whether user wants old or new style of adding layers
// a different component is returned and added.	
	private JComponent getAddBtn(){
		if (LManager.getLManager().addLayerStyle.equals("old")){
			addBtn = new JMenuBar();
			addBtn.setBorder(null);
			rebuildAddMenu();
			return addBtn;
		}
		else{
			addBtn1 = new JButton(actAdd);
			return addBtn1;
		}
	}
	
	public MainPanel() {
		JComponent addButton = getAddBtn();
		editBar = new JMenuBar();
		editBar.setBorder(null);
		editBtn = new JMenu("Edit Selected");
		editBtn.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				tooltip.setSelected(!LManager.getLManager().getTooltipDisabledStatus());
			}
		});
		editBar.add(editBtn);
		buildEditMenu();
		
		Dimension d1 = addButton.getPreferredSize();
		Dimension d2 = editBar.getPreferredSize();
		Dimension d = new Dimension(Math.max(d1.width, d2.width), Math.max(
				d1.height, d2.height));
		setAll(d, addButton);
		setAll(d, editBar.getMenu(0));
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.insets = new Insets(10, 5, 10, 5);
		c.weightx = 1;
		c.anchor = GridBagConstraints.CENTER;
		add(addButton, c);

		c.anchor = GridBagConstraints.WEST;
		add(editBar, c);

		c.gridy = 1;
		c.gridwidth = 2;
		c.insets = new Insets(0, 0, 8, 0);
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.BOTH;
		add(new JSeparator(), c);

		c.insets = new Insets(0, 5, 5, 5);
		c.gridy = 2;
		c.weighty = 2;

		rowsPanel.setBackground(lightBlue);
		rowsPanel.setOpaque(true);

		rowScrollPane = new JScrollPane(rowsPanel,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		rowScrollPane.getVerticalScrollBar().setUnitIncrement(20);
		
		add(rowScrollPane, c);
		

	}

	private void setAll(Dimension d, JComponent c) {
		d = new Dimension(d);
		c.setMinimumSize(d);
		c.setMaximumSize(d);
		c.setPreferredSize(d);
	}
	// These actions are used in the drop down menu, right click menu, and
	// also double clicking of the rows.

	public Action actDelete = new AbstractAction("Delete") {
		public void actionPerformed(ActionEvent e) {
			LManager.getLManager().deleteSelectedLayer();
		}
	};

	Action actRename = new AbstractAction("Rename") {
		public void actionPerformed(ActionEvent e) {
			LManager.getLManager().renameSelectedLayer();
		}
	};

	Action actOptions = new AbstractAction("Open") {
		public void actionPerformed(ActionEvent e) {
			LManager.getLManager().accessSelectedOptions(false);
		}
	};
	
	Action actDocked = new AbstractAction("Open Docked"){
		public void actionPerformed(ActionEvent e){
			LManager.getLManager().accessSelectedOptions(true);
		}
	};
	
	Action actTooltip = new AbstractAction("Show Tooltip"){
		public void actionPerformed(ActionEvent e) {
			boolean show = true;;
			if(e.getSource() == tooltip){
				show = tooltip.isSelected();
			}
			if(e.getSource() == tooltipsItem){
				show = tooltipsItem.isSelected();
			}
			LManager.getLManager().showTooltipForSelectedLayer(show);
			
		};
	};

	// This action is used to pull up the 'add layer' jframe with the
	// button
	Action actAdd = new AbstractAction("Add New Layer") {
		public void actionPerformed(ActionEvent e) {
			LManager.getLManager().displayAddNewLayer();
		}
	};

	// ------------------------------------------------------------------------//

	public void addView(Layer.LView view) {
		Row r = new Row(view, rowMouseHandler);
		rowsPanel.add(r);
		rows.add(0, r);
	}

	public void updateRows() {
		for (Iterator i = rows.iterator(); i.hasNext();)
			((Row) i.next()).updateRow();
	}

	void setDragOffset(int dragOffset) {
		if (dragOffset == 0 || rows.isEmpty()) {
			dragSrc = -1;
			dragDst = -1;
			dragPos = -1;
		} else {
			int h = ((Row) rows.get(0)).getPreferredSize().height;
			dragSrc = rows.indexOf(dragRow);
			dragPos = Util.bound(0, h * dragSrc + dragOffset,
					h * (rows.size() - 1));
			dragDst = (dragPos + h / 2 - 1) / h;
		}
	}

	public MouseInputListener rowMouseHandler = new MouseInputAdapter() {
		int pressed;

		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() != 2)
				return;
			if(Config.get("openDocked").equalsIgnoreCase("false"))
				LManager.getLManager().accessSelectedOptions(false);
			else
				LManager.getLManager().accessSelectedOptions(true);
		}

		public void mousePressed(MouseEvent e) {
			Component c = e.getComponent();
			dragRow = (Row) (c instanceof Row ? c : c.getParent());
			if (DARK_DRAG) {
				dragRow.setBackground(darkColor);
			}
			setDragOffset(0);
			pressed = screenY(e);
			rowsPanel.moveToFront(dragRow);
			LManager.getLManager().setActiveLView(dragRow.getView());

			/*************** Right Click popup menu *****************************/
			if (SwingUtilities.isRightMouseButton(e)) {
				// Create popup menus and menu items
				JPopupMenu editMenu = new JPopupMenu();
				JMenuItem deleteItem = new JMenuItem(actDelete);
				JMenuItem renameItem = new JMenuItem(actRename);
				JMenuItem optionsItem = new JMenuItem(actOptions);
				JMenuItem dockedItem = new JMenuItem(actDocked);
				tooltipsItem = new JCheckBoxMenuItem(actTooltip);
				tooltipsItem.setSelected(!LManager.getLManager().getTooltipDisabledStatus());

				// Add items to menu
				editMenu.add(optionsItem);
				editMenu.add(dockedItem);
				editMenu.add(renameItem);
				editMenu.add(deleteItem);
				editMenu.add(tooltipsItem);

				// Display popup menu
				editMenu.show(e.getComponent(), e.getX(), e.getY());
			}

			/********************************************************************/
		}

		public void mouseDragged(MouseEvent e) {
			setDragOffset(screenY(e) - pressed);
			rowsPanel.revalidate();
			rowsPanel.repaint();
			if (rowScrollPane != null) {
				rowScrollPane.revalidate();
				rowScrollPane.repaint();
			}

		}

		public void mouseReleased(MouseEvent e) {
			if (dragSrc != dragDst) {
				int dragSrcRev = rows.size() - 1 - dragSrc;
				int dragDstRev = rows.size() - 1 - dragDst;

				// Move the user-visible row, the actual view list
				// order, and the focus tabs. The latter two are
				// in reverse order from the first one.
				rows.add(dragDst, rows.remove(dragSrc));
				LManager.getLManager().viewList.move(dragSrcRev, dragDstRev);
				LManager.getLManager().setActiveLView(dragDstRev);
			}
			if (DARK_DRAG) {
				dragRow.setBackground(LManager.getLManager().getBackground());
			}
			dragRow = null;
			setDragOffset(0);
			rowsPanel.revalidate();
			rowsPanel.repaint();
			if (rowScrollPane != null) {
				rowScrollPane.revalidate();
				rowScrollPane.repaint();
			}

		}
	};
	
	private static int screenY(MouseEvent e) {
		Point pt = e.getPoint();
		SwingUtilities.convertPointToScreen(pt, e.getComponent());
		return pt.y;
	}

}