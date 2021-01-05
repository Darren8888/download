package com.darren.download.file;

public class FileInfo {
    private long length;
    private boolean isAcceptRanges;

    public FileInfo(long length, boolean isAcceptRanges) {
        this.length = length;
        this.isAcceptRanges = isAcceptRanges;
    }

    public void setLength(long length) {
        this.length = length;
    }
    public long getLength() {
        return this.length;
    }

    public void setAcceptRanges(boolean isAcceptRanges) {
        this.isAcceptRanges = isAcceptRanges;
    }
    public boolean getAcceptRanges() {
        return this.isAcceptRanges;
    }
}
