package com.ihs.demo.message_2013011371;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.json.JSONObject;

import test.contacts.demo.friends.api.HSContactFriendsMgr;
//import android.R;
import com.ihs.message_2013011371.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

//import com.baidu.mapapi.SDKInitializer;
import com.ihs.account.api.account.HSAccountManager;
import com.ihs.account.api.account.HSAccountManager.HSAccountSessionState;
import com.ihs.app.framework.HSApplication;
import com.ihs.app.framework.HSSessionMgr;
import com.ihs.commons.keepcenter.HSKeepCenter;
import com.ihs.commons.notificationcenter.HSGlobalNotificationCenter;
import com.ihs.commons.notificationcenter.INotificationObserver;
import com.ihs.commons.utils.HSBundle;
import com.ihs.commons.utils.HSLog;
import com.ihs.contacts.api.HSPhoneContactMgr;
import com.ihs.message_2013011371.managers.HSMessageChangeListener;
import com.ihs.message_2013011371.managers.HSMessageManager;
import com.ihs.message_2013011371.types.HSBaseMessage;
import com.ihs.message_2013011371.types.HSImageMessage;
import com.ihs.message_2013011371.types.HSMessageType;
import com.ihs.message_2013011371.types.HSOnlineMessage;
import com.ihs.message_2013011371.types.HSTextMessage;
import com.ihs.message_2013011371.utils.Utils;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

public class DemoApplication extends HSApplication implements HSMessageChangeListener, INotificationObserver {

    /*
     * 同步好友列表的服务器 URL
     */
    public static final String URL_SYNC = "http://54.223.212.19:8024/template/contacts/friends/get";
    public static final String URL_ACK = "http://54.223.212.19:8024/template/contacts/friends/get";

    private static final String TAG = DemoApplication.class.getName(); // 用于打印 log
    public static MyDatabaseHelper dbHelper; //用于操作数据库
    public static NotificationManager notificationManager; //用于控制通知中心
    public MediaPlayer mediaPlayer = new MediaPlayer();//用于播放音频（通知音及语音）
    
    //数据库操作种类常量定义
    public static final int SQL_MARK_READ = 0;
    public static final int SQL_UPDATE = 1;
    public static final int SQL_DELETE = 2;
    
    //通知中心操作种类常量定义
    public static final int NOTI_CANCEL = 0;
    public static final int NOTI_UPDATE = 1;
    
    @Override
    public void onCreate() {
        super.onCreate();

        HSAccountManager.getInstance();

        doInit();

        initImageLoader(this);

        // 初始化百度地图 SDK
        //SDKInitializer.initialize(getApplicationContext());

        // 初始化通讯录管理类，同步通讯录，用于生成好友列表
        HSPhoneContactMgr.init();
        HSPhoneContactMgr.enableAutoUpload(true);
        HSPhoneContactMgr.startSync();

        // 初始化好友列表管理类，同步好友列表
        HSContactFriendsMgr.init(this, null, URL_SYNC, URL_ACK);
        HSContactFriendsMgr.startSync(true);

        // 将本类添加为 HSMessageManager 的监听者，监听各类消息变化事件
        // 参见 HSMessageManager 类与 HSMessageChangeListener 接口
        HSMessageManager.getInstance().addListener(this, new Handler());

        // 为 HSGlobalNotificationCenter 功能设定监听接口
        INotificationObserver observer = this;
        HSGlobalNotificationCenter.addObserver(SampleFragment.SAMPLE_NOTIFICATION_NAME, observer);// 演示HSGlobalNotificationCenter功能：增加名为 SAMPLE_NOTIFICATION_NAME 的观察者
        
        //创建数据库
        dbHelper = new MyDatabaseHelper(getApplicationContext(), "RecentMsg.db", null, 1);
        //获取系统通知服务
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //准备提示铃声
        copy();
    }

