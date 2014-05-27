package com.singnal.sense.me;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import com.markupartist.android.widget.PullToRefreshListView;
import com.markupartist.android.widget.PullToRefreshListView.OnRefreshListener;
import com.singnal.sense.me.AddMacDialogFragment.AddMacDialogListener;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.FragmentManager;
import android.app.NotificationManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

public class MainActivity extends Activity implements AddMacDialogListener {
	// settings
	SharedPreferences pref;
	Boolean isFirstIn = false;
	// initial local mac from local db;
	public String db_name = "local.db";
	final DbHelper helper = new DbHelper(this, db_name, null, 1);
	// define List of BT;
	static PullToRefreshListView P2RView;
	ArrayAdapter<String> array_adapter;
	List<String> myList = new ArrayList<String>();
	Context mContext = MainActivity.this;

	private SimpleAdapter madapter;
	ArrayList<HashMap<String, String>> list_2 = new ArrayList<HashMap<String, String>>();

	// define views;
	Switch sw_AUTO;
	Spinner sp_state;
	ImageButton bt_add, bt_setting;

	// states
	int current_state = 1;
	final static int STATE_ACTIVE = 0;
	final static int STATE_NORMAL = 1;
	final static int STATE_OFFLINE = 2;

	String device_name, device_address;

	// add mac dialog
	AddMacDialogFragment addDialog;
	FragmentManager fragmentManager;

	// define Blue-tooth;
	BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	int REQUEST_ENABLE_BT;

	// mReceiver;
	BroadcastReceiver mReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// first tutor
		pref = mContext.getSharedPreferences("MainActivity", 0);
		isFirstIn = pref.getBoolean("isFirstIn", true);
		if (isFirstIn) {
			// helper.addterm("18:26:66:CC:9E:82", "web",
			// "http://freshman.uestc.edu.cn/yx/images/pic-23.jpg");
			helper.addterm("B0:D0:9C:58:D9:DE", "web", "http://youhui.kfc.com.cn/mobile/");
		}
		Editor editor = pref.edit();
		editor.putBoolean("isFirstIn", false);
		editor.commit();

		// action bar customize
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		View view = View.inflate(getApplicationContext(), R.layout.menu_top, null);
		actionBar.setCustomView(view);
		sw_AUTO = (Switch) findViewById(R.id.auto_switch);
		sw_AUTO.setChecked(isAutoServiceRunning());
		sw_AUTO.setOnCheckedChangeListener(new onCheckedChangeListener());

		// DialogFragment Manage
		fragmentManager = getFragmentManager();
		addDialog = new AddMacDialogFragment();
		addDialog.setRetainInstance(true);

		// 新增SimpleAdapter
		madapter = new SimpleAdapter(this, list_2, android.R.layout.simple_list_item_2, new String[] { "bt_name",
				"bt_mac" }, new int[] { android.R.id.text1, android.R.id.text2 });

