/*
 * Copyright (c) 2012 EDC
 * 
 * This file is part of Stepping Stone.
 * 
 * Stepping Stone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Stepping Stone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Stepping Stone.  If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */
package org.edc.sstone.j2me.mod;

import gnu.classpath.java.util.zip.ZipEntry;
import gnu.classpath.java.util.zip.ZipInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;

import org.edc.sstone.CheckedException;
import org.edc.sstone.Constants;
import org.edc.sstone.io.InputStreamProvider;
import org.edc.sstone.log.Log;
import org.edc.sstone.nav.ModuleManager;
import org.edc.sstone.record.reader.RecordFactory;
import org.edc.sstone.record.reader.model.MenuItemRecord;
import org.edc.sstone.util.StdLib;

/**
 * @author Greg Orlowski
 */
public class FileModuleLoader extends ModuleLoader {

    private String rootPath;
    private static final byte FILETYPE_FILE = 1;
    private static final byte FILETYPE_DIR = 2;

    public FileModuleLoader(InputStreamProvider streamProvider, RecordFactory rf, String memoryCardPath) {
        super(streamProvider, rf);
        if (memoryCardPath != null) {
            rootPath = memoryCardPath;
        }
    }

    protected String getRootPath() {
        // If the root path is null (if the phone does return a value for fileconn.dir.memorycard,
        // set the rootPath to the LAST root directory, which is likely to be the memory card
        // if one exists
        if (rootPath == null) {
            String root = null;
            for (Enumeration e = FileSystemRegistry.listRoots(); e.hasMoreElements();) {
                root = (String) e.nextElement();
            }
            rootPath = StdLib.absPath("/", root, true);
        }
        return rootPath;
    }

