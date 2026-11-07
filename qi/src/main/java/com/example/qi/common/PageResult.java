package com.example.qi.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 分页返回体（7.12：列表页统一分页）。
 */
@Data
public class PageResult<T> {

    private List<T> list;
    private long total;
    private long pageNum;
    private long pageSize;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> r = new PageResult<>();
        r.list = page.getRecords();
        r.total = page.getTotal();
        r.pageNum = page.getCurrent();
        r.pageSize = page.getSize();
        return r;
    }
}
