package com.threethan.launcher.lib;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Objects;


// Contains functions which are not application-specific
public class FileLib {

    public static void delete(String path) {
        delete(new File(path));
    }
    public static void delete(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : Objects.requireNonNull(fileOrDirectory.listFiles()))
                delete(child);

        final boolean ignored = fileOrDirectory.delete();
    }

    /** @noinspection IOStreamConstructor*/ // Fix requires higher API
    public static void copy(File fIn, File fOut) {
        try {
            InputStream in = new FileInputStream(fIn);
            //noinspection ResultOfMethodCallIgnored
            Objects.requireNonNull(fOut.getParentFile()).mkdirs();
            OutputStream out = new FileOutputStream(fOut);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.v("PATH", fOut.getAbsolutePath());
    }
}
