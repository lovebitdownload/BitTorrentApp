package com.frostwire.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ProgressFileEntity  {
    
    private final File file;
    private final String contentType; 

    private ProgressFileEntityListener _listener;

    public ProgressFileEntity(File file) {
        this.file = file;
        this.contentType = "binary/octet-stream";
    }
    
    public String getContentType() {
        return contentType;
    }

    public ProgressFileEntityListener getProgressFileEntityListener() {
        return _listener;
    }

    public void setProgressFileEntityListener(ProgressFileEntityListener listener) {
        _listener = listener;
    }

    public void writeTo(OutputStream outstream) throws IOException {
        if (outstream == null) {
            throw new IllegalArgumentException("Output stream may not be null");
        }

        InputStream instream = new FileInputStream(this.file);
        try {
            byte[] tmp = new byte[4096];
            int l;
            while ((l = instream.read(tmp)) != -1) {
                outstream.write(tmp, 0, l);
                fireOnWrite(l);

                if (_listener != null && !_listener.isRunning()) {
                    break;
                }
            }
            outstream.flush();
        } finally {
            instream.close();
        }
    }

    protected void fireOnWrite(int written) {
        if (_listener != null) {
            _listener.onWrite(this, written);
        }
    }

    public interface ProgressFileEntityListener {

        public void onWrite(ProgressFileEntity progressFileEntity, int written);

        public boolean isRunning();
    }
}
