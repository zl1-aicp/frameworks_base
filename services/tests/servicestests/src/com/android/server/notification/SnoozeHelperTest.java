/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.server.notification;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@SmallTest
@RunWith(AndroidJUnit4.class)
public class SnoozeHelperTest {
    @Mock SnoozeHelper.Callback mCallback;
    @Mock AlarmManager mAm;
    @Mock ManagedServices.UserProfiles mUserProfiles;

    private SnoozeHelper mSnoozeHelper;

    private Context getContext() {
        return InstrumentationRegistry.getTargetContext();
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mSnoozeHelper = new SnoozeHelper(getContext(), mCallback, mUserProfiles);
        mSnoozeHelper.setAlarmManager(mAm);
    }

    @Test
    public void testSnooze() throws Exception {
        NotificationRecord r = getNotificationRecord("pkg", 1, "one", UserHandle.SYSTEM);
        mSnoozeHelper.snooze(r , UserHandle.USER_SYSTEM, 1000);
        verify(mAm, times(1)).setExactAndAllowWhileIdle(
                anyInt(), eq((long) 1000), any(PendingIntent.class));
        assertTrue(mSnoozeHelper.isSnoozed(
                UserHandle.USER_SYSTEM, r.sbn.getPackageName(), r.getKey()));
    }

    @Test
    public void testCancelByApp() throws Exception {
        NotificationRecord r = getNotificationRecord("pkg", 1, "one", UserHandle.SYSTEM);
        NotificationRecord r2 = getNotificationRecord("pkg", 2, "two", UserHandle.SYSTEM);
        mSnoozeHelper.snooze(r , UserHandle.USER_SYSTEM, 1000);
        mSnoozeHelper.snooze(r2 , UserHandle.USER_SYSTEM, 1000);
        assertTrue(mSnoozeHelper.isSnoozed(
                UserHandle.USER_SYSTEM, r.sbn.getPackageName(), r.getKey()));
        assertTrue(mSnoozeHelper.isSnoozed(
                UserHandle.USER_SYSTEM, r2.sbn.getPackageName(), r2.getKey()));

        mSnoozeHelper.cancel(UserHandle.USER_SYSTEM, r.sbn.getPackageName(), "one", 1);
        // 3 = one for each snooze, above + one for cancel itself.
        verify(mAm, times(3)).cancel(any(PendingIntent.class));
        assertFalse(mSnoozeHelper.isSnoozed(
                UserHandle.USER_SYSTEM, r.sbn.getPackageName(), r.getKey()));
        assertTrue(mSnoozeHelper.isSnoozed(
                UserHandle.USER_SYSTEM, r2.sbn.getPackageName(), r2.getKey()));
    }

    @Test
    public void testCancelAllForUser() throws Exception {
        NotificationRecord r = getNotificationRecord("pkg", 1, "one", UserHandle.SYSTEM);
        NotificationRecord r2 = getNotificationRecord("pkg", 2, "two", UserHandle.SYSTEM);
        NotificationRecord r3 = getNotificationRecord("pkg", 3, "three", UserHandle.ALL);
        mSnoozeHelper.snooze(r , UserHandle.USER_SYSTEM, 1000);
        mSnoozeHelper.snooze(r2 , UserHandle.USER_SYSTEM, 1000);
        mSnoozeHelper.snooze(r3 , UserHandle.USER_ALL, 1000);
        assertTrue(mSnoozeHelper.isSnoozed(
                UserHandle.USER_SYSTEM, r.sbn.getPackageName(), r.getKey()));
        assertTrue(mSnoozeHelper.isSnoozed(
                UserHandle.USER_SYSTEM, r2.sbn.getPackageName(), r2.getKey()));
        assertTrue(mSnoozeHelper.isSnoozed(
                UserHandle.USER_ALL, r3.sbn.getPackageName(), r3.getKey()));

        mSnoozeHelper.cancel(UserHandle.USER_SYSTEM, false);
        // 5 = once for each snooze above (3) + once for each notification canceled (2).
        verify(mAm, times(5)).cancel(any(PendingIntent.class));
        assertFalse(mSnoozeHelper.isSnoozed(
                UserHandle.USER_SYSTEM, r.sbn.getPackageName(), r.getKey()));
        assertFalse(mSnoozeHelper.isSnoozed(
                UserHandle.USER_SYSTEM, r2.sbn.getPackageName(), r2.getKey()));
        assertTrue(mSnoozeHelper.isSnoozed(
                UserHandle.USER_ALL, r3.sbn.getPackageName(), r3.getKey()));
    }

