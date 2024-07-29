package com.jx.blockchain.service.tron.vo;

public class TronWallet {
    private String address;
    private String publicKey;
    private String privateKey;
    private String mnemonic;

    public TronWallet() {
    }

    public TronWallet(String address, String publicKey, String privateKey, String mnemonic) {
        this.address = address;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.mnemonic = mnemonic;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }


    public String getMnemonic() {
        return mnemonic;
    }

    public void setMnemonic(String mnemonic) {
        this.mnemonic = mnemonic;
    }

    @Override
    public String toString() {
        return "TronWallet{" +
                "address='" + address + '\'' +
                ", publicKey='" + publicKey + '\'' +
                ", privateKey='" + privateKey + '\'' +
                ", mnemonic='" + mnemonic + '\'' +
                '}';
    }
}
