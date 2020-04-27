/**
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any means.
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 * 
 * For more information, please refer to <https://unlicense.org>
 */
package com.fungus_soft.utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import roycurtis.softplugin.SoftPlugin;

import javax.tools.JavaCompiler.CompilationTask;

/**
 * A Java source compiler that does not require the Java Development Kit to compile
 * 
 * @author Isaiah Patton
 */
public class AbstractCompiler {

    private JavacClassLoader cl;
    private boolean finished;

    private String toolsURL = "https://files.fungus-soft.com/javac/tools.jar";
    private int toolsSize = 18350729;

    public AbstractCompiler() {
        this.finished = false;
    }

    public boolean compile(List<File> sources, List<String> arguments, DiagnosticListener<JavaFileObject> diagnostics) {
        JavaCompiler javac = null;
        try {
            javac = getJavaCompiler();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        StandardJavaFileManager fileManager = javac.getStandardFileManager(diagnostics, null, Charset.forName("UTF-8"));
        CompilationTask task = javac.getTask(null, fileManager, diagnostics, arguments, null, fileManager.getJavaFileObjectsFromFiles(sources));

        return task.call();
    }

    private JavaCompiler getJavaCompiler() throws Exception {
        if (null != ToolProvider.getSystemJavaCompiler())
            return ToolProvider.getSystemJavaCompiler();

        System.out.println("Detected JRE instead of JDK.");
        System.out.println("Downloading Java compiler from \"" + toolsURL + "\"...");

        downloadToolsJar();
        while (cl == null)
            pause(); // Wait because downloading is done on separate thread

        return (JavaCompiler) Class.forName("com.sun.tools.javac.api.JavacTool", true, cl).newInstance();
    }

    private void downloadToolsJar() {
        try {
            this.finished = false;
            File toolsJar = new File(new File(SoftPlugin.INSTANCE.getDataFolder(), "jdk"), "tools.jar");
            toolsJar.getParentFile().mkdir();

            Download d = new Download(new URL(toolsURL), toolsJar.getParentFile());
            if (toolsJar.exists()) {
                int size = (int) toolsJar.length();
                if (size == toolsSize) {
                    System.out.println("Downloaded tools.jar size matches. No need to download again.");
                    try {
                        cl = new JavacClassLoader(new URL[] {toolsJar.toURI().toURL()}, this.getClass().getClassLoader());
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                d.setDownloaded((int)toolsJar.length());
            }
            d.startDownloading();
            System.out.println((int)toolsJar.length());

             Timer t = new Timer();t.schedule(new TimerTask() { @Override public void run() {
                System.out.println("Downloading Java compiler: " + (int)d.getProgress() + "%");
                pause();

                if ((d.getStatus() == 2)&& !finished) {
                    finished = true;
                    t.cancel();

                    try {
                        cl = new JavacClassLoader(new URL[] {toolsJar.toURI().toURL()}, this.getClass().getClassLoader());
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            }}, 50, 500);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}