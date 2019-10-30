package com.tobee.gis;

import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.util.List;

import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.dbf.DbaseFileWriter;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.geotools.data.shapefile.shp.ShapefileWriter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;

public class ShpHandler extends SwingWorker<Void, Integer> {
	private Logger LOGGER = LoggerFactory.getLogger(ShpHandler.class);

	private final File shpFile;
	private final File targetFile;
	private final String fromEPSG;
	private final String toEPSG;
	private final JProgressBar progressbar;
	private final Component cmpnent;
	private final int TotalCount;

	public ShpHandler(final File shpFile, final File targetFile, final String fromEPSG, final String toEPSG,
			final JProgressBar progressbar, final Component cmpnent) {
		this.shpFile = shpFile;
		this.targetFile = targetFile;
		this.fromEPSG = fromEPSG;
		this.toEPSG = toEPSG;
		this.progressbar = progressbar;
		this.cmpnent = cmpnent;
		this.TotalCount = getShpFileDataCount(shpFile);
		progressbar.setMaximum(TotalCount);
	}

	protected void process(List<Integer> chunks) {
		for (Integer chunk : chunks) {
			progressbar.setValue(chunk);
		}
	}

	protected void done() {
		cmpnent.setEnabled(true);
	}

	@Override
	protected Void doInBackground() throws Exception {
		FileDataStore store = null;
		SimpleFeatureSource featureSource = null;
		CoordinateReferenceSystem dataCRS = null;
		CoordinateReferenceSystem targetdataCRS = null;
		MathTransform transform = null;
		boolean lenient = true; // allow for some error due to different datums
		FileChannel shpCh = null;
		FileChannel shxCh = null;
		FileChannel dbfCh = null;

		ShapefileReader shpOrg = null;
		DbaseFileReader dbfOrg = null;
		ShapefileWriter shpW = null;
		DbaseFileWriter dbfW = null;
		DbaseFileHeader dbfH = null;
		Envelope mbr = null;
		FileOutputStream shpfout = null;
		FileOutputStream shxfout = null;
		FileOutputStream dbfout = null;

		ShapeType shapeType = null;

		SimpleFeatureCollection featureCollection = null;
		SimpleFeatureIterator iterator = null;

		int totalcount = 0;

		try {
			store = FileDataStoreFinder.getDataStore(shpFile);
			featureSource = store.getFeatureSource();
		} catch (IOException e1) {
			System.err.println(e1.getMessage());
		}

		try {
			dataCRS = CRS.decode(fromEPSG);
			targetdataCRS = CRS.decode(toEPSG);
			transform = CRS.findMathTransform(dataCRS, targetdataCRS, lenient);
		} catch (NoSuchAuthorityCodeException e1) {
			System.err.println(e1.getMessage());
		} catch (FactoryException e1) {
			System.err.println(e1.getMessage());
		}

		String abpath = targetFile.getAbsolutePath();
		String dbf = abpath.substring(0, abpath.indexOf('.')) + ".dbf";
		String shx = abpath.substring(0, abpath.indexOf('.')) + ".shx";

		try {

			shpOrg = new ShapefileReader(new ShpFiles(shpFile.getAbsolutePath()), true, true, new GeometryFactory(),
					false);
			dbfOrg = new DbaseFileReader(new ShpFiles(shpFile.getAbsolutePath()), true, Charset.forName("MS949"));

			shpfout = new FileOutputStream(targetFile);
			shxfout = new FileOutputStream(shx);
			dbfout = new FileOutputStream(dbf);

			shpCh = shpfout.getChannel();
			shxCh = shxfout.getChannel();
			dbfCh = dbfout.getChannel();

			shpW = new ShapefileWriter(shpCh, shxCh);
			dbfW = new DbaseFileWriter(dbfOrg.getHeader(), dbfCh, Charset.forName("MS949"));
			shapeType = shpOrg.getHeader().getShapeType();

			mbr = null;

			shpW.writeHeaders(new Envelope(), shapeType, 0, 0);
			dbfH = dbfOrg.getHeader();

		} catch (Exception e) {
			e.printStackTrace();
		}

		int rsize = dbfH.getNumFields();

		try {
			featureCollection = featureSource.getFeatures();
			iterator = featureCollection.features();

			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();

				com.vividsolutions.jts.geom.Geometry geometry = (com.vividsolutions.jts.geom.Geometry) feature
						.getDefaultGeometry();
				com.vividsolutions.jts.geom.Geometry geometry2 = null;

				geometry2 = JTS.transform(geometry, transform);

				Envelope geomEnv = geometry2.getEnvelopeInternal();
				if (mbr == null)
					mbr = geomEnv;
				else
					mbr.expandToInclude(geomEnv);

				shpW.writeGeometry(geometry2);

				Object[] polyRecord = new Object[rsize];

				dbfOrg.readEntry(polyRecord);
				dbfW.write(polyRecord);

				publish(totalcount);
				totalcount++;

				// if (cnt == 500) {
				// Runtime.getRuntime().gc();
				// }

				feature = null;
				polyRecord = null;
				geometry2 = null;
			}

			if (shpCh.isOpen()) {
				shpW.writeHeaders(mbr == null ? new Envelope() : mbr, shapeType, totalcount, (int) shpCh.size());
			}

			if (dbfCh.isOpen()) {
				RandomAccessFile raf = new RandomAccessFile(new File(dbf), "rw");
				FileChannel f = raf.getChannel();
				MappedByteBuffer buffer = f.map(MapMode.READ_WRITE, 4, 4);
				buffer.order(ByteOrder.LITTLE_ENDIAN).putInt(totalcount);
				raf.close();
				f.close();
				buffer.clear();
			}
			// else {
			// throw new UnsupportedOperationException(dbfCh.toString());
			// }

		} catch (MismatchedDimensionException e) {
			System.err.println(e.getMessage());
		} catch (TransformException e) {
			System.err.println(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (shpOrg != null)
					shpOrg.close();
				if (dbfOrg != null)
					dbfOrg.close();

				if (shpW != null)
					shpW.close();
				if (dbfW != null)
					dbfW.close();

				if (shpfout != null)
					shpfout.close();
				if (shxfout != null)
					shxfout.close();
				if (dbfout != null)
					dbfout.close();

				if (shpCh != null)
					shpCh.close();
				if (shxCh != null)
					shxCh.close();
				if (dbfCh != null)
					dbfCh.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			if (dbfH != null)
				dbfH = null;
			if (mbr != null)
				mbr = null;
			if (shapeType != null)
				shapeType = null;

			if (featureCollection != null)
				featureCollection = null;
			if (iterator != null)
				iterator.close();

			if (transform != null)
				transform = null;
			if (targetdataCRS != null)
				targetdataCRS = null;
			if (dataCRS != null)
				dataCRS = null;
			if (featureSource != null)
				featureSource = null;
			if (store != null)
				store.dispose();

			store = null;
			featureSource = null;
			dataCRS = null;
			targetdataCRS = null;
			transform = null;
			abpath = null;
			dbf = null;
			shx = null;
			shpCh = null;
			shxCh = null;
			dbfCh = null;
			shpOrg = null;
			dbfOrg = null;
			shpW = null;
			dbfW = null;

			System.out.println("==========================" + totalcount);
		}
		// Runtime.getRuntime().gc();

		return null;
	}

