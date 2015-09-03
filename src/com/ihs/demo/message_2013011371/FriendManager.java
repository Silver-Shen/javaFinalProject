package com.ihs.demo.message_2013011371;

import java.util.ArrayList;
import java.util.List;

import test.contacts.demo.friends.api.HSContactFriendsMgr;
import test.contacts.demo.friends.api.HSContactFriendsMgr.IFriendSyncListener;
import test.contacts.demo.friends.dao.ContactFriendsDao;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.ihs.commons.notificationcenter.HSGlobalNotificationCenter;
import com.ihs.commons.utils.HSLog;
import com.ihs.contacts.api.HSPhoneContactMgr;
import com.ihs.contacts.api.IContactBase;
import com.ihs.contacts.api.IPhoneContact;
import com.ihs.contacts.api.IPhoneContact.HSContactContent;

public class FriendManager implements IFriendSyncListener {

    /**
     * 当好友列表发生变化后会发出此通知
     */
    public static final String NOTIFICATION_NAME_FRIEND_CHANGED = "NOTIFICATION_NAME_FRIEND_CHANGED";

    private static FriendManager sInstannce = null;
    private static String TAG = FriendManager.class.getName();
    private List<Contact> friends;
    private HandlerThread handlerThread;
    private Handler serialHandler = null;

    public static synchronized FriendManager getInstance() {
        if (sInstannce == null) {
            sInstannce = new FriendManager();
        }
        return sInstannce;
    }

    /**
     * 获取好友列表
     * 
     * @return 好友列表
     */
    synchronized public List<Contact> getAllFriends() {
        return this.friends;
    }

    /**
     * 根据 mid 去查询一个 Friend，如果查到则返回对应的联系人，否则返回空
     * 
     * @param mid 要查询的 mid
     * @return 查询到 mid 对应的联系人
     */
    synchronized public Contact getFriend(String mid) {
        for (Contact c : this.friends) {
            if (TextUtils.equals(c.getMid(), mid))
                return c;
        }
        return null;
    }

    private FriendManager() {
        friends = new ArrayList<Contact>();
        handlerThread = new HandlerThread("FriendsManager");
        handlerThread.start();
        serialHandler = new Handler(handlerThread.getLooper());
        HSContactFriendsMgr.addSyncFinishListener(this);
        refresh();
    }

    synchronized void updateFriends(List<Contact> newFriends) {
        this.friends.clear();
        this.friends.addAll(newFriends);
    }

    @Override
    public void onFriendsSyncFinished(boolean result, ArrayList<IContactBase> friendList) {
        this.refresh();
    }

    void refresh() {
        serialHandler.post(new Runnable() {
            @Override
            public void run() {

                List<IPhoneContact> contacts = HSPhoneContactMgr.getContacts();
                HSLog.d(TAG, "all contacts " + contacts);
                final List<Contact> newFriends = new ArrayList<Contact>();
                for (IPhoneContact contact : contacts) {
                    List<HSContactContent> contents = contact.getNumbers();
                    for (HSContactContent c : contents) {
                        if (c.isFriend()) {
                            String number = c.getContent();
                            PhoneNumberUtil instance = PhoneNumberUtil.getInstance();
                            try {
                                PhoneNumber phoneNumber = instance.parse(number, "CN");
                                String e164 = instance.format(phoneNumber, PhoneNumberFormat.E164);
                                Contact myContact = new Contact(c.getContent(), c.getLabel(), c.getContactId(), c.getType(), true);
                                String mid = ContactFriendsDao.getInstance().getFriendMid(e164);
                                if (TextUtils.isEmpty(mid) == false) {
                                    myContact.setMid(mid);
                                    myContact.setName(contact.getDisplayName());
                                    newFriends.add(myContact);
                                }
                                updateFriends(newFriends);
                            } catch (NumberParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                HSGlobalNotificationCenter.sendNotificationOnMainThread(NOTIFICATION_NAME_FRIEND_CHANGED);
            }
        });
    }

}
