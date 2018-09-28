// Copyright 2008, Arizona Board of Regents
// on behalf of Arizona State University
// 
// Prepared by the Mars Space Flight Facility, Arizona State University,
// Tempe, AZ.
// 
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.


/*
 * @(#)SCLayout.java	1.02 2 Jan 2002
 *
 * Copyright John Redmond (John.Redmond@mq.edu.au).
 * 
 * This software is freely available for commercial and non-commercial purposes.
 * Acknowledgement of its source would be appreciated, but is not required.
 * 
 */

package edu.asu.jmars.swing;
// package au.com.pegasustech.demos.layout;

/**
 * Scaled Column Layout: 
 * For information on usage, see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip129.html">
http://www.javaworld.com/javaworld/javatips/jw-javatip129.html</a>
 */
public class SCLayout extends SGLayout {

  /**
   * Creates a layout with the specified number of rows
   * and default alignments and gaps.
   * <p>
   * vertical gaps are set to 0 and
   * X- and Y-alignments are set to FILL.
   * @param     rows   the rows.
   */
  public SCLayout(int rows) {
    super(rows, 1);
  }

  /**
   * Creates a layout with the specified number of rows
   * and specified gap.
   * <p>
   * vertical gaps are set to 0 and
   * X- and Y-alignments are set to FILL.
   * @param     rows   the rows.
   * @param     gap   the vertical gap, in pixels.
   */
  public SCLayout(int rows, int gap) {
    super(rows, 1, FILL, FILL, gap, 0);
  }

  /**
   * Creates a layout with the specified number of rows
   * and specified alignments and gaps.
   * <p>
   * vertical gaps are set to 0 and
   * X- and Y-alignments are set to FILL.
   * @param     rows   the rows.
   * @param     hAlignment the X-alignment.
   * @param     vAlignment the Y-alignment.
   * @param     gap   the vertical gap, in pixels.
   */
  public SCLayout(int rows, int hAlignment, int vAlignment, int gap) {
    super(rows, 1, hAlignment, vAlignment, 0, gap);
  }

  /**
   * Set up alignment for a specific cell.
   * <p>
   * @param     index the cell number.
   * @param     hAlignment  the X-alignment for the cell.
   * @param     vAlignment  the Y-alignment for the cell.
   */
  public void setAlignment(int index, int hAlignment, int vAlignment) {
    setRowAlignment(index, hAlignment, vAlignment);
  }

  /**
   * Set up scale value for a specific cell.
   * <p>
   * @param     index the column number.
   * @param     scale  the scale value for the column.
   */
  public void setScale(int cell, double scale) {
    setRowScale(cell, scale);
  }
}
