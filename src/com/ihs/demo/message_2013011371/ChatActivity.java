package com.ihs.demo.message_2013011371;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.json.JSONObject;

import com.ihs.account.api.account.HSAccountManager;
import com.ihs.app.framework.HSApplication;
import com.ihs.app.framework.HSSessionMgr;
import com.ihs.app.framework.activity.HSActivity;
import com.ihs.commons.notificationcenter.HSGlobalNotificationCenter;
import com.ihs.commons.utils.HSBundle;
import com.ihs.commons.utils.HSError;
import com.ihs.commons.utils.HSLog;
import com.ihs.demo.message_2013011371.RefreshListView.OnRefreshListener;
import com.ihs.message_2013011371.R;
import com.ihs.message_2013011371.managers.HSMessageChangeListener;
import com.ihs.message_2013011371.managers.HSMessageManager;
import com.ihs.message_2013011371.managers.HSMessageChangeListener.HSMessageChangeType;
import com.ihs.message_2013011371.managers.HSMessageManager.QueryResult;
import com.ihs.message_2013011371.managers.HSMessageManager.SendMessageCallback;
import com.ihs.message_2013011371.types.HSAudioMessage;
import com.ihs.message_2013011371.types.HSBaseMessage;
import com.ihs.message_2013011371.types.HSImageMessage;
import com.ihs.message_2013011371.types.HSMessageType;
import com.ihs.message_2013011371.types.HSOnlineMessage;
import com.ihs.message_2013011371.types.HSTextMessage;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;

