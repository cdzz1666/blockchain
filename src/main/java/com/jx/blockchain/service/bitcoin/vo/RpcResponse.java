package com.jx.blockchain.service.bitcoin.vo;

public class RpcResponse {
    private String result;
    private String error;
    private String id;

    public RpcResponse() {
    }

    public RpcResponse(String result, String error, String id) {
        this.result = result;
        this.error = error;
        this.id = id;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "CurlResponse{" +
                "result='" + result + '\'' +
                ", error='" + error + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
