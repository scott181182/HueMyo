package com.bixfordstudios.hue;

import java.util.HashMap;

import com.bixfordstudios.myo.MyoController;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

public class MainController
{
	public MyoController myo;
	public HueController hue;
	
	public MainFrame frame;
	
	public MainController()
	{
		hue = new HueController(this);
		myo = new MyoController(this);
	}
	
	public void setFrame(MainFrame frame)
	{
		this.frame = frame;
	}
	
	
	
	public void turnCurrLight(boolean on)
	{
		if(frame.currentLight.equals("All"))
		{
			for(int i = 1; i < frame.lightSelection.getItemCount() - 1; i++)
			{
				PHLight light = hue.getLight(frame.lightSelection.getItemAt(i));
				PHLightState lightState = new PHLightState();
				lightState.setOn(on);
				lightState.setTransitionTime(0);
				hue.phHueSDK.getSelectedBridge().updateLightState(light, lightState, hue.lightListener);
			}
		}
		else
		{
			PHLight light = hue.getLight(frame.currentLight);
			if(light == null) { return; }
			PHLightState lightState = new PHLightState();
			lightState.setOn(on);
			lightState.setTransitionTime(0);
			hue.phHueSDK.getSelectedBridge().updateLightState(light, lightState, hue.lightListener);
		}
	}
	
	public void signalLight(String lightName)
	{
		if(lightName.equals("None")) { return; }
		HashMap<String, PHLightState> lightModes = new HashMap<>();
		if(lightName.equals("All"))
		{
			for(int i = 1; i < frame.lightSelection.getItemCount() - 1; i++)
			{
				String name = frame.lightSelection.getItemAt(i);
				PHLight light = hue.getLight(name);
				PHLightState oldState = light.getLastKnownLightState();
				PHLightState lightState = new PHLightState();
				lightState.setHue(oldState.getHue());
				lightState.setSaturation(oldState.getSaturation());
				lightState.setBrightness(oldState.getBrightness());
				lightState.setTransitionTime(oldState.getTransitionTime());
				lightState.setOn(oldState.isOn());
				lightModes.put(name, lightState);
			}
		}
		else
		{
			PHLight light = hue.getLight(lightName);
			if(light != null)
			{
				PHLightState oldState = light.getLastKnownLightState();
				PHLightState lightState = new PHLightState();
				lightState.setHue(oldState.getHue());
				lightState.setSaturation(oldState.getSaturation());
				lightState.setBrightness(oldState.getBrightness());
				lightModes.put(lightName, lightState);
			}
		}
		
		try
		{
			hue.setLight(lightName, HueController.MAX_HUE / 2, 254, 254, 0);
			Thread.sleep(750);
			turnCurrLight(false);
			Thread.sleep(750);
		}
		catch(InterruptedException ie) { ie.printStackTrace(); }

		for(String name : lightModes.keySet())
		{
			PHLightState state = lightModes.get(name);
			hue.setLight(name, state.getHue(), state.getSaturation(), state.getBrightness(), 0);
		}
	}
}
