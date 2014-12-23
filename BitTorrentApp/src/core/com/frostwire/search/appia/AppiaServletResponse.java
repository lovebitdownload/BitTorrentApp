package com.frostwire.search.appia;

import java.util.List;
import java.util.Map;

/**
 * Modeled after the client's search response object, but no need to implement any interfaces (at least for now)
 * @author gubatron
 *
 */
public class AppiaServletResponse {
    public Map<String,List<AppiaServletResponseItem>> results;
}