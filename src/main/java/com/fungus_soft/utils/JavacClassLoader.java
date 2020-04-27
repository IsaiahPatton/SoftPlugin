package com.fungus_soft.utils;

import java.net.URL;
import java.net.URLClassLoader;

public class JavacClassLoader extends URLClassLoader {

    public JavacClassLoader(URL[] arg0, ClassLoader parent) {
        super(arg0, parent);
    }

}