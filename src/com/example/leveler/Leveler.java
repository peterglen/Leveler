package com.example.leveler;

import java.sql.Date;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

//////////////////////////////////////////////////////////////////////////

public class Leveler extends Activity implements SensorEventListener,
					OnGestureListener {

	// Constants
	static final double pideg = 180 / Math.PI;	// Degree to Radian constant    
	static final double gravity = 10;			// 9.81, but sensors differ    

	final String calibx_name = "calibx";		// Shared preferences keys
	final String caliby_name = "caliby";
	final String calibz_name = "calibz";
	
	static double accel = 0, prevaccel = 0;
	static double voldown = 0, volup = 0;
	static int dir = 0, prevdir = 0;
	
	// Configurables	
	final double delayalpha = 0.1;		// dampening for sensor read
	final double delayaccel = 0.05;		// dampening for acceleration
	
	public static Context mContext = null;
	public static String acname = "No sensor present"; 
	public static String acstr  = "No sensor value"; 
	
	public static String vertstr  = ""; 
	public static String horstr  = ""; 
	public static String speedstr  = "";
	public static String stepstr = "0"; 
	
	double calibx = 0, caliby = 0, calibz = 0;
	
	private double 	delayxx = 0, delayyy = 0, delayzz = 0, xx = 0, yy = 0, zz = 0;
	
	GestureDetector  gesturedet;
	
	int speedval = SensorManager.SENSOR_DELAY_NORMAL;
	
	boolean	swapxy = false; boolean negatex = false; boolean negatey = false;
	boolean pref_beep = false; boolean pref_vibrate = false;
	int sndcnt = 0, vibcnt = 0;
	int idx = 0, steps = 0, hardsteps = 0;
	
	// Sensor related declarations
	SensorManager 	mSensorManager;	Sensor	mAccelerometer;
	
	// Working on this view
	LevelerView myMidView;
	
	float appliedAcceleration = 0, currentAcceleration = 0;		
	float velocity = 0;
	
	Date lastUpdate, lastStep;
	
	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        mContext = this;
		
        //Debug.printf("*******************onCreate %d", android.os.Build.VERSION.SDK_INT);
        
        setContentView(R.layout.activity_leveler);
              
        myMidView = (LevelerView)findViewById(R.id.myMidView);                 
        lastUpdate = new Date(System.currentTimeMillis());  
        lastStep = new Date(System.currentTimeMillis());  
        
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor. TYPE_ACCELEROMETER);
             
        gesturedet = new  GestureDetector(this);
        
        acname = mAccelerometer.getName();              
    }
        
	@Override
	public boolean onTouchEvent(MotionEvent me) {

		//Debug.printf("onTouchEvent %d", me.getAction());
		//return 
		gesturedet.onTouchEvent(me);
		return true;

		}
   /* @Override
	protected  void onPostCreate (Bundle savedInstanceState)
    {
    	super.onPostCreate (savedInstanceState);
    	//Debug.printf("onPostCreate");
    }*/
    
    
    /*@Override
    public void onConfigurationChanged(Configuration _newConfig) {    	
    	super.onConfigurationChanged(_newConfig);
    	
    	//Debug.printf("%s %d", "Configuration changed", _newConfig.orientation);
    	    	
    	@SuppressWarnings("unused")
		boolean orient = false;
    	    	    
    	if (_newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
    		//Debug.print( "ORIENTATION_LANDSCAPE");   
    		orient = true; 	
    	}
    	
    	if (_newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
    		//Debug.print("ORIENTATION_PORTRAIT");   	
    		orient = false;
    	}    	
    }*/
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_leveler, menu);
        return true;
    }
    
    // Handle menu items
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
    	//Debug.printf("onOptionsItemSelected: %s", item.getTitle());
    	
    	if (item.getItemId() == R.id.menu_about) {
    		    		
    		Intent i = new Intent(this, About.class);
    		startActivity(i);    	
    		return(true);
    	}
    	
    	if (item.getItemId() == R.id.menu_settings) {
    		
    		//String message="Menu settings";    		
    		//Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    		
    		Intent i = new Intent(this, Leveler_Preferences.class);
    		startActivity(i);
    		
    		return(true);
    	}
    	
    	if (item.getItemId() == R.id.menu_Calibrate) {
    		
    		//Debug.printf("Calibrated xx=%2.2f yy=%2.2f", xx, yy);
    		
    		calibx = delayxx; caliby = delayyy; calibz = delayzz;
    	
    		// Save to shared preferences
    		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);    		    	
    		SharedPreferences.Editor editor = prefs.edit();
    		
    		editor.putFloat(calibx_name,  (float)calibx);
    		editor.putFloat(caliby_name,  (float)caliby);
    		editor.putFloat(calibz_name,  (float)calibz);
    		
   		 	// Commit the edits!
   	     	editor.commit();
    		    		
    		String message = "Calibrated CENTER to this particular orientation.";
    		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    		
    		return(true);
    	}
    	
   	return(super.onOptionsItemSelected(item));
    }
    
    protected void onResume() {
        super.onResume();
        
        //Debug.printf("onResume");
        ReadSettings();
        
        mSensorManager.registerListener(this, mAccelerometer, speedval);                                    
    }    		
        
    protected void onPause() {
        super.onPause();
       
        // Conserve power while not visible
        mSensorManager.unregisterListener(this);
        
        // Save bubble size to shared preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);    		    	
		SharedPreferences.Editor editor = prefs.edit();		
		String bubsizestr = String.valueOf(myMidView.bubble_size); 
		editor.putString(getString(R.string.pref_bubble),  bubsizestr);
				   
	 	// Commit the edits!
     	editor.commit();
    }
	
    //////////////////////////////////////////////////////////////////////
    
    private void ReadSettings()
    {
        // Re-read preferences
        SharedPreferences prefs =
        		PreferenceManager.getDefaultSharedPreferences(this);
        
        calibx = (double) prefs.getFloat(calibx_name,  0);
        caliby = (double) prefs.getFloat(caliby_name,  0);
        calibz = (double) prefs.getFloat(calibz_name,  0);
		       
        swapxy = prefs.getBoolean(getString(R.string.swap_xy),  false);
        
        //Debug.printf("swap_xy %b (%s)", swapxy, getString(R.string.swap_xy));
        
        negatex = prefs.getBoolean(getString(R.string.revese_x),  false);
        negatey = prefs.getBoolean(getString(R.string.revese_y),  false);
        
        pref_beep = prefs.getBoolean(getString(R.string.beep_on),  false);
        //Debug.printf("pref_beep %b (%s)", pref_beep, getString(R.string.beep_on));
        
        pref_vibrate = prefs.getBoolean(getString(R.string.vibrate_on),  false);
        //Debug.printf("pref_vibrate %b (%s)", pref_vibrate, getString(R.string.vibrate_on));

        String bubstr = prefs.getString(getString(R.string.pref_bubble),  "");
        
        try
	        {
	        myMidView.bubble_size = Integer.parseInt(bubstr);
	        } 
        catch(Exception ex)
	       	{
        	//Debug.printex("parseInt", ex);      	
        	// No need for intervention, already set to zero 
	       	}
                
        //Debug.printf("myMidView.bubble_size %d ", myMidView.bubble_size);
        
        myMidView.LimitBubbleSize();
        
        //Debug.printf("bubble_size=%d pref_bubble=%s", 
        //		myMidView.bubble_size, getString(R.string.pref_bubble), bubstr);
        
        speedstr = prefs.getString(getString(R.string.scanning_speed),  "");
        
        if(speedstr.equals(getString(R.string.pref_item_1)))
        		{
        		speedval = SensorManager.SENSOR_DELAY_UI;
        		}
        else if (speedstr.equals(getString(R.string.pref_item_2)))
        		{
        		speedval = SensorManager.SENSOR_DELAY_NORMAL;
        		}
        else if (speedstr.equals(getString(R.string.pref_item_3)))
        		{
        		speedval = SensorManager.SENSOR_DELAY_GAME;
        		}
        else if (speedstr.equals(getString(R.string.pref_item_4)))
        		{
        		speedval = SensorManager.SENSOR_DELAY_FASTEST;
        		}
        
        //Debug.printf("speedstr %s speedval %d)", speedstr, speedval);

        String bubcolorstr = prefs.getString(getString(R.string.bubble_color),  "");
                
        if(bubcolorstr.equals(getString(R.string.pref_col_blue)))
		 	{
        	myMidView.bubble_color = Color.argb(100, 100, 100, 255);		        	
		    }
        else if(bubcolorstr.equals(getString(R.string.pref_col_red)))
	    	{
	    	myMidView.bubble_color = Color.argb(100, 255, 100, 100);        	
	    	}
        else if(bubcolorstr.equals(getString(R.string.pref_col_gray)))
	    	{
	    	myMidView.bubble_color = Color.argb(100, 100, 100, 100);        	
	    	}        
        else if(bubcolorstr.equals(getString(R.string.pref_col_green)))
	    	{
	    	myMidView.bubble_color = Color.argb(100, 100, 255, 100);        	
	    	}   
        
        //Debug.printf("Bubble color '%s' %x", bubcolorstr, myMidView.bubble_color);
        
        String backstr = prefs.getString(getString(R.string.background),  "");        
        if(backstr.equals(getString(R.string.pref_bg_steel)))
		 	{
	    	myMidView.bgdrawable = R.drawable.background;                	
		    }
        if(backstr.equals(getString(R.string.pref_bg_wood)))
		 	{
	    	myMidView.bgdrawable = R.drawable.wood;                	
		    }
        if(backstr.equals(getString(R.string.pref_bg_stone)))
		 	{
	    	myMidView.bgdrawable = R.drawable.stone;                	
		    }
        if(backstr.equals(getString(R.string.pref_bg_bricks)))
		 	{
	    	myMidView.bgdrawable = R.drawable.bricks;                	
		    }
        if(backstr.equals(getString(R.string.pref_bg_white)))
  		 	{
  	    	myMidView.bgdrawable = R.drawable.white;                	
  		    }
        if(backstr.equals(getString(R.string.pref_bg_gray)))
		 	{
	    	myMidView.bgdrawable = R.drawable.gray;                	
		    } 
        if(backstr.equals(getString(R.string.pref_bg_dgray)))
		 	{
	    	myMidView.bgdrawable = R.drawable.dgray;                	
		    }
        
        //Debug.printf("Bubble BG '%s' %x", backstr,  myMidView.bgdrawable);        
        //myMidView.ApplySettings();
    }
    	
	//@Override
	public void onSensorChanged(SensorEvent event) {
		
		zz = event.values[2];
    	
		if(swapxy)
			{
			xx = event.values[1]; yy = event.values[0];
			}
		else
			{
			xx = event.values[0]; yy = event.values[1];
			}
		
		if(negatex)	{
			xx = -xx;
		}
		if (negatey) {
			yy = - yy;
		}
				     	
		// Delayed effect 
    	delayxx = delayxx * (1 - delayalpha) + xx * delayalpha;
    	delayyy = delayyy * (1 - delayalpha) + yy * delayalpha;
    	delayzz = delayzz * (1 - delayalpha) + zz * delayalpha;
        	
    	//accel = Math.sqrt(delayxx *  delayxx + delayyy * delayyy +
    		//	delayzz * delayzz);
    	
    	double orgaccel = Math.sqrt(xx *  xx + yy * yy + zz * zz);
    	accel = accel * (1 - delayaccel) + orgaccel * delayaccel;
            	
    	myMidView.AddAccel(accel);
    	
    	// Handle dynamic changes
    	if(prevaccel > accel)
    		{
    		dir = 1;
    		voldown = accel;
    		}
    	else if(prevaccel < accel)
    		{
	    	dir = 2;
	    	volup = accel;
    		}
    	
    	// Upper peak
		if(dir != prevdir && dir == 1)
			{
			double voldiff = Math.abs(voldown - volup);
			//Debug.printf("voldown %.2f  volup %2f voldiff %.3f",
			//		voldown, volup, voldiff);
			
			// Sufficient volume
			if(voldiff > 0.02)
				{
				Date timeNow = new Date(System.currentTimeMillis());
		    	long timeDelta = timeNow.getTime() - lastStep.getTime();    	
		    	
		    	// Sufficient time
		    	if(timeDelta > 200) 
		    		{
		    		steps++;
					
					if(voldiff > 0.2)
						{
						hardsteps ++;
						}					
					stepstr = String.format("%d (%d)", steps, hardsteps);
		    		}		    		
		    	//Debug.printf("Step update diff %d ms", timeDelta);
		    	lastStep.setTime(timeNow.getTime());    	
				}			
			}    	
    	prevdir = dir; prevaccel = accel;
    	
    	// Just in case it is a real fast sensor
    	Date timeNow = new Date(System.currentTimeMillis());
    	long timeDelta = timeNow.getTime() - lastUpdate.getTime();    	
    	if(timeDelta < 20) {
    		return;
    		}
    	
    	//Debug.printf("Update diff %d ms", timeDelta);
    	lastUpdate.setTime(timeNow.getTime());    	
		
    	acstr = String.format(
    			"  x=%+2.2f  y=%+2.2f  z=+%2.2f  accel=%2.2f", xx, yy, zz, accel);
    
    	// Create new values for display adjusted for calibration
    	double xx2 = delayxx - calibx, yy2 = delayyy - caliby;
    	    	
    	double xxx = Math.asin(xx2 / gravity) * pideg;
    	double yyy = Math.asin(yy2 / gravity) * pideg;
    	    			
    	// ===============================================================
    	// Format degree fields
    	
    	if(xxx > 65 || xxx == Double.NaN)
    		vertstr = "Left/Right: larger than 65 degrees";    	
    	else if(xxx < -65|| xxx == Double.NaN)
    		vertstr = "Left/Right: larger than -65 degrees";
    	else
    		vertstr = String.format("Left/Right: %+2.0f deg", xxx); 
    		
    	// ===============================================================
    	
    	if(yyy > 65|| yyy == Double.NaN)
    		horstr = "Up/Down: larger then 65 degrees";
    	else if(yyy < -65|| yyy == Double.NaN)
    		horstr = "Up/Down: larger then -65 degrees";
    	else
    		horstr = String.format("Up/Down: %+2.0f deg", yyy); 
    	
    	// ===============================================================
    	
    	double multi = myMidView.GetMinDim() / gravity;
    	double adjx = multi * xx2, adjy = multi * yy2;
    	    
    	if(pref_vibrate)
    		{
	    	if( Math.abs(xxx) < 1 && Math.abs(yyy) < 1)
	    		{
	    		if(vibcnt++ == 0)
	    			SimpleVibrator.vibrate(1000);	    		
	    		}
	    	else
    			vibcnt	= 0;
    		}
	    	
	    // Handle sound
    	if(pref_beep)
    		{   
    		if( Math.abs(xxx) < 1 && Math.abs(yyy) < 1)
	    		{
	    		if(sndcnt < 3)
	    			{
	    			if(playRCSound.play(R.raw.cricket) == true)
	    				sndcnt++;
	    			}
	    		}
	    	else 
	    		sndcnt = 0;
	    	}
	    	    		
		// Tell the display the new values delay is now done in pre-stage
    	myMidView.SetCoord(adjx, adjy);    	    			        	 
		}
	
   @Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	   //Debug.printf("onAccuracyChanged %d", accuracy);
   }

 
