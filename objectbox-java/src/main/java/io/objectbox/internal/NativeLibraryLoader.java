package io.objectbox.internal;

import org.greenrobot.essentials.io.IoUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Separate class, so we can mock BoxStore.
 */
public class NativeLibraryLoader {
    static {
        String libname = "objectbox";
        // For Android, os.name is also "Linux", so we need an extra check
        if (!System.getProperty("java.vendor").contains("Android")) {
            String osName = System.getProperty("os.name");
            String sunArch = System.getProperty("sun.arch.data.model");
            if (osName.contains("Windows")) {
                libname += "-windows" + ("32".equals(sunArch) ? "-x86" : "-x64");
                checkUnpackLib(libname + ".dll");
            } else if (osName.contains("Linux")) {
                libname += "-linux" + ("32".equals(sunArch) ? "-x86" : "-x64");
                checkUnpackLib("lib" + libname + ".so");
            }
        }
        System.loadLibrary(libname);
    }

    private static void checkUnpackLib(String filename) {
        String path = "/native/" + filename;
        URL resource = NativeLibraryLoader.class.getResource(path);
        if (resource == null) {
            System.err.println("Not available in classpath: " + path);
        } else {
            File file = new File(filename);
            try {
                URLConnection urlConnection = resource.openConnection();
                int length = urlConnection.getContentLength();
                long lastModified = urlConnection.getLastModified();
                if (!file.exists() || file.length() != length || file.lastModified() != lastModified) {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    try {
                        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                        try {
                            IoUtils.copyAllBytes(in, out);
                        } finally {
                            IoUtils.safeClose(out);
                        }
                    } finally {
                        IoUtils.safeClose(in);
                    }
                    if (lastModified > 0) {
                        file.setLastModified(lastModified);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void ensureLoaded() {
    }
}
