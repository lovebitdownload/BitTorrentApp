package org.appwork.utils.net.httpconnection;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.ByteBuffer;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.logging.Log;

public class HTTPConnectionUtils {

    private static byte R = (byte) 13;
    private static byte N = (byte) 10;

    public static String getFileNameFromDispositionHeader(String header) {
        // http://greenbytes.de/tech/tc2231/
        if (StringUtils.isEmpty(header)) { return null; }
        final String orgheader = header;
        String contentdisposition = header;

        String filename = null;
        for (int i = 0; i < 2; i++) {
            if (contentdisposition.contains("filename*")) {
                /* Codierung default */
                /*
                 * Content-Disposition: attachment;filename==?UTF-8?B?
                 * RGF2aWQgR3VldHRhIC0gSnVzdCBBIExpdHRsZSBNb3JlIExvdmUgW2FMYnlsb3ZlciBYLUNsdXNpdiBSZW1peF0uTVAz
                 * ?=
                 */
                /* remove fallback, in case RFC 2231/5987 appear */
                contentdisposition = contentdisposition.replaceAll("filename=.*?;", "");
                contentdisposition = contentdisposition.replaceAll("filename\\*", "filename");
                final String format = new Regex(contentdisposition, ".*?=[ \"']*(.+)''").getMatch(0);
                if (format == null) {
                    Log.L.severe("Content-Disposition: invalid format: " + header);
                    filename = null;
                    return filename;
                }
                contentdisposition = contentdisposition.replaceAll(format + "''", "");
                filename = new Regex(contentdisposition, "filename.*?=[ ]*\"(.+)\"").getMatch(0);
                if (filename == null) {
                    filename = new Regex(contentdisposition, "filename.*?=[ ]*'(.+)'").getMatch(0);
                }
                if (filename == null) {
                    header = header.replaceAll("=", "=\"") + "\"";
                    header = header.replaceAll(";\"", "\"");
                    contentdisposition = header;
                } else {
                    try {
                        filename = URLDecoder.decode(filename, format);
                    } catch (final Exception e) {
                        Log.L.severe("Content-Disposition: could not decode filename: " + header);
                        filename = null;
                        return filename;
                    }
                }
            } else if (new Regex(contentdisposition, "=\\?.*?\\?.*?\\?.*?\\?=").matches()) {
                /*
                 * Codierung Encoded Words, TODO: Q-Encoding und mehrfach
                 * tokens, aber noch nicht in freier Wildbahn gesehen
                 */
                final String tokens[][] = new Regex(contentdisposition, "=\\?(.*?)\\?(.*?)\\?(.*?)\\?=").getMatches();
                if (tokens.length == 1 && tokens[0].length == 3 && tokens[0][1].trim().equalsIgnoreCase("B")) {
                    /* Base64 Encoded */
                    try {
                        filename = URLDecoder.decode(new String(Base64.decode(tokens[0][2].trim()), tokens[0][0].trim()), tokens[0][0].trim());
                    } catch (final Exception e) {
                        Log.L.severe("Content-Disposition: could not decode filename: " + header);
                        filename = null;
                        return filename;
                    }
                }
            } else if (new Regex(contentdisposition, "=\\?.*?\\?.*?\\?=").matches()) {
                /* Unicode Format wie es 4Shared nutzt */
                final String tokens[][] = new Regex(contentdisposition, "=\\?(.*?)\\?(.*?)\\?=").getMatches();
                if (tokens.length == 1 && tokens[0].length == 2) {
                    try {
                        contentdisposition = new String(tokens[0][1].trim().getBytes("ISO-8859-1"), tokens[0][0].trim());
                        continue;
                    } catch (final Exception e) {
                        Log.L.severe("Content-Disposition: could not decode filename: " + header);
                        filename = null;
                        return filename;
                    }
                }
            } else {
                /* ohne Codierung */
                filename = new Regex(contentdisposition, "filename.*?=[ ]*\"(.+)\"").getMatch(0);
                if (filename == null) {
                    filename = new Regex(contentdisposition, "filename.*?=[ ]*'(.+)'").getMatch(0);
                }
                if (filename == null) {
                    header = header.replaceAll("=", "=\"") + "\"";
                    header = header.replaceAll(";\"", "\"");
                    contentdisposition = header;
                }
            }
            if (filename != null) {
                break;
            }
        }
        if (filename != null) {
            filename = filename.trim();
            if (filename.startsWith("\"")) {
                Log.L.info("Using Workaround for broken filename header!");
                filename = filename.substring(1);
            }
        }
        if (filename == null) {
            Log.L.severe("Content-Disposition: could not parse header: " + orgheader);
        }
        return filename;
    }

    public static ByteBuffer readheader(final InputStream in, final boolean readSingleLine) throws IOException {
        ByteBuffer bigbuffer = ByteBuffer.wrap(new byte[4096]);
        final byte[] minibuffer = new byte[1];
        int position;
        int c;
        boolean complete = false;
        while ((c = in.read(minibuffer)) >= 0) {
            if (bigbuffer.remaining() < 1) {
                final ByteBuffer newbuffer = ByteBuffer.wrap(new byte[bigbuffer.capacity() * 2]);
                bigbuffer.flip();
                newbuffer.put(bigbuffer);
                bigbuffer = newbuffer;
            }
            if (c > 0) {
                bigbuffer.put(minibuffer);
            }
            if (readSingleLine) {
                if (bigbuffer.position() >= 1) {
                    /*
                     * \n only line termination, for fucking buggy non rfc
                     * servers
                     */
                    position = bigbuffer.position();
                    if (bigbuffer.get(position - 1) == HTTPConnectionUtils.N) {
                        break;
                    }
                }
                if (bigbuffer.position() >= 2) {
                    /* \r\n, correct line termination */
                    position = bigbuffer.position();
                    if (bigbuffer.get(position - 2) == HTTPConnectionUtils.R && bigbuffer.get(position - 1) == HTTPConnectionUtils.N) {
                        break;
                    }
                }
            } else {
                if (bigbuffer.position() >= 4) {
                    /* RNRN for header<->content divider */
                    position = bigbuffer.position();
                    complete = bigbuffer.get(position - 4) == HTTPConnectionUtils.R;
                    complete &= bigbuffer.get(position - 3) == HTTPConnectionUtils.N;
                    complete &= bigbuffer.get(position - 2) == HTTPConnectionUtils.R;
                    complete &= bigbuffer.get(position - 1) == HTTPConnectionUtils.N;
                    if (complete) {
                        break;
                    }
                }
            }
        }
        bigbuffer.flip();
        return bigbuffer;
    }
}
