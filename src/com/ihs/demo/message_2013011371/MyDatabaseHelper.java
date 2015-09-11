package com.ihs.demo.message_2013011371;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;
//数据库初始化类
public class MyDatabaseHelper extends SQLiteOpenHelper {
	//初始化数据库字段，共有五部分组成（除去id）
	public static final String CREATE_BOOK = "create table PackContact ("
			+ "id integer priamry key antoincrement, "
			+ "name text, " //姓名
			+ "mid text, "  //id
			+ "recent text, " //最近消息
			+ "time number, " //最近联系时间
			+ "unread number)";//最近未读消息
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
