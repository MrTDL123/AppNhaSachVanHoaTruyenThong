package com.webbansach.app.ui.order_tien;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.webbansach.app.core.network.ApiCallback;
import com.webbansach.app.core.network.ApiClient;
import com.webbansach.app.core.utils.CurrencyUtils;
import com.webbansach.app.databinding.ActivityMh07CartBinding;
import com.webbansach.app.models.CartDto;
import com.webbansach.app.models.CartItemDto;
import com.webbansach.app.models.UpdateCartItemQuantityDto;
import com.webbansach.app.ui.order_tien.adapter.CartAdapter;

/**
 * MH07 - Giỏ hàng
 * Phụ trách: Nguyễn Việt Tiến
 *
 * Chức năng:
 * - Tải giỏ hàng (GET /api/cart)
 * - Hiển thị danh sách sản phẩm trong giỏ (RecyclerView)
 * - Tăng/Giảm số lượng (PUT /api/cart/items/{productId})
 * - Xóa sản phẩm (DELETE /api/cart/items/{productId})
 * - Hiển thị tổng tiền
 * - Nút "Thanh toán" -> CheckoutActivity (MH08)
 */

public class CartActivity extends AppCompatActivity implements CartAdapter.OnCartItemActionListener {
    private ActivityMh07CartBinding binding;
    private CartAdapter adapter;
    private CartDto currentCart; // Lưu giữ giỏ hàng hiện tại trong bộ nhớ
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMh07CartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupRecyclerView();
        setupEvents();
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Mỗi lần quay lại màn hình giỏ hàng thì nạp lại dữ liệu mới nhất
        loadCartData();
    }
    // 1. Cấu hình danh sách hiển thị
    private void setupRecyclerView() {
        adapter = new CartAdapter(this);
        binding.rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCartItems.setAdapter(adapter);
    }
    // 2. Bắt sự kiện bấm nút Thanh toán
    private void setupEvents() {
        binding.btnCheckout.setOnClickListener(v -> {
            if (currentCart == null || currentCart.items == null || currentCart.items.isEmpty()) {
                Toast.makeText(this, "Giỏ hàng của bạn đang trống!", Toast.LENGTH_SHORT).show();
                return;
            }
            // Chuyển sang Màn hình MH08 - Thanh toán
            Intent intent = new Intent(CartActivity.this, CheckoutActivity.class);
            startActivity(intent);
        });
    }
    // 3. Gọi API lấy toàn bộ dữ liệu giỏ hàng (GET /api/cart)
//    private void loadCartData() {
//        showLoading(true);
//        ApiClient.getApi(this).getCart().enqueue(new ApiCallback<CartDto>() {
//            @Override
//            public void onSuccess(CartDto data, String message) {
//                showLoading(false);
//                currentCart = data;
//                updateUI(data);
//            }
//            @Override
//            public void onError(String errorMessage) {
//                showLoading(false);
//                Toast.makeText(CartActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
    private void loadCartData() {
        showLoading(true);

        // In ra Logcat để biết đã bắt đầu gửi request
        android.util.Log.d("TEST_CART", "Đang gửi request GET /api/cart lên Server...");

        ApiClient.getApi(this).getCart().enqueue(new ApiCallback<CartDto>() {
            @Override
            public void onSuccess(CartDto data, String message) {
                showLoading(false);

                // 1. Kiểm tra API trả về thành công:
                android.util.Log.d("TEST_CART", "API thành công! Số lượng món sách nhận được: " + (data.items != null ? data.items.size() : 0));
                android.util.Log.d("TEST_CART", "Tổng tiền backend tính: " + data.totalAmount);

                currentCart = data;

                // 2. Đổ lên giao diện:
                updateUI(data);

                Toast.makeText(CartActivity.this, "Đã tải dữ liệu giỏ hàng thành công!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);

                // Nếu có lỗi, in ra rõ nguyên nhân
                android.util.Log.e("TEST_CART", "Lỗi gọi API: " + errorMessage);
                Toast.makeText(CartActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
    // 4. Đổ dữ liệu lên màn hình
    private void updateUI(CartDto cart) {
        if (cart == null || cart.items == null || cart.items.isEmpty()) {
            binding.tvEmptyCart.setVisibility(View.VISIBLE);
            binding.rvCartItems.setVisibility(View.GONE);
            binding.tvTotalAmount.setText("0 đ");
            adapter.submitList(null);
        } else {
            binding.tvEmptyCart.setVisibility(View.GONE);
            binding.rvCartItems.setVisibility(View.VISIBLE);
            // Cập nhật tổng tiền đỏ nổi bật
            binding.tvTotalAmount.setText(CurrencyUtils.formatVnd(cart.totalAmount));
            // Đưa danh sách sách vào Adapter
            adapter.submitList(cart.items);
        }
    }
    // ================= XỬ LÝ SỰ KIỆN TỪ ADAPTER BẮN VỀ =================
    // Xử lý khi bấm nút Tăng (+)
    @Override
    public void onIncreaseQuantity(CartItemDto item) {
        int newQuantity = item.quantity + 1;
        updateItemQuantity(item.productId, newQuantity);
    }
    // Xử lý khi bấm nút Giảm (-)
    @Override
    public void onDecreaseQuantity(CartItemDto item) {
        int newQuantity = item.quantity - 1;
        // Nếu số lượng về 0, backend sẽ tự xóa sản phẩm khỏi giỏ
        updateItemQuantity(item.productId, newQuantity);
    }
    // Xử lý khi bấm Icon Thùng rác (Xóa)
    @Override
    public void onRemoveItem(CartItemDto item) {
        showLoading(true);
        ApiClient.getApi(this).removeCartItem(item.productId).enqueue(new ApiCallback<CartDto>() {
            @Override
            public void onSuccess(CartDto data, String message) {
                showLoading(false);
                currentCart = data;
                updateUI(data);
                Toast.makeText(CartActivity.this, "Đã xóa sản phẩm khỏi giỏ hàng", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                Toast.makeText(CartActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
    // Hàm gọi API cập nhật số lượng RESTful (PUT /api/cart/items/{productId})
    private void updateItemQuantity(int productId, int quantity) {
        showLoading(true);
        UpdateCartItemQuantityDto dto = new UpdateCartItemQuantityDto(quantity);
        ApiClient.getApi(this).updateCartItemQuantity(productId, dto).enqueue(new ApiCallback<CartDto>() {
            @Override
            public void onSuccess(CartDto data, String message) {
                showLoading(false);
                currentCart = data; // Cập nhật lại giỏ hàng trong bộ nhớ
                updateUI(data);     // Vẽ lại giao diện với giá tiền mới
            }
            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                Toast.makeText(CartActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
    // Tiện ích ẩn/hiện thanh xoay chờ loading
    private void showLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
