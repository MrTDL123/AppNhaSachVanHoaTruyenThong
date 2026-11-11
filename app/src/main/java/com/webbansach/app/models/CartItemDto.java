package com.webbansach.app.models;

import com.google.gson.annotations.SerializedName;

/**
 * DTO đại diện cho một món sách trong giỏ hàng.
 * Tương thích 100% với WebApp.Shared (C# backend: UnitPrice, FinalUnitPrice, SubTotal)
 * và chấp nhận cả alternate names (price, finalPrice, subtotal) theo tài liệu Android.
 */
public class CartItemDto {
    public int productId;
    public String title;
    public String authorName;
    public String mainImageUrl;

    @SerializedName(value = "unitPrice", alternate = {"price"})
    public double unitPrice;

    @SerializedName(value = "finalUnitPrice", alternate = {"finalPrice"})
    public double finalUnitPrice;

    public double discountPercent;
    public int quantity;
    public int stockQuantity;

    @SerializedName(value = "subTotal", alternate = {"subtotal"})
    public double subTotal;

    // Hàm lấy giá bán hiển thị (ưu tiên giá sau giảm giá nếu có)
    public double getDisplayPrice() {
        return finalUnitPrice > 0 ? finalUnitPrice : unitPrice;
    }
}
