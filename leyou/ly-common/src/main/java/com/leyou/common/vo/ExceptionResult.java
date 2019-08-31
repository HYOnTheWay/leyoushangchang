package com.leyou.common.vo;

import com.leyou.common.enums.ExceptionEnum;
import lombok.Data;

@Data
public class ExceptionResult {
    private int status;
    private String message;
    private Long timetamp;


    public ExceptionResult(ExceptionEnum e) {
        this.status = e.getCode();
        this.message = e.getMsg();
        this.timetamp = System.currentTimeMillis();
    }
}