    @Test
    public void testCancelAllByApp() throws Exception {
        NotificationRecord r = getNotificationRecord("pkg", 1, "one", UserHandle.SYSTEM);
        NotificationRecord r2 = getNotificationRecord("pkg", 2, "two", UserHandle.SYSTEM);
        NotificationRecord r3 = getNotificationRecord("pkg2", 3, "three", UserHandle.SYSTEM);
        mSnoozeHelper.snooze(r , UserHandle.USER_SYSTEM, 1000);
        mSnoozeHelper.snooze(r2 , UserHandle.USER_SYSTEM, 1000);
        mSnoozeHelper.snooze(r3 , UserHandle.USER_SYSTEM, 1000);
        assertTrue(mSnoozeHelper.isSnoozed(
                UserHandle.USER_SYSTEM, r.sbn.getPackageName(), r.getKey()));
        assertTrue(mSnoozeHelper.isSnoozed(
                UserHandle.USER_SYSTEM, r2.sbn.getPackageName(), r2.getKey()));
        assertTrue(mSnoozeHelper.isSnoozed(
                UserHandle.USER_SYSTEM, r3.sbn.getPackageName(), r3.getKey()));

        mSnoozeHelper.cancel(UserHandle.USER_SYSTEM, "pkg2");
        // 4 = once for each snooze above (3) + once for each notification canceled (1).
        verify(mAm, times(4)).cancel(any(PendingIntent.class));
        assertTrue(mSnoozeHelper.isSnoozed(
                UserHandle.USER_SYSTEM, r.sbn.getPackageName(), r.getKey()));
        assertTrue(mSnoozeHelper.isSnoozed(
                UserHandle.USER_SYSTEM, r2.sbn.getPackageName(), r2.getKey()));
        assertFalse(mSnoozeHelper.isSnoozed(
                UserHandle.USER_SYSTEM, r3.sbn.getPackageName(), r3.getKey()));
    }

    @Test
    public void testRepost() throws Exception {
        NotificationRecord r = getNotificationRecord("pkg", 1, "one", UserHandle.SYSTEM);
        mSnoozeHelper.snooze(r , UserHandle.USER_SYSTEM, 1000);
        NotificationRecord r2 = getNotificationRecord("pkg", 2, "one", UserHandle.ALL);
        mSnoozeHelper.snooze(r2 , UserHandle.USER_ALL, 1000);
        mSnoozeHelper.repost(r.sbn.getPackageName(), r.getKey(), UserHandle.USER_SYSTEM);
        verify(mCallback, times(1)).repost(UserHandle.USER_SYSTEM, r);
    }

    @Test
    public void testUpdate() throws Exception {
        NotificationRecord r = getNotificationRecord("pkg", 1, "one", UserHandle.SYSTEM);
        mSnoozeHelper.snooze(r , UserHandle.USER_SYSTEM, 1000);
        r.getNotification().category = "NEW CATEGORY";

        mSnoozeHelper.update(UserHandle.USER_SYSTEM, r);
        verify(mCallback, never()).repost(anyInt(), any(NotificationRecord.class));

        mSnoozeHelper.repost(r.sbn.getPackageName(), r.getKey(), UserHandle.USER_SYSTEM);
        verify(mCallback, times(1)).repost(UserHandle.USER_SYSTEM, r);
    }

    private NotificationRecord getNotificationRecord(String pkg, int id, String tag,
            UserHandle user) {
        Notification n = new Notification.Builder(getContext())
                .setContentTitle("A")
                .setGroup("G")
                .setSortKey("A")
                .setWhen(1205)
                .build();
        return new NotificationRecord(getContext(), new StatusBarNotification(
                pkg, pkg, id, tag, 0, 0, 0, n, user),
                new NotificationChannel(NotificationChannel.DEFAULT_CHANNEL_ID, "name"));
    }

}
