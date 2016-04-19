import java.io.File;
import java.lang.reflect.Array;

public class Main {

    public static void main(String[] args) throws Exception {
        try {
        	System.out.println("Running");
            LodeServlet lodeServlet = new LodeServlet();
            if (Array.getLength(args) > 0) {
            	System.out.println(args[0].toString());
                File file = new File(args[0]);
                if (!file.isDirectory()) {
                    lodeServlet.doGet(args[0]);
                }
            }
            else
            {
            	System.out.println("Please supply a file to transform");
            }
        } catch (Exception e) {
        	System.out.println(e.getMessage());
        }
        System.out.println("Finished");
        System.exit(0);
    }
}
