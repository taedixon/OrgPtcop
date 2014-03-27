/*
 * Name: Org converter Application
 */

import java.util.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.filechooser.FileFilter;

public class OrgPtcopConvApp 
{
	//declare all GUI objects
	//frame and panels
	public static JFrame frame = new JFrame("ORG-PTCOP Converter");
	public static JPanel titlePanel = new JPanel();
	public static JPanel radioPanel = new JPanel();
	public static JPanel fileSelectPanel = new JPanel();
	public static JPanel textboxPanel = new JPanel();
	
	//labels
	public static JLabel titleLabel = new JLabel(".org => .ptcop Conversion program");
	public static JLabel fileLabel = new JLabel("File:");
	public static JLabel comboLabel = new JLabel("Add to drum length");
	
	//radio buttons
	public static ButtonGroup group1 = new ButtonGroup();
	public static ButtonGroup group2 = new ButtonGroup();
	//public static JRadioButton ptcop2OrgRadio = new JRadioButton("ptcop => org", false);
	//public static JRadioButton org2PtcopRadio = new JRadioButton("org => ptcop", true);
	public static JRadioButton singleRadio = new JRadioButton("Single file", true);
	public static JRadioButton multiRadio = new JRadioButton("All files in folder", false);
	
	//buttons
	public static JButton fileSelectPopupButton = new JButton("Find file..");
	public static JButton convertButton = new JButton("Convert!");
	public static JButton clearButton = new JButton("Clear output");
	//public static JButton previewButton = new JButton("Preview");
	
	//text fields
	public static JTextField filenameTextBox = new JTextField();
	public static JTextArea outputTextBox = new JTextArea();
	public static JScrollPane areaScrollPane;
	
	//combo box options
	public static String[] comboOpt = {"0", "1", "7", "11", "15", "31", "63", "127"};
	//combo box (drop down picker)
	public static JComboBox drumCombo = new JComboBox(comboOpt);
	//combo box value
	public static int comboVal = 4;
	
	//meow
	public static String kitty = "           (\\(\\\n           ).. \\\n           \\Y_, '-.\n             )     '.\n             |  \\/   \\ \n             \\\\ |\\_  |_\n             ((_/(__/_,'.\n                  (,----'\n                   `\n";
	//public static String paneBGCol = "0xFFBAAF";
	
	//used to make file dialog easier to use
	public static File prevLocation = new File(System.getProperty("user.dir"));
	
	
	OrgPtcopConvApp()
	{
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		java.net.URL iconURL = OrgPtcopConvApp.class.getResource("icon.png");
		Image icon = Toolkit.getDefaultToolkit().createImage(iconURL);
		frame.setIconImage(icon);
		addComponentsToPane(frame.getContentPane());
		frame.pack();
		//frame.setMinimumSize(frame.getSize());
		frame.setResizable(false);
		frame.setVisible(true);	
	}//main
	
