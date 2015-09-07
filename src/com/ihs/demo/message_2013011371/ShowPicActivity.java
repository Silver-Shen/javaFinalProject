package com.ihs.demo.message_2013011371;

import java.io.File;

import com.ihs.app.framework.activity.HSActivity;
import com.ihs.message_2013011371.R;
import com.ihs.message_2013011371.types.HSImageMessage;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

public class ShowPicActivity extends HSActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show_pic);
		Intent intent = getIntent();
		String path = intent.getStringExtra("path");
		File img = new File(path);
		Uri uri = Uri.fromFile(img);
		ImageView showNorm = (ImageView) findViewById(R.id.normal_pic);
		showNorm.setImageURI(uri);
		showNorm.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finish();
			}
		});
	}
}
