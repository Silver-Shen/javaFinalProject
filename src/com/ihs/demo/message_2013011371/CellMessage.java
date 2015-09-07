package com.ihs.demo.message_2013011371;

public class CellMessage {
	public static final int RECEIVED = 0;
	public static final int SEND = 1;
	private String content;
	private int type;
	
	public CellMessage(String content, int type){
		this.content = content;
		this.type  = type;
	}
	
	public String getContent() {
		return  content;
	}
	public int getType(){
		return type;
	}
}
