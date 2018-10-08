package edu.asu.jmars.util.stable;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.table.TableColumn;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.stamp.networking.StampLayerNetworking;
import edu.asu.jmars.util.stable.FilteringColumnModel.FilterChangeListener;

/**
 * Allows selecting which columns are displayed in the table.
 */
public class ColumnDialog extends JDialog implements FilterChangeListener {
	private static final long serialVersionUID = 1L;
	private FilteringColumnModel columnModel;
	private JPanel columnPanel;
	
	public ColumnDialog(Frame parent, final FilteringColumnModel columnModel, final String saveKey) {
		super((Frame) parent, "Columns", false);
		this.columnModel = columnModel;
		// listen to the column model's changes from now until this dialog is disposed
		columnModel.addListener(this);
		addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				columnModel.removeListener(ColumnDialog.this);
			}
		});
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());

		// Set up list of columns in the middle of the dialog
		columnPanel = new JPanel();
		columnPanel.setLayout(new BoxLayout(columnPanel, BoxLayout.Y_AXIS));
		buildCheckboxes();
		JScrollPane scrollPane = new JScrollPane(columnPanel);
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		scrollPane.setPreferredSize(new Dimension(250,400));

		// Set up the button panel at the bottom of the panel.
		JPanel buttonPanel = new JPanel();
		JButton allButton = new JButton(new AbstractAction("Show All") {
			public void actionPerformed(ActionEvent e) {
				columnModel.setAllVisible(true);
			}
		});

		JButton nothingButton = new JButton(new AbstractAction("Hide All") {
			public void actionPerformed(ActionEvent e) {
				columnModel.setAllVisible(false);
			}
		});

		final JCheckBox persistChoices = new JCheckBox("Set selection as defaults", false);
			
		JButton okButton = new JButton(new AbstractAction("OK") {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				
				if (persistChoices.isSelected()) {
					Enumeration<TableColumn> colEnum=columnModel.getColumns();
					
					String cols[] = new String[columnModel.getColumnCount()];
					
					for (int i=0; i<cols.length; i++) {
						cols[i]=colEnum.nextElement().getHeaderValue().toString();
					}
					
					StampLayerNetworking.setDefaultColumns(saveKey, cols);
				}
			}
		});

		allButton.setFocusPainted(false);
		nothingButton.setFocusPainted(false);
		okButton.setFocusPainted(false);

		buttonPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.gridx = 0;
		buttonPanel.add(allButton, gbc);
		gbc.gridx = 1;
		buttonPanel.add(nothingButton, gbc);
		gbc.gridy = 1;
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		gbc.insets.top = 0;
		buttonPanel.add(okButton, gbc);
		
		if (saveKey!=null && saveKey.length()>0) {
			gbc.gridy = 2;
			gbc.gridx = 0;
			gbc.gridwidth = 2;
		
			buttonPanel.add(persistChoices, gbc);
			
			if (Main.USER!=null&&Main.USER.length()>0) {
				persistChoices.setEnabled(true);
			} else {
				persistChoices.setEnabled(false);
				persistChoices.setToolTipText("You must be logged in to use this feature");
			}

		}
		
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		pack();

		// Display this dialog in the middle of the screen.
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension d = getSize();
		d.width = Math.min(d.width, (screen.width / 2));
		d.height = Math.min(d.height, (screen.height / 2));
		setSize(d);
		int x = (screen.width - d.width) / 2;
		int y = (screen.height - d.height) / 2;
		setLocation(x, y);
	}

	public void buildCheckboxes() {
		columnPanel.removeAll();
		for (final TableColumn column: columnModel.getAllColumns()) {
			String name = column.getHeaderValue().toString();
			boolean visible = (null != columnModel.getVisColumn(column.getIdentifier()));
			JCheckBox cb = new JCheckBox (name, null, visible);
			cb.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					boolean checked = (e.getStateChange() == ItemEvent.SELECTED);
					columnModel.setVisible(column, checked);
				}
			});
			columnPanel.add(cb);
		}
		validate();
		paint (getGraphics());
	}
	public void filtersChanged() {
		buildCheckboxes();
	}
}
