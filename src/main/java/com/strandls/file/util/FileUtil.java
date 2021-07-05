package com.strandls.file.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

public class FileUtil {

    private FileUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static StreamingOutput fromFileToStream(File src) throws IOException {
        StreamingOutput sout;

        try (InputStream in = new FileInputStream(src)) {
            sout = new StreamingOutput() {
                @Override
                public void write(OutputStream out) throws IOException, WebApplicationException {
                    byte[] buf = new byte[8192];
                    int c;
                    while ((c = in.read(buf, 0, buf.length)) > 0) {
                        out.write(buf, 0, c);
                        out.flush();
                    }
                    out.close();
                }
            };
            return sout;
        }
    }
}
