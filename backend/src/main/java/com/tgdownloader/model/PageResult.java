package com.tgdownloader.model;

import java.util.List;

/**
 * 简单分页封装，替代 MyBatisFlex 的 Page
 */
public class PageResult<T> {
    private List<T> records;
    private long totalRow;
    private int pageNumber;
    private int pageSize;

    public PageResult() {}

    public PageResult(List<T> records, long totalRow, int pageNumber, int pageSize) {
        this.records = records;
        this.totalRow = totalRow;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    public static <T> PageResult<T> of(List<T> records, long totalRow, int page, int size) {
        return new PageResult<>(records, totalRow, page, size);
    }

    public List<T> getRecords() { return records; }
    public void setRecords(List<T> records) { this.records = records; }
    public long getTotalRow() { return totalRow; }
    public void setTotalRow(long totalRow) { this.totalRow = totalRow; }
    public int getPageNumber() { return pageNumber; }
    public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public long getTotal() { return totalRow; }
}