@Override
public boolean onDown(MotionEvent e) {
	//Debug.printf("onDown %d", e.getAction());
	return false;
}

@Override
public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
		float velocityY) {
	
	//Debug.printf("onFling %d  (%d) velocityX=%.2f velocityY=%.2f", e1.getAction(),
	//			e2.getAction(), velocityX, velocityY);
	
	return false;
}

@Override
public void onLongPress(MotionEvent e) {
	Debug.printf("onLongPress %d", e.getAction());
	
}

@Override
public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
		float distanceY) {
	Debug.printf("onScroll %d %s distanceX=%.2f, distanceY=%.2f", 
			e1.getAction(), e1.getAction(), distanceX, distanceY);
	
	//myMidView.SetCoord(-distanceX, distanceY);
	
	return false;
}

@Override
public void onShowPress(MotionEvent e) {
	Debug.printf("onShowPress %d", e.getAction());
	
}

@Override
public boolean onSingleTapUp(MotionEvent e) {
	Debug.printf("onSingleTapUp %d", e.getAction());
	return false;
}  
   

}

// -----------------------------------------------------------------------

class LevelerView extends TextView  {
	
	public double xdiff = 0; public double ydiff = 0;	
	float startdx = 0;	float startdy = 0;
	
	private AccelArr accelarr = new AccelArr();
	
