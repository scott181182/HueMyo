package com.bixfordstudios.hue;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

public class MainFrame extends JFrame implements WindowListener
{
	private static final long serialVersionUID = 1L;
	
	public static final String TITLE = "HueMyo Interface";
	public static final Dimension STARTING_SIZE = new Dimension(800, 600);
	
	// Hue Frame Variables
	public String currentLight;
	public JLabel bridgeStatus;
	public JComboBox<String> lightSelection;
	public LightOptions lightOptionWindow;
	public JPanel lightControlPanel;
	
	// Myo Frame Variables
	public JLabel myoConnectedLabel, myoStatusLabel, myoGyroLabel, myoOrientationLabel;
	public JButton myoConnectionButton, myoTimedUnlockButton, myoUnlockButton, myoLockButton, myoTareButton, myoControlButton;

	private MainController controller;
	
	public MainFrame(MainController c)
	{
		this.controller = c;
		
		this.setTitle(TITLE);
		this.setSize(STARTING_SIZE);
		this.setLayout(new GridLayout(1, 2));
		this.addWindowListener(this);
		
		createGUI();
		this.setVisible(true);
	}
	private void createGUI()
	{
		JPanel huePanel = new JPanel();
		huePanel.setLayout(new BoxLayout(huePanel, BoxLayout.Y_AXIS));
		huePanel.setBorder(BorderFactory.createTitledBorder("Hue Lights"));
		JPanel searchPanel = new JPanel();
		JButton searchButton = new JButton("Search for Bridges");
		searchButton.addActionListener((event) -> { controller.hue.findBridges(); });
		bridgeStatus = new JLabel("Not Connected.");
		searchPanel.add(searchButton);
		searchPanel.add(bridgeStatus);
		huePanel.add(searchPanel);
		
		
		JPanel lightSelectionPanel = new JPanel();
		lightSelectionPanel.setLayout(new BoxLayout(lightSelectionPanel, BoxLayout.Y_AXIS));
		lightSelectionPanel.setBorder(BorderFactory.createTitledBorder("Light Control"));
		lightSelection = new JComboBox<>(new String[] { "None" });
		lightSelection.addItemListener((event) ->
			{
				if(event.getStateChange() == ItemEvent.SELECTED)
				{
					currentLight = (String)event.getItem();
					formatLightPanel(currentLight);
					controller.signalLight(currentLight);
				}
			});
		lightSelectionPanel.add(lightSelection);
		
		lightControlPanel = new JPanel();
		lightControlPanel.setLayout(new BoxLayout(lightControlPanel, BoxLayout.Y_AXIS));
		
		
		lightSelectionPanel.add(lightControlPanel);
		huePanel.add(lightSelectionPanel);
		JButton lightConfigButton = new JButton("Light Config");
		lightConfigButton.addActionListener((event) -> 
			{ 
				if(lightOptionWindow == null) { lightOptionWindow = new LightOptions(); }
				lightOptionWindow.setVisible(true);
			});
		huePanel.add(lightConfigButton);
		
		
		
		
		
		JPanel myoPanel = new JPanel();
		myoPanel.setLayout(new BoxLayout(myoPanel, BoxLayout.Y_AXIS));
		myoPanel.setBorder(BorderFactory.createTitledBorder("Myo Armband"));
		
		myoConnectedLabel = new JLabel("Myo : Disconnected");
		myoPanel.add(myoConnectedLabel);
		myoConnectionButton = new JButton("Connect Myo");
		myoConnectionButton.addActionListener((event) -> { this.controller.myo.connect(); });
		myoPanel.add(myoConnectionButton);
		
		myoStatusLabel = new JLabel("Status : Disconnected");
		myoPanel.add(myoStatusLabel);
		myoTimedUnlockButton = new JButton("Unlock (timed)");
		myoTimedUnlockButton.setEnabled(false);
		myoTimedUnlockButton.addActionListener((event) -> { this.controller.myo.unlockMyo(true); });
		myoUnlockButton = new JButton("Unlock (hold)");
		myoUnlockButton.setEnabled(false);
		myoUnlockButton.addActionListener((event) -> { this.controller.myo.unlockMyo(false); });
		myoLockButton = new JButton("Lock");
		myoLockButton.setEnabled(false);
		myoLockButton.addActionListener((event) -> { this.controller.myo.lockMyo(); });
		myoPanel.add(myoTimedUnlockButton);
		myoPanel.add(myoUnlockButton);
		myoPanel.add(myoLockButton);
		
		myoGyroLabel = new JLabel("Gyro : (-,-,-)");
		myoPanel.add(myoGyroLabel);
		
		myoOrientationLabel = new JLabel("Orientation : (-,-,-)");
		myoPanel.add(myoOrientationLabel);
		myoTareButton = new JButton("Tare Orientation");
		myoTareButton.addActionListener((event) ->
			{
				this.controller.myo.roll = 0;
				this.controller.myo.pitch = 0;
				this.controller.myo.yaw = 0;
				this.controller.myo.tare = true;
			});
		myoPanel.add(myoTareButton);
		myoControlButton = new JButton("Control Lights");
		myoControlButton.addActionListener((event) ->
			{
				this.controller.myo.prevControlTime = -1;
				this.controller.myo.lightControl = !this.controller.myo.lightControl;
			});
		myoPanel.add(myoControlButton);
		
		
		
		this.add(huePanel);
		this.add(myoPanel);
	}
	private void formatLightPanel(String lightName)
	{
		lightControlPanel.setVisible(false);
		lightControlPanel.removeAll();
		if(lightName.equals("None")) { return; }
		
		JPanel huePanel = new JPanel();
		JTextField hueField = new JTextField(5);
		huePanel.add(new JLabel("Light Hue : "));
		huePanel.add(hueField);
		JPanel satPanel = new JPanel();
		JTextField satField = new JTextField(3);
		satPanel.add(new JLabel("Light Saturation : "));
		satPanel.add(satField);
		JPanel briPanel = new JPanel();
		JTextField briField = new JTextField(3);
		briPanel.add(new JLabel("Light Brightness : "));
		briPanel.add(briField);
		
		JButton changeLightButton = new JButton("Change Lights");
		changeLightButton.addActionListener((event) ->
			{ 
				System.out.println("Changing light: " + currentLight);
				if(currentLight.isEmpty()) { lightControlPanel.setVisible(false); }
				else if(currentLight.equals("All"))
				{
					controller.hue.setAllLights(
						Integer.parseInt(hueField.getText()),
						Integer.parseInt(satField.getText()),
						Integer.parseInt(briField.getText()),
						40);
				}
				else
				{
					controller.hue.setLight(currentLight,
						Integer.parseInt(hueField.getText()),
						Integer.parseInt(satField.getText()),
						Integer.parseInt(briField.getText()),
						40);
				}
			});
		lightControlPanel.add(huePanel);
		lightControlPanel.add(satPanel);
		lightControlPanel.add(briPanel);
		lightControlPanel.add(changeLightButton);
		
		lightControlPanel.setVisible(true);
	}
	
