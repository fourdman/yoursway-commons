package com.yoursway.utils;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class YsFileUtils {
    
    public static String readAsString(File source) throws IOException {
        return readAsString(source, "utf-8");
    }
    
    public static String readAsString(File source, String encoding) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        loadFromFile(source, baos);
        byte[] bytes = baos.toByteArray();
        return new String(bytes, encoding);
    }
    
    public static void writeString(File destination, String data) throws IOException {
        writeString(destination, data, "utf-8");
    }
    
    public static void writeString(File destination, String data, String encoding) throws IOException {
        writeBytes(destination, data.getBytes(encoding));
    }
    
    public static void writeBytes(File destination, byte[] data) throws FileNotFoundException, IOException {
        saveToFile(new ByteArrayInputStream(data), destination);
    }
    
    public static void cp_r(File source, File destinationParentFolder) throws IOException {
        List<File> e = emptyList();
        cp_r_exclude(source, destinationParentFolder, e);
    }
    
    public static void cp_r_exclude(File source, File destinationParentFolder, Collection<File> excluded)
            throws IOException {
        destinationParentFolder.mkdirs();
        cp_r_(source, destinationParentFolder, excluded);
    }
    
    /**
     * Prereq: <code>destinationParentFolder</code> exists.
     * 
     * @param excluded
     */
    private static void cp_r_(File source, File destinationParentFolder, Collection<File> excluded)
            throws IOException {
        if (excluded.contains(source))
            return;
        if (!source.isDirectory()) {
            fileCopy(source, new File(destinationParentFolder, source.getName()));
        } else {
            File childrenDestination = new File(destinationParentFolder, source.getName());
            cp_r_children(source, childrenDestination, excluded);
        }
    }
    
    public static void cp_r_children(File source, File childrenDestination) throws IOException {
        List<File> e = emptyList();
        cp_r_children(source, childrenDestination, e);
    }
    
    public static void cp_r_children(File source, File childrenDestination, Collection<File> excluded)
            throws IOException {
        childrenDestination.mkdirs();
        
        File[] children = source.listFiles();
        if (children != null)
            for (File child : children)
                cp_r_(child, childrenDestination, excluded);
    }
    
    public static void fileCopy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            saveToFile(in, dst);
        } finally {
            in.close();
        }
    }
    
    public static void download(URL url, File dst) throws IOException {
        InputStream in = url.openStream();
        try {
            saveToFile(in, dst);
        } finally {
            in.close();
        }
    }
    
    public static void saveToFile(InputStream in, File dst) throws FileNotFoundException, IOException {
        OutputStream out = new FileOutputStream(dst);
        try {
            transfer(in, out);
        } finally {
            out.close();
        }
    }
    
    public static void loadFromFile(File src, OutputStream out) throws FileNotFoundException, IOException {
        InputStream in = new FileInputStream(src);
        try {
            transfer(in, out);
        } finally {
            in.close();
        }
    }
    
    public static void transfer(InputStream in, OutputStream out) throws IOException {
        byte[] buf = allocateBuffer();
        int len;
        while ((len = in.read(buf)) > 0)
            out.write(buf, 0, len);
    }
    
    private static byte[] allocateBuffer() {
        return new byte[1024 * 1024];
    }
    
    public static File createTempFolder(String prefix, String suffix) throws IOException {
        File file = File.createTempFile(prefix, suffix);
        return new File(file.getParentFile(), file.getName() + ".dir");
    }
    
    public static File findEclipsePluginJar(File folder, String bundleName) {
        return chooseLatestVersion(findEclipsePluginJars(folder, bundleName));
    }
    
    public static File chooseLatestVersion(Collection<File> jars) {
        List<File> jarsList = newArrayList(jars);
        if (jarsList.isEmpty())
            return null;
        Collections.sort(jarsList, YsStrings.toStringComparator(YsStrings.getNaturalComparatorAscii()));
        return jarsList.get(jarsList.size() - 1);
    }
    
    public static Collection<File> findEclipsePluginJars(File folder, String bundleName) {
        Collection<File> result = newArrayList();
        File[] files = folder.listFiles();
        if (files != null)
            for (File file : files)
                addPluginIfMatches(file, bundleName, result);
        return result;
    }
    
    public static void addPluginIfMatches(File folderOrJar, String bundleName, Collection<File> result) {
        String name = folderOrJar.getName();
        if (folderOrJar.isFile()) {
            if (name.equals(bundleName + ".jar") || name.startsWith(bundleName + "_")
                    && name.endsWith(".jar"))
                result.add(folderOrJar);
        } else {
            if (name.equals(bundleName) || name.startsWith(bundleName + "_"))
                result.add(folderOrJar);
        }
    }
    
    public static File urlToFileWithProtocolCheck(URL url) {
        if (!url.getProtocol().equals("file"))
            throw new IllegalArgumentException("URL is not a file: " + url);
        return new File(url.getPath());
    }
    
    public static void deleteFile(File file) {
        if (file.exists() && !file.delete())
            throw new RuntimeException("Cannot delete file " + file);
    }
    
    public static void deleteRecursively(File directory) {
        File[] children = directory.listFiles();
        if (children != null) {
            for (File child : children)
                if (child.isDirectory())
                    deleteRecursively(child);
                else
                    deleteFile(child);
            
            if (!directory.delete())
                throw new RuntimeException("Cannot delete directory " + directory);
        }
    }
    
    public static void zipFolderContents(File folder, File zip) throws IOException {
        FileOutputStream fout = new FileOutputStream(zip);
        ZipOutputStream out = new ZipOutputStream(fout);
        try {
            zipChildren(folder, "", out);
        } finally {
            out.close();
        }
    }
    
    private static void zipChildren(File folder, String path, ZipOutputStream out) throws IOException {
        File[] files = folder.listFiles();
        if (files == null)
            return;
        for (File file : files) {
            if (file.isFile()) {
                String name = path + file.getName();
                out.putNextEntry(new ZipEntry(name));
                loadFromFile(file, out);
                out.closeEntry();
            } else if (file.isDirectory()) {
                zipChildren(file, path + file.getName() + "/", out);
            }
        }
        
    }
    
}