	int count = 0;
	
	Paint circlePaint2, circlePaint3, circlePaint4;
	Paint mTextPaint, LinePaint, mStepPaint;
	
	// Configurable(s), assign defaults
	int 	old_bgdrawable = -1;		
	int 	bgdrawable = R.drawable.background;	
	int		old_bubble_color = -1;	
	int		bubble_color = Color.argb(100, 100, 255, 100); 	
	int 	bubble_size = 0; 	
	
	boolean 	firstdraw = true;		// Was there a prev. drawing 
	
	Path path;	Rect bound;	BitmapDrawable logoBitmap;	

	public LevelerView (Context context, AttributeSet ats, int defStyle) {
		super(context, ats, defStyle);
		InitView();		
	}
	
	public LevelerView (Context context) {
		super(context);
		InitView();		
	}
	
	public LevelerView (Context context, AttributeSet attrs) {
		super(context, attrs);
		InitView();	
	}
	
	
	void SetCoord(double inxdiff, double inydiff)
	{
		xdiff = inxdiff; ydiff = inydiff;
		
		// Tell it to update the screen
    	invalidate();
	}
		
	void AddAccel(double acc)
	{
		accelarr.Add(acc);		
	}
	
	void InitView() {
		
		//Debug.printf("LevelerView.InitView() width=%d", getMeasuredWidth() );
				
		//mScaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
		
		mStepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mStepPaint.setColor(Color.BLACK);
		mStepPaint.setTextSize(48);
		
		// Create the new paint brushes.
		mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mTextPaint.setColor(Color.BLACK);
		mTextPaint.setTextSize(24);
		
		circlePaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
		circlePaint2.setColor(Color.BLACK);		
		circlePaint2.setStyle(Paint.Style.STROKE);		
				
		circlePaint3 = new Paint(Paint.ANTI_ALIAS_FLAG);
		circlePaint3.setColor(Color.GRAY);		
		circlePaint3.setStyle(Paint.Style.STROKE);
		
		circlePaint4 = new Paint(Paint.ANTI_ALIAS_FLAG);
		//circlePaint4.setColor(bubble_color);		
		circlePaint4.setStyle(Paint.Style.FILL);
		circlePaint4.setAlpha(100);
				
		LinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		LinePaint.setColor(Color.GRAY);		
		LinePaint.setStyle(Paint.Style.STROKE);
			
		bound = new Rect();		
		mTextPaint.getTextBounds(Leveler.acname, 0,
						Leveler.acname.length(), bound);
						
		path = new Path(); 
	
		//Debug.printf("LevelerView.InitView 3");
		
		if(android.os.Build.VERSION.SDK_INT >= 11)
			{
			setLayerType(View.LAYER_TYPE_SOFTWARE, null);
			}
	
		
		setOnTouchListener(new View.OnTouchListener() {
		@Override public boolean onTouch(View v, MotionEvent event) {
		//if (MotionEvent.ACTION_DOWN != event.getAction()) {
			//Debug.printf("MotionEvent %d xx=%.2f yy=%.2f pointercount=%d", event.getAction(), 
				//		event.getAxisValue(MotionEvent.AXIS_X), 
					//			event.getAxisValue(MotionEvent.AXIS_Y),
						//			event.getPointerCount());
			
			if (MotionEvent.ACTION_DOWN == event.getAction()) 
				{
				//Debug.printf("Action down: x=%.2f y=%.2f ",	
					//	event.getAxisValue(MotionEvent.AXIS_X, 0), 
						//	event.getAxisValue(MotionEvent.AXIS_Y, 0));
				
				//ApplySettings();
				
				if(event.getPointerCount() == 2) {
				startdx = Math.abs(event.getAxisValue(MotionEvent.AXIS_X, 0) - 			
						event.getAxisValue(MotionEvent.AXIS_X, 1));
				startdy = Math.abs(event.getAxisValue(MotionEvent.AXIS_Y, 0) - 
						event.getAxisValue(MotionEvent.AXIS_Y, 1));
				
				//Debug.printf("Pinch start: x=%.2f y=%.2f ",	startdx, startdy);
				}								
			}
				
			if (MotionEvent.ACTION_UP == event.getAction()) 
			{				
				if(event.getPointerCount() == 2) {
					
				//Debug.printf("Pinch end: x=%.2f y=%.2f ",	startdx, startdy);
					
				startdx = Math.abs(event.getAxisValue(MotionEvent.AXIS_X, 0) - 
						event.getAxisValue(MotionEvent.AXIS_X, 1));
				startdy = Math.abs(event.getAxisValue(MotionEvent.AXIS_Y, 0) - 
						event.getAxisValue(MotionEvent.AXIS_Y, 1));
				
				//Debug.printf("Pinch end at: x=%.2f y=%.2f ",	startdx, startdy);
				}								
			}
			if (MotionEvent.ACTION_MOVE == event.getAction()) {
				if(event.getPointerCount() == 2) {
					//Debug.printf("Pinch: x=%.2f y=%.2f --- x=%.2f y=%.2f ", 
					//			event.getAxisValue(MotionEvent. AXIS_X, 0),
						//		event.getAxisValue(MotionEvent. AXIS_Y, 0),
							//	event.getAxisValue(MotionEvent. AXIS_X, 1),
						//		event.getAxisValue(MotionEvent. AXIS_Y, 1)
							//	);
					
					float currdx = Math.abs(event.getAxisValue(MotionEvent.AXIS_X, 0) - 
							event.getAxisValue(MotionEvent.AXIS_X, 1));
					
					float currdy = Math.abs(event.getAxisValue(MotionEvent.AXIS_Y, 0) -
							event.getAxisValue(MotionEvent.AXIS_Y, 1));
					
					//Debug.printf("Pinch delta: x=%.2f y=%.2f ", 
					//				currdx - startdx, currdy - startdy);
					
					if(Math.abs(currdy - startdy) < 20)
						{
							bubble_size += (currdy - startdy);
							LimitBubbleSize();
						}
					
					startdx = currdx;	startdy = currdy;
					invalidate();
					}
				}
					
		return true;
		} 
		});
		
				 		
	}
	
	
	// Contain bubble size within reasonable limits
	void LimitBubbleSize() {
			
		int mindim = GetMinDim();
		
		// Not inited yet
		if(mindim <= 0)
			return;
		
		// Give it a default value
		if(bubble_size == 0)
			bubble_size = mindim / 8;
		
		// Contain bubble size up
		if(bubble_size > mindim / 2)
			bubble_size = mindim / 2;
		
		if(bubble_size < 20)
			bubble_size = 20;
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int measuredHeight = measureHeight(heightMeasureSpec);
		int measuredWidth = measureWidth(widthMeasureSpec);
		setMeasuredDimension(measuredWidth, measuredHeight);
	}
	
