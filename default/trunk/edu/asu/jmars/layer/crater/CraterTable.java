package edu.asu.jmars.layer.crater;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import edu.asu.jmars.Main;
import edu.asu.jmars.swing.ColorCell;
import edu.asu.jmars.swing.ColorCombo;
import edu.asu.jmars.swing.STable;
import edu.asu.jmars.util.stable.DoubleCellEditor;
import edu.asu.jmars.util.stable.NumberCellRenderer;
import edu.asu.jmars.util.stable.Sorter;

public class CraterTable extends STable 
{
	CraterTableModel tableModel;
    
    public CraterTableModel getTableModel() {
    	if (tableModel==null) {
    		tableModel=new CraterTableModel(craterView);
    	}
    	return tableModel;
    }

    CraterLView craterView;
    
    public NumberCellRenderer numberRenderer = new NumberCellRenderer();
    
	public CraterTable(CraterLView lview)
	{
		super();
	
		craterView=lview;
				
		setUnsortedTableModel(getTableModel());
		
		final CraterSettings settings = ((CraterLayer)craterView.getLayer()).settings;
		ColorCombo colorComboBox = new ColorCombo(new Color(settings.nextColor.getRGB() & 0xFFFFFF, false));
		
		TableColumn colorColumn = this.getColumnModel().getColumn(CraterTableModel.COLOR_COLUMN);
		
		ColorCellEditor editor = new ColorCellEditor();
		colorColumn.setCellEditor(editor);
		
		colorColumn.setResizable(false);
		
	    ColorCell renderer = new ColorCell();
     	colorColumn.setCellRenderer(renderer);
          	
		ToolTipManager.sharedInstance().registerComponent( this );
		
		addMouseListener(new TableMouseAdapter());

		getSelectionModel().addListSelectionListener(new ListSelectionListener(){
		
			public void valueChanged(ListSelectionEvent e) {
				int selectedRows[]=getSelectedRows();
				
				Sorter sorter = getSorter();

				ArrayList<Crater> selectedCraters = new ArrayList<Crater>();
				
				for (int row : selectedRows) {
					Crater s = getCrater(sorter.unsortRow(row));
					selectedCraters.add(s);
				}
											
				craterView.selectedCraters.clear();
				craterView.selectedCraters.addAll(selectedCraters);
				
				craterView.drawSelectedCraters();
				craterView.repaint();
				
				((CraterLView)craterView.getChild()).drawSelectedCraters();
				((CraterLView)craterView.getChild()).repaint();

				return;
			}
		});

		setTypeSupport(Double.class, numberRenderer, new DoubleCellEditor());

		final DecimalFormat diamterFormatter = new DecimalFormat("0.##");
		
		TableCellRenderer diameterRenderer = new TableCellRenderer() {
		
			public Component getTableCellRendererComponent(JTable table, Object value,
					boolean isSelected, boolean hasFocus, int row, int column) {
				JLabel label = new JLabel();
				if (value instanceof String) {
					label.setText(""+value);
					return label;
				}
				double val = (Double)value;
				if (val>500) {
					label.setText(diamterFormatter.format(val / 1000.0) + " km");
				} else {
					label.setText(diamterFormatter.format(val) + " m");
				}
				return label;
			}
		};
		
		TableColumn col = getColumnModel().getColumn(2);
		col.setCellRenderer(diameterRenderer);
		
		setAutoResizeMode(STable.AUTO_RESIZE_OFF);
		setColumnSelectionAllowed(false);
		setPreferredScrollableViewportSize(new Dimension(getPreferredSize().width,400));
	}
	
	/**
	 ** returns the crater that corresponds to the specified row.
	 ** returns null if the crater cannot be found.
	 **
	 ** @param row - the table row whose stamp is to be fetched.
	 **/
	public Crater getCrater(int row){
		CraterTableModel model = (CraterTableModel)getUnsortedTableModel();

		return (Crater)model.getValueAt(row);				
	}
		
	public void selectRows(List<Crater> cratersToSelect) {
		CraterTableModel model = (CraterTableModel)getUnsortedTableModel();

		clearSelection();
		
		int row = -1;
		
		for (Crater crater : cratersToSelect) {
			row = model.getRow(crater);
			if (row<0) continue;
			row=getSorter().sortRow(row);
			addRowSelectionInterval(row, row);			
		}		
		
		makeRowVisible(row);
	}
	
	public int getRow(Crater crater){
		CraterTableModel model = (CraterTableModel)getUnsortedTableModel();

		int row=model.getRow(crater);
		row=getSorter().sortRow(row);

		return row;
	}
			
