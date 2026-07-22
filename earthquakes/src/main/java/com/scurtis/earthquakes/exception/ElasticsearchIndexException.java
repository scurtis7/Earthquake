package com.scurtis.earthquakes.exception;

import lombok.Getter;

@Getter
public class ElasticsearchIndexException extends RuntimeException {

    public ElasticsearchIndexException(String message) {
        super(message);
    }

    public ElasticsearchIndexException(String message, Throwable cause) {
        super(message, cause);
    }

}