		// Return measured widget height.
		private int measureHeight(int measureSpec) {
			int specMode = MeasureSpec.getMode(measureSpec);
			int specSize = MeasureSpec.getSize(measureSpec);
			// Default size if no limits are specified.
			int result = specSize;
			if (specMode == MeasureSpec.AT_MOST) {
			// Calculate the ideal size of your control within this maximum size.
			// If your control fills the available space return the outer bound.
			result = specSize;
			} else if (specMode == MeasureSpec.EXACTLY) {
			// If your control can fit within these bounds return that value.
			result = specSize;
			}
			return result;
	}
				
		private int measureWidth(int measureSpec) {
			int specMode = MeasureSpec.getMode(measureSpec);
			int specSize = MeasureSpec.getSize(measureSpec);
			// Default size if no limits are specified.
			int result = specSize;
			if (specMode == MeasureSpec.AT_MOST) {
			// Calculate the ideal size of your control within this maximum size.
			// If your control fills the available space return the outer bound.
			result = specSize;
			} else if (specMode == MeasureSpec.EXACTLY) {
			// If your control can fit within these bounds return that value.
			result = specSize;
			}
			return result;
		}
		
	@Override
	public void onDraw(Canvas canvas) {	
		
	// Render the text as usual using the TextView base class.
	super.onDraw(canvas);

	// Get the size of the control based on the last call to onMeasure.
	int height = getMeasuredHeight(); int width = getMeasuredWidth();
	
	if(firstdraw)
	{	
		firstdraw = false;
		//Debug.printf("firstdraw width=%d", width);
		accelarr.SetArrSize(width / 8);
	}
	
	// Find the center
	int px = width/2;	int py = height/2;
	
	//////////////////////////////////////////////////////////////////////
	// Look for new paint configurations, apply it
	
	if(old_bubble_color != bubble_color)
		{
		//Debug.printf("Setting bubble color %x", bubble_color);
		
		circlePaint4.setColor(bubble_color);	
		old_bubble_color = bubble_color;		
		}
	
	if(old_bgdrawable != bgdrawable)
		{
		//Debug.printf("Setting background %x", bgdrawable);
		
		logoBitmap = (BitmapDrawable)getResources().getDrawable(bgdrawable);		
		old_bgdrawable = bgdrawable;
		}
		
	//////////////////////////////////////////////////////////////////////
	// Draw background
	try
	{	
		for(int yy = 0; yy < height; yy += logoBitmap.getIntrinsicHeight())
		{
			for(int xx = 0; xx < width; xx += logoBitmap.getIntrinsicWidth())
			{
			canvas.drawBitmap(logoBitmap.getBitmap(), xx, yy, LinePaint);
			}
		}
	}
	catch(Exception ex)
	{
		//Debug.printf("EXCEPTION in Draw background");	
	}
	
	//////////////////////////////////////////////////////////////////
	// Draw Sensor info on top			
	float space = mTextPaint.getTextSize();
	float gap =  mTextPaint.descent();
	float textWidth = mTextPaint.measureText(Leveler.acname);	
	canvas.drawText(Leveler.acname, px - textWidth/2, space + gap, mTextPaint);
	
	//////////////////////////////////////////////////////////////////
	// Draw Sensor values on bottom			
	textWidth = mTextPaint.measureText(Leveler.acstr);	
	canvas.drawText(Leveler.acstr, px - textWidth/2, height - gap, mTextPaint);
	
	//////////////////////////////////////////////////////////////////
	// Draw step 			
	//textWidth = mStepPaint.measureText(Leveler.stepstr);	
	//canvas.drawText(Leveler.stepstr, px / 4 - textWidth/2, py, mStepPaint);
			
	//////////////////////////////////////////////////////////////////
	// Draw deviation on left			
	canvas.drawText(Leveler.vertstr, space, py / 2, mTextPaint);
		
	textWidth = mTextPaint.measureText(Leveler.horstr);	
	canvas.drawText(Leveler.horstr, width - (textWidth + space),
			py / 2, mTextPaint);
	
	// Calculate cross hair dimensions
	int www = 3 * Math.min(px,  py) / 4;
	
	// Draw horizontal cross hairs
	for(int zz = px; zz < px + www; zz += 10)
		{
		canvas.drawLine(zz, py - 5,  zz, py + 5, LinePaint);
		}	
	for(int zzz = px; zzz > px - www; zzz -= 10)
		{
		canvas.drawLine(zzz, py - 5,  zzz, py + 5, LinePaint);
		}	
	canvas.drawLine(px - www, py,  px + www, py, LinePaint);
	
	// Draw vertical cross hairs
	for(int zz = py; zz < py + www; zz += 10)
		{
		canvas.drawLine(px - 5, zz,  px + 5, zz, LinePaint);
		}	
	for(int zzz = py; zzz > py - www; zzz -= 10)
		{
		canvas.drawLine(px-5, zzz,  px+5, zzz, LinePaint);
		}		
	canvas.drawLine(px, py - www,  px,  py + www, LinePaint);

	/*
	// Draw accel graph	
	int nument = accelarr.NumEnties();
	for(int loopa = 0; loopa < nument; loopa++)	
		{
		double val = accelarr.GetAt(loopa);		
		
		canvas.drawCircle((float)loopa * 4 + width / 4, 
				(float)(height - (val * 20)), (float)2, circlePaint2);
		}
	*/
	
	// Limit paint to visible area (adj. bubble size)	
	LimitBubbleSize();
	
	ydiff = Math.max(ydiff, -py + bubble_size); 
	ydiff = Math.min(ydiff, py - bubble_size); 
	xdiff = Math.max(xdiff, -px + bubble_size); 
	xdiff = Math.min(xdiff, px - bubble_size); 
	
	px += xdiff; py -= ydiff;
			
	int holesize = (int)bubble_size / 4;	holesize = Math.min(holesize, 50);
	
	if(android.os.Build.VERSION.SDK_INT >= 11)
		{
		// Draw bubble hole
		path.reset();	
		path.addCircle(px, py, holesize, Path.Direction.CCW);
		path.setFillType(Path.FillType.INVERSE_EVEN_ODD);
		try
			{
			canvas.clipPath(path);
			}
		catch(Exception ex)
			{
			//Debug.printf("Exception in clip ");
			}
		}
	else
		{
		canvas.drawCircle(px, py, holesize, circlePaint2);
		}
	
	canvas.drawCircle(px, py, bubble_size, circlePaint4);
	canvas.drawCircle(px, py, bubble_size - 3, circlePaint3);
	canvas.drawCircle(px, py, bubble_size - 6, circlePaint2);	
	}

