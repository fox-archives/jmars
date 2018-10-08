package edu.asu.jmars.swing;

import javax.swing.*;

public class LongField
 extends PasteField
 {
	public LongField()
	 {
	 }

	public LongField(long val)
	 {
		setText(String.valueOf(val));
	 }

	public Long getLong()
	 {
		try
		 {
			return  new Long(getText());
		 }
		catch(NumberFormatException e)
		 {
			if(getText().length() != 0)
				JOptionPane.showMessageDialog(
					null,
					"Invalid integer: '" + getText() + "'",
					"FORMAT ERROR",
					JOptionPane.ERROR_MESSAGE);
			return  null;
		 }
	 }
 }
