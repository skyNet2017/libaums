package com.github.mjdev.libaums.usbfileman;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.github.magnusja.libaums.javafs.JavaFsFileSystemCreator;
import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.FileSystemFactory;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.partition.Partition;
import com.github.mjdev.libaums.usb.UsbCommunicationFactory;


import java.util.Arrays;

import me.jahnen.libaums.libusbcommunication.LibusbCommunicationCreator;

/**
 * https://www.codenong.com/cs70146041/
 * https://github.com/1hakr/AnExplorer
 */
public class UsbUtil {
    private static final String ACTION_USB_PERMISSION = "com.android.hss01248.USB_PERMISSION";

    public static final String  TAG = "usb";

    private  static BroadcastReceiver mUsbReceiver;
    static Context context;

    static {

        FileSystemFactory.registerFileSystem(new JavaFsFileSystemCreator());
        UsbCommunicationFactory.registerCommunication(new LibusbCommunicationCreator());
        UsbCommunicationFactory.UnderlyingUsbCommunication  underlyingUsbCommunication
                = UsbCommunicationFactory.UnderlyingUsbCommunication.OTHER;
        //UsbCommunicationFactory.setUnderlyingUsbCommunication(underlyingUsbCommunication);

    }
    public static void regist(Context context){
        UsbUtil.context = context;
        /*if(mUsbReceiver == null){
            //监听otg插入 拔出
            IntentFilter usbDeviceStateFilter = new IntentFilter();
            usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
//注册监听自定义广播
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            mUsbReceiver = initUsbReceiver();
            context.registerReceiver(mUsbReceiver, filter);
        }*/
        readUsbDevice(null);

    }

    private static BroadcastReceiver initUsbReceiver() {
        BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    case ACTION_USB_PERMISSION://接受到自定义广播
                        synchronized (this) {
                            UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) { //允许权限申请
                                if (usbDevice != null) {
                                    //Do something
                                    readUsbDevice(usbDevice);
                                }
                            } else {
                                TShow("用户未授权，读取失败");
                            }
                        }
                        break;
                    case UsbManager.ACTION_USB_DEVICE_ATTACHED://接收到存储设备插入广播
                        UsbDevice device_add = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (device_add != null) {
                            TShow("接收到存储设备插入广播");
                            readUsbDevice(device_add);
                        }
                        break;
                    case UsbManager.ACTION_USB_DEVICE_DETACHED://接收到存储设备拔出广播
                        UsbDevice device_remove = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (device_remove != null) {
                            TShow("接收到存储设备拔出广播");
                            //拔出或者碎片 Activity销毁时 释放引用
                            //device.close();
                        }
                        break;
                }
            }
        };

        //监听otg插入 拔出
        IntentFilter usbDeviceStateFilter = new IntentFilter();
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
//注册监听自定义广播
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        //mUsbReceiver = initUsbReceiver();
        context.registerReceiver(mUsbReceiver, filter);
        return mUsbReceiver;
    }

    private static void readUsbDevice(UsbDevice device_add) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //获取管理者
                final UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                //枚举设备
                UsbMassStorageDevice[] storageDevices = UsbMassStorageDevice.getMassStorageDevices(context);//获取存储设备
                Log.w("usb", Arrays.toString(storageDevices));
                //需要activity?
                final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
                for (final UsbMassStorageDevice device : storageDevices) {//可能有几个 一般只有一个 因为大部分手机只有1个otg插口
                    if (usbManager.hasPermission(device.getUsbDevice())) {//有就直接读取设备是否有权限
                        read(device);
                    } else {//没有就去发起意图申请
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                initUsbReceiver();
                                usbManager.requestPermission(device.getUsbDevice(), pendingIntent); //该代码执行后，系统弹出一个对话框，
                            }
                        });


                    }
                }
            }
        }).start();

    }

    private static void read(UsbMassStorageDevice massDevice) {
        // before interacting with a device you need to call init()!
        try {

            massDevice.init();//初始化
            Log.w("usbread","massDevice:"+massDevice.getUsbDevice().toString());
            Log.w("usbread","patitions:"+ Arrays.toString(massDevice.getPartitions().toArray()));
            //Only uses the first partition on the device
            Partition partition = massDevice.getPartitions().get(0);
            FileSystem currentFs = partition.getFileSystem();
            //fileSystem.getVolumeLabel()可以获取到设备的标识
            //通过FileSystem可以获取当前U盘的一些存储信息，包括剩余空间大小，容量等等
            Log.w(TAG, "Capacity: " + currentFs.getCapacity());
            Log.w(TAG, "Occupied Space: " + currentFs.getOccupiedSpace());
            Log.w(TAG, "Free Space: " + currentFs.getFreeSpace());
            Log.w(TAG, "Chunk size: " + currentFs.getChunkSize());
            UsbFile root = currentFs.getRootDirectory();


        } catch (Throwable e) {
            e.printStackTrace();
            TShow("读取失败"+e.getMessage());
        }
    }

    private static void TShow(final String str) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context,str,Toast.LENGTH_LONG).show();
            }
        });

    }
}