    public Vector getModules() {
        Vector ret = new Vector();
        String modPath = getModulePath();
        // Log.debug("modpath : " + modPath);

        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(modPath, Connector.READ);
            // assume the fc is a directory we could use isDirectory() to check
            // and throw some exception otherwise, but it is prob better to just alow
            // an ioexception in that case.
            for (Enumeration e = fc.list(); e.hasMoreElements();) {
                String subdir = (String) e.nextElement();
                // Log.debug("found module dir: " + subdir);
                try {
                    MenuItemRecord mod = new ModuleManager(recordFactory,
                            modPath + subdir, streamProvider)
                            .getModuleHeaderRecord();
                    ret.addElement(mod);
                } catch (CheckedException mer) {
                    Log.warn("Error reading module from:" + StdLib.addSpacesToPath(modPath + "/" + subdir), mer);
                }

            }
        } catch (IOException e) {
            // TODO: better exception handling when we cannot read a module, although
            // failure to read a module might be a fatal exception
            Log.error("io error reading module path: " + StdLib.addSpacesToPath(modPath), e);
            throw new RuntimeException(e.getMessage() + "; modPath = " + modPath);
        } finally {
            if (fc != null && fc.isOpen()) {
                try {
                    fc.close();
                } catch (IOException ignoreFcCloseException) {
                }
            }
        }
        return ret;
    }

    public Vector listUpdates() {
        Vector ret = new Vector();
        String rootPath = getRootPath();

        // Log.debug("listUpdates() for rootPath: " + StdLib.addSpacesToPath(rootPath));
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(rootPath, Connector.READ);
            if (fc.isDirectory()) {
                String filename;
                for (Enumeration e = fc.list(); e.hasMoreElements();) {
                    filename = (String) e.nextElement();
                    if (filename.endsWith("." + Constants.PROJECT_FILE_EXT)) {
                        Log.debug("Found module: " + rootPath + filename);
                        ret.addElement(rootPath + filename);
                    }
                }
            }
        } catch (IOException e) {
            Log.warn("error in listUpdates() with rootPath: " + StdLib.addSpacesToPath(rootPath), e);
        } finally {
            if (fc != null) {
                try {
                    fc.close();
                } catch (IOException ignoreCloseError) {
                }
            }
        }
        return ret;
    }

    public void rm_rf(String path) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(path, Connector.READ_WRITE, false);
            if (path != null && path.trim().length() > 0 && fc.exists()) {
                if (fc.isDirectory()) {
                    String[] files = listFiles(fc, FILETYPE_FILE);
                    String[] dirs = listFiles(fc, FILETYPE_DIR);
                    // close it
                    if (files.length > 0 && dirs.length > 0) {
                        fc.close();
                        fc = null;
                    }

                    int i = 0;
                    for (i = 0; i < dirs.length; i++) {
                        rm_rf(path + dirs[i]);
                    }
                    for (i = 0; i < files.length; i++) {
                        rm_rf(path + files[i]);
                    }

                    // reopen then delete
                    if (fc == null || !fc.isOpen()) {
                        fc = (FileConnection) Connector.open(path, Connector.READ_WRITE, false);
                    }
                }
                // Log.debug("Deleting file: " + StdLib.addSpacesToPath(path));
                fc.delete();
            }
            fc.close();
            fc = null;
        } catch (IOException ioe) {
            Log.error("Error deleting path: " + path, ioe);
        } catch (Throwable t) {
            Log.error("Throwable deleting path:\n" + path, t);
        } finally {
            if (fc != null && fc.isOpen()) {
                try {
                    fc.close();
                } catch (IOException ignoreCloseFailed) {
                }
            }
        }
    }

    /*
     * Nokia c101 throws "IllegalArgumentException: invalid character in filter" when we use
     * filters. (maybe it is when we use "*"+"/" as a filter?) The easiest workaround is to use the
     * no-arg fc.list() method rather than the overloaded variant that accepts filters.
     */
    static String[] listFiles(FileConnection fc, byte fileType) throws IOException {
        Enumeration e = fc.list();
        Vector v = new Vector();
        String nm = null;
        while (e.hasMoreElements()) {
            nm = (String) e.nextElement();
            if (fileType == FILETYPE_DIR) {
                if (nm.endsWith("/"))
                    v.addElement(nm);
            } else if (fileType == FILETYPE_FILE) {
                if (!nm.endsWith("/"))
                    v.addElement(nm);
            }
        }
        String[] ret = new String[v.size()];
        v.copyInto(ret);
        return ret;
    }

    public void unpackUpdate(final String path) {
        Log.info("Unpacking update: " + StdLib.addSpacesToPath(path));

        FileConnection fc = null;
        InputStream in = null;

        // always expand to lowercase
        String basename = StdLib.basename(path).toLowerCase();
        String outdir = basename.substring(0, basename.indexOf("." + Constants.PROJECT_FILE_EXT)).replace(' ',
                '_');
        outdir = StdLib.absPath(getModulePath(), outdir, true);

        // Delete the existing module directory before we unzip. This does nothing
        // if the directory does not exist.
        rm_rf(outdir);

        // Log.debug("outdir deleted:\n" + StdLib.addSpacesToPath(outdir));

        try {
            fc = (FileConnection) Connector.open(path, Connector.READ_WRITE);
            in = fc.openInputStream();
            unzip(in, outdir);
            if (deleteArchivesAfterInstall()) {
                fc.delete();
            }
        } catch (IOException e) {
            Log.warn("Could not unpack update: " + StdLib.addSpacesToPath(path), e);
        } catch (Throwable t) {
            Log.error("Caught throwable unpacking update: " + StdLib.addSpacesToPath(path), t);
        } finally {
            try {
                if (fc != null && fc.isOpen()) {
                    fc.close();
                    fc = null;
                }
            } catch (IOException ignoreCloseFailure) {
            }

            try {
                if (in != null) {
                    in.close();
                    in = null;
                }
            } catch (IOException ignoreCloseFailure) {
            }
        }
    }

    protected String getModulePath() {
        return StdLib.absPath(getRootPath(), "sstone/modules", true);
    }

    /**
     * Create a directory for the given file path. If it already exists, do nothing.
     * 
     * @param dir
     * @throws IOException
     */
    private void mkdir(String dir) {
        // Log.debug("mkdir: " + StdLib.addSpacesToPath(dir));
        FileConnection fc = null;
        if (dir.trim().length() == 0) {
            Log.debug("mkdir got empty dir.");
        }
        // Log.debug("mkdir: " + dir);
        try {
            fc = (FileConnection) Connector.open(dir, Connector.READ_WRITE);
            if (!fc.exists()) {
                fc.mkdir();
            } else {
                if (!fc.isDirectory()) {
                    throw new RuntimeException("Tried to create dir [" + dir + "], but it is a file.");
                }
                // if directory DOES exist and is a directory, do nothing
            }
        } catch (IOException e) {
            Log.error("IOException with mkdir:\n" + StdLib.addSpacesToPath(dir), e);
        } catch (Throwable t) {
            Log.error("Throwable with mkdir:\n" + StdLib.addSpacesToPath(dir), t);
        } finally {
            if (fc != null) {
                try {
                    fc.close();
                    fc = null;
                } catch (IOException ignoreCloseError) {
                }
            }
        }
    }

    private void mkdir_p(String dir, String aboveRootDir) throws IOException {
        String[] parentDirs = StdLib.parentDirs(dir, aboveRootDir);
        // create all the parent dirs
        // Log.debug("mkdir_p: " + StdLib.addSpacesToPath(dir));

        for (int i = 0; i < parentDirs.length; i++) {
            mkdir(StdLib.absPath(parentDirs[i], true));
        }
        // Log.debug("making dir itself: " + StdLib.addSpacesToPath(dir));
        // finally create the directory itself
        mkdir(StdLib.absPath(dir, true));
    }

    private void writeZipEntryToFile(ZipInputStream zin, ZipEntry ze, String extractionRootDir, byte[] buff) {
        FileConnection fc = null;
        OutputStream out = null;
        int bytesRead = -1;

        try {
            String filepath = extractionRootDir.endsWith("/")
                    ? extractionRootDir + ze.getName()
                    : extractionRootDir + '/' + ze.getName();

            // Log.debug("Writing file: \n" + StdLib.addSpacesToPath(filepath));
            fc = (FileConnection) Connector.open(filepath, Connector.READ_WRITE);
            if (!fc.exists()) {
                StringBuffer sb = new StringBuffer(filepath.length() + 10);
                for (int i = 0; i < filepath.length(); i += 15) {
                    sb.append(filepath.substring(i, Math.min(filepath.length(), i + 15)));
                    sb.append(" ");
                }
                // Log.debug("Creating file: \n" + sb.toString());
                fc.create();
            }
            // Log.debug("after create: \n" + StdLib.addSpacesToPath(filepath));
            out = fc.openOutputStream();
            while ((bytesRead = zin.read(buff, 0, buff.length)) != -1) {
                out.write(buff, 0, bytesRead);
            }
            out.flush();
        } catch (IOException ioe) {
            Log.error("IOException writing file:\n ", ioe);
        } catch (Throwable t) {
            Log.error("Throwable writing file:\n ", t);
        } finally {
            if (fc != null) {
                try {
                    fc.close();
                    fc = null;
                } catch (IOException ignore) {
                }
            }

            if (out != null) {
                try {
                    out.close();
                    out = null;
                } catch (IOException ignore) {
                }
            }
        }
    }

    private void unzip(InputStream in, String extractionRootDir) {
        ZipInputStream zin = null;
        ZipEntry ze = null;
        byte[] buff = new byte[1024];

        extractionRootDir = StdLib.absPath(extractionRootDir, true);

        // Ensure that the output directory exists. We *DO* need this even though we use mkdir_p
        // as we process directories contained within the zip file b/c that processing
        // will fail if files exist at the root directory of the zip file.

        try {
            mkdir_p(extractionRootDir, getRootPath());

            zin = (in instanceof ZipInputStream)
                    ? (ZipInputStream) in
                    : new ZipInputStream(in);

            // Log.debug("processing zip entries:\n");
            while ((ze = zin.getNextEntry()) != null) {
                // Log.debug("Processing entry:\n" + ze.getName());
                if (ze.isDirectory()) {
                    mkdir_p(extractionRootDir + ze.getName(), getRootPath());
                } else {
                    writeZipEntryToFile(zin, ze, extractionRootDir, buff);
                }
            }
        } catch (IOException e) {
            Log.error("IOException unzipping project:\n", e);
        } catch (Throwable t) {
            Log.error("Throwable unzipping project:\n", t);
        } finally {
            if (zin != null) {
                try {
                    zin.close();
                    zin = null;
                } catch (IOException ignoreClose) {
                }
            }
        }
    }

    protected boolean deleteArchivesAfterInstall() {
        return true;
    }

}
