package com.jx.blockchain.service.bitcoin.vo;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

public class UtxoData {
    @JSONField(name = "address")
    private String address;

    @JSONField(name = "canTransferAmount")
    private String canTransferAmount;

    @JSONField(name = "lockAmount")
    private String lockAmount;

    @JSONField(name = "utxoType")
    private int utxoType;

    @JSONField(name = "totalUtxoNum")
    private int totalUtxoNum;

    @JSONField(name = "utxoList")
    private List<Utxo> utxoList;

    // Getters and setters
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCanTransferAmount() {
        return canTransferAmount;
    }

    public void setCanTransferAmount(String canTransferAmount) {
        this.canTransferAmount = canTransferAmount;
    }

    public String getLockAmount() {
        return lockAmount;
    }

    public void setLockAmount(String lockAmount) {
        this.lockAmount = lockAmount;
    }

    public int getUtxoType() {
        return utxoType;
    }

    public void setUtxoType(int utxoType) {
        this.utxoType = utxoType;
    }

    public int getTotalUtxoNum() {
        return totalUtxoNum;
    }

    public void setTotalUtxoNum(int totalUtxoNum) {
        this.totalUtxoNum = totalUtxoNum;
    }

    public List<Utxo> getUtxoList() {
        return utxoList;
    }

    public void setUtxoList(List<Utxo> utxoList) {
        this.utxoList = utxoList;
    }

    @Override
    public String toString() {
        return "UtxoData{" +
                "address='" + address + '\'' +
                ", canTransferAmount='" + canTransferAmount + '\'' +
                ", lockAmount='" + lockAmount + '\'' +
                ", utxoType=" + utxoType +
                ", totalUtxoNum=" + totalUtxoNum +
                ", utxoList=" + utxoList +
                '}';
    }


    public static class Utxo {
        @JSONField(name = "txHash")
        private String txHash;

        @JSONField(name = "vout")
        private int vout;

        @JSONField(name = "coinAmount")
        private int coinAmount;

        @JSONField(name = "confirmations")
        private int confirmations;

        @JSONField(name = "utxoType")
        private int utxoType;

        @JSONField(name = "status")
        private int status;

        @JSONField(name = "utxoStatusType")
        private int utxoStatusType;

        @JSONField(name = "hasNft")
        private boolean hasNft;

        @JSONField(name = "hasCheckNftExist")
        private boolean hasCheckNftExist;

        @JSONField(name = "nftLocaltionVOs")
        private List<NftLocation> nftLocaltionVOs;

        @JSONField(name = "key")
        private String key;

        @JSONField(name = "spending")
        private boolean spending;

        @JSONField(name = "dummy")
        private boolean dummy;

        // Getters and setters
        public String getTxHash() {
            return txHash;
        }

        public void setTxHash(String txHash) {
            this.txHash = txHash;
        }

        public int getVout() {
            return vout;
        }

        public void setVout(int vout) {
            this.vout = vout;
        }

        public int getCoinAmount() {
            return coinAmount;
        }

        public void setCoinAmount(int coinAmount) {
            this.coinAmount = coinAmount;
        }

        public int getConfirmations() {
            return confirmations;
        }

        public void setConfirmations(int confirmations) {
            this.confirmations = confirmations;
        }

        public int getUtxoType() {
            return utxoType;
        }

        public void setUtxoType(int utxoType) {
            this.utxoType = utxoType;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public int getUtxoStatusType() {
            return utxoStatusType;
        }

        public void setUtxoStatusType(int utxoStatusType) {
            this.utxoStatusType = utxoStatusType;
        }

        public boolean isHasNft() {
            return hasNft;
        }

        public void setHasNft(boolean hasNft) {
            this.hasNft = hasNft;
        }

        public boolean isHasCheckNftExist() {
            return hasCheckNftExist;
        }

        public void setHasCheckNftExist(boolean hasCheckNftExist) {
            this.hasCheckNftExist = hasCheckNftExist;
        }

        public List<NftLocation> getNftLocaltionVOs() {
            return nftLocaltionVOs;
        }

        public void setNftLocaltionVOs(List<NftLocation> nftLocaltionVOs) {
            this.nftLocaltionVOs = nftLocaltionVOs;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public boolean isSpending() {
            return spending;
        }

        public void setSpending(boolean spending) {
            this.spending = spending;
        }

        public boolean isDummy() {
            return dummy;
        }

        public void setDummy(boolean dummy) {
            this.dummy = dummy;
        }

        @Override
        public String toString() {
            return "Utxo{" +
                    "txHash='" + txHash + '\'' +
                    ", vout=" + vout +
                    ", coinAmount=" + coinAmount +
                    ", confirmations=" + confirmations +
                    ", utxoType=" + utxoType +
                    ", status=" + status +
                    ", utxoStatusType=" + utxoStatusType +
                    ", hasNft=" + hasNft +
                    ", hasCheckNftExist=" + hasCheckNftExist +
                    ", nftLocaltionVOs=" + nftLocaltionVOs +
                    ", key='" + key + '\'' +
                    ", spending=" + spending +
                    ", dummy=" + dummy +
                    '}';
        }
    }

    public static class NftLocation {
        @JSONField(name = "brc20sNft")
        private boolean brc20sNft;

        @JSONField(name = "brc20Nft")
        private boolean brc20Nft;

        // Getters and setters
        public boolean isBrc20sNft() {
            return brc20sNft;
        }

        public void setBrc20sNft(boolean brc20sNft) {
            this.brc20sNft = brc20sNft;
        }

        public boolean isBrc20Nft() {
            return brc20Nft;
        }

        public void setBrc20Nft(boolean brc20Nft) {
            this.brc20Nft = brc20Nft;
        }

        @Override
        public String toString() {
            return "NftLocation{" +
                    "brc20sNft=" + brc20sNft +
                    ", brc20Nft=" + brc20Nft +
                    '}';
        }
    }
}