package com.strandls.file.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import javax.ws.rs.core.StreamingOutput;

public class FileUtil {

    public static StreamingOutput fromFileToStream(File src) throws Exception {
        InputStream in = new FileInputStream(src);

        StreamingOutput sout = (OutputStream out) -> {
            byte[] buf = new byte[8192];
            int c;
            while ((c = in.read(buf, 0, buf.length)) > 0) {
                out.write(buf, 0, c);
                out.flush();
            }
            in.close();
            out.close();
        };
        return sout;
    }
}