    public static void doInit() {
        HSLog.d(TAG, "doInit invoked");

        // 验证登录状态
        if (HSAccountManager.getInstance().getSessionState() == HSAccountSessionState.VALID) {
            HSLog.d(TAG, "doInit during session is valid");
            HSMessageManager.getInstance();

            // 初始化长连接服务管理类 HSKeepCenter
            // 需传入标记应用的 App ID、标记帐户身份的 mid 和标记本次登录的 Session ID，三项信息均可从 HSAccountManager 获得
            HSKeepCenter.getInstance().set(HSAccountManager.getInstance().getAppID(), HSAccountManager.getInstance().getMainAccount().getMID(),
                    HSAccountManager.getInstance().getMainAccount().getSessionID());
            // 建立长连接
            HSKeepCenter.getInstance().connect();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /**
     * 返回配置文件名
     */
    @Override
    protected String getConfigFileName() {
        return "config.ya";
    }

    public static void initImageLoader(Context context) {

        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(context);
        config.threadPriority(Thread.NORM_PRIORITY - 2);
        config.denyCacheImageMultipleSizesInMemory();
        config.diskCacheFileNameGenerator(new Md5FileNameGenerator());
        config.diskCacheSize(50 * 1024 * 1024); // 50 MiB
        config.tasksProcessingOrder(QueueProcessingType.LIFO);

        // Initialize ImageLoader with configuration.
        ImageLoader.getInstance().init(config.build());
    }

    /**
     * 返回多媒体消息的文件存储路径
     */
    void getMediaFilePath() {
        HSLog.d("getMediaFilePath: ", Utils.getMediaPath());
    }

    /**
     * 收到 “正在输入” 消息时被调用
     * 
     * @param fromMid “正在输入” 消息发送者的 mid
     */
    @Override
    public void onTypingMessageReceived(String fromMid) {
    	
    }

    /**
     * 收到在线消息时被调用
     * 
     * @param message 收到的在线消息，其 content 值由用户定制，可实现自己的通讯协议和交互逻辑
     */
    @Override
    public void onOnlineMessageReceived(HSOnlineMessage message) {
        HSLog.d(TAG, "onOnlineMessageReceived");

        // 弹出 Toast 演示示例在线消息的 content 消息体内容
        HSBundle bundle = new HSBundle();
        bundle.putString(SampleFragment.SAMPLE_NOTIFICATION_BUNDLE_STRING, message.getContent().toString());
        HSGlobalNotificationCenter.sendNotificationOnMainThread(SampleFragment.SAMPLE_NOTIFICATION_NAME, bundle);
    }

    /**
     * 当来自某人的消息中，未读消息数量发生变化时被调用
     * 
     * @param mid 对应人的 mid
     * @param newCount 变化后的未读消息数量
     */
    @Override
    public void onUnreadMessageCountChanged(String mid, int newCount) {
        // 消息未读数量的变化大家可以在这里进行处理，比如修改每条会话的未读数量等。
    }

    /**
     * 当收到服务器通过长连接发送过来的推送通知时被调用，用途是进行新消息在通知窗口的通知，通知格式如下： alert 项为提示文字，fmid 代表是哪个 mid 发来的消息
     * {"act":"msg","aps":{"alert":"@: sent to a message","sound":"push_audio_1.wav","badge":1},"fmid":"23"}
     * 
     * @param pushInfo 收到通知的信息
     */
    @Override
    public void onReceivingRemoteNotification(JSONObject userInfo) {
        HSLog.d(TAG, "receive remote notification: " + userInfo);
        if (HSSessionMgr.getTopActivity() == null) {
            // 大家在这里做通知中心的通知即可
        }
    }

    /**
     * 有消息发生变化时的回调方法
     * 
     * @param changeType 变化种类，消息增加 / 消息删除 / 消息状态变化
     * @param messages 变化涉及的消息对象
     */
    @Override
    public void onMessageChanged(HSMessageChangeType changeType, List<HSBaseMessage> messages) {
        // 同学们可以根据 changeType 的消息增加、删除、更新信息进行会话数据的构建
    	if (messages == null) return;
        if (changeType == HSMessageChangeType.ADDED && !messages.isEmpty()) {        	
        	for (HSBaseMessage msg:messages){
        		if (msg == null) continue;
        		String charId = msg.getChatterMid(); 
        		if (FriendManager.getInstance().getFriend(charId) == null) continue;
        		if (charId.equals(msg.getFrom())){
        			initMediaPlayer(1);
        			mediaPlayer.start();
        		}
        		else{
        			initMediaPlayer(0);
        			mediaPlayer.start();
        		}
        		refreshDataBase(SQL_UPDATE, charId, msg);
        		if (HSSessionMgr.getTopActivity() == null && charId.equals(msg.getFrom())) refreshNotification(NOTI_UPDATE, charId);
        	}
        }
        else if (changeType == HSMessageChangeType.DELETED && !messages.isEmpty()){
        	
        }
        else if (changeType == HSMessageChangeType.UPDATED && !messages.isEmpty()){

        }
    }

    /**
     * 收到推送通知时的回调方法
     */
    @Override
    public void onReceive(String notificaitonName, HSBundle bundle) {
        // 供 HSGlobalNotificationCenter 功能参考，弹出 Toast 演示通知的效果
        String string = TextUtils.isEmpty(bundle.getString(SampleFragment.SAMPLE_NOTIFICATION_BUNDLE_STRING)) ? "消息为空" : bundle
                .getString(SampleFragment.SAMPLE_NOTIFICATION_BUNDLE_STRING); // 取得 bundle 中的信息
        Toast toast = Toast.makeText(getApplicationContext(), string, Toast.LENGTH_LONG);
        toast.show();
    }
    
    //通知中心的全局管理
    public void refreshNotification(int mode, String mid){ 
    	if (mode == NOTI_CANCEL) notificationManager.cancelAll(); 
    	else if (mode == NOTI_UPDATE){
    		try{
    			notificationManager.cancel(Integer.parseInt(mid));
    		}catch (Exception e){
    			HSLog.d("Onch, Don't cancel a Unexsit noti!");
    		}finally{
    			SQLiteDatabase db = dbHelper.getWritableDatabase();
        		Cursor c = db.rawQuery("select * from PackContact where mid=?",new String[]{mid});
        		NotificationCompat.Builder notify = new NotificationCompat.Builder(getApplicationContext());
        		Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        		intent.putExtra("name", FriendManager.getInstance().getFriend(mid).getName());
    			intent.putExtra("mid", mid);
    			PendingIntent pend = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);
        		if (c.moveToFirst()){
        			notify.setTicker("You have receive a message from "+ FriendManager.getInstance().getFriend(mid))
        			      .setSmallIcon(R.drawable.monkey_notification_board_credits_2x)
        			      .setContentTitle(FriendManager.getInstance().getFriend(mid).getName())
        			      .setContentText(c.getString(c.getColumnIndex("recent")))
        			      .setWhen(System.currentTimeMillis())
        			      .setDefaults(Notification.DEFAULT_ALL)
        			      .setContentIntent(pend);
        			Notification notification = notify.build();
        			notification.flags = Notification.FLAG_AUTO_CANCEL;
        			notificationManager.notify(Integer.parseInt(mid), notification);
        		}
    		}    		
    	}
    }
    
    //数据库的全局管理
    public static void refreshDataBase(int mode, String mid, HSBaseMessage msg){
    	SQLiteDatabase db = dbHelper.getWritableDatabase();
    	if (mode == SQL_MARK_READ){
    		Cursor c = db.rawQuery("select * from PackContact where mid=?",new String[]{mid});
    		if (c.moveToFirst()){
    			ContentValues values = new ContentValues();
    			values.put("unread", 0);
    			db.update("PackContact",values, "mid = ?", new String[]{mid});
    		}
    	}else if (mode == SQL_DELETE){
    		db.delete("PackContact", "mid = ?",new String[]{mid});
    	}else if (mode == SQL_UPDATE){
    		Cursor c = db.rawQuery("select * from PackContact where mid=?",new String[]{mid});
    		ContentValues values = new ContentValues();
    		values.put("name", FriendManager.getInstance().getFriend(mid).getName());
    		values.put("mid", mid);
    		values.put("time", msg.getTimestamp().getTime());
    		values.put("unread", HSMessageManager.getInstance().queryUnreadCount(mid));
    		switch (msg.getType()){
    		case TEXT:
    			values.put("recent", ((HSTextMessage)msg).getText());
    			break;
    		case AUDIO:
    			values.put("recent", "[audio]");
    			break;
    		case IMAGE:
    			values.put("recent", "[image]");
    			break;
    		case LOCATION:
    			values.put("recent", "[location]");
    			break;
    		default:break;
    		}	
    		if (c.moveToFirst()) db.update("PackContact",values, "mid = ?", new String[]{mid});
    		else db.insert("PackContact", null, values);
    	}
    }
    //初始化媒体播放器
    private void initMediaPlayer(int mode){
    	try{
    		File file;
    		if (mode == 0) file = new File(HSApplication.getContext().getCacheDir() + "/" + "sent.wav");
    		else file = new File(HSApplication.getContext().getCacheDir() + "/" + "received.wav");
    		mediaPlayer.reset();
    		mediaPlayer.setDataSource(file.getPath());
    		mediaPlayer.prepare();
    	}catch (Exception e){
    		e.printStackTrace();
    	}
    }
    
    static void copy() {
    	final File receivedAudioFile = new File(HSApplication.getContext().getCacheDir() + "/" + "received.wav");
    	final File sentAudioFile = new File(HSApplication.getContext().getCacheDir() + "/" + "sent.wav");
        if (!receivedAudioFile.exists())
            try {
                InputStream is = HSApplication.getContext().getResources().openRawResource(R.raw.message_ringtone_received);
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();

                FileOutputStream fos = new FileOutputStream(receivedAudioFile);
                fos.write(buffer);
                fos.close();
            } catch (Exception e) {}
        if (!sentAudioFile.exists())
        	try {
                InputStream is = HSApplication.getContext().getResources().openRawResource(R.raw.message_ringtone_sent);
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();

                FileOutputStream fos = new FileOutputStream(sentAudioFile);
                fos.write(buffer);
                fos.close();
            } catch (Exception e) {}
    }
}
