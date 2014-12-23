//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.http.requests;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.parser.html.Form;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.CountingOutputStream;
import org.appwork.utils.net.NullOutputStream;
import org.appwork.utils.net.httpconnection.HTTPConnection.RequestMethod;

public class PostRequest extends Request {
    private static enum SEND {
        VARIABLES,
        STRING,
        BYTES,
        NOTHING
    }

    public static java.util.List<RequestVariable> variableMaptoArray(final LinkedHashMap<String, String> post) {
        if (post == null) { return null; }
        final java.util.List<RequestVariable> ret = new ArrayList<RequestVariable>();
        for (final Entry<String, String> entry : post.entrySet()) {
            ret.add(new RequestVariable(entry.getKey(), entry.getValue()));
        }
        return ret;
    }

    private final java.util.List<RequestVariable> postVariables;
    private String                           postString  = null;
    private String                           contentType = null;
    private byte[]                           postBytes   = null;
    private SEND                             sendWHAT    = null;

    public PostRequest(final Form form) throws MalformedURLException {
        super(form.getAction(null));
        this.postVariables = new ArrayList<RequestVariable>();
    }

    public PostRequest(final String url) throws MalformedURLException {
        super(Browser.correctURL(url));
        this.postVariables = new ArrayList<RequestVariable>();
    }

    public void addAll(final java.util.List<RequestVariable> post) {
        this.postVariables.addAll(post);
    }

    public void addAll(final HashMap<String, String> post) {
        for (final Entry<String, String> entry : post.entrySet()) {
            this.postVariables.add(new RequestVariable(entry));
        }
    }

    public void addVariable(final String key, final String value) {
        this.postVariables.add(new RequestVariable(key, value));
    }

    public String getPostDataString() {
        final StringBuilder buffer = new StringBuilder();
        for (final RequestVariable rv : this.postVariables) {
            if (rv.getKey() != null) {
                buffer.append("&");
                buffer.append(rv.getKey());
                buffer.append("=");
                if (rv.getValue() != null) {
                    buffer.append(rv.getValue());
                } else {
                    buffer.append("");
                }
            }
        }
        if (buffer.length() == 0) { return ""; }
        return buffer.substring(1);
    }

    public String log() {
        if (this.sendWHAT == null) { return null; }
        switch (this.sendWHAT) {
        case NOTHING:
            return "zero content send";
        case BYTES:
            return this.postBytes.length + " raw-bytes send";
        case STRING:
            return this.postString;
        case VARIABLES:
            return this.getPostDataString();
        default:
            return "unknown postData send";
        }
    }

    @SuppressWarnings("resource")
    private long postContent(final URLConnectionAdapter httpConnection) throws IOException {
        if (this.sendWHAT == null) { throw new IOException("preRequest needs to be called first!"); }
        String postString = null;
        CountingOutputStream output = null;
        if (httpConnection != null && httpConnection.getOutputStream() != null) {
            output = new CountingOutputStream(httpConnection.getOutputStream());
        } else {
            output = new CountingOutputStream(new NullOutputStream());
        }
        switch (this.sendWHAT) {
        case NOTHING:
            return 0;
        case BYTES:
            output.write(this.postBytes);
            return output.transferedBytes();
        case STRING:
            postString = this.postString;
            break;
        case VARIABLES:
            postString = this.getPostDataString();
            break;
        default:
            throw new IOException("not implemented " + this.sendWHAT.name());
        }
        try {
            final OutputStreamWriter wr = new OutputStreamWriter(output, "UTF-8");
            wr.write(postString);
            wr.flush();
            output.flush();
        } finally {
        }
        return output.transferedBytes();
    }

    /**
     * send the postData of the Request. in case httpConnection is null, it
     * outputs the data to a NullOutputStream
     */
    @Override
    public long postRequest() throws IOException {
        return this.postContent(this.httpConnection);
    }

    @Override
    public void preRequest() throws IOException {
        this.httpConnection.setRequestMethod(RequestMethod.POST);
        if (this.contentType != null) {
            /* set Content Type */
            this.httpConnection.setRequestProperty("Content-Type", this.contentType);
        }
        /*
         * set Content-Length
         */
        if (this.postVariables != null && this.postVariables.size() > 0) {
            this.sendWHAT = SEND.VARIABLES;
            this.httpConnection.setRequestProperty("Content-Length", this.postContent(null) + "");
        } else if (!StringUtils.isEmpty(this.postString)) {
            this.sendWHAT = SEND.STRING;
            this.httpConnection.setRequestProperty("Content-Length", this.postContent(null) + "");
        } else if (this.postBytes != null) {
            this.sendWHAT = SEND.BYTES;
            this.httpConnection.setRequestProperty("Content-Length", this.postContent(null) + "");
        } else {
            this.sendWHAT = SEND.NOTHING;
            this.httpConnection.setRequestProperty("Content-Length", "0");
        }
    }

    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    public void setPostBytes(final byte[] post) {
        this.postBytes = post;
    }

    public void setPostDataString(final String post) {
        this.postString = post;
    }
}
