package com.intellectualsites.web.object;

import com.intellectualsites.web.util.CookieManager;
import com.sun.istack.internal.NotNull;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * The HTTP Request Class
 *
 * This is generated when a client
 * connects to the web server, and
 * contains the information needed
 * for the server to generate a
 * proper response. This is what
 * everything is based around!
 *
 * @author Citymonstret
 */
public class Request {

    private Map<String, Object> meta;
    private final Map<String, String> headers;
    private Cookie[] cookies;
    private Query query;
    private PostRequest postRequest;
    private final Socket socket;
    private Session session;

    /**
     * The post request is basically... a POST request.
     *
     * @param postRequest The post request
     */
    public void setPostRequest(PostRequest postRequest) {
        this.postRequest = postRequest;
    }

    /**
     * The query, for example:
     * "http://localhost/query?example=this"
     */
    public static class Query {
        private final Method method;
        private final String resource;

        /**
         * The query constructor
         *
         * @param method   Request Method
         * @param resource The requested resource
         */
        public Query(Method method, String resource) {
            this.method = method;
            this.resource = resource;
        }

        /**
         * The Request method
         *
         * @return Request method
         */
        public Method getMethod() {
            return this.method;
        }

        /**
         * Get the request resource name
         *
         * @return Requested resource
         */
        public String getResource() {
            return this.resource;
        }

        /**
         * Build a logging string... for logging?
         *
         * @return compiled string
         */
        public String buildLog() {
            return "Query >\n\t\tMethod: " + method.toString() + "\n\t\tResource: " + resource;
        }
    }

    /**
     * Get the PostRequest
     *
     * @return PostRequest if exists, null if not
     */
    public PostRequest getPostRequest() {
        return this.postRequest;
    }

    /**
     * The request constructor
     *
     * @throws RuntimeException if the request doesn't contain a query
     *
     * @param request Request (from the client)
     * @param socket The socket which sent the request
     */
    public Request(final String request, final Socket socket) {
        this.socket = socket;
        String[] parts = request.split("\\|");
        this.headers = new HashMap<>();
        for (String part : parts) {
            String[] subParts = part.split(":");
            if (subParts.length < 2) {
                headers.put("query", subParts[0]);
            } else {
                headers.put(subParts[0], subParts[1]);
            }
        }
        if (!this.headers.containsKey("query")) {
            throw new RuntimeException("Couldn't find query header...");
        }
        getResourceRequest();
        this.cookies = CookieManager.getCookies(this);
        this.meta = new HashMap<>();
    }

    /**
     * Get all request cookies
     *
     * @return Request cookies
     */
    public Cookie[] getCookies() {
        return this.cookies;
    }

    /**
     * Get a request header. These
     * are sent by the client, and
     * are not to be confused with the
     * response headers.
     *
     * @param name Header Name
     * @return The header value, if the header exists. Otherwise an empty string will be returned.
     */
    public String getHeader(final String name) {
        if (this.headers.containsKey(name)) {
            return this.headers.get(name);
        }
        return "";
    }

    private void getResourceRequest() {
        String[] parts = getHeader("query").split(" ");
        if (parts.length < 3) {
            this.query = new Query(Method.GET, "/");
        } else {
            this.query = new Query(parts[0].equalsIgnoreCase("GET") ? Method.GET : Method.POST, parts[1]);
        }
    }

    /**
     * Get the built query
     *
     * @return Compiled query
     */
    public Query getQuery() {
        return this.query;
    }

    /**
     * Build a string for logging
     *
     * @return Compiled string
     */
    public String buildLog() {
        return "Request >\n\tAddress: " + socket.getRemoteSocketAddress().toString() + "\n\tUser Agent: " + getHeader("User-Agent") + "\n\tRequest String: " + getHeader("query") + "\n\tHost: " + getHeader("Host") + "\n\tQuery: " + this.query.buildLog() + (postRequest != null ? "\n\tPost: " + postRequest.buildLog() : "");
    }

    /**
     * Add a meta value, which can
     * be used to share an object
     * throughout the lifespan of
     * the request.
     *
     * @see #getMeta(String) To get the value
     *
     * @param name Key (which will be used to get the meta value)
     * @param var Value (Any object will do)
     */
    public void addMeta(String name, Object var) {
        meta.put(name, var);
    }

    /**
     * Get a meta value
     *
     * @see #addMeta(String, Object) To set a meta value
     *
     * @param name The key
     * @return Meta value if exists, else null
     */
    public Object getMeta(String name) {
        if (!meta.containsKey(name)) {
            return null;
        }
        return meta.get(name);
    }

    /**
     * Set the internal session
     *
     * @param session Session
     */
    public void setSession(@NotNull final Session session) {
        this.session = session;
    }

    /**
     * Get the internal session
     *
     * @return true|null
     */
    public Session getSession() {
        return this.session;
    }
}
