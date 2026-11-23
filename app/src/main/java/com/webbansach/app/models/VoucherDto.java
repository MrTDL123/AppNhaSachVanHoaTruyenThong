package com.webbansach.app.models;

import com.google.gson.annotations.SerializedName;

public class VoucherDto {
    @SerializedName("voucherId")
    public int voucherId;

    @SerializedName("code")
    public String code;

    @SerializedName("description")
    public String description;

    @SerializedName("discountAmount")
    public double discountAmount;

    @SerializedName("discountPercent")
    public double discountPercent;

    @SerializedName("minOrderAmount")
    public double minOrderAmount;

    @SerializedName("remainingUsage")
    public int remainingUsage;

    @SerializedName("expiredAt")
    public String expiredAt;

    @SerializedName("isActive")
    public boolean isActive;
}