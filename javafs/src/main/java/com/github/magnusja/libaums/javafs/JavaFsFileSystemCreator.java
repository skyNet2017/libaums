package com.github.magnusja.libaums.javafs;

import android.util.Log;

import com.github.magnusja.libaums.javafs.wrapper.device.DeviceWrapper;
import com.github.magnusja.libaums.javafs.wrapper.device.FSBlockDeviceWrapper;
import com.github.magnusja.libaums.javafs.wrapper.fs.FileSystemWrapper;
import com.github.mjdev.libaums.driver.BlockDeviceDriver;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.FileSystemCreator;
import com.github.mjdev.libaums.partition.PartitionTableEntry;

import org.apache.log4j.Logger;
import org.jnode.fs.FileSystemException;
import org.jnode.fs.FileSystemType;
import org.jnode.fs.exfat.ExFatFileSystemType;
import org.jnode.fs.ext2.Ext2FileSystemType;
import org.jnode.fs.hfs.HfsWrapperFileSystemType;
import org.jnode.fs.hfsplus.HfsPlusFileSystemType;
import org.jnode.fs.iso9660.ISO9660FileSystemType;
import org.jnode.fs.jfat.FatFileSystemType;
import org.jnode.fs.ntfs.NTFSFileSystemType;
import org.jnode.fs.xfs.XfsFileSystemType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import de.mindpipe.android.logging.log4j.LogCatAppender;

/**
 * Created by magnusja on 3/1/17.
 */

public class JavaFsFileSystemCreator implements FileSystemCreator {

    private static final String TAG = JavaFsFileSystemCreator.class.getSimpleName();

    private static List<FileSystemType> fsTypes = new ArrayList<>();

    static {
        final Logger root = Logger.getRootLogger();
        final LogCatAppender logCatAppender = new LogCatAppender();
        root.addAppender(logCatAppender);

        fsTypes.add(new NTFSFileSystemType());
        fsTypes.add(new ExFatFileSystemType());

        fsTypes.add(new FatFileSystemType());
        fsTypes.add(new org.jnode.fs.fat.FatFileSystemType());

        fsTypes.add(new HfsPlusFileSystemType());
        fsTypes.add(new HfsWrapperFileSystemType());

        fsTypes.add(new Ext2FileSystemType());
        fsTypes.add(new XfsFileSystemType());

        fsTypes.add(new ISO9660FileSystemType());
    }


    @Override
    public FileSystem read(PartitionTableEntry entry, BlockDeviceDriver blockDevice) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        blockDevice.read(0, buffer);

        FSBlockDeviceWrapper wrapper = new FSBlockDeviceWrapper(blockDevice, entry);
        Log.e(TAG, "fsTypes  " + fsTypes.size());

        try {
            FileSystem fileSystem = new FileSystemWrapper(new NTFSFileSystemType().create(new DeviceWrapper(blockDevice, entry), true));
            Log.e(TAG, "fs is the type  NTFSFileSystemType" );
            return fileSystem;
        } catch (FileSystemException e) {
            e.printStackTrace();
        }
        /*for (FileSystemType type : fsTypes) {
            try {
                byte[] bytes = buffer.array();
                Log.e(TAG, "bytes:" + new String(bytes, "utf-8"));
                boolean support = type.supports(wrapper.getPartitionTableEntry(), bytes, wrapper);

                if (support) {

                    FileSystem fileSystem = new FileSystemWrapper(type.create(new DeviceWrapper(blockDevice, entry), false));
                    Log.e(TAG, "fs is the type " + type.getName());
                    return fileSystem;

                } else {
                    Log.e(TAG, "fs not the type " + type.getName());
                }
            } catch (Exception e) {
                Log.e(TAG, "error creating fs with type " + type.getName());
                e.printStackTrace();
            }
        }*/

        return null;
    }
}
