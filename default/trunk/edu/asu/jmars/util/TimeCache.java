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


package edu.asu.jmars.util;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.Date;

/**
 ** A cache of time conversion data. Converts from sclk, orbit+offset,
 ** and utc into et. Reverse conversions may be provided in the future.
 **
 ** <p>No public constructor is provided, use {@link #getInstance()}
 ** instead.
 **/
final public class TimeCache
 {
	private static DebugLog log = DebugLog.instance();

	/**
	 ** Added to database UTC, required to align the database UTC with
	 ** the Mars UTC produced by showtime_themis.
	 **/
	public static final int UTC_OFFSET_MILLIS = -64183;

	/**
	 ** Maps from String to TimeCache.
	 **/
	private static Map instances;

	/**
	 ** Maps from String (craft name) to String (craft id number).
	 **/
	private static Map craftIDs;

	/**
	 ** This class has no public constructor, users should acquire
	 ** access to instances through this method.
	 **
	 ** @param craft Must be one of the crafts from the config file.
	 **/
	public static synchronized TimeCache getInstance(String craft)
	 throws TimeException
	 {
		if(instances == null)
		 {
			String[] tmp = Config.getAll("time.db.craft");
			craftIDs = new HashMap();
			for(int i=0; i<tmp.length; i+=2)
				craftIDs.put(tmp[i], tmp[i+1]);
			instances = new HashMap();
		 }

		String ship = (String) craftIDs.get(craft);
		if(ship == null)
			throw  new TimeException("Invalid spacecraft name: " + craft);

		TimeCache instance = (TimeCache) instances.get(craft);
		if(instance == null)
			try
			 {
				instance = new TimeCache(craft,
										 new URL(Config.get("time.db") +
											 "?ship=" + ship));
				instances.put(craft, instance);
			 }
			catch(MalformedURLException e)
			 {
				throw  new TimeException(
					"Invalid time.db entry in config file", e);
			 }

		return  instance;
	 }

	/**
	 ** Specialized method for better efficiency.
	 **/
	public static TimeCache getInstance(String craft,
										int minOrbit, int maxOrbit)
	 throws TimeException
	 {
		try
		 {
			return  new TimeCache(craft,
								  new URL(Config.get("time.db") +
										  "?min_orbit=" + minOrbit +
										  "&max_orbit=" + maxOrbit));
		 }
		catch(MalformedURLException e)
		 {
			throw  new TimeException(
				"Invalid time.db entry in config file", e);
		 }
	 }

	/**
	 ** Converts a utc string into a millisecond offset from 1970.
	 **
	 ** <p>Currently only accepts "yyyy-dddThh:mm:ss.sss" format
	 ** strings.
	 **/
	public static long utc2millis(String utc)
	 throws TimeException
	 {
		Date date = UTC_DF.parse(utc, new ParsePosition(0));
		if(date == null)
			throw  new TimeException("Invalid UTC format! (" + utc + ")");
		return  date.getTime();
	 }
	/**
	 ** Converts a millisecond offset from 1970 into a utc string.
	 **
	 ** <p>Currently outputs "yyyy-dddThh:mm:ss.sss" format strings.
	 **/
	public static String millis2utc(long millis)
	 throws TimeException
	 {
		return  UTC_DF.format(new Date(millis));
	 }
	public static final SimpleDateFormat UTC_DF = new SimpleDateFormat(
		"yyyy-DDD'T'HH:mm:ss.SSS");

	/**
	 ** Converts a fraction-encoded floating-point sclk value into
	 ** et. For example, 1234.128 as a fraction-encoded sclk is
	 ** actually 1234:128.
	 **/
	public double sclkf2et(double sclkFrac)
	 throws TimeException
	 {
		long sclk = (long) sclkFrac;
		int frac = (int) Math.round( (sclkFrac % 1) * 1000 );
		return  sclk2et(sclk, frac);
	 }

	/**
	 ** Converts a decimal-encoded floating-point sclk value into
	 ** et. For example, 1234.5 as a decimal-encoded sclk is actually
	 ** 1234:128.
	 **/
	public double sclkd2et(double sclkDec)
	 throws TimeException
	 {
		long sclk = (long) sclkDec;
		int frac = (int) ( (sclkDec % 1) * 256 );
		return  sclk2et(sclk, frac);
	 }

	/**
	 ** Converts a sclk:fraction into et.
	 **/
	public double sclk2et(long sclk, int frac)
	 throws TimeException
	 {
		long packedSclk = sclk * 256 + frac;
		DequaxPair pair = forSclk(packedSclk);
		if(pair == null)
			throw  new TimeException("The sclk " + sclk + " is out of range!");
		return  pair.interpolateToEt(packedSclk,
									 pair.a.packedSclk,
									 pair.b.packedSclk);
	 }

	/**
	 ** Converts an orbit+offset into et.
	 **/
	public double orbit2et(int orbit, double offset)
	 throws TimeException
	 {
		DequaxPair pair = forOrbit(orbit);
		if(pair == null)
			throw  new TimeException("The orbit " +orbit+ " is out of range!");
		return  pair.interpolateToEt(orbit,
									 pair.a.orbit,
									 pair.b.orbit) + offset;
	 }

	/**
	 ** Converts a utc string into et. See {@link #utc2millis} for
	 ** accepted utc formats.
	 **/
	public double utc2et(String utc)
	 throws TimeException
	 {
		long utcMillis = utc2millis(utc);
		DequaxPair pair = forUtc(utcMillis);
		if(pair == null)
			throw  new TimeException("The UTC " + utc + " is out of range!");
		return  pair.interpolateToEt(utcMillis,
									 pair.a.utcMillis,
									 pair.b.utcMillis);
	 }

	/**
	 ** Converts a millisecond offset from 1970 into et.
	 **/
	public double millis2et(long utcMillis)
	 throws TimeException
	 {
		DequaxPair pair = forUtc(utcMillis);
		if(pair == null)
			throw  new TimeException(utcMillis + " is out of range!");
		return  pair.interpolateToEt(utcMillis,
									 pair.a.utcMillis,
									 pair.b.utcMillis);
	 }

	/**
	 ** Converts an et into a fraction-encoded floating-point sclk.
	 **/
	public double et2sclkf(double et)
	 throws TimeException
	 {
		DequaxPair pair = forEt(et);

		if(pair == null)
			throw  new TimeException("The ET " + et + " is out of range!");

		long psclk = pair.interpolateFromEtL(et,
											 pair.a.packedSclk,
											 pair.b.packedSclk);
		// Convert packed sclk to fraction-encoded
		return  (psclk / 256) + (psclk%256) / 1000.0;
	 }

	/**
	 ** Converts an et into an orbit+offset, as a pair of ints in
	 ** that order.
	 **/
	public int[] et2orbitn(double et)
	 throws TimeException
	 {
		DequaxPair pair = forEt(et);

		if(pair == null)
			throw  new TimeException("The ET " + et + " is out of range!");

		double orbitF = pair.interpolateFromEtF(et,
											   pair.a.orbit,
											   pair.b.orbit);
		// Convert partial orbit to orbit+secs
		int orbit = (int) Math.floor(orbitF);
		int secs = (int) Math.round( (orbitF-orbit) *
							   (pair.b.utcMillis - pair.a.utcMillis) / 1000 );
		return  new int[] { orbit, secs };
	 }

	/**
	 ** Converts an et into an orbit+offset.
	 **/
	public String et2orbit(double et)
	 throws TimeException
	 {
		DequaxPair pair = forEt(et);

		if(pair == null)
			throw  new TimeException("The ET " + et + " is out of range!");

		double orbitF = pair.interpolateFromEtF(et,
											   pair.a.orbit,
											   pair.b.orbit);
		// Convert partial orbit to orbit+secs
		int orbit = (int) Math.floor(orbitF);
		int secs = (int) Math.round( (orbitF-orbit) *
							   (pair.b.utcMillis - pair.a.utcMillis) / 1000 );
		DecimalFormat twoDigits = new DecimalFormat("00");
		if(secs == 0)
			return  Integer.toString(orbit);
		return orbit
			+ "+" + twoDigits.format(secs / 60 / 60)
			+ ":" + twoDigits.format(secs / 60 % 60)
			+ ":" + twoDigits.format(secs % 60);
	 }

	/**
	 ** Converts an et into a UTC string.
	 **/
	public String et2utc(double et)
	 throws TimeException
	 {
		DequaxPair pair = forEt(et);

		if(pair == null)
			throw  new TimeException("The ET " + et + " is out of range!");

		long millis = pair.interpolateFromEtL(et,
											  pair.a.utcMillis,
											  pair.b.utcMillis);
		return  millis2utc(millis);
	 }

	/**
	 ** Converts an et into milliseconds since 1970.
	 **/
	public long et2millis(double et)
	 throws TimeException
	 {
		DequaxPair pair = forEt(et);

		if(pair == null)
			throw  new TimeException("The ET " + et + " is out of range!");

		long millis = pair.interpolateFromEtL(et,
											  pair.a.utcMillis,
											  pair.b.utcMillis);
		return  millis;
	 }

	/**
	 ** Converts an et into a {@link Date} object.
	 **/
	public Date et2date(double et)
	 throws TimeException
	 {
		DequaxPair pair = forEt(et);

		if(pair == null)
			throw  new TimeException("The ET " + et + " is out of range!");

		long millis = pair.interpolateFromEtL(et,
											  pair.a.utcMillis,
											  pair.b.utcMillis);
		return  new Date(millis);
	 }


////////////////////////////////////////////////////////////////////////////
/////////////  ALL OF THE BELOW IS NON-PUBLIC  /////////////////////////////
////////////////////////////////////////////////////////////////////////////



	private DequaxPair forOrbit(int orbit)
	 throws TimeException
	 {
		Dequax last = null;
		for(int i=0; i<data.length; i++)
			if(data[i] != null)
				if(data[i].orbit > orbit)
					if(last == null)
						throw new TimeException(
							"Time not covered! (orbit:" + orbit + ")");
					else
						return  new DequaxPair(last, data[i]);
				else
					last = data[i];
		return  null;
	 }

	private DequaxPair forEt(double et)
	 throws TimeException
	 {
		Dequax last = null;
		for(int i=0; i<data.length; i++)
			if(data[i] != null)
				if(data[i].et > et)
					if(last == null)
						throw new TimeException(
							"Time not covered! (et:" + et + ")");
					else
						return  new DequaxPair(last, data[i]);
				else
					last = data[i];
		return  null;
	 }

	private DequaxPair forSclk(long packedSclk)
	 throws TimeException
	 {
		Dequax last = null;
		for(int i=0; i<data.length; i++)
			if(data[i] != null)
				if(data[i].packedSclk > packedSclk)
					if(last == null)
						throw new TimeException(
							"Time not covered! (sclk:" + packedSclk/256 +
							":" + packedSclk%256 + ")");
					else
						return  new DequaxPair(last, data[i]);
				else
					last = data[i];
		return  null;
	 }

	private DequaxPair forUtc(long utcMillis)
	 throws TimeException
	 {
		Dequax last = null;
		for(int i=0; i<data.length; i++)
			if(data[i] != null)
				if(data[i].utcMillis > utcMillis)
					if(last == null)
						throw new TimeException(
							"Time not covered! (utcMillis:" + utcMillis + ")");
					else
						return  new DequaxPair(last, data[i]);
				else
					last = data[i];
		return  null;
	 }

	/**
	 ** Encapsulates a pair of equator crossings that together form an
	 ** interpolatable interval in time.
	 **/
	private static class DequaxPair
	 {
		Dequax a;
		Dequax b;

		DequaxPair(Dequax a, Dequax b)
		 {
			this.a = a;
			this.b = b;
		 }

		/**
		 ** Calculates an interpolated et from some other source time
		 ** format, given the boundary values for the source time at
		 ** points a and b.
		 **
		 ** <p>For instance, invoking this.interpolateToEt(packedSclk,
		 ** this.a.packedSclk, this.b.packedSclk) will interpolate the
		 ** sclk value packedSclk to an et.
		 **/
		double interpolateToEt(double srcVal,
							   double srcA,
							   double srcB)
		 {
			return
				(srcVal - srcA) /
				(srcB   - srcA) *
				(b.et   - a.et) +
				a.et;
		 }

		/**
		 ** Calculates an interpolated time in some destination format
		 ** from et, given the boundary values for the destination
		 ** time at points a and b.
		 **
		 ** <p>For instance, invoking this.interpolateFromEt(etTime,
		 ** this.a.packedSclk, this.b.packedSclk) will interpolate the
		 ** et value etTime to a sclk.
		 **
		 ** <p>Generally only useful (and only needed) for sclk and
		 ** utcMillis.
		 **/
		long interpolateFromEtL(double srcEt,
								long dstA,
								long dstB)
		 {
			return Math.round(
				(srcEt - a.et) /
				(b.et  - a.et) *
				(dstB  - dstA) +
				dstA
				);
		 }

		double interpolateFromEtF(double srcEt,
								 int dstA,
								 int dstB)
		 {
			return (
				(srcEt - a.et) /
				(b.et  - a.et) *
				(dstB  - dstA) +
				dstA
				);
		 }
	 }

	private static class Dequax
	 {
		int orbit;
		String utc;
		long utcMillis;
		long packedSclk;
		double et;
	 }

	private int FIRST_ORBIT;
	private Dequax[] data;

	/**
	 ** Loads time records through the web.
	 **/
	private TimeCache(String craft, URL url)
	 throws TimeException
	 {
		int state = 0;
		try
		 {
			log.aprintln(
				"Retrieving " + craft + " time conversion database...");
			log.println(url);

			BufferedReader fin =
				new BufferedReader(new InputStreamReader(url.openStream()));
			StringTokenizer tok;

			// Grab orbit ranges
			state = 1;
			tok = new StringTokenizer(fin.readLine());
			int count = Integer.parseInt(tok.nextToken());
			int min = Integer.parseInt(tok.nextToken());
			int max = Integer.parseInt(tok.nextToken());
			if(count == 0  ||  min == 0  ||  max == 0)
				throw  new TimeException("Server returned no time data!");

			// Allocate space
			FIRST_ORBIT = min;
			data = new Dequax[max-min+1];

			// Grab actual data
			state = 2;
			String line;
			while((line=fin.readLine()) != null)
			 {
				tok = new StringTokenizer(line);
				int orbit = Integer.parseInt(tok.nextToken());
				int idx = orbit - FIRST_ORBIT;
				if(idx < 0  ||  idx >= data.length)
					continue;
				Dequax d = data[idx] = new Dequax();
				d.orbit = orbit;
				d.utc = tok.nextToken();
				d.utcMillis = utc2millis(d.utc) + UTC_OFFSET_MILLIS;
				long sclk = Long.parseLong(tok.nextToken());
				int frac = Integer.parseInt(tok.nextToken());
				d.packedSclk = sclk * 256 + frac;
				d.et = Double.parseDouble(tok.nextToken());
			 }				
			log.aprintln("Successfully loaded all time records!");
			log.printStack(-1);
		 }
		catch(Throwable e)
		 {
			log.aprintln(e);
			String msg = "INVALID STATE";
			switch(state)
			 {
			 case 0: msg="Unable to contact time server"               ; break;
			 case 1: msg="Unable to get summary data from time server" ; break;
			 case 2: msg="Unable to get data from time server"         ; break;
			 case 3: msg="Error while processing data from time server"; break;
			 }
			throw  new TimeException(msg, e);
		 }
	 }

	/**
	 ** Loads time records from the database.
	 **
	 ** @deprecated Requires a user login.
	 **/
	private TimeCache(String dbUrl)
	 throws TimeException
	 {
		int state = 0;
		try
		 {
			log.aprintln("Retrieving time conversion database...");
			String sql = "from events where name='DEQUAX' order by orbit";
			Statement stmt = DriverManager
				.getConnection(dbUrl)
				.createStatement();

			// Grab orbit ranges
			state = 1;
			ResultSet rs = stmt.executeQuery(
				"select count(*), min(orbit), max(orbit) " + sql);
			rs.next();
			int count = rs.getInt(1);
			int min = rs.getInt(2);
			int max = rs.getInt(3);
			if(count == 0  ||  min == 0  ||  max == 0)
				throw  new TimeException("Server returned no time data!");

			// Allocate space
			FIRST_ORBIT = min;
			data = new Dequax[max-min+1];

			// Grab actual data
			state = 2;
			rs = stmt.executeQuery(
				"select orbit, trim(timetext), sclk, sclk_frac, ephemeris "
				+ sql);
			while(rs.next())
			 {
				int orbit = rs.getInt(1);
				int idx = orbit - FIRST_ORBIT;
				if(idx < 0  ||  idx >= data.length)
					continue;
				Dequax d = data[idx] = new Dequax();
				d.orbit = orbit;
				d.utc = rs.getString(2);
				d.utcMillis = utc2millis(d.utc) + UTC_OFFSET_MILLIS;
				d.packedSclk = rs.getLong(3) * 256 + rs.getInt(4);
				d.et = rs.getDouble(5);
			 }				
			log.aprintln("Successfully loaded all time records!");
		 }
		catch(SQLException e)
		 {
			log.aprintln(e);
			String msg = "INVALID STATE";
			switch(state)
			 {
			 case 0: msg="Unable to contact time server"               ; break;
			 case 1: msg="Unable to get summary data from time server" ; break;
			 case 2: msg="Unable to get data from time server"         ; break;
			 case 3: msg="Error while processing data from time server"; break;
			 }
			throw  new TimeException(msg, e);
		 }
	 }

	public static void main(String[] av)
	 throws Throwable
	 {
		String utc = "2002-065T20:45:18.052";

		Date d = new Date(UTC_DF.parse(utc).getTime());

		log.aprintln(utc);
		log.aprintln(UTC_DF.format(d));

		System.exit(0);
	 }
 }
