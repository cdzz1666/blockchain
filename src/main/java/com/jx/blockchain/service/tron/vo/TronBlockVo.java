package com.jx.blockchain.service.tron.vo;

import java.io.Serializable;

public class TronBlockVo implements Serializable {
    private long blockNumber;
    private String blockId;

    public TronBlockVo() {
    }

    public long getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(long blockNumber) {
        this.blockNumber = blockNumber;
    }

    public String getBlockId() {
        return blockId;
    }

    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }

    @Override
    public String toString() {
        return "TronBlockVo{" +
                "blockNumber=" + blockNumber +
                ", blockId='" + blockId + '\'' +
                '}';
    }
}
