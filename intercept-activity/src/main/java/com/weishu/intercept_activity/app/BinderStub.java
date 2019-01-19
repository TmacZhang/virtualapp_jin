package com.weishu.intercept_activity.app;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import java.io.FileDescriptor;

/**
 * Created by zhangguangjin on 2018-4-4.
 */

public class BinderStub implements IBinder {
    private IBinder mBaseBinder;
    private IInterface iInterface;

    public BinderStub(IBinder baseBinder) {
        mBaseBinder = baseBinder;
    }

    @Override
    public String getInterfaceDescriptor() throws RemoteException {
        return mBaseBinder.getInterfaceDescriptor();
    }

    @Override
    public boolean pingBinder() {
        return mBaseBinder.pingBinder();
    }

    @Override
    public boolean isBinderAlive() {
        return mBaseBinder.isBinderAlive();
    }

    @Override
    public IInterface queryLocalInterface(String descriptor) {
        Log.i("jin1", "descriptor = " + descriptor  + "  iInterface="+iInterface.getClass().getName(), new Exception());
        Log.i("jin1", "descriptor = " + descriptor  + "  iInterface="+iInterface.toString());
        return iInterface;
    }

    @Override
    public void dump(FileDescriptor fd, String[] args) throws RemoteException {
        mBaseBinder.dump(fd, args);
    }

    @Override
    public void dumpAsync(FileDescriptor fd, String[] args) throws RemoteException {
        mBaseBinder.dumpAsync(fd, args);
    }

    @Override
    public boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        return mBaseBinder.transact(code, data, reply, flags);
    }

    @Override
    public void linkToDeath(DeathRecipient recipient, int flags) throws RemoteException {
        mBaseBinder.linkToDeath(recipient, flags);
    }

    @Override
    public boolean unlinkToDeath(DeathRecipient recipient, int flags) {
        return mBaseBinder.unlinkToDeath(recipient, flags);
    }

    public void setIIerface(IInterface iInterface1) {
        iInterface = iInterface1;
    }
}