		// P2RView & List's Function.
		P2RView = (PullToRefreshListView) findViewById(R.id.listview_main);
		P2RView.setAdapter(madapter);
		P2RView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				device_name = list_2.get(position - 1).get("bt_name");
				device_address = list_2.get(position - 1).get("bt_mac");
				// Compare
//				if (!localQuery(device_name, device_address))
//					remoteQuery(device_name, device_address);
			}
		});
		// Set a listener to be invoked when the list should be refreshed.
		P2RView.setOnRefreshListener(new OnRefreshListener() {
			@Override
			public void onRefresh() {
				// Do work to refresh the list here.
				new GetDataTask().execute();
			}
		});

		// Request BlueTooth
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

		// add a breadCastReceiver to receive messages from service;
		mReceiver = new broadcastReceiver();

		// Register the BroadcastReceiver
		IntentFilter filter = new IntentFilter("com.singnal.action.findBluetooth");
		registerReceiver(mReceiver, filter);
		// Don't forget to unregister during onDestroy
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (!sw_AUTO.isChecked()) {
			Intent intent = new Intent(mContext, AutoService.class);
			mContext.stopService(intent);
		}
		this.unregisterReceiver(mReceiver);

	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		sp_state = (Spinner) menu.findItem(R.id.item_layout).getActionView().findViewById(R.id.spinner_states);
		int lastState = pref.getInt("lastState", 1);
		sp_state.setSelection(lastState, true);
		sp_state.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				current_state = pos;
				Editor editor = pref.edit();
				editor.putInt("lastState", current_state);
				editor.commit();
			}

			public void onNothingSelected(AdapterView<?> arg0) {
			}

		});
		bt_add = (ImageButton) menu.findItem(R.id.item_layout).getActionView().findViewById(R.id.button_add);
		bt_add.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				addDialog.show(fragmentManager, "fragment_name");
			}
		});
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_ENABLE_BT) {
			if (resultCode == RESULT_OK) {
				Intent intent = new Intent(mContext, AutoService.class);
				intent.putExtra("isAutoChecked", sw_AUTO.isChecked());
				mContext.startService(intent);
			} else {
				sw_AUTO.setChecked(false);
				P2RView.onRefreshComplete();
			}
		}
	}

	class onCheckedChangeListener implements OnCheckedChangeListener {
		@Override
		public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
			if (isChecked) {
				if (mBluetoothAdapter.isEnabled()) {
					Intent intent = new Intent(mContext, AutoService.class);
					intent.putExtra("isAutoChecked", true);
					mContext.stopService(intent);
					mContext.startService(intent);
				} else {
					Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
					startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
				}
			} else {
				Intent intent = new Intent(mContext, AutoService.class);
				mContext.stopService(intent);
			}
		}

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case 12:
			Toast.makeText(mContext, "under construction", Toast.LENGTH_LONG).show();
			// TODO Setting!
			return true;
		case 13:
			// showHelp();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	class GetDataTask extends AsyncTask<Void, Void, String[]> {
		@Override
		protected void onPostExecute(String[] result) {
			// after pull to refresh.
			list_2.clear();
			if (mBluetoothAdapter.isEnabled()) {
				Intent intent = new Intent(mContext, AutoService.class);
				intent.putExtra("isAutoChecked", sw_AUTO.isChecked());
				mContext.stopService(intent);
				mContext.startService(intent);
			} else {
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
				if (mBluetoothAdapter.isEnabled()) {
					Intent intent = new Intent(mContext, AutoService.class);
					intent.putExtra("isAutoChecked", sw_AUTO.isChecked());
					mContext.stopService(intent);
					mContext.startService(intent);
				}
			}
			super.onPostExecute(result);
		}

		@Override
		protected String[] doInBackground(Void... params) {
			return null;
		}
	}

	public class broadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context arg0, Intent intent) {
			String action = intent.getAction();
			if (action.equals("com.singnal.action.findBluetooth")) {
				if (intent.getBooleanExtra("isCycle", false))
					list_2.clear();
				if (!isBlueItemExist(intent))
					addNewBlueItem(intent);
			}

		}

	}

	public boolean isBlueItemExist(Intent intent) {
		if (list_2.size() == 0) {
			return false;
		} else {
			for (int i = 0; i != list_2.size(); i++) {
				if (intent.getStringExtra("bt_mac").equalsIgnoreCase(list_2.get(i).get("bt_mac")))
					return true;
			}
		}
		return false;
	}

	public void addNewBlueItem(Intent intent) {
		HashMap<String, String> item = new HashMap<String, String>();
		item.put("bt_name", intent.getStringExtra("bt_name"));
		item.put("bt_mac", intent.getStringExtra("bt_mac"));
		list_2.add(item);
		P2RView.onRefreshComplete();
	}

	public boolean isAutoServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (AutoService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}


	@Override
	public void onFinishAddMacDialog(String inputMac, String inputFunc, String inputContent) {
		helper.addterm(inputMac, inputFunc, inputContent);
		Toast.makeText(mContext, inputMac, Toast.LENGTH_SHORT).show();
	}

}