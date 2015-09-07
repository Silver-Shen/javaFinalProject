package com.ihs.demo.message_2013011371;

import com.ihs.account.api.account.HSAccountManager;
import com.ihs.commons.utils.HSLog;
import com.ihs.message_2013011371.R;
import com.ihs.message_2013011371.types.HSAudioMessage;
import com.ihs.message_2013011371.types.HSBaseMessage;
import com.ihs.message_2013011371.types.HSBaseMessage.HSMessageStatus;
import com.ihs.message_2013011371.types.HSImageMessage;
import com.ihs.message_2013011371.types.HSMessageType;
import com.ihs.message_2013011371.types.HSTextMessage;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatAdapter extends ArrayAdapter<HSBaseMessage> {
	private int sourceId;
	private String myId = HSAccountManager.getInstance().getMainAccount().getMID();
	public ChatAdapter(Context context, int textViewSourceId,
			List<HSBaseMessage> objects) {
		// TODO Auto-generated constructor stub 
		super(context, textViewSourceId, objects);
		sourceId = textViewSourceId;
	}
	public View getView(int position, View convertView, ViewGroup parent){
		HSBaseMessage msg = getItem(position);
		View view;
		ViewHolder viewHolder;
		if (convertView == null){
			view = LayoutInflater.from(getContext()).inflate(sourceId, null);
			viewHolder = new ViewHolder();
			viewHolder.leftLayout = (LinearLayout) view.findViewById(R.id.left_layout);
			viewHolder.rightLayout = (LinearLayout) view.findViewById(R.id.right_layout);
			viewHolder.leftMsg = (TextView) view.findViewById(R.id.left_msg);
			viewHolder.rightMsg = (TextView) view.findViewById(R.id.right_msg);
			viewHolder.leftImg = (ImageView) view.findViewById(R.id.left_img);
			viewHolder.rightImg = (ImageView) view.findViewById(R.id.right_img);
			viewHolder.sendTime = (TextView) view.findViewById(R.id.sendtime);
			viewHolder.msgStatus = (ImageView) view.findViewById(R.id.msg_status);
			viewHolder.progress = (ProgressBar) view.findViewById(R.id.progress_bar);
			view.setTag(viewHolder);
		}else{
			view = convertView;
			viewHolder = (ViewHolder) view.getTag();
		}
		viewHolder.sendTime.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(msg.getTimestamp()));
		boolean isMy;
		if (!msg.getFrom().equals(myId)){
			viewHolder.leftLayout.setVisibility(View.VISIBLE);
			viewHolder.rightLayout.setVisibility(View.GONE);
			isMy = false;
		}
		else {
			viewHolder.leftLayout.setVisibility(View.GONE);
			viewHolder.rightLayout.setVisibility(View.VISIBLE);
			if (msg.getStatus() == HSMessageStatus.SENDING){
				viewHolder.msgStatus.setVisibility(View.GONE);
				viewHolder.progress.setVisibility(View.VISIBLE);
			}
			else if (msg.getStatus() == HSMessageStatus.SENT){
				viewHolder.progress.setVisibility(View.GONE);
				viewHolder.msgStatus.setVisibility(View.VISIBLE);
				viewHolder.msgStatus.setImageResource(R.drawable.monkey_android_help_right);
			}
			else if (msg.getStatus()==HSMessageStatus.FAILED){
				viewHolder.progress.setVisibility(View.GONE);
				viewHolder.msgStatus.setVisibility(View.VISIBLE);
				viewHolder.msgStatus.setImageResource(R.drawable.monkey_android_help_wrong);
			}
			isMy = true;
		}
		switch (msg.getType()){
		case TEXT:
			if (!isMy){
				viewHolder.leftImg.setVisibility(View.GONE);
				viewHolder.leftMsg.setVisibility(View.VISIBLE);
				viewHolder.leftMsg.setText(((HSTextMessage)msg).getText());
			}
			else{
				viewHolder.rightImg.setVisibility(View.GONE);
				viewHolder.rightMsg.setVisibility(View.VISIBLE);
				viewHolder.rightMsg.setText(((HSTextMessage)msg).getText());			
			}
			break;
		case IMAGE:
			File img = new File(((HSImageMessage)msg).getThumbnailFilePath());
			Uri uri = Uri.fromFile(img);
			if (!isMy){
				viewHolder.leftImg.setVisibility(View.VISIBLE);
				viewHolder.leftMsg.setVisibility(View.GONE);
				viewHolder.leftImg.setImageURI(uri);
			}
			else{
				viewHolder.rightImg.setVisibility(View.VISIBLE);
				viewHolder.rightMsg.setVisibility(View.GONE);
				viewHolder.rightImg.setImageURI(uri);
			}
			break;
		case AUDIO:
			if (!isMy){
				viewHolder.leftImg.setVisibility(View.VISIBLE);
				viewHolder.leftMsg.setVisibility(View.GONE);
				viewHolder.leftImg.setImageResource(R.drawable.chat_multimedia_audio_play_3);
			}
			else{
				viewHolder.rightImg.setVisibility(View.VISIBLE);
				viewHolder.rightMsg.setVisibility(View.GONE);
				viewHolder.rightImg.setImageResource(R.drawable.chat_multimedia_self_audio_play_0);
			}
			break;
		case LOCATION:break;
		default:break;
		}
		return view;
	}
}

class ViewHolder{
	LinearLayout  leftLayout;
	LinearLayout  rightLayout;
	TextView leftMsg;
	TextView rightMsg;
	TextView sendTime;
	ImageView leftImg;
	ImageView rightImg;
	ImageView msgStatus;
	ProgressBar progress;
}	
