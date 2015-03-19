package com.nice;

import java.io.File;
import java.lang.reflect.Array;

public class Main {

    public static void main(String[] args) throws Exception {
        try {

            LodeServlet lodeServlet = new LodeServlet();
            if (Array.getLength(args) > 0) {
                File file = new File(args[0]);
                if (file.exists() && !file.isDirectory()) {
                    lodeServlet.doGet(args[0]);
                }
            }
        } catch (Exception e) {
            throw e;
        }
    }
}
