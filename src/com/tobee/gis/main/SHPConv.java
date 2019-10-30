package com.tobee.gis.main;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.tobee.gis.GisConverter;
import com.tobee.gis.ShpHandler;

public class SHPConv {
	public static void main(String[] args) {

		// ShpHandler.reprojectShpFileLarge(shpFile, targetFile, fromEPSG,
		// toEPSG, out)

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				//JFrame frame = new ProgressBarFrame();
				//frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				//frame.setVisible(true);
				
				ShpConverter dialog = new ShpConverter();
				dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
		});
	}
	
	static class ShpConverter extends JDialog {
		private static final long serialVersionUID = 8177141453274699495L;
		public String sCustomerID;
		public String sName;
		public String sEmail;
		public String sCountryCode;
		public String sBudget;
		public String sUsed;
		
		private ShpHandler activity;
		private JProgressBar progressBar;
		final int MAX = 1000;
		
		private void putAllEspgCodeToCombo(JComboBox<String> comb)
		{
			comb.addItem("");
			
			List<String> epsgCodeList = GisConverter.getEPSGCodeList();
			
			for(Iterator<String> iter = epsgCodeList.iterator(); iter.hasNext() ; )
			{
				comb.addItem(iter.next());
			}
		}
		

		public ShpConverter() {
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			setBounds(100, 100, 474, 303);
			setTitle("좌표변환 프로그램(SHP)");
			getContentPane().setLayout(null);
			
			progressBar = new JProgressBar(0, MAX);
			progressBar.setStringPainted(true);
			
			
			// Header 
			JLabel lblTitle = new JLabel("< 좌표변환 프로그램 >");
			lblTitle.setFont(new Font(lblTitle.getName(), Font.CENTER_BASELINE, 15));
			lblTitle.setBounds(144, 21, 200, 14);
			getContentPane().add(lblTitle);

			// *** Header ***//
			JLabel lblSrcShp = new JLabel("원본 좌표계 :");
			lblSrcShp.setFont(new Font(lblSrcShp.getName(), Font.PLAIN, 13));
			lblSrcShp.setBounds(50, 51, 89, 14);
			getContentPane().add(lblSrcShp);

			JLabel lblCovShp = new JLabel("변환 좌표계 :");
			lblCovShp.setFont(new Font(lblCovShp.getName(), Font.PLAIN, 13));
			lblCovShp.setBounds(50, 76, 89, 14);
			getContentPane().add(lblCovShp);

			JLabel lblSrcPath = new JLabel("원본 SHP 위치 :");
			lblSrcPath.setFont(new Font(lblSrcPath.getName(), Font.PLAIN, 13));
			lblSrcPath.setBounds(40, 100, 95, 14);
			getContentPane().add(lblSrcPath);
            
			JLabel lblOutPath = new JLabel("출력 SHP 위치 :");
			lblOutPath.setFont(new Font(lblOutPath.getName(), Font.PLAIN, 13));
			lblOutPath.setBounds(40, 123, 95, 14);
			getContentPane().add(lblOutPath);
           
			// *** Add Form ***//
			final JComboBox<String> cbSrcShp = new JComboBox<String>();
			cbSrcShp.setBounds(155, 51, 150, 20);
			getContentPane().add(cbSrcShp);
			putAllEspgCodeToCombo(cbSrcShp);

			final JComboBox<String> cbConvShp = new JComboBox<String>();
			cbConvShp.setBounds(155, 76, 150, 20);
			getContentPane().add(cbConvShp);
			putAllEspgCodeToCombo(cbConvShp);

			final TextField txtSrcPath = new TextField("");
			txtSrcPath.setBounds(155, 100, 200, 20);
			getContentPane().add(txtSrcPath);
			
			final JButton btnSrcPath = new JButton("찾기");
			btnSrcPath.setBounds(370, 100, 70, 20);
			getContentPane().add(btnSrcPath);
			btnSrcPath.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					JFileChooser fileChooser = new JFileChooser(); 
					FileNameExtensionFilter shp = new FileNameExtensionFilter("Esri SHP file(.shp)", "shp", "SHP");
					fileChooser.addChoosableFileFilter(shp);
					fileChooser.setFileFilter(shp);
					fileChooser.setAcceptAllFileFilterUsed(false);
					//fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					

					int result = fileChooser.showOpenDialog(ShpConverter.this); 
					if (result == JFileChooser.APPROVE_OPTION) { 

						File selectedFile = fileChooser.getSelectedFile();
						
						//寃쎈줈 異쒕젰 
						txtSrcPath.setText(selectedFile.getAbsolutePath()); 
					}
				}
			});

			final TextField txtOutPath = new TextField("");
			txtOutPath.setBounds(155, 123, 200, 20);
			getContentPane().add(txtOutPath);
			
			final JButton btnOutPath = new JButton("찾기");
			btnOutPath.setBounds(370, 123, 70, 20);
			getContentPane().add(btnOutPath);
			btnOutPath.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					JFileChooser fileChooser = new JFileChooser(); 
					FileNameExtensionFilter shp = new FileNameExtensionFilter("Esri SHP file(.shp)", "shp", "SHP");
					fileChooser.addChoosableFileFilter(shp);
					fileChooser.setFileFilter(shp);
					fileChooser.setAcceptAllFileFilterUsed(false);

					int result = fileChooser.showOpenDialog(ShpConverter.this); 
					if (result == JFileChooser.APPROVE_OPTION) { 
						File selectedFile = fileChooser.getSelectedFile(); 
						txtOutPath.setText(selectedFile.getAbsolutePath()); 
					}
				}
			});

			progressBar.setBounds(50, 150, 350, 20);
			getContentPane().add(progressBar);

			final JButton btnConv = new JButton("변환");
			btnConv.setBounds(200, 181, 90, 20);
			getContentPane().add(btnConv);
			
			btnConv.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					File shpFile = new File(txtSrcPath.getText().trim());
					File targetFile = new File(txtOutPath.getText().trim());
					String fromEPSG = (String)cbSrcShp.getSelectedItem();
					String toEPSG = (String)cbConvShp.getSelectedItem();
					
					activity = new ShpHandler(shpFile, targetFile, fromEPSG, toEPSG, progressBar, btnConv);
					activity.execute();
				}
			});
			
		}
	}
}

//https://www.experts-exchange.com/questions/27507961/How-to-make-a-JFrame-constantly-stay-in-front-other-JFrames.html
