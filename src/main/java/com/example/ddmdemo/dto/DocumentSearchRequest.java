package com.example.ddmdemo.dto;

import java.util.List;

public record DocumentSearchRequest(
        List<String> searchKeywords
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private List<String> searchKeywords;

        public Builder withSearchKeywords(List<String> searchKeywords) {
            this.searchKeywords = searchKeywords;
            return this;
        }

        public DocumentSearchRequest build() { return new DocumentSearchRequest(searchKeywords); }
    }
}