	// Return minimal of x / y dimensions
	public int GetMinDim()
		{
		int height = getMeasuredHeight(); int width = getMeasuredWidth();
		return Math.min(width, height);
		}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
	
		// Perform some special processing ... 
		
		// Use the existing functionality in the base class
		
	return super.onKeyDown(keyCode, keyEvent);
	}
}

//////////////////////////////////////////////////////////////////////////
// Set DEBUGTAG to an application unique value, and call logcat

class Debug extends Object
{
	public static final  String DEBUGTAG = "LevelerDebug";
	
	public static void print(String str)
		{
		Log.d(DEBUGTAG, str);
		}

	public static void printf(final String format, final Object... arguments)
		{
		try
			{
			String str = String.format(format, arguments);
			print(str);
			}
		catch(Exception ex)
			{
			String strerr = String.format(
					"Exception: Invalid arguments to Debug.printf %s", ex.getMessage());			
			print(strerr);
			}		
		}
	
		public static void printex(String str, Exception ex)
			{
			printf("EXCEPTION occured at %s Message: %s", str, ex.getMessage());		
			}
		
	public static void Exception(String str, Exception ex)
		{
		Log.d(DEBUGTAG, str, ex);		
		}
	
}


//////////////////////////////////////////////////////////////////////////
// Play sound from resource 

