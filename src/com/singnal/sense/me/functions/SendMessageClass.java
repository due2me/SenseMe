package com.singnal.sense.me.functions;

import java.util.List;

import com.singnal.sense.me.AutoService;

import android.app.Activity;
import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

public class SendMessageClass extends IntentService {


	public SendMessageClass() {
		super("SendMessageClass");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		target = intent.getStringExtra("target");
		message = intent.getStringExtra("message");
		Log.i("beforesend", "beforesend");
		sendMessage();
	}
	
	String target, message;

	public void sendMessage() {
		SmsManager smsManager = SmsManager.getDefault();
		if (message.length() <= 70) {
			smsManager.sendTextMessage(target, null, message, null, null);
		} else {
			// SmsManger 类中 divideMessage 会将信息按每70 字分割
			List<String> smsDivs = smsManager.divideMessage(message);
			for (String sms : smsDivs) {
				smsManager.sendTextMessage(target, null, sms, null, null);
			}
		}
		Log.i("aftersend", "aftersend");
	}

	

}