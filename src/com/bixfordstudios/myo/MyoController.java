package com.bixfordstudios.myo;

import com.bixfordstudios.hue.MainController;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.FirmwareVersion;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.Vector3;
import com.thalmic.myo.enums.Arm;
import com.thalmic.myo.enums.UnlockType;
import com.thalmic.myo.enums.VibrationType;
import com.thalmic.myo.enums.XDirection;

import static com.bixfordstudios.hue.HueController.MAX_HUE;

public class MyoController implements DeviceListener
{
	public boolean lightControl = false, tare = false;
	public boolean canUpdateLight = false;
	public double roll = 0, pitch = 0, yaw = 0, accelTare = 0;
	public long prevControlTime = -1;
	
	protected Hub hub;
	
	private Myo myo;
	private MainController parent;
	private Thread sensor;
	
	public MyoController(MainController p)
	{
		this.parent = p;
		
		hub = new Hub();
		hub.addListener(this);
	}
	public void connect()
	{
		System.out.println();
		myo = hub.waitForMyo(-1);
		myo.vibrate(VibrationType.VIBRATION_MEDIUM);
		
		sensor = new Thread(myoThread);
		sensor.start();
	}
	
	public void unlockMyo(boolean timed) { myo.unlock(timed ? UnlockType.UNLOCK_TIMED : UnlockType.UNLOCK_HOLD); }
	public void lockMyo() { myo.lock(); }
	
	protected Runnable myoThread = () ->
		{
			hub.run(-1);
		};
	
	
	
	
	
	private boolean listening = false;
	
	@Override
	public void onPair(Myo myo, long timestamp, FirmwareVersion firmwareVersion)
	{
		System.out.println("Myo paired!");
	}

	@Override
	public void onUnpair(Myo myo, long timestamp)
	{
		System.out.println("Myo unpaired.");
	}

	@Override
	public void onConnect(Myo myo, long timestamp, FirmwareVersion firmwareVersion)
	{
		System.out.println("Myo connected!");
		parent.frame.myoConnectedLabel.setText("Myo : Connected!");
		parent.frame.myoConnectionButton.setEnabled(false);
		parent.frame.myoStatusLabel.setText("Status : Locked");
		parent.frame.myoTimedUnlockButton.setEnabled(true);
		parent.frame.myoUnlockButton.setEnabled(true);
	}

	@Override
	public void onDisconnect(Myo myo, long timestamp)
	{
		System.out.println("Myo disconnected.");
		parent.frame.myoConnectedLabel.setText("Myo : Disconnected");
		parent.frame.myoConnectionButton.setEnabled(true);
		parent.frame.myoStatusLabel.setText("Status : Disconnected");
		parent.frame.myoTimedUnlockButton.setEnabled(false);
		parent.frame.myoUnlockButton.setEnabled(false);
		parent.frame.myoLockButton.setEnabled(false);
		
		sensor.interrupt();
		System.out.println("Sensor has been interrupted");
	}

	@Override
	public void onArmSync(Myo myo, long timestamp, Arm arm, XDirection xDirection)
	{
		System.out.println("Myo Arm synced!");
	}

	@Override
	public void onArmUnsync(Myo myo, long timestamp)
	{
		System.out.println("Myo Arm unsynced.");
	}

	@Override
	public void onUnlock(Myo myo, long timestamp)
	{
		System.out.println("Myo unlocked!");
		listening = true;
		parent.frame.myoStatusLabel.setText("Status : Unlocked");
		parent.frame.myoTimedUnlockButton.setEnabled(false);
		parent.frame.myoUnlockButton.setEnabled(false);
		parent.frame.myoLockButton.setEnabled(true);
	}

	@Override
	public void onLock(Myo myo, long timestamp) 
	{
		System.out.println("Myo locked.");
		listening = false;
		parent.frame.myoStatusLabel.setText("Status : Locked");
		parent.frame.myoTimedUnlockButton.setEnabled(true);
		parent.frame.myoUnlockButton.setEnabled(true);
		parent.frame.myoLockButton.setEnabled(false);
	}

	@Override
	public void onPose(Myo myo, long timestamp, Pose pose)
	{
		System.out.println("Myo posed : " + pose.toString());
		if(listening)
		{
			int index = -1;
			switch(pose.getType())
			{
				case FIST:
					myo.vibrate(VibrationType.VIBRATION_SHORT);
					boolean on = parent.hue.getLight(parent.frame.currentLight).getLastKnownLightState().isOn();
					parent.turnCurrLight(!on);
					break;
				case FINGERS_SPREAD:
					parent.frame.myoTareButton.doClick();
					parent.frame.myoControlButton.doClick();
					break;
				case WAVE_IN:
					index = parent.frame.lightSelection.getSelectedIndex();
					if(index > 0)
					{
						parent.frame.lightSelection.setSelectedIndex(index - 1);
					}
					break;
				case WAVE_OUT:
					index = parent.frame.lightSelection.getSelectedIndex();
					if(index < parent.frame.lightSelection.getItemCount() - 1)
					{
						parent.frame.lightSelection.setSelectedIndex(index + 1);
					}
					break;
				default:
					break;
			}
		}
	}

	@Override
	public void onOrientationData(Myo myo, long timestamp, Quaternion rotation)
	{
		
	}

	@Override
	public void onAccelerometerData(Myo myo, long timestamp, Vector3 accel)
	{
		if(tare)
		{
			accelTare = accel.getX();
			tare = false;
		}
		//System.out.println("[accel] (" + accel.getX() + ", " + accel.getY() + ", " + accel.getZ() + ")");
		if(lightControl)
		{
			if(prevControlTime == -1) { prevControlTime = System.currentTimeMillis(); canUpdateLight = true; }
			//long currTime = System.currentTimeMillis();
			//long delta = currTime - prevControlTime;
			if(canUpdateLight)
			{
				parent.hue.setLight(parent.frame.currentLight, (int)Math.abs((accel.getX() - accelTare) * MAX_HUE), 254, 254, 1);
				System.out.println("[accel] " + (int)Math.abs((accel.getX() - accelTare) * MAX_HUE));
				canUpdateLight = false;
			}
			/*
			if(delta > 250)
			{
				canUpdateLight = true;
				prevControlTime = currTime;
			} */
		}
		parent.frame.myoOrientationLabel.setText(String.format("Accel: (%5.3f, %5.3f, %5.3f)", accel.getX(), accel.getY(), accel.getZ()));
	}

	@Override
	public void onGyroscopeData(Myo myo, long timestamp, Vector3 gyro)
	{
		//System.out.println("Myo rotated : (" + gyro.getX() + ", " + gyro.getY() + ", " + gyro.getZ() + ")");
		//parent.frame.myoGyroLabel.setText(String.format("Gyro : (%8.0f, %8.0f, %8.0f)", gyro.getX(), gyro.getY(), gyro.getZ()));
	}

	@Override
	public void onRssi(Myo myo, long timestamp, int rssi)
	{
		System.out.println("Myo RSSI : " + rssi);
	}

	@Override
	public void onEmgData(Myo myo, long timestamp, byte[] emg)
	{
		System.out.println("Myo EMG Data!");
	}
}