class playRCSound 
	{
	//public AsyncPlayer player;
	static MediaPlayer mp = null;
	
	// Return true if played it
	public static boolean play(int rc) 
		{
	    if (mp != null) 
	    	{
	    	if(mp.isPlaying())
	    		return false;
	    	
	    	//mp.release();  
	    	mp.reset();
	    	}
	    
	    mp = MediaPlayer.create(Leveler.mContext, rc);
	    mp.start();
	    
	    return true;
		}    
  }
  
//////////////////////////////////////////////////////////////////////////

class SimpleVibrator
{
	static Vibrator vib = null;
	
	//long[] pattern = {1000, 2000, 4000, 8000, 16000 };
	//vibrator.vibrate(pattern, 0);
	
	public static void vibrate(long length)
		{
		// Deferred to first usage
		if(vib == null)
			{
			String vibratorService = Context.VIBRATOR_SERVICE;			
			vib = (Vibrator)Leveler.mContext.getSystemService(vibratorService);
			}			
		try
			{
			vib.vibrate(length); // Vibrate for length msecs
			}
		catch(Exception ex)
			{
			//Debug.printex("No vibrator access", ex);
			}		
		}
}

//////////////////////////////////////////////////////////////////////////
// TODO Copy array with faster function

class AccelArr

{
	public static final int ARRSIZE = 500;
	public static final int OVERLAP = 50;

