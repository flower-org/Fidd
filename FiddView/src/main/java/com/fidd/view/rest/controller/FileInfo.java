package com.fidd.view.rest.controller;

import javax.annotation.Nullable;
import java.io.InputStream;

public class FileInfo {
    protected @Nullable final InputStream inputStream;
    protected @Nullable String acceptRanges;
    protected @Nullable String contentLength;
    protected @Nullable String contentDisposition;
    protected @Nullable String contentRange;
    protected @Nullable String contentType;

    public FileInfo(@Nullable InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public @Nullable InputStream getInputStream() {
        return inputStream;
    }

    public @Nullable String getAcceptRanges() { return acceptRanges; }

    public void setAcceptRanges(String acceptRanges) {
        this.acceptRanges = acceptRanges;
    }

    public @Nullable String getContentLength() {
        return contentLength;
    }

    public void setContentLength(String contentLength) {
        this.contentLength = contentLength;
    }

    public @Nullable String getContentDisposition() {
        return contentDisposition;
    }

    public void setContentDisposition(String contentDisposition) {
        this.contentDisposition = contentDisposition;
    }

    public @Nullable String getContentRange() {
        return contentRange;
    }

    public void setContentRange(String contentRange) {
        this.contentRange = contentRange;
    }

    public @Nullable String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
