package com.jx.blockchain.vo;

import com.alibaba.fastjson.JSON;

import java.io.Serializable;

public record JxResponse(int code, Object data) implements Serializable {

    public static JxResponse error(int code, String msg) {
        return new JxResponse(code, msg);
    }

    public static JxResponse success(Object data) {
        return new JxResponse(0, data);
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