	//Adds GUI components to the main window
	public static void addComponentsToPane(Container pane)
	{
		//make it look like a native application (Carrotlord)
		try {			
		UIManager.setLookAndFeel(
		UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException exceptionInfo) {
		// Ignore this exception.
		} catch (ClassNotFoundException exceptionInfo) {
		// Ignore this exception.
		} catch (InstantiationException exceptionInfo) {
		// Ignore this exception.
		} catch (IllegalAccessException exceptionInfo) {
		// Ignore this exception.
		}
		
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		//titlePanel.setBackground(Color.decode(paneBGCol));
		//radioPanel.setBackground(Color.decode(paneBGCol));
		radioPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		//fileSelectPanel.setBackground(Color.decode(paneBGCol));
		//textboxPanel.setBackground(Color.decode(paneBGCol));
		Listener l = new Listener();
		//setup and add title panel
		titlePanel.add(titleLabel);
		convertButton.addActionListener(l);
		titlePanel.add(convertButton);
		clearButton.addActionListener(l);
		titlePanel.add(clearButton);
		pane.add(titlePanel);
		
		//setup and add radio buttons panel
		//group1.add(ptcop2OrgRadio);
		//group1.add(org2PtcopRadio);
		group2.add(singleRadio);
		group2.add(multiRadio);
		//radioPanel.add(ptcop2OrgRadio);
		//radioPanel.add(org2PtcopRadio);
		java.net.URL iconURL = OrgPtcopConvApp.class.getResource("cat_radio_select.gif");
		Icon radioSelectIcon = new ImageIcon(iconURL);
		iconURL = OrgPtcopConvApp.class.getResource("cat_radio_no.gif");
		Icon radioDefaultIcon = new ImageIcon(iconURL);
		iconURL = OrgPtcopConvApp.class.getResource("cat_radio_press.gif");
		Icon radioPressIcon = new ImageIcon(iconURL);
		iconURL = OrgPtcopConvApp.class.getResource("cat_radio_rollover_s.gif");
		Icon radioOverSelectIcon = new ImageIcon(iconURL);
		iconURL = OrgPtcopConvApp.class.getResource("cat_radio_rollover.gif");
		Icon radioOverIcon = new ImageIcon(iconURL);
		singleRadio.setSelectedIcon(radioSelectIcon);
		singleRadio.setPressedIcon(radioPressIcon);
		singleRadio.setIcon(radioDefaultIcon);
		singleRadio.setRolloverSelectedIcon(radioOverSelectIcon);
		singleRadio.setRolloverIcon(radioOverIcon);
		//singleRadio.setBackground(Color.decode(paneBGCol));
		radioPanel.add(singleRadio);
		multiRadio.setSelectedIcon(radioSelectIcon);
		multiRadio.setPressedIcon(radioPressIcon);
		multiRadio.setIcon(radioDefaultIcon);
		multiRadio.setRolloverSelectedIcon(radioOverSelectIcon);
		multiRadio.setRolloverIcon(radioOverIcon);
		//multiRadio.setBackground(Color.decode(paneBGCol));
		radioPanel.add(multiRadio);
		drumCombo.setSelectedIndex(2);
		drumCombo.addActionListener(l);
		radioPanel.add(drumCombo);
		radioPanel.add(comboLabel);
		pane.add(radioPanel);
		
		//setup and add file select text box dialogue.. whatever
		fileSelectPanel.add(fileLabel);
		Dimension d1 = new Dimension();
		d1.setSize(256, 25);
		filenameTextBox.setPreferredSize(d1);
		filenameTextBox.setText("");
		filenameTextBox.addKeyListener(new KeyListener() {

			public void keyPressed(KeyEvent arg0) {
				// TODO Auto-generated method stub
				if (arg0.getKeyCode() == KeyEvent.VK_ENTER)
					convertButton.doClick();
			}

			public void keyReleased(KeyEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			public void keyTyped(KeyEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
		});
		fileSelectPanel.add(filenameTextBox);
		fileSelectPopupButton.addActionListener(l);
		fileSelectPanel.add(fileSelectPopupButton);
		pane.add(fileSelectPanel);
		
		//setup and add the text area
		outputTextBox.setLineWrap(true);
		outputTextBox.setWrapStyleWord(true);
		outputTextBox.setEditable(false);
		outputTextBox.setFont(new Font("Courier", Font.PLAIN, 12));
		outputTextBox.setText(kitty);
		outputTextBox.append("This program is supposed to be very easy to use.\nIf you want to convert one file,\ngive the path to the file name.\n");
		outputTextBox.append("If you want to convert a folder full of files,\ngive the path to the folder.\nAll files output to the same location\nthat the input file was in.\n");
		outputTextBox.append("--Noxid\n");
		areaScrollPane = new JScrollPane(outputTextBox);
		areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		areaScrollPane.setPreferredSize(new Dimension(200, 300));
		areaScrollPane.setAutoscrolls(true);
		textboxPanel.add(areaScrollPane);
		pane.add(areaScrollPane);
	}
	
	//Handles all user actions
	public static class Listener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{			
			String filename = filenameTextBox.getText();
			
			if (e.getSource().equals(convertButton))
			{
				if (drumCombo.getItemAt(drumCombo.getSelectedIndex()).equals("infinity")) {
					JOptionPane.showMessageDialog(frame, "thats TOO MANY\ntry agaig", "No you bloated doushe", JOptionPane.INFORMATION_MESSAGE);
					return;
				} else {
					comboVal = Integer.parseInt((String) drumCombo.getItemAt(drumCombo.getSelectedIndex()));
				}
				if (singleRadio.isSelected())
				{
					//single file, org to ptcop
					//get filename
					if (!filename.endsWith(".org"))
						filename += ".org";
					File file = new File(filename);
					if (!file.exists())
					{
						outputTextBox.append(filename + " does not exist!\n");
						return;
					}
					//unnecessary?
					if (file.isDirectory())
					{
						outputTextBox.append("This is a directory, not a filepath\n");
						return;
					}
					outputTextBox.append("\nReading " + file.getName() + "\n");
					org2Ptcop(file);
					
				} else if (multiRadio.isSelected()) {
					//multi file, org to ptcop
					//get filename
					File theFile = new File(filename);
					if (!theFile.isDirectory())
					{
						outputTextBox.append("location not a directory!\n");
						return;
					}
					outputTextBox.append("========================\n");
					outputTextBox.append("Multi org>ptcop conversion\n");
					BatchConverter convert = new BatchConverter(theFile);
					convert.execute();
				}//end convert button
			} else if (e.getSource().equals(fileSelectPopupButton))	{
				JFileChooser chooser = new JFileChooser();
				chooser.setCurrentDirectory(prevLocation);
				if (multiRadio.isSelected())
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				else
				{
					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					chooser.setFileFilter(new OrgFileFilter());
				}
				if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
				{
					File theFile = chooser.getSelectedFile();
					prevLocation = theFile;
					filenameTextBox.setText(theFile.getAbsolutePath());
				}				
			} else if (e.getSource().equals(clearButton)) {
				outputTextBox.setText("Remember, smiles are free :]\n");
			}
		}//actionPerformed
	}//Listener
	public static void org2Ptcop(File orgFile)
	{
		OrganyaFile org = new OrganyaFile(orgFile);
		outputTextBox.append("File size: " + orgFile.length() + " bytes\n");
		outputTextBox.append("Org has " + org.getNumEvent() + " events\n");
		if (org.getNumEvent() > 0) {
			PtcopFile ptcop = new PtcopFile(org);
			String orgName = orgFile.getPath();
			String ptName = orgName.replace(".org", ".ptcop");
			File ptFile = new File(ptName);
			ptcop.saveToFile(ptFile);
			outputTextBox.append("Ptcop has " + ptcop.getNumEvent() + " events\n");
			outputTextBox.append("Converted size: " + ptFile.length() + " bytes\n");
			outputTextBox.append("Conversion complete\n");
			outputTextBox.setCaretPosition(outputTextBox.getText().length());
		} else {
			outputTextBox.append("Skipping conversion\n");
		}
	}
	//filename filters for folder search
	public static class OrgFilter implements FilenameFilter 
	{		
		public boolean accept(File directory, String filename)
		{
			filename.toLowerCase();
			if (filename.endsWith(".org"))
				return true;
			File targetFile = new File(directory.getAbsolutePath() + "\\" + filename);
			if (targetFile.isDirectory())
				return true;
			else 
				return false;
		}
	}
	
	//file filters for FileSelectDialogue
	public static class OrgFileFilter extends FileFilter
	{
		public boolean accept(File theFile) 
		{
			if (theFile.isDirectory())
				return true;
			if (theFile.getName().toLowerCase().endsWith(".org"))
				return true;
			else
				return false;
		}		
		public String getDescription() {return "Organya files";}
	}
	
	static class BatchConverter extends SwingWorker {

		File originalDirectory;
		
		BatchConverter(File searchDir) {
			originalDirectory = searchDir;

			drumCombo.setEnabled(false);
			convertButton.setEnabled(false);
			clearButton.setEnabled(false);
			fileSelectPopupButton.setEnabled(false);
		}
		protected Object doInBackground() throws Exception {
			FilenameFilter filter = new OrgFilter();
			Queue directoryQueue = new LinkedList();
			directoryQueue.add(originalDirectory);
			//seach all directories
			while (!directoryQueue.isEmpty())
			{
				File currentDir = (File) directoryQueue.remove();
				String[] dirList = currentDir.list(filter);
				int count;
				for (count = 0; count < dirList.length; count++)
				{
					File currentFile = new File(currentDir.getAbsolutePath() + "\\" + dirList[count]);
					if (currentFile.isDirectory())
						directoryQueue.add(currentFile);
					else
					{
						outputTextBox.append("\nReading " + currentFile.getName() + "\n");
						org2Ptcop(currentFile);
					}							
				}//for each filename in the list	
				outputTextBox.append("\nProcessed " + count + " files in directory \n" + currentDir + "\n");
			}//while we have more directories to search	
			setProgress(100);
			return null;
		}
		
		public void done() {
			drumCombo.setEnabled(true);
			convertButton.setEnabled(true);
			clearButton.setEnabled(true);
			fileSelectPopupButton.setEnabled(true);
			outputTextBox.append("\nFinished batch conversion\n========================\n");
			outputTextBox.setCaretPosition(outputTextBox.getText().length());
		}
	}
	
	public static void main(String[] args) {
	      // Use the event dispatch thread to build the UI for thread-safety.
	      SwingUtilities.invokeLater(new Runnable() 
	      {
	         public void run() { new OrgPtcopConvApp(); }
	      });
	}
}
