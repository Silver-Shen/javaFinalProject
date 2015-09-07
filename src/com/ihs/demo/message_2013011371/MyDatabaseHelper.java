package com.ihs.demo.message_2013011371;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

public class MyDatabaseHelper extends SQLiteOpenHelper {
	public static final String CREATE_BOOK = "create table PackContact ("
			+ "id integer priamry key antoincrement, "
			+ "name text, "
			+ "mid text, "
			+ "recent text, "
			+ "time number, "
			+ "unread number)";
	private Context mContext;
	public MyDatabaseHelper(Context context, String name, CursorFactory
			factory, int version) {
		// TODO Auto-generated constructor stub
		super(context, name, factory, version);
		mContext = context;
	}
	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		db.execSQL(CREATE_BOOK);
		Toast.makeText(mContext, "Create Succeed", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		
	}

}
