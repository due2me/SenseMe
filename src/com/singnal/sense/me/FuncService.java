package com.singnal.sense.me;

import java.util.ArrayList;
import java.util.List;

import com.singnal.sense.me.functions.SendMessageClass;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.util.Log;

public abstract class FuncService extends Service {
	// settings:
	SharedPreferences pref;
	// states
	int current_state = 1;
	final static int STATE_ACTIVE = 0;
	final static int STATE_NORMAL = 1;
	final static int STATE_OFFLINE = 2;
	// func:
	final static int ID_NOTIF = 1;
	String funcs[] = { "web", "phone" };
	ArrayList<String> al_funcs;

	public FuncService() {
	}

	public void onCreate() {
		super.onCreate();
		// read states setting
		pref = getSharedPreferences("MainActivity", 0);
		current_state = pref.getInt("lastState", 1);
		// define functions
		al_funcs = new ArrayList<String>(5);
		al_funcs.add("web");
		al_funcs.add("alarm");
		al_funcs.add("message");
	}

	protected void executeAccordingStates(String device_name, String device_address, String mac, String func,
			String content) {
		switch (current_state) {
		case STATE_ACTIVE:
			openDirectly(device_name, device_address, mac, func, content);
			break;
		case STATE_NORMAL:
			sendNotification(device_name, device_address, mac, func, content);
			break;
		case STATE_OFFLINE:
			sendNotification(device_name, device_address, mac, func, content);
			break;
		}
	}

	private Intent intentAccordingFunc(String func, String content) {
		Intent intent = null;
		switch (al_funcs.indexOf(func)) {
		case 0:
			// web
			intent = openWebPage(content);
			break;
		case 1:
			// alarm
			intent = createAlarm(content);
			break;
		case 2:
			// message
			intent = composeMmsMessage(content);
			break;
		default:
			intent = null;
		}
		return intent;
	}

	public Intent openWebPage(String content) {
		Uri webpage = Uri.parse(content);
		Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		// if (intent.resolveActivity(getPackageManager()) != null) {
		// startActivity(intent);
		// }
		return intent;
	}

	public Intent createAlarm(String content) {
		int hour, minutes;
		hour = Integer.parseInt(content.substring(0, 2));
		// Log.i("hour", content.substring(0, 2));
		minutes = Integer.parseInt(content.substring(3, 5));
		// Log.i("hour", content.substring(3, 4));
		Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR, hour).putExtra(
				AlarmClock.EXTRA_MINUTES, minutes);
		// intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		// if (intent.resolveActivity(getPackageManager()) != null) {
		// startActivity(intent);
		// }
		return intent;
	}

	public Intent composeMmsMessage(String content) {
		String target, message;
		String[] split_content = content.split(":");
		target = split_content[0];
		message = split_content[1];
		Log.i("target", target);
		Log.i("message", message);
		// Intent intent = new Intent(Intent.ACTION_SEND);
		// intent.setData(Uri.parse("smsto:")); // This ensures only SMS apps
		// respond
		// intent.putExtra("sms_body", message);
		Intent intent = new Intent(this, SendMessageClass.class);
		// intent.setData(Uri.parse("nothing"));
		intent.setType("service");
		intent.putExtra("target", target);
		intent.putExtra("message", message);
		// intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		// this.startActivity(intent);
		// this.startService(intent);
		return intent;
	}

	NotificationCompat.Builder notifBuilder;
	NotificationCompat.InboxStyle inboxStyle;
	NotificationManager mNotificationManager;
	int numMessage;

	public void sendNotification(String str_btname, String str_btmac, String mac, String func, String content) {

		Intent intent = intentAccordingFunc(func, content);

		// if(numMessage >= 1){
		// inboxStyle.setBigContentTitle("Surrounding Bluetooth details:");
		// inboxStyle.addLine(str_btname);
		// notifBuilder.setStyle(inboxStyle);
		// notifBuilder.setNumber(++numMessage);
		// Log.i("numMessages", Integer.toString(numMessage));
		// }
		notifBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle("正在靠近" + str_btname).setContentText("最新信息：" + content);
		inboxStyle = new NotificationCompat.InboxStyle();

		// notifBuilder.setAutoCancel(true);

		// set intent
		PendingIntent resultPendingIntent;
		if (intent.getType() != null) {
			resultPendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		} else
			resultPendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		notifBuilder.setContentIntent(resultPendingIntent);
		// notify
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(ID_NOTIF, notifBuilder.build());
		
	}

	public void openDirectly(String str_btname, String str_btmac, String mac, String func, String content) {
		Intent intent = intentAccordingFunc(func, content);
		if (intent.getType() != null) {
			this.startService(intent);
		} else
			this.startActivity(intent);
	}
}
