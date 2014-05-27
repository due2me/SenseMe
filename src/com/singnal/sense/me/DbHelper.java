package com.singnal.sense.me;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {
	
	public static final String TB_NAME = "local_mac_table";
	public static final String ID = "_id";
	public static final String MAC = "mac";
	public static final String FUNC = "func";
	public static final String CONTENT = "content";

	public DbHelper(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
		// TODO Auto-generated constructor stub
	}
	
	public void Close() {
		this.getWritableDatabase().close();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		
		String sql = "CREATE  TABLE IF NOT EXISTS local_mac_table (_id INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL , mac TEXT, func TEXT, content TEXT)";
		db.execSQL(sql);
		
	}

	public Cursor query(String TBL_NAME) {
		SQLiteDatabase db = getWritableDatabase();
		Cursor c = db.query(TBL_NAME, null, null, null, null, null, null);
		return c;
	}
	
	public void addterm(String mac, String func, String target) {
		ContentValues values = new ContentValues();
		values.put(DbHelper.MAC, mac);
		values.put(DbHelper.FUNC, func);
		values.put(DbHelper.CONTENT, target);
		
		this.getWritableDatabase().insert(DbHelper.TB_NAME, DbHelper.ID,values);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		
	}


}