	public static int reprojectShpFileLarge(File shpFile, File targetFile, String fromEPSG, String toEPSG,
			PrintWriter out) {
		FileDataStore store = null;
		SimpleFeatureSource featureSource = null;
		int totalcount = 0;

		out.print(shpFile.getName());
		out.print(",");
		out.print(targetFile.getName());
		out.print(",");
		out.print(fromEPSG);
		out.print(",");
		out.print(toEPSG);

		try {
			store = FileDataStoreFinder.getDataStore(shpFile);
			featureSource = store.getFeatureSource();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			return -1;
		}

		CoordinateReferenceSystem dataCRS = null;
		CoordinateReferenceSystem targetdataCRS = null;
		MathTransform transform = null;
		boolean lenient = true; // allow for some error due to different datums

		try {
			dataCRS = CRS.decode(fromEPSG);
			targetdataCRS = CRS.decode(toEPSG);
			transform = CRS.findMathTransform(dataCRS, targetdataCRS, lenient);
		} catch (NoSuchAuthorityCodeException e1) {
			// TODO Auto-generated catch block
			System.err.println(e1.getMessage());
			return -1;
		} catch (FactoryException e1) {
			// TODO Auto-generated catch block
			System.err.println(e1.getMessage());
			return -1;
		}

		SimpleFeatureCollection featureCollection = null;
		SimpleFeatureIterator iterator = null;

		String abpath = targetFile.getAbsolutePath();
		String dbf = abpath.substring(0, abpath.indexOf('.')) + ".dbf";
		String shx = abpath.substring(0, abpath.indexOf('.')) + ".shx";
		FileChannel shpCh = null;
		FileChannel shxCh = null;
		FileChannel dbfCh = null;

		ShapefileReader shpOrg = null;
		DbaseFileReader dbfOrg = null;
		ShapefileWriter shpW = null;
		DbaseFileWriter dbfW = null;
		DbaseFileHeader dbfH = null;
		Envelope mbr = null;
		FileOutputStream shpfout = null;
		FileOutputStream shxfout = null;
		FileOutputStream dbfout = null;

		ShapeType shapeType = null;

		try {

			shpOrg = new ShapefileReader(new ShpFiles(shpFile.getAbsolutePath()), true, true, new GeometryFactory(),
					false);
			dbfOrg = new DbaseFileReader(new ShpFiles(shpFile.getAbsolutePath()), true, Charset.forName("MS949"));

			shpfout = new FileOutputStream(targetFile);
			shxfout = new FileOutputStream(shx);
			dbfout = new FileOutputStream(dbf);

			shpCh = shpfout.getChannel();
			shxCh = shxfout.getChannel();
			dbfCh = dbfout.getChannel();

			shpW = new ShapefileWriter(shpCh, shxCh);
			dbfW = new DbaseFileWriter(dbfOrg.getHeader(), dbfCh, Charset.forName("MS949"));
			shapeType = shpOrg.getHeader().getShapeType();

			mbr = null;

			shpW.writeHeaders(new Envelope(), shapeType, 0, 0);
			dbfH = dbfOrg.getHeader();

		} catch (Exception e) {
			e.printStackTrace();
		}

		int cnt = 0;

		int rsize = dbfH.getNumFields();

		try {
			featureCollection = featureSource.getFeatures();
			iterator = featureCollection.features();

			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();

				com.vividsolutions.jts.geom.Geometry geometry = (com.vividsolutions.jts.geom.Geometry) feature
						.getDefaultGeometry();
				com.vividsolutions.jts.geom.Geometry geometry2 = null;

				geometry2 = JTS.transform(geometry, transform);

				Envelope geomEnv = geometry2.getEnvelopeInternal();
				if (mbr == null)
					mbr = geomEnv;
				else
					mbr.expandToInclude(geomEnv);
				shpW.writeGeometry(geometry2);

				Object[] polyRecord = new Object[rsize];

				dbfOrg.readEntry(polyRecord);

				dbfW.write(polyRecord);

				totalcount++;

				if (cnt == 500) {
					Runtime.getRuntime().gc();
				}

				feature = null;
				polyRecord = null;
				geometry2 = null;
			}

			if (shpCh.isOpen()) {
				shpW.writeHeaders(mbr == null ? new Envelope() : mbr, shapeType, totalcount, (int) shpCh.size());
			}

			if (dbfCh.isOpen()) {
				RandomAccessFile raf = new RandomAccessFile(new File(dbf), "rw");
				FileChannel f = raf.getChannel();
				MappedByteBuffer buffer = f.map(MapMode.READ_WRITE, 4, 4);
				buffer.order(ByteOrder.LITTLE_ENDIAN).putInt(totalcount);
				raf.close();
				f.close();
				buffer.clear();
			}

		} catch (MismatchedDimensionException e) {
			System.err.println(e.getMessage());
			return -1;
		} catch (TransformException e) {
			System.err.println(e.getMessage());
			return -1;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			iterator.close();
			store = null;
			featureSource = null;
			dataCRS = null;
			targetdataCRS = null;
			transform = null;
			abpath = null;
			dbf = null;
			shx = null;

			try {
				shpCh.close();
				shxCh.close();
				dbfCh.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			shpCh = null;
			shxCh = null;
			dbfCh = null;

			try {
				shpOrg.close();
				dbfOrg.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (store != null)
				store.dispose();

			shpOrg = null;
			dbfOrg = null;
			shpW = null;
			dbfW = null;
			dbfH = null;
			mbr = null;

			shapeType = null;
		}
		// Runtime.getRuntime().gc();
		System.out.println("==========================" + totalcount);
		return totalcount;
	}

	public int getShpFileDataCount(final File shpFile) {
		
		FileDataStore store = null;
		SimpleFeatureSource featureSource = null;
		int totalcount = 0;
		SimpleFeatureCollection featureCollection = null;
		ShapefileReader shpOrg = null;
		SimpleFeatureIterator iterator = null;
		
		try {
			store = FileDataStoreFinder.getDataStore(shpFile);
			featureSource = store.getFeatureSource();
			featureCollection = featureSource.getFeatures();
			iterator = featureCollection.features();
			
			shpOrg = new ShapefileReader(new ShpFiles(shpFile.getAbsolutePath()), true, true, new GeometryFactory(),
					false);
			
			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				totalcount++;
				feature = null;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			iterator.close();
			store = null;
			featureSource = null;

			try {
				shpOrg.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			shpOrg = null;

		}

		return totalcount;
	}

}
