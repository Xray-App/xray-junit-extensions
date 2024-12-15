package app.getxray.xray.junit.customjunitxml;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class XrayURI {

    protected static final String QUERY_ID = "id";
    protected static final String QUERY_KEY = "key";
    protected static final String QUERY_SUMMARY = "summary";
    protected static final String QUERY_DESCRIPTION = "description";

    private XrayURI() {
    }

    public static Builder builder() {
        return new Builder();
    }

    protected static Optional<String> readQueryValue(URI uri, String key) {
        String query = uri.getQuery();
        if (query == null) {
            return Optional.empty();
        }

        return Arrays.stream(query.split("&"))
                .filter(p -> p.startsWith(key + "="))
                .map(p -> p.substring(key.length() + 1))
                .map(value -> {
                    try {
                        return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
                    } catch (UnsupportedEncodingException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .findFirst();
    }

    public static class Builder {
        private String id;
        private String key;
        private String summary;
        private String description;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public URI build(Class<?> testClass) {
            List<String> query = new ArrayList<>();
            try {
                if (id != null) {
                    query.add(QUERY_ID + "=" + URLEncoder.encode(id, StandardCharsets.UTF_8.name()));
                }
                if (key != null) {
                    query.add(QUERY_KEY + "=" + URLEncoder.encode(key, StandardCharsets.UTF_8.name()));
                }
                if (summary != null) {
                    query.add(QUERY_SUMMARY + "=" + URLEncoder.encode(summary, StandardCharsets.UTF_8.name()));
                }
                if (description != null) {
                    query.add(QUERY_DESCRIPTION + "=" + URLEncoder.encode(description, StandardCharsets.UTF_8.name()));
                }
            } catch (UnsupportedEncodingException e) {
                throw new UncheckedIOException(e);
            }

            return URI.create("xray://" + testClass.getName() + "?" + String.join("&", query));
        }
    }

}
