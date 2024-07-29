package com.jx.blockchain.service.tron.vo;


public class TronTransactionFee {
    /**
     * 交易总计燃烧的trx(sun)
     */
    private long fee;

    /**
     * 用户质押的带宽使用量
     */
    private long netUsage;

    /**
     * 燃烧获取带宽的trx(sun)
     */
    private long netFee;

    /**
     * 用户质押的能量使用量
     */
    private long energyUsage;

    /**
     * 燃烧获取能量的trx(sun)
     */
    private long energyFee;

    /**
     * 交易总计消耗能量
     */
    private long energyUsageTotal;

    /**
     * 本次合约创建者提供的Energy数量
     */
    private long originEnergyUsage;

    public TronTransactionFee() {
    }

    public TronTransactionFee(long fee, long netUsage, long netFee, long energyUsage, long energyFee, long energyUsageTotal, long originEnergyUsage) {
        this.fee = fee;
        this.netUsage = netUsage;
        this.netFee = netFee;
        this.energyUsage = energyUsage;
        this.energyFee = energyFee;
        this.energyUsageTotal = energyUsageTotal;
        this.originEnergyUsage = originEnergyUsage;
    }

    public long getFee() {
        return fee;
    }

    public void setFee(long fee) {
        this.fee = fee;
    }

    public long getNetUsage() {
        return netUsage;
    }

    public void setNetUsage(long netUsage) {
        this.netUsage = netUsage;
    }

    public long getNetFee() {
        return netFee;
    }

    public void setNetFee(long netFee) {
        this.netFee = netFee;
    }

    public long getEnergyUsage() {
        return energyUsage;
    }

    public void setEnergyUsage(long energyUsage) {
        this.energyUsage = energyUsage;
    }

    public long getEnergyFee() {
        return energyFee;
    }

    public void setEnergyFee(long energyFee) {
        this.energyFee = energyFee;
    }

    public long getEnergyUsageTotal() {
        return energyUsageTotal;
    }

    public void setEnergyUsageTotal(long energyUsageTotal) {
        this.energyUsageTotal = energyUsageTotal;
    }

    public long getOriginEnergyUsage() {
        return originEnergyUsage;
    }

    public void setOriginEnergyUsage(long originEnergyUsage) {
        this.originEnergyUsage = originEnergyUsage;
    }

    @Override
    public String toString() {
        return "TransactionFee{" +
                "fee=" + fee +
                ", netUsage=" + netUsage +
                ", netFee=" + netFee +
                ", energyUsage=" + energyUsage +
                ", energyFee=" + energyFee +
                ", energyUsageTotal=" + energyUsageTotal +
                ", originEnergyUsage=" + originEnergyUsage +
                '}';
    }
}
