package com.ihs.demo.message_2013011371;
//格式化的联系人，在自带的Contact上增加了一些字段，并提供的排序的功能(comparable)
public class PackContact implements Comparable<PackContact>{
	private String name;
	private String mid;
	private String recentMsg;
	private long time;
	private long unread;
	public PackContact(String name, String mid, String recentMsg,
			long time, long unread) {
		// TODO Auto-generated constructor stub
		this.name = name;
		this.mid = mid;
		this.recentMsg = recentMsg;
		this.time = time;
		this.unread = unread;
	}
	public String getName(){
		return name;
	}
	public String getMid(){
		return mid;
	}
	public String getRecentMsg(){
		return recentMsg;
	}
	public long getTime(){
		return time;
	}
	public long getUnread(){
		return unread;
	}
	@Override
	public int compareTo(PackContact another) {
		// TODO Auto-generated method stub
		if (this.time < another.time) return 1;
		else if (this.time > another.time) return -1;
		else return 0;
	}
}
