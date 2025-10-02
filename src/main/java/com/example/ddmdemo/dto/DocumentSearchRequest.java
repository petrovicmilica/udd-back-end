package com.example.ddmdemo.dto;

import java.util.List;

public record DocumentSearchRequest(
        List<String> searchKeywords,
        String booleanQuery
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private List<String> searchKeywords;
        private String booleanQuery;

        public Builder withSearchKeywords(List<String> searchKeywords) {
            this.searchKeywords = searchKeywords;
            return this;
        }

        public Builder withBooleanQuery(String booleanQuery) {
            this.booleanQuery = booleanQuery;
            return this;
        }

        public DocumentSearchRequest build() { return new DocumentSearchRequest(searchKeywords, booleanQuery); }
    }
}
