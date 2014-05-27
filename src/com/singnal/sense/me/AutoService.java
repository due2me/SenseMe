package com.singnal.sense.me;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import android.app.NotificationManager;
import android.app.PendingIntent;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.AlarmClock;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.TextView;
//import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

public class AutoService extends FuncService {

	// settings:
	SharedPreferences pref;

	AudioManager mAudioManager;
	boolean isCycle = false;
	boolean isAutoChecked = true;

	// initial local mac from local db;
	public String db_name = "local.db";
	final DbHelper helper = new DbHelper(this, db_name, null, 1);
	String[] localMAC = { "B0:D0:9C:58:D9:DE", "B0:D0:9C:58:D9:DA", "50:32:75:FC:B8:36", "18:26:66:CC:9E:82" };

	// states
	int current_state = 1;
	final static int STATE_ACTIVE = 0;
	final static int STATE_NORMAL = 1;
	final static int STATE_OFFLINE = 2;

	// func:
	final static int ID_NOTIF = 1;
	String funcs[] = { "web", "phone" };
	ArrayList<String> al_funcs;

	String device_name;
	String device_address;

	// test
	int test = 0;

	BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	BroadcastReceiver mReceiver;

	@Override
	public void onDestroy() {
		super.onDestroy();
		Toast.makeText(AutoService.this, "Stopped", Toast.LENGTH_SHORT).show();
		// unregister
		this.unregisterReceiver(mReceiver);
	}

	public void onStart(Intent intent, int startId) {
		Toast.makeText(AutoService.this, "Started", Toast.LENGTH_SHORT).show();
		isAutoChecked = intent.getBooleanExtra("isAutoChecked", false);
		current_state = pref.getInt("lastState", 1);
		if (isAutoChecked)
			AutoSearching();
		else {
			mBluetoothAdapter.cancelDiscovery();
			mBluetoothAdapter.startDiscovery();
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		final Intent mIntent = new Intent();
		// read states setting
		pref = getSharedPreferences("MainActivity", 0);
		// define functions
		al_funcs = new ArrayList<String>(5);
	    al_funcs.add("web");
	    al_funcs.add("alarm");
	    al_funcs.add("message");

		// Create a BroadcastReceiver for ACTION_FOUND
		mReceiver = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				// When discovery finds a device
				if (BluetoothDevice.ACTION_FOUND.equals(action)) {
					// Get the BluetoothDevice object from the Intent
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					device_name = device.getName();
					device_address = device.getAddress();
					mIntent.setAction("com.singnal.action.findBluetooth");
					mIntent.putExtra("bt_name", device_name);
					mIntent.putExtra("bt_mac", device_address);
					mIntent.putExtra("isCycle", isCycle);
					isCycle = false;
					sendBroadcast(mIntent);

					// Compare
					if ((!localQuery(device_name, device_address)) && (current_state != STATE_OFFLINE))
						remoteQuery(device_name, device_address);

					// test
					test++;
					Toast.makeText(AutoService.this, Integer.toString(test), Toast.LENGTH_LONG).show();

				}
			}
		};
		// Register the BroadcastReceiver
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter); // Don't forget to unregister
												// during onDestroy

	}

	public void AutoSearching() {
		Timer timer = new Timer();
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				isCycle = true;
				current_state = pref.getInt("lastState", 1);
				mBluetoothAdapter.cancelDiscovery();
				mBluetoothAdapter.startDiscovery();
			}
		};
		timer.schedule(task, 3000, 15000);

	}

	public boolean localQuery(String str_btname, String str_btmac) {
		Cursor c;
		c = helper.query("local_mac_table");
		for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
			String db_mac = c.getString(1);
			String db_func = c.getString(2);
			String db_content = c.getString(3);
			if (str_btmac.equalsIgnoreCase(db_mac)) {
				// execute
				executeAccordingStates(str_btname, str_btmac, db_mac, db_func, db_content);
				return true;
			}

		}

		return false;
	}

	public boolean remoteQuery(String str_btname, String str_btmac) {
		// Toast.makeText(AutoService.this, "Querying on the internet...",
		// Toast.LENGTH_SHORT).show();
		new Thread(remoteQuery).start();
		return false;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Bundle data = msg.getData();
			String mac = data.getString("mac");
			String func = data.getString("func");
			String content = data.getString("content");

			executeAccordingStates(device_name, device_address, mac, func, content);
			// sendNotification(device_name, device_address, mac, func,
			// content);
		}
	};

	Runnable remoteQuery = new Runnable() {
		@Override
		public void run() {
			String url_base = "http://www.due2me.tk/mainServlet";
			String key = "mac";
			String st_mac = device_address;
			String url = url_base + "?" + key + "=" + st_mac;
			// Log.i("sending_mac", url);
			try {
				HttpClient httpclient = new DefaultHttpClient();
				HttpPost httppost = new HttpPost(url);
				// 发送请求、获取回复
				HttpResponse httpResponse = httpclient.execute(httppost);
				String retSrc = EntityUtils.toString(httpResponse.getEntity());
				// 得到JSONObject并解析
				JSONObject result = new JSONObject(retSrc);
				String result_func = result.getString("func");
				String result_content = result.getString("content");

				// 传出结果
				Message msg = new Message();
				Bundle data = new Bundle();
				data.putString("func", result_func);
				data.putString("content", result_content);
				msg.setData(data);
				handler.sendMessage(msg);
			} catch (Exception e) {
				Log.e("log_tag", "Error converting result " + e.toString());
			}

		}
	};

	

}
