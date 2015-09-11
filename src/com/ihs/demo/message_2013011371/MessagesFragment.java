package com.ihs.demo.message_2013011371;

import java.util.List;

import org.json.JSONObject;

import java.text.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import com.ihs.commons.utils.HSLog;
import com.ihs.message_2013011371.R;
import com.ihs.message_2013011371.managers.HSMessageChangeListener;
import com.ihs.message_2013011371.managers.HSMessageManager;
import com.ihs.message_2013011371.managers.HSMessageChangeListener.HSMessageChangeType;
import com.ihs.message_2013011371.types.HSBaseMessage;
import com.ihs.message_2013011371.types.HSOnlineMessage;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
//会话列表碎片的布局和事件处理
public class MessagesFragment extends Fragment implements HSMessageChangeListener{
	private MessageAdapter adapter = null; //适配器，本文件中作为内部类实现
	private ListView listView;
	private int operatingPosition;
	final List<PackContact> contacts = new ArrayList<PackContact>(); //规格化联系人的数据
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		HSMessageManager.getInstance().addListener(this, new Handler());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_messages, container, false);
		listView = (ListView)view.findViewById(R.id.fragmsg_listview);
		//为listview设置适配器
		adapter = new MessageAdapter(this.getActivity(), R.layout.contact_item, contacts);
		listView.setAdapter(adapter);
		//进入会话列表界面时清空通知中心
		DemoApplication.notificationManager.cancelAll();
		//设置表项长按事件，可以选择删除整个联系人或者标记为已读
		listView.setOnItemLongClickListener(new OnItemLongClickListener(){

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
				// TODO Auto-generated method stub
				listView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

					@Override
					public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
						// TODO Auto-generated method stub
						menu.add(0,0,0,"Delete Chat History");
						menu.add(0,1,1,"Mark All Message Readed");
						operatingPosition = position;
					}
				});
				return false;
			}

		});
		//设置表项点击事件，进入对应的会话界面
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String mid = contacts.get(position).getMid();
				String name = contacts.get(position).getName();
				Intent intent = new Intent(getActivity(), ChatActivity.class);
				intent.putExtra("mid", mid);
				intent.putExtra("name", name);
				startActivity(intent);
			}

		});
		return view;
	}
	//长按菜单的事件处理
	public boolean onContextItemSelected(MenuItem item){
		PackContact cont = contacts.get(operatingPosition);
		if (item.getItemId() == 0){
			HSMessageManager.getInstance().deleteMessages(cont.getMid());
			DemoApplication.refreshDataBase(DemoApplication.SQL_DELETE, cont.getMid(),null);
			refresh();
			return true;
		}
		else if (item.getItemId() == 1){
			DemoApplication.refreshDataBase(DemoApplication.SQL_MARK_READ, cont.getMid(), null);
			refresh();
			return true;
		}
		return false;
	}

	@Override
	public void onStart(){
		super.onStart();
		refresh();
	}
	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}

	//刷新联系人列表
	void refresh(){
		//if (this.getActivity() == null) return;
		SQLiteDatabase db = DemoApplication.dbHelper.getWritableDatabase();
		Cursor c = db.query("PackContact",null,null,null,null,null,null);
		contacts.clear();
		if (c.moveToFirst()){
			do{
				PackContact contact = new PackContact(c.getString(c.getColumnIndex("name")), 
						c.getString(c.getColumnIndex("mid")), 
						c.getString(c.getColumnIndex("recent")), 
						c.getLong(c.getColumnIndex("time")),
						c.getLong(c.getColumnIndex("unread")));
				contacts.add(contact);
			}while (c.moveToNext());
			Collections.sort(contacts);
		}
		adapter.notifyDataSetChanged();
		listView.setSelection(0);
	}
	//listview的适配器，这里作为内部类
	public class MessageAdapter extends ArrayAdapter<PackContact> {
		private int sourceId;
		private Context myContext;
		DisplayImageOptions options;

		public MessageAdapter(Context context, int resource, List<PackContact> objects) {
			super(context, resource, objects);
			// TODO Auto-generated constructor stub
			sourceId = resource;
			myContext = context;
			options = new DisplayImageOptions.Builder().showImageOnLoading(R.drawable.chat_avatar_default_icon).showImageForEmptyUri(R.drawable.chat_avatar_default_icon)
					.showImageOnFail(R.drawable.chat_avatar_default_icon).cacheInMemory(true).cacheOnDisk(true).considerExifParams(true).bitmapConfig(Bitmap.Config.RGB_565).build();
		}

		public View getView(int position, View convertView, ViewGroup parent){
			View view;
			PackContact contact = getItem(position);
			ViewHolder viewHolder;
			if (convertView == null){
				view = LayoutInflater.from(myContext).inflate(sourceId, null);
				viewHolder = new ViewHolder();
				//用户头像
				viewHolder.userHead = (ImageView)view.findViewById(R.id.user_head);
				//用户姓名
				viewHolder.nameView = (TextView)view.findViewById(R.id.contact_name);
				//最近聊天内容
				viewHolder.recentView = (TextView)view.findViewById(R.id.recent_msg);
				//最近聊天事件
				viewHolder.timeView = (TextView)view.findViewById(R.id.contact_time);
				//未读数量
				viewHolder.unreadView = (TextView)view.findViewById(R.id.unread_num);
				view.setTag(viewHolder);
			}else{
				view = convertView;
				viewHolder = (ViewHolder) view.getTag();
			}
			viewHolder.nameView.setText(contact.getName());
			viewHolder.recentView.setText(contact.getRecentMsg());
			viewHolder.timeView.setText(getTime(contact.getTime()));
			if (contact.getUnread() == 0) viewHolder.unreadView.setVisibility(View.GONE);
			else{
				viewHolder.unreadView.setVisibility(View.VISIBLE);
				viewHolder.unreadView.setText(new Long(contact.getUnread()).toString());
			}
			Contact tempContact = FriendManager.getInstance().getFriend(contact.getMid());
			if (tempContact != null)
				ImageLoader.getInstance().displayImage("content://com.android.contacts/contacts/" + tempContact.getContactId(), viewHolder.userHead, options);
			return view;
		}
		public String getTime(long time){ 
			SimpleDateFormat recentFmt=new SimpleDateFormat("HH:mm");
			SimpleDateFormat agoFmt=new SimpleDateFormat("yyyy-MM-dd");
			Date date = new Date(time);
			long offset = new Date().getTime()-time;
			if (offset > 8.64e7) return agoFmt.format(date);
			else return recentFmt.format(date);
		}
		class ViewHolder{ 
			ImageView userHead;
			TextView nameView;
			TextView recentView;
			TextView timeView;
			TextView unreadView;
		}
	}
	//如果收到新消息，刷新联系人列表
	@Override
	public void onMessageChanged(HSMessageChangeType changeType, List<HSBaseMessage> messages) {
		// TODO Auto-generated method stub
		if (changeType == HSMessageChangeType.ADDED && !messages.isEmpty()){
			refresh();
			//Toast.makeText(this.getActivity(), "你收到了消息", Toast.LENGTH_LONG).show();
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
}
