/**
 * Copyright (C) 2016 Roy Adrian Curtis
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the Software), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED AS IS WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package roycurtis.softplugin;

import com.fungus_soft.utils.AbstractCompiler;
import com.google.common.io.PatternFilenameFilter;
import org.bukkit.Bukkit;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static roycurtis.softplugin.SoftPlugin.SOFTLOG;

/**
 * Pipeline class that handles the discovery and compilation of Java source files
 */
public class Compiler {

    // Path to JAR that server's Bukkit API is running from, for classpath use
    private static final String BUKKIT_JAR = Bukkit.class.getProtectionDomain().getCodeSource().getLocation().getPath();

    private AbstractCompiler javac;
    private Diagnostics diagnostics;
    private ArrayList<File> sourceFiles;

    Compiler(Diagnostics diagnostics) {
        this.javac = new AbstractCompiler();
        this.diagnostics = diagnostics;

        SOFTLOG.fine("Compiler created");
    }

    void compile() {
        cleanCache();
        discoverFiles();
        compileFiles();

        SOFTLOG.fine("Compiler pipeline finished");
    }

    /**
     * Recursively looks for and deletes existing class files in the cache directory
     */
    private void cleanCache() {
        try (Stream<Path> fileWalker = Files.walk(Config.Dirs.cache)) {
            fileWalker
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".class"))
                .forEach(path -> {
                    if (path.toFile().delete())
                        SOFTLOG.finest("Deleted cached class file " + path);
                    else throw new RuntimeException("Could not delete file " + path);
                });
        } catch (Exception e) {
            throw new RuntimeException("Could not clean cache", e);
        }
    }

    /**
     * Recursively looks for and collects any Java source files in the source directory
     */
    private void discoverFiles() {
        sourceFiles = new ArrayList<>();

        try (Stream<Path> fileWalker = Files.walk(Config.Dirs.source)) {
            fileWalker
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(path -> {
                    File file = path.toFile();

                    SOFTLOG.finest("Discovered source file " + path);
                    sourceFiles.add(file);
                });
        } catch (Exception e) {
            throw new RuntimeException("Could not discover source files", e);
        }

        int fileCount = sourceFiles.size();
        if (fileCount < 1)
            throw new RuntimeException("No source files were found to compile");
        else SOFTLOG.fine("Discovered source files: " + fileCount);
    }

    /**
     * Attempts to compile all found source files into the cache directory, as class files
     */
    private void compileFiles() {
        List<String> options = Arrays.asList(
            "-d", Config.Dirs.cache.toString(), // Built class output directory
            "-classpath", generateClasspath(),  // Use Bukkit + plugins as classpath
            "-Xlint"                            // Report all code warnings
        );

        SOFTLOG.info("Compiling " + sourceFiles.size() + " source files. . .");

        if (!javac.compile(sourceFiles, options, diagnostics))
            throw new CompilerException();
    }

    /**
     * Generates classpath string using paths of all plugins available and the Bukkit JAR
     */
    private String generateClasspath() {
        File pluginDir = new File("plugins/").getAbsoluteFile();
        File[] plugins = pluginDir.listFiles( new PatternFilenameFilter(".+\\.jar") );
        String classpath = BUKKIT_JAR;

        for (File plugin : plugins)
            classpath += ";" + plugin.getAbsolutePath();

        return classpath;
    }

}