	/**
	 ** specifies the tooltip to be displayed whenever the cursor
	 ** halts over a cell.  This is called because the table's panel
	 ** is registered with the tooltip manager in the constructor.
	 **/
	public String getToolTipText(MouseEvent e){
		Point p = e.getPoint();
		int col = columnAtPoint(p);
		int row = rowAtPoint(p);
		if (col == -1  ||  row == -1) {
			return  null;
		}
		String name = getColumnName(col);
		Object value = getValueAt(row, col);
		
		if (value == null) {
			value = "-NULL-";
		} else if (col == CraterTableModel.COLOR_COLUMN) {
			ColorCombo cc = (ColorCombo)value;
			value = " RGB: " + cc.getColor().getRed() + " " + cc.getColor().getGreen()  + " " + cc.getColor().getBlue(); 
		}

		return  name + " = " + value;
	}
		
	protected class TableMouseAdapter extends MouseAdapter {
		public void mousePressed(MouseEvent e){
				if (SwingUtilities.isLeftMouseButton(e)){
					return;
				} 
				
				// if this was a right click, bring up the menu options
				JPopupMenu menu = new JPopupMenu();
				
				JMenuItem removeCraters = new JMenuItem("Remove selected craters");
				removeCraters.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						// Not sure this is right
						List<Crater> selectedCraters = (List<Crater>)craterView.selectedCraters.clone();
						craterView.deleteSelectedCraters();

						getSorter().clearSorts();
						craterView.repaint();
						
						((CraterLView)craterView.getChild()).drawSelectedCraters();
						((CraterLView)craterView.getChild()).repaint();
					}
				});
				
				// JMenu that will be under removeCraters JMenuItem
				JMenu colorMenu = new JMenu("Change selected colors");
				
				// get colors
				Color[] allColors = (new ColorCombo()).getColorList();
				Color defaultSelectedBg = (Color) UIManager.get("MenuItem.selectionBackground");
				Color defaultSelectedFg = (Color) UIManager.get("MenuItem.selectionForeground");
				
				// create JMenuItems
				for(int i = 0; i < allColors.length; i++)
				{
					// set selection colors
					UIManager.put("MenuItem.selectionBackground",allColors[i]);
					UIManager.put("MenuItem.selectionForeground",allColors[i]);
					/* // JNN: color complement to show hidden text upon mouse over?
					UIManager.put("MenuItem.selectionForeground",
							new Color(255-allColors[i].getRed(),
									255-allColors[i].getGreen(),
									255-allColors[i].getBlue())
							);
					//*/
					
					final JMenuItem temp = new JMenuItem("Change Color"); // hidden text
					temp.setBackground(allColors[i]); // cell color
					temp.setForeground(allColors[i]); // text color
					
					temp.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							// change the color of each crater
							List<Crater> selectedCraters = (List<Crater>)craterView.selectedCraters; // I didn't clone...
							for(int i = 0; i < selectedCraters.size(); i++)
							{
								int row = tableModel.getRow(selectedCraters.get(i));
								tableModel.setValueAt(temp.getBackground(), row, tableModel.COLOR_COLUMN);
							}
						}
					});
					
					colorMenu.add(temp);
				}
				
				// reset selection colors back to default settings
				UIManager.put("MenuItem.selectionBackground",defaultSelectedBg);
				UIManager.put("MenuItem.selectionForeground",defaultSelectedFg);
				
				menu.add(removeCraters);
				menu.add(colorMenu);

				menu.show(e.getComponent(), e.getX(), e.getY());
			}
		
		public void mouseClicked(MouseEvent e) {
			if (SwingUtilities.isLeftMouseButton(e)){
				int row = rowAtPoint(e.getPoint());
				if (row>-1) {
					row = getSorter().unsortRow(row);
					
					Crater crater = getCrater(row);
					
					Point2D p = new Point2D.Double(360-crater.getLon(), crater.getLat());
					
					Point2D worldPt = Main.PO.convSpatialToWorld(p);
					
					Main.testDriver.locMgr.setLocation(worldPt, true);
				}
			} 
		}
	};
	
	void makeRowVisible(int row) {
		setAutoscrolls(true);
		
		// Scroll to make the toggled row visible for the last stamp in the list.
		if (row >= 0) {
			Rectangle r = getCellRect(row, 0, false).union(
								       getCellRect(row, getColumnCount()-1, false));
			int extra = Math.min(r.height * 3, getHeight() / 4);
			r.y -= extra;
			r.height += 2 * extra;
			scrollRectToVisible(r);
		}					
	}
}

//This editor is used instead of the default because it sets the color properly, instead of
//  defaulting to black which is what the other editor does as soon as you click on the combo.
class ColorCellEditor extends AbstractCellEditor implements TableCellEditor{

	ColorCombo combo;
	
	public Object getCellEditorValue() {
		return combo.getColor();
	}

	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int column) {
		combo = (ColorCombo)value;
		return combo;
	}
	
}