	@Override public void windowOpened(WindowEvent e) {  }
	@Override public void windowClosing(WindowEvent e) { this.dispose(); }
	@Override public void windowClosed(WindowEvent e)
	{ 
		controller.hue.phHueSDK.disableAllHeartbeat();
        System.out.println("Disabled heartbeat.");
        System.exit(0);
	}
	@Override public void windowIconified(WindowEvent e) {  }
	@Override public void windowDeiconified(WindowEvent e) {  }
	@Override public void windowActivated(WindowEvent e) {  }
	@Override public void windowDeactivated(WindowEvent e) {  }
	
	
	
	
	
	public class LightOptions extends JFrame implements WindowListener
	{
		private static final long serialVersionUID = 1L;
		
		public ArrayList<String> enabledLights = new ArrayList<>();
		TableModel tableModel;

		public LightOptions()
		{
			this.setTitle("Light Options");
			this.addWindowListener(this);
			createGUI();
			this.pack();
		}
		private void createGUI()
		{
			this.add(new JLabel("Detected Lights"));
			
			int numLights = controller.hue.lights.size();
			tableModel = new DefaultTableModel(numLights, 3)
				{
					private static final long serialVersionUID = 1L;
					@Override public boolean isCellEditable(int r, int c) { return c == 0; }
					@Override public Class<?> getColumnClass(int c) { switch(c) { case 0: return Boolean.class; default: return String.class; } }
					@Override public String getColumnName(int c) { return c == 0 ? "Can Control" : c == 1 ? "Light Name" : c == 2 ? "Status" : ""; }
				};
			for(int i = 0; i < numLights; i++)
			{
				tableModel.setValueAt(enabledLights.contains(controller.hue.lights.get(i).getName()) ? true : false, i, 0);
				tableModel.setValueAt(controller.hue.lights.get(i).getName(), i, 1);
				tableModel.setValueAt(controller.hue.lights.get(i).getLastKnownLightState().isOn() ? "On" : "Off", i, 2);
			}
			JTable lightTable = new JTable(tableModel);
			JScrollPane tablePane = new JScrollPane(lightTable);
			this.add(tablePane);
		}
		
		
		
		@Override public void windowOpened(WindowEvent e) {  }
		@Override public void windowClosing(WindowEvent e)
		{ 
			enabledLights.clear();
			for(int i = 0; i < tableModel.getRowCount(); i++)
			{
				String name = (String)tableModel.getValueAt(i, 1);
				boolean value = (boolean)tableModel.getValueAt(i, 0);
				if(value) { this.enabledLights.add(name); }
			}
			lightSelection.removeAllItems();
			lightSelection.addItem("None");
			for(String name : enabledLights) { lightSelection.addItem(name); }
			lightSelection.addItem("All");
			this.setVisible(false);
		}
		@Override public void windowClosed(WindowEvent e) {  }
		@Override public void windowIconified(WindowEvent e) {  }
		@Override public void windowDeiconified(WindowEvent e) {  }
		@Override public void windowActivated(WindowEvent e) {  }
		@Override public void windowDeactivated(WindowEvent e) {  }
	}
}