	// We allocate some, just in case the sensor fires before init
	public	double dblarr[] = new double[ARRSIZE];	
	public  int size = ARRSIZE;
	public  int head = 0;
	public  int tail = 0;
	
	public void	SetArrSize(int newsize)	
	{
		//Debug.printf("AccelArr SetSize=%d",  newsize);
		try
			{
			dblarr = new double[newsize + OVERLAP];
			}
		catch(Exception ex)
			{
			Debug.printex("SetSize", ex);
			}		
		size = newsize;
	}
	
	public int	GetArrSize()
	{
		//Debug.printf("AccelArr GetSize=%d",  size);
		return size;
	}
		
	public int	NumEnties()
	{
		int ret;
		
		//Debug.printf("AccelArr NumEnties=%d",  head);
		
		if(head > size) 
			ret = size;		
		else			
			ret = head;
		
		return ret;
	}
	
	public void	Add(double val)
		{
		//Debug.printf("AccelArr Add val=%f size=%d",  val, size);
		
		if(head < size + OVERLAP)
			{
			dblarr[head] = val;
			head++;
			}
		else
			{
			//Debug.printf("AccelArr overflow %d", size);
			// Copy to beginning
			int posx = 0;
			for(int loopx = OVERLAP; loopx < size + OVERLAP; loopx++)
				{
				dblarr[posx++] = dblarr[loopx];
				}
			head = size;
			}						
		}
	
	//////////////////////////////////////////////////////////////////////
	// Get at from position aligned with end
	
	public double GetAt(int idx)
		{		
		double ret;
		
		if(head > size) 
			{
			ret = dblarr[idx + (head - size)];
			}
		else
			{
			ret = dblarr[idx];
			}
		
		return ret;
		}
}


/*
class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

	private float mScaleFactor = 1.f;

	@Override
    public boolean onScale(ScaleGestureDetector detector) {
        mScaleFactor *= detector.getScaleFactor();
        
        // Don't let the object get too small or too large.
        mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));

        Debug.printf("ScaleListener %f", mScaleFactor);
        //invalidate();
        return true;
    }
}

*/
