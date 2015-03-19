package com.nice;

import java.io.File;
import java.io.PrintWriter;

/**
 * Created by Nate on 19/03/15.
 */
public class GetSource {
    private static final long serialVersionUID = 1L;

    public GetSource() {
        super();
        // TODO Auto-generated constructor stub
    }

    protected void doGet(String path) throws Exception {

        SourceExtractor extractor = new SourceExtractor();
        File f = new File(path);

        try {
            String content = "";
            content = extractor.exec(f);
            PrintWriter out = new PrintWriter(content);
            out.println(content);
        } catch (Exception e) {
            throw e;
        }
    }



}
