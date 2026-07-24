package com.mbhoni_creative.admindto;

public class PageMeta {

    private final int number;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean hasPrevious;
    private final boolean hasNext;

    public PageMeta(int number, int size, long totalElements, int totalPages) {
        this.number = number;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.hasPrevious = number > 0;
        this.hasNext = number + 1 < totalPages;
    }

    public int getNumber() {
        return number;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public boolean isHasNext() {
        return hasNext;
    }
}
