package com.bixfordstudios.hue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import com.philips.lighting.data.HueProperties;
import com.bixfordstudios.hue.PushLinkFrame;
import com.philips.lighting.hue.listener.PHLightListener;
import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHBridgeSearchManager;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHMessageType;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHBridgeResource;
import com.philips.lighting.model.PHBridgeResourcesCache;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHHueParsingError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

public class HueController
{
    public static final int MAX_HUE=65535, MIN_HUE = 0;

    public PHHueSDK phHueSDK;
    
    public ArrayList<PHLight> lights;
    private PushLinkFrame pushLinkDialog;
    //private LightColoursFrame lightColoursFrame;
    
    private MainController parent;

    public HueController(MainController p)
    {
    	this.parent = p;
    	lights = new ArrayList<>();

		phHueSDK = PHHueSDK.create();
		phHueSDK.setAppName("HueMyo");     // e.g. phHueSDK.setAppName("QuickStartApp");
		//phHueSDK.setDeviceName("MyoController");  // e.g. If you are programming for Android: phHueSDK.setDeviceName(android.os.Build.MODEL);
        
		HueProperties.loadProperties();  // Load in HueProperties, if first time use a properties file is created.

        phHueSDK.getNotificationManager().registerSDKListener(listener);
    }

    public void findBridges() {
        phHueSDK = PHHueSDK.getInstance();
        PHBridgeSearchManager sm = (PHBridgeSearchManager) phHueSDK.getSDKService(PHHueSDK.SEARCH_BRIDGE);
        sm.search(true, true);
        parent.frame.bridgeStatus.setText("Looking for Bridges...");
    }

    // Local SDK Listener
    private PHSDKListener listener = new PHSDKListener()
    {
        @Override
        public void onAccessPointsFound(List<PHAccessPoint> accessPoint) {
        	// Handle your bridge search results here.  Typically if multiple results are returned you will want to display them in a list 
        	// and let the user select their bridge.   If one is found you may opt to connect automatically to that bridge.
        	if(accessPoint.size() <= 0)
        	{
                parent.frame.bridgeStatus.setText("No Bridges Found.");
        		JOptionPane.showMessageDialog(parent.frame, "Could not find a Hue Bridge", "Bridge Search Error", JOptionPane.ERROR_MESSAGE);
        	}
        	else
        	{
                parent.frame.bridgeStatus.setText("Bridges Found!");
        		int option = JOptionPane.showOptionDialog(parent.frame, "Select a bridge to use:", "Bridges Found", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, accessPoint.stream().map((PHAccessPoint ap) -> { return ap.getIpAddress(); }).toArray(), null);
        		if(option == JOptionPane.CANCEL_OPTION) { parent.frame.bridgeStatus.setText("Not connected."); }
        		else
        		{
        			PHAccessPoint selection = accessPoint.get(option);
        			selection.setUsername("newdeveloper");
            		phHueSDK.connect(selection);
        			parent.frame.bridgeStatus.setText("Connected to " + selection.getIpAddress());
        		}
        	}
        }
        
        @Override
        public void onCacheUpdated(List<Integer> cacheNotificationsList, PHBridge bridge) {
             // Here you receive notifications that the BridgeResource Cache was updated. Use the PHMessageType to   
             // check which cache was updated, e.g.
            if (cacheNotificationsList.contains(PHMessageType.LIGHTS_CACHE_UPDATED)) {
               System.out.println("Lights Cache Updated ");
               lights.clear();
               lights.addAll(bridge.getResourceCache().getAllLights());
            }
            
            if(parent.myo.lightControl)
            {
            	parent.myo.canUpdateLight = true;
            }
        }

        @Override
        public void onBridgeConnected(PHBridge b)
        {
            phHueSDK.setSelectedBridge(b);
            phHueSDK.enableHeartbeat(b, 1000);
            lights.clear();
            lights.addAll(b.getResourceCache().getAllLights());
            System.out.println("Connected and Enabled Heartbeat!");
            // Here it is recommended to set your connected bridge in your sdk object (as above) and start the heartbeat.
            // At this point you are connected to a bridge so you should pass control to your main program/activity.
            // Also it is recommended you store the connected IP Address/ Username in your app here.  This will allow easy automatic connection on subsequent use. 
        }

        @Override
        public void onAuthenticationRequired(PHAccessPoint accessPoint)
        {
        	System.out.println("Authentication Required...");
            phHueSDK.startPushlinkAuthentication(accessPoint);
            // Arriving here indicates that Pushlinking is required (to prove the User has physical access to the bridge).  Typically here
            // you will display a pushlink image (with a timer) indicating to to the user they need to push the button on their bridge within 30 seconds.
            
            pushLinkDialog = new PushLinkFrame(parent);
            pushLinkDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            pushLinkDialog.setModal(true);
            pushLinkDialog.setLocationRelativeTo(null); // Center the dialog.
            pushLinkDialog.setVisible(true);
        }

        @Override
        public void onConnectionResumed(PHBridge bridge) 
        {
        	//System.out.println("Connection Resumed!");
        }

        @Override
        public void onConnectionLost(PHAccessPoint accessPoint)
        {
        	// Here you would handle the loss of connection to your bridge.
        	System.out.println("Connection Lost :(");
        }
        
        @Override
        public void onError(int code, final String message)
        {
        	// Here you can handle events such as Bridge Not Responding, Authentication Failed and Bridge Not Found
        	if(code == 1157) // No bridge found
        	{
                parent.frame.bridgeStatus.setText("No Bridges Found.");
        		JOptionPane.showMessageDialog(parent.frame, "Could not find a Hue Bridge", "Bridge Search Error", JOptionPane.ERROR_MESSAGE);
        	}
        	else
        	{
	        	System.err.println("[Error] Code " + code);
	        	System.err.println("   " + message);
        	}
        }

        @Override
        public void onParsingErrors(List<PHHueParsingError> parsingErrorsList)
        {
            // Any JSON parsing errors are returned here.  Typically your program should never return these.
        	System.out.println("JSON Parsing Error.");    
        }
    };

