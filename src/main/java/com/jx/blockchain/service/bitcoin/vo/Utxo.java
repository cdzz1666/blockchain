package com.jx.blockchain.service.bitcoin.vo;

import com.alibaba.fastjson.JSON;

public class Utxo {
    private String txid;
    private int vout;
    private String coinAmount;
    private String scriptpubkeyAddress;
    private String scriptpubkey;
    private String scriptpubkeyAsm;
    private String scriptpubkeyType;

    public Utxo() {
    }

    public String getTxid() {
        return txid;
    }

    public void setTxid(String txid) {
        this.txid = txid;
    }

    public int getVout() {
        return vout;
    }

    public void setVout(int vout) {
        this.vout = vout;
    }

    public String getCoinAmount() {
        return coinAmount;
    }

    public void setCoinAmount(String coinAmount) {
        this.coinAmount = coinAmount;
    }

    public String getScriptpubkeyAddress() {
        return scriptpubkeyAddress;
    }

    public void setScriptpubkeyAddress(String scriptpubkeyAddress) {
        this.scriptpubkeyAddress = scriptpubkeyAddress;
    }

    public String getScriptpubkey() {
        return scriptpubkey;
    }

    public void setScriptpubkey(String scriptpubkey) {
        this.scriptpubkey = scriptpubkey;
    }

    public String getScriptpubkeyAsm() {
        return scriptpubkeyAsm;
    }

    public void setScriptpubkeyAsm(String scriptpubkeyAsm) {
        this.scriptpubkeyAsm = scriptpubkeyAsm;
    }

    public String getScriptpubkeyType() {
        return scriptpubkeyType;
    }

    public void setScriptpubkeyType(String scriptpubkeyType) {
        this.scriptpubkeyType = scriptpubkeyType;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
