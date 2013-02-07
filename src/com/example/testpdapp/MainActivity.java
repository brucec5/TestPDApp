package com.example.testpdapp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.puredata.android.service.PdPreferences;
import org.puredata.android.service.PdService;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final String TAG = "StupidPdApp";
	private static final String TOUCH = "touch";
	
	private static final float MAX_SPEED = 1.5f;
	private static final float MAX_AMP = 0.8f;
	
	private static final float MIN_SPEED = 0.5f;
	private static final float MIN_AMP = 0.3f;
	
	private TextView canvas;
	private Button stopButton;
	private Button resetButton;
	
	private PdService pdService = null;
	
	private final ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			pdService = ((PdService.PdBinder)service).getService();
			initPd();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// This method will never be called,
			// it just needs to be here to satisfy the interface.
		}
	};
	
	@Override
	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PdPreferences.initPreferences(getApplicationContext());
		initGui();
		bindService(new Intent(this, PdService.class),
				connection, BIND_AUTO_CREATE);
	};
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		cleanup();
	}
	
	private void initGui() {
		setContentView(R.layout.activity_main);
		
		canvas = (TextView) findViewById(R.id.log_view);
		canvas.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				float freq = normalize(event.getY(),
						MAX_SPEED, MIN_SPEED, canvas.getHeight());
				float amp = normalize(event.getX(),
						MAX_AMP, MIN_AMP, canvas.getWidth());
				PdBase.sendList(TOUCH, freq, amp);
				return true;
			}
		});
		
		stopButton = (Button) findViewById(R.id.stopButton);
		stopButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				PdBase.sendList(TOUCH, 0.0f, 0.0f);
				return true;
			}
		});
		
		resetButton = (Button) findViewById(R.id.resetButton);
		resetButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				PdBase.sendList(TOUCH, 1.0f, 0.5f);
				return true;
			}
		});
	}
	
	private void initPd() {
		Resources res = getResources();
		File patch = null;
		File audio = null;
		try {
			PdBase.subscribe("android");
			
			InputStream inp = res.openRawResource(R.raw.synth);
			InputStream ina = res.openRawResource(R.raw.icke);
			
			patch = IoUtils.extractResource(inp, "synth.pd", getCacheDir());
			audio = IoUtils.extractResource(ina, "icke.wav", getCacheDir());
			
			PdBase.openPatch(patch);
			startAudio();
		} catch (IOException e) {
			Log.e(TAG, e.toString());
			finish();
		} finally {
			if (patch != null)
				patch.delete();
			if (audio != null)
				audio.delete();
		}
	}
	
	private void startAudio() {
		String name = getResources().getString(R.string.app_name);
		try {
			pdService.initAudio(-1, -1, -1, -1);
			pdService.startAudio(new Intent(this, MainActivity.class),
					R.drawable.icon, name, "Return to " + name + ".");
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		}
	}
	
	private void cleanup() {
		try {
			unbindService(connection);
		} catch (IllegalArgumentException e) {
			// already unbound
			pdService = null;
		}
	}
	
	private float normalize(float in, float oMax, float oMin, float inMax) {
		return oMin + in * (oMax - oMin) / inMax;
	}
}