    public PHSDKListener getListener() {
        return listener;
    }

    public void setListener(PHSDKListener listener) {
        this.listener = listener;
    }

    public void randomLights() {
        PHBridge bridge = phHueSDK.getSelectedBridge();
        PHBridgeResourcesCache cache = bridge.getResourceCache();

        List<PHLight> allLights = cache.getAllLights();
        Random rand = new Random();

        for (PHLight light : allLights) {
            PHLightState lightState = new PHLightState();
            lightState.setHue(rand.nextInt(MAX_HUE));
            bridge.updateLightState(light, lightState); // If no bridge response is required then use this simpler form.
        }
    }
    public void setAllLights(int h, int s, int b, int t)
    {
        for (int i = 1; i < parent.frame.lightSelection.getItemCount() - 1; i++)
        {
        	setLight(parent.frame.lightSelection.getItemAt(i), h, s, b, t);
        }
    }
    public void setLight(String name, int h, int s, int b, int t)
    {
    	if(name.equals("All")) { setAllLights(h, s, b, t); return; }
    	for(PHLight light : lights)
    	{
    		if(light.getName().equals(name))
    		{
    			if(!parent.frame.lightOptionWindow.enabledLights.contains(light.getName())) { return; }
                PHLightState lightState = new PHLightState();
                lightState.setHue(h);
                lightState.setSaturation(s);
                lightState.setBrightness(b);
                lightState.setTransitionTime(t);
                lightState.setOn(true);
                phHueSDK.getSelectedBridge().updateLightState(light, lightState, lightListener);
    		}
    	}
    }
    public PHLight getLight(String name)
    {
    	for(PHLight light : lights)
    	{
    		if(light.getName().equals(name))
    		{
    			return light;
    		}
    	}
    	return null;
    }
    
    PHLightListener lightListener = new PHLightListener()
    {
		@Override public void onError(int code, String message)
		{
        	System.err.println("Light Error: Code " + code);
        	System.err.println(message);
		}
		@Override public void onStateUpdate(Map<String, String> successAttribute, List<PHHueError> errorAttribute)
		{
			//System.out.println("Light Updated.");
		}
		@Override public void onSuccess()
		{
			System.out.println("Light Updated Successfully!");
		}
		@Override public void onReceivingLightDetails(PHLight light)
		{
			
		}
		@Override public void onReceivingLights(List<PHBridgeResource> lights)
		{
			
		}
		@Override public void onSearchComplete()
		{
			
		}
    };
}
