package com.jx.blockchain.service.tron.vo;

import java.math.BigDecimal;


public class AccountResourceVo {
    private BigDecimal freeNetLimit;
    private BigDecimal netLimit;
    private BigDecimal totalNetLimit;
    private BigDecimal totalNetWeight;
    private BigDecimal netUsed;
    private BigDecimal freeNetUsed;
    private BigDecimal tronPowerUsed;
    private BigDecimal tronPowerLimit;
    private BigDecimal energyLimit;
    private BigDecimal totalEnergyLimit;
    private BigDecimal totalEnergyWeight;
    private BigDecimal energyUsed;

    public BigDecimal getFreeNetLimit() {
        return freeNetLimit;
    }

    public void setFreeNetLimit(BigDecimal freeNetLimit) {
        this.freeNetLimit = freeNetLimit;
    }

    public BigDecimal getNetLimit() {
        return netLimit;
    }

    public void setNetLimit(BigDecimal netLimit) {
        this.netLimit = netLimit;
    }

    public BigDecimal getTotalNetLimit() {
        return totalNetLimit;
    }

    public void setTotalNetLimit(BigDecimal totalNetLimit) {
        this.totalNetLimit = totalNetLimit;
    }

    public BigDecimal getTotalNetWeight() {
        return totalNetWeight;
    }

    public void setTotalNetWeight(BigDecimal totalNetWeight) {
        this.totalNetWeight = totalNetWeight;
    }

    public BigDecimal getTronPowerUsed() {
        return tronPowerUsed;
    }

    public void setTronPowerUsed(BigDecimal tronPowerUsed) {
        this.tronPowerUsed = tronPowerUsed;
    }

    public BigDecimal getTronPowerLimit() {
        return tronPowerLimit;
    }

    public void setTronPowerLimit(BigDecimal tronPowerLimit) {
        this.tronPowerLimit = tronPowerLimit;
    }

    public BigDecimal getEnergyLimit() {
        return energyLimit;
    }

    public void setEnergyLimit(BigDecimal energyLimit) {
        this.energyLimit = energyLimit;
    }

    public BigDecimal getTotalEnergyLimit() {
        return totalEnergyLimit;
    }

    public void setTotalEnergyLimit(BigDecimal totalEnergyLimit) {
        this.totalEnergyLimit = totalEnergyLimit;
    }

    public BigDecimal getTotalEnergyWeight() {
        return totalEnergyWeight;
    }

    public void setTotalEnergyWeight(BigDecimal totalEnergyWeight) {
        this.totalEnergyWeight = totalEnergyWeight;
    }

    public BigDecimal getNetUsed() {
        return netUsed;
    }

    public void setNetUsed(BigDecimal netUsed) {
        this.netUsed = netUsed;
    }

    public BigDecimal getEnergyUsed() {
        return energyUsed;
    }

    public void setEnergyUsed(BigDecimal energyUsed) {
        this.energyUsed = energyUsed;
    }

    public BigDecimal getFreeNetUsed() {
        return freeNetUsed;
    }

    public void setFreeNetUsed(BigDecimal freeNetUsed) {
        this.freeNetUsed = freeNetUsed;
    }
}