public class ChatActivity extends HSActivity implements HSMessageChangeListener{
	private RefreshListView msgListView;
	private EditText inputText;
	private Button send;
	private ChatAdapter adapter;
	private TextView title;
	private Button chooseButton;
	private ImageButton choosePic;
	private ImageButton chooseAud;
	private ImageButton takePic;
	private ImageButton sendLoc;
	private ImageButton recordAud;
	private TableLayout media;
	private List<HSBaseMessage> msgList = new ArrayList<HSBaseMessage>();
	String mid;
	String name;
	private File outputImage;
	private Uri imageUri;
	private long cursor = -1;
	//长按删除的位置
	private int operatingPosition;
	//发送消息类型的常量定义
	public static final int SEND_IMAGE = 0;
	public static final int SEND_AUDIO = 1;
	public static final int SEND_LOCATION = 2;
	public static final int TAKE_PHOTO = 3;
	//语音播放
	private MediaPlayer mPlayer = null;
	//完成录音
	private MediaRecorder mRecorder = null;
	//设置录音文件的存放路径（文件夹）
	private static final String AUDIOPATH = HSApplication.getContext().getCacheDir() + "/";
	private String audioPath;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		//监听消息的变化
		HSMessageManager.getInstance().addListener(this, new Handler());
		//界面预处理，隐藏标题并设置布局
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_chat);
		//进入聊天页面之后取消所有通知
		DemoApplication.notificationManager.cancelAll();
		//加载基本聊天界面，即当前的聊天对象
		Intent intent = getIntent();
		mid = intent.getStringExtra("mid");
		name = intent.getStringExtra("name");
		title = (TextView) findViewById(R.id.title_name);
		title.setText(name);
		//通过Id映射各个组件
		inputText = (EditText) findViewById(R.id.input_text);
		send = (Button) findViewById(R.id.send);
		chooseButton = (Button) findViewById(R.id.choose_media);
		msgListView = (RefreshListView) findViewById(R.id.msg_list_view);
		choosePic = (ImageButton) findViewById(R.id.choose_pic);
		chooseAud = (ImageButton) findViewById(R.id.choose_audio);
		takePic = (ImageButton) findViewById(R.id.take_pic);
		sendLoc = (ImageButton) findViewById(R.id.send_loc);
		media = (TableLayout) findViewById(R.id.media_holder);
		recordAud = (ImageButton) findViewById(R.id.voice_button);
		//设置adapter
		adapter = new ChatAdapter(ChatActivity.this, R.layout.msg_item, msgList);
		msgListView.setAdapter(adapter);
		msgListView.setSelection(getHistory(10));
		//标记已读
		setReaded();
		//清空通知
		DemoApplication.notificationManager.cancelAll();

		//设置刷新列表监听事件
		msgListView.setOnRefreshListener(new OnRefreshListener(){

			@Override
			public void onRefresh() {
				// TODO Auto-generated method stub
				msgListView.onRefreshComplete(getHistory(10));
			}

		});
		//设置表项点击事件，用于夺取输入框的焦点
		msgListView.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				msgListView.requestFocus();
				media.setVisibility(View.GONE);
				recordAud.setVisibility(View.GONE);
				HSBaseMessage msg = msgList.get(position-1);
				if (msg.getType()==HSMessageType.IMAGE){
					Intent intent = new Intent(ChatActivity.this, ShowPicActivity.class);
					intent.putExtra("path", ((HSImageMessage)msg).getNormalImageFilePath());
					startActivity(intent);
				}else if (msg.getType() == HSMessageType.AUDIO){
					audioPlay(((HSAudioMessage)msg).getAudioFilePath());
				}
			}

		});
		//设置列表长按事件，删除消息
		msgListView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
				// TODO Auto-generated method stub
				msgListView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

					@Override
					public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
						// TODO Auto-generated method stub
						menu.add(0,0,0,"Delete This Message");
						operatingPosition = position - 1;
					}
				});
				return false;
			}

		});
		//设置输入框焦点事件，在失去焦点后收起软键盘
		final InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		inputText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(hasFocus) inputMethodManager.showSoftInput(v,InputMethodManager.SHOW_FORCED);
				else inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(),0);
			}
		});
		//为输入框监听回车事件，发送消息
		inputText.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				if(keyCode == KeyEvent.KEYCODE_ENTER){ 
					if(inputMethodManager.isActive()){  						  
						String content = inputText.getText().toString();
						if (!"".equals(content)){
							HSTextMessage msg = new HSTextMessage(mid, content);
							inputText.setText("");
							sendMessages(msg);
						} 
					}
					return true;
				}
				return false;
			}
		});
		//为发送按钮监听点击事件，发送文本消息
		send.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String content = inputText.getText().toString();
				if (!"".equals(content)){
					HSTextMessage msg = new HSTextMessage(mid, content);
					sendMessages(msg);
					inputText.setText("");
				}		
			}
		});	
		//监听选择多媒体按钮点击时间，发送多媒体文件
		chooseButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				media.setVisibility(View.VISIBLE);
			}
		});
		//监听选择图片按钮点击事件，发送图片
		choosePic.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent i = new Intent(
						Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
				startActivityForResult(i, SEND_IMAGE);
			}
		});
		//监听拍照按钮点击事件，进入相机程序拍照
		takePic.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Random random = new Random();
				int hashCode = Math.abs(random.nextInt(10000));
				outputImage = new File(Environment.getExternalStorageDirectory(), "output_image_"+hashCode+".jpg");
				try{
					if (outputImage.exists()){
						outputImage.delete();
					}
					outputImage.createNewFile();
				}catch(IOException e){
					HSLog.d("Ouch!!!");
				}
				imageUri = Uri.fromFile(outputImage);
				Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
				intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
				startActivityForResult(intent, TAKE_PHOTO);
			}
		});
		//监听选择录音按钮点击事件，进入录音界面
		chooseAud.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				recordAud.setVisibility(View.VISIBLE);
				media.setVisibility(View.GONE);
			}
			
		});
		//监听录音按钮点击事件，开始和结束录音
		recordAud.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					recordAud.setImageResource(R.drawable.chat_audio_message_release_to_send);
					startVoice();
					break;
				case MotionEvent.ACTION_UP:
					recordAud.setImageResource(R.drawable.chat_audio_message_hold_to_talk);
					stopVoice();
					File file = new File(audioPath);
					if (file.exists()){
						Uri uri = Uri.fromFile(file);
						mPlayer = MediaPlayer.create(ChatActivity.this, uri);
						HSAudioMessage audioMessage = new HSAudioMessage(mid, file.getAbsolutePath(), mPlayer.getDuration());
						sendMessages(audioMessage);
					}
					break;
				default:
					break;
				}
				return false;
			}
		});
	}
	
	//开始录音
	private void startVoice() {
		// 设置录音保存路径
		Random random = new Random();
		int hashCode = Math.abs(random.nextInt(10000));
		audioPath = AUDIOPATH + new Integer(hashCode).toString() + ".wav";
		Toast.makeText(getApplicationContext(), "开始录音", 0).show();
		mRecorder = new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
		mRecorder.setOutputFile(audioPath);
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		try {
			mRecorder.prepare();
		} catch (IOException e) {
			e.printStackTrace();
		}
		mRecorder.start();
	}

	//停止录音
	private void stopVoice() {
		mRecorder.stop();
		mRecorder.release();
		mRecorder = null;
		Toast.makeText(getApplicationContext(), "保存录音" + audioPath, 0).show();
	}
	
	public boolean onContextItemSelected(MenuItem item){
    	HSBaseMessage msg = msgList.get(operatingPosition);
    	if (item.getItemId() == 0){
    		List<HSBaseMessage> deleteList = new ArrayList<HSBaseMessage>();
    		deleteList.add(msg);
    		HSMessageManager.getInstance().deleteMessages(deleteList);
    		msgList.remove(operatingPosition);
    		if (operatingPosition == msgList.size()){
    			long tempCursor = cursor;
    			QueryResult result = HSMessageManager.getInstance().queryMessages(mid, 1, tempCursor);
    			if (msgList.size() == 0){
    				if (result.getMessages().isEmpty())
    					DemoApplication.refreshDataBase(DemoApplication.SQL_DELETE, mid, null);
    			}else{
    				DemoApplication.refreshDataBase(DemoApplication.SQL_UPDATE, mid, msgList.get(msgList.size()-1));
    			}
    		} 
    		adapter.notifyDataSetChanged();
    		return true;
    	}
    	return false;
    }
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		media.setVisibility(View.GONE);
		if (requestCode == SEND_IMAGE && null != data && resultCode == RESULT_OK ) {
			imageUri = data.getData();
			String[] filePathColumn = { MediaStore.Images.Media.DATA };
			Cursor cursor = getContentResolver().query(imageUri,
					filePathColumn, null, null, null);
			cursor.moveToFirst();
			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			String picturePath = cursor.getString(columnIndex);
			cursor.close();
			HSImageMessage imageMessage = new HSImageMessage(mid, picturePath);
			sendMessages(imageMessage);
		}
		else if (requestCode == TAKE_PHOTO && resultCode == RESULT_OK){
			HSImageMessage imageMessage = new HSImageMessage(mid, outputImage.getAbsolutePath());
			sendMessages(imageMessage);
		}	
		else if(requestCode == SEND_AUDIO && resultCode == RESULT_OK){

		}
		else if (requestCode == SEND_LOCATION && resultCode == RESULT_OK){

		}
	}


	@Override
	public void onMessageChanged(HSMessageChangeType changeType, List<HSBaseMessage> messages) {
		// TODO Auto-generated method stub
		// 同学们可以根据 changeType 的消息增加、删除、更新信息进行会话数据的构建
		if (messages == null) return;
		if (changeType == HSMessageChangeType.ADDED && !messages.isEmpty()) {
			for (HSBaseMessage msg:messages){
				if (msg==null || !mid.equals(msg.getChatterMid())
			|| FriendManager.getInstance().getFriend(msg.getChatterMid()) == null) continue;
				if (HSSessionMgr.getTopActivity()==ChatActivity.this) setReaded();
				HSLog.d("receive from(or send to) " + msg.getChatterMid());
				msgList.add(msg);
			}
			adapter.notifyDataSetChanged();
			msgListView.setSelection(msgList.size());
		}
		else if (changeType == HSMessageChangeType.DELETED && !messages.isEmpty()){
			//Toast.makeText(this, "你删除了消息", Toast.LENGTH_LONG).show();
		}
		else if (changeType == HSMessageChangeType.UPDATED && !messages.isEmpty()){
			adapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onTypingMessageReceived(String fromMid) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOnlineMessageReceived(HSOnlineMessage message) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUnreadMessageCountChanged(String mid, int newCount) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onReceivingRemoteNotification(JSONObject pushInfo) {
		// TODO Auto-generated method stub

	}

	//清除数据库中与此人的未读消息
	void setReaded(){ 		
		HSMessageManager.getInstance().markRead(mid);
		DemoApplication.refreshDataBase(DemoApplication.SQL_MARK_READ, mid, null);
	}

	//从服务器查询最近的消息，消息数目由参数指定
	private int getHistory(int number){
		QueryResult result = HSMessageManager.getInstance().queryMessages(mid, number, cursor);
		List<HSBaseMessage> list = result.getMessages();
		if (list.isEmpty()) return 0;
		cursor = result.getCursor();
		adapter.addAll(list);
		Collections.sort(msgList);
		return list.size();
	}

	//发送消息
	void sendMessages(HSBaseMessage msg){
		HSMessageManager.getInstance().send(msg, new SendMessageCallback() {

			@Override
			public void onMessageSentFinished(HSBaseMessage message, boolean success, HSError error) {
				HSLog.d("success: " + success);
			}
		}, new Handler());
	}
	
	//初始化媒体播放器
    private void audioPlay(String path){
    	try{
    		File file = new File(path);
    		mPlayer = new MediaPlayer();
    		mPlayer.reset();
    		mPlayer.setDataSource(file.getPath());
    		mPlayer.prepare();
    		mPlayer.start();
    	}catch (Exception e){
    		e.printStackTrace();
    	}
    }
    
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	HSMessageManager.getInstance().removeListener(this);
    }
}

