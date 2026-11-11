package com.webbansach.app.ui.order_tien;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.webbansach.app.core.utils.Constants;
import com.webbansach.app.databinding.ActivityMh09SuccessBinding;
import com.webbansach.app.ui.catalog_hieu.HomeActivity;

/**
 * MH09 - Đặt hàng thành công
 * Phụ trách: Nguyễn Việt Tiến
 *
 * Chức năng:
 * - Nhận EXTRA_ORDER_ID và EXTRA_ORDER_CODE từ CheckoutActivity
 * - Hiển thị thông tin đặt hàng thành công (mã đơn, thông báo)
 * - Nút "Xem đơn hàng" -> OrderHistoryActivity (MH12)
 * - Nút "Tiếp tục mua sắm" -> HomeActivity (MH04)
 */
public class OrderSuccessActivity extends AppCompatActivity {

    private ActivityMh09SuccessBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMh09SuccessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        int orderId = getIntent().getIntExtra(Constants.EXTRA_ORDER_ID, -1);
        String orderCode = getIntent().getStringExtra(Constants.EXTRA_ORDER_CODE);

        if (orderCode != null && !orderCode.isEmpty()) {
            binding.tvOrderCode.setText("Mã đơn hàng: #" + orderCode);
        } else if (orderId > 0) {
            binding.tvOrderCode.setText("Mã đơn hàng: #" + orderId);
        } else {
            binding.tvOrderCode.setText("Đặt hàng thành công!");
        }

        // Nút "Xem đơn hàng" -> Mở Lịch sử đơn hàng (MH12)
        binding.btnViewOrder.setOnClickListener(v -> {
            Intent intent = new Intent(OrderSuccessActivity.this, OrderHistoryActivity.class);
            intent.putExtra(Constants.EXTRA_ORDER_ID, orderId);
            startActivity(intent);
            finish();
        });

        // Nút "Tiếp tục mua sắm" -> Quay về Trang chủ (MH04)
        binding.btnContinueShopping.setOnClickListener(v -> navigateToHome());

        // Bắt sự kiện nút quay lại (Back) để điều hướng về Trang chủ tránh back lại Checkout
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToHome();
            }
        });
    }

    private void navigateToHome() {
        Intent intent = new Intent(OrderSuccessActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
