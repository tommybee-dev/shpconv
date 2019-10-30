package com.tobee.gis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.CRS;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKTReader;

public class GisConverter {
	// http://docs.geotools.org/latest/userguide/library/api/jts.html
	// http://www.osgeo.kr/17
	public static final String EPSG_WGS84_LONLAT = "EPSG:4326";
	public static final String EPSG_BESSEL_1841_LONLAT = "EPSG:4004";
	public static final String EPSG_GRS80_LONLAT = "EPSG:4019";
	public static final String EPSG_GOOGLE_LONLAT = "EPSG:3857";

	public static final String EPSG_KOR_UTM_BESSEL = "EPSG:5178";
	public static final String EPSG_KOR_UTM_GRS80 = "EPSG:5179";

	public static final String EPSG_KOR_TM_EST = "EPSG:2096";
	public static final String EPSG_KOR_TM_MID = "EPSG:2097";
	public static final String EPSG_KOR_TM_WST = "EPSG:2098";

	public static final String EPSG_KOR_TM_WST_OLD = "EPSG:5173";
	public static final String EPSG_KOR_TM_MID_OLD = "EPSG:5174";
	public static final String EPSG_KOR_TM_JJ_OLD = "EPSG:5175";
	public static final String EPSG_KOR_TM_EST_OLD = "EPSG:5176";
	public static final String EPSG_KOR_TM_UL_OLD = "EPSG:5177";

	public static final String EPSG_KOR_TM_WST_TMP = "EPSG:5180";
	public static final String EPSG_KOR_TM_MID_TMP = "EPSG:5181";
	public static final String EPSG_KOR_TM_JJ_TMP = "EPSG:5182";
	public static final String EPSG_KOR_TM_EST_TMP = "EPSG:5183";
	public static final String EPSG_KOR_TM_UL_TMP = "EPSG:5184";

	public static final String EPSG_KOR_TM_WST_CUR = "EPSG:5185";
	public static final String EPSG_KOR_TM_MID_CUR = "EPSG:5186";
	public static final String EPSG_KOR_TM_EST_CUR = "EPSG:5187";
	public static final String EPSG_KOR_TM_UL_CUR = "EPSG:5188";

	public static final String[] KOR_ESPG_LIST = new String[] { EPSG_WGS84_LONLAT, EPSG_BESSEL_1841_LONLAT,
			EPSG_GRS80_LONLAT, EPSG_GOOGLE_LONLAT, EPSG_KOR_UTM_BESSEL, EPSG_KOR_UTM_GRS80, EPSG_KOR_TM_EST,
			EPSG_KOR_TM_MID, EPSG_KOR_TM_WST, EPSG_KOR_TM_WST_OLD, EPSG_KOR_TM_MID_OLD, EPSG_KOR_TM_JJ_OLD,
			EPSG_KOR_TM_EST_OLD, EPSG_KOR_TM_UL_OLD, EPSG_KOR_TM_WST_TMP, EPSG_KOR_TM_MID_TMP, EPSG_KOR_TM_JJ_TMP,
			EPSG_KOR_TM_EST_TMP, EPSG_KOR_TM_UL_TMP, EPSG_KOR_TM_WST_CUR, EPSG_KOR_TM_MID_CUR, EPSG_KOR_TM_EST_CUR,
			EPSG_KOR_TM_UL_CUR };

	public static String CURRENT_CRS_NAME;
	public static String CURRENT_CRS_KEY;
	public static Hashtable<String, String> CRSTable = new Hashtable<String, String>();

	static {
		CRSTable.put(EPSG_KOR_TM_WST_CUR, "GRS80_�꽌遺��썝�젏");
		CRSTable.put(EPSG_KOR_TM_MID_CUR, "GRS80_以묐��썝�젏");
		CRSTable.put(EPSG_KOR_TM_EST_CUR, "GRS80_�룞遺��썝�젏");
		CRSTable.put(EPSG_KOR_TM_UL_CUR, "GRS80_�슱由됱썝�젏");
	}

	// EPSG_KOR_TM_MID, EPSG_KOR_TM_MID_OLD, EPSG_KOR_TM_MID_TMP,
	// EPSG_KOR_TM_MID_CUR

	private static final boolean lenient = true;

	public static String[] readWktPoint(final String wktString) throws Exception {
		Point jtsGeometry = null;
		WKTReader reader = new WKTReader();

		if (wktString == null)
			return null;

		try {
			jtsGeometry = (Point) reader.read(wktString);
		} catch (Exception ex) {
			throw new Exception("WKT parsing problem [" + wktString + "]");
		}

		// System.out.println("-----------------" + jtsGeometry.toString() +
		// "-------------------------");

		return new String[] { String.valueOf(jtsGeometry.getX()), String.valueOf(jtsGeometry.getY()) };
	}

	private static double[] transformCRSs(String coordX, String coordY, String orginCRS) throws Exception {
		// System.out.println("------------------------------------------");
		// System.out.println("Creating a math transform between two CRSs:");

		double x = Double.parseDouble(coordX);
		double y = Double.parseDouble(coordY);

		// START SNIPPET: mathTransformBetweenCRSs
		CoordinateReferenceSystem sourceCrs = CRS.decode(orginCRS);
		CoordinateReferenceSystem targetCrs = CRS.decode(EPSG_WGS84_LONLAT);

		MathTransform mathTransform = CRS.findMathTransform(sourceCrs, targetCrs, lenient);
		DirectPosition2D srcDirectPosition2D = new DirectPosition2D(sourceCrs, x, y);
		DirectPosition2D destDirectPosition2D = new DirectPosition2D();
		mathTransform.transform(srcDirectPosition2D, destDirectPosition2D);

		// double transX = destDirectPosition2D.x;
		// double transY = destDirectPosition2D.y;

		// System.out.println("output point: " + destDirectPosition2D);

		// System.out.println("Inverse of output point: " +
		// mathTransform.inverse().transform(destDirectPosition2D, null));
		// END SNIPPET: mathTransformBetweenCRSs
		// System.out.println("------------------------------------------");
		double[] xy = new double[2];

		xy[0] = destDirectPosition2D.x;
		xy[1] = destDirectPosition2D.y;

		return xy;
	}

	public static double[] transformWgs84(String coordX, String coordY, String orginCRS) throws Exception {
		return transformCRSs(coordX, coordY, orginCRS);
	}

	public static List<String> getEPSGCodeList() {
		List<String> codelist = new ArrayList<String>();
		
		BufferedReader breader = null;
		
		StringBuilder stbuld = new StringBuilder();
		String line = null;
		
		try {
			breader = new BufferedReader(new FileReader("conf/EPSG_Code.json"));
			
			while((line = breader.readLine()) != null)
			{
				stbuld.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally
		{
			try { breader.close(); } catch (IOException e) {}
		}
		
		JSONArray jsonArr = new JSONArray(stbuld.toString());

		for (int i = 0; i < jsonArr.length(); i++) {
			JSONObject jsonObj = (JSONObject)jsonArr.get(i);

			//System.out.println(jsonObj.getString("EPSG"));
			codelist.add("EPSG"+":"+jsonObj.getString("EPSG"));
		}
		
		return codelist;
	}
}
