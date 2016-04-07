package main.java;

import java.io.File;
import java.io.PrintWriter;

/**
 * Created by Nate on 19/03/15.
 */
public class GetSource {

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
            out.close();
        } catch (Exception e) {
            throw e;
        }
    }



}
