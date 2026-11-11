package com.webbansach.app.ui.order_tien;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.webbansach.app.core.network.ApiCallback;
import com.webbansach.app.core.network.ApiClient;
import com.webbansach.app.core.utils.Constants;
import com.webbansach.app.core.utils.CurrencyUtils;
import com.webbansach.app.databinding.ActivityMh12OrderHistoryBinding;
import com.webbansach.app.models.CancelOrderRequestDto;
import com.webbansach.app.models.OrderDto;
import com.webbansach.app.models.OrderItemDto;
import com.webbansach.app.models.OrderListItemDto;
import com.webbansach.app.ui.auth_long.ReturnOrderActivity;
import com.webbansach.app.ui.catalog_hieu.WriteReviewActivity;
import com.webbansach.app.ui.order_tien.adapter.OrderAdapter;

import java.util.List;

/**
 * MH12 - Lịch sử & Chi tiết đơn hàng
 * Phụ trách: Nguyễn Việt Tiến
 *
 * Chức năng:
 * - Danh sách đơn hàng (GET /api/orders/my-orders?status=)
 * - Lọc theo tab trạng thái: Tất cả, Chờ xử lý, Đang xử lý, Đang giao, Hoàn thành, Đã hủy, Đã trả
 * - Xem chi tiết đơn (GET /api/orders/{id})
 * - Hủy đơn (PUT /api/orders/{id}/cancel) - chỉ khi Pending
 * - Nút "Trả hàng" -> truyền EXTRA_ORDER_ID sang ReturnOrderActivity (MH13)
 * - Nút "Đánh giá" -> truyền EXTRA_PRODUCT_ID, EXTRA_ORDER_ID sang WriteReviewActivity (MH14)
 */
public class OrderHistoryActivity extends AppCompatActivity implements OrderAdapter.OnOrderActionListener {

    private ActivityMh12OrderHistoryBinding binding;
    private OrderAdapter adapter;
    private Integer currentFilterStatus = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMh12OrderHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupTabs();
        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders(currentFilterStatus);
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Tất cả").setTag(null));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Chờ xử lý").setTag(0));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Đang xử lý").setTag(1));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Đang giao").setTag(2));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Hoàn thành").setTag(3));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Đã hủy").setTag(4));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Đã trả").setTag(5));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentFilterStatus = (Integer) tab.getTag();
                loadOrders(currentFilterStatus);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new OrderAdapter(this);
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrders.setAdapter(adapter);
    }

    private void loadOrders(Integer status) {
        binding.progressBar.setVisibility(View.VISIBLE);
        ApiClient.getApi(this).getMyOrders(status).enqueue(new ApiCallback<List<OrderListItemDto>>() {
            @Override
            public void onSuccess(List<OrderListItemDto> data, String message) {
                binding.progressBar.setVisibility(View.GONE);
                adapter.submitList(data);
                boolean isEmpty = (data == null || data.isEmpty());
                binding.tvEmptyOrders.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(OrderHistoryActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onViewOrderDetail(OrderListItemDto item) {
        binding.progressBar.setVisibility(View.VISIBLE);
        ApiClient.getApi(this).getOrderDetail(item.orderId).enqueue(new ApiCallback<OrderDto>() {
            @Override
            public void onSuccess(OrderDto order, String message) {
                binding.progressBar.setVisibility(View.GONE);
                showOrderDetailDialog(order);
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(OrderHistoryActivity.this, "Lỗi xem chi tiết: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showOrderDetailDialog(OrderDto order) {
        if (order == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("Mã đơn hàng: #").append(order.orderCode != null ? order.orderCode : order.orderId).append("\n");
        sb.append("Ngày đặt: ").append(order.orderDate != null ? order.orderDate : "").append("\n");
        sb.append("Trạng thái: ").append(order.statusText != null ? order.statusText : "").append("\n\n");
        sb.append("Người nhận: ").append(order.receiverName != null ? order.receiverName : "").append("\n");
        sb.append("SĐT: ").append(order.receiverPhone != null ? order.receiverPhone : "").append("\n");
        sb.append("Địa chỉ: ").append(order.shippingAddress != null ? order.shippingAddress : "").append("\n\n");
        sb.append("Phương thức thanh toán: ").append(order.paymentMethodText != null ? order.paymentMethodText : "COD").append("\n\n");

        if (order.items != null && !order.items.isEmpty()) {
            sb.append("DANH SÁCH MÓN HÀNG:\n");
            for (OrderItemDto oi : order.items) {
                sb.append("- ").append(oi.title).append(" (x").append(oi.quantity).append("): ")
                        .append(CurrencyUtils.formatVnd(oi.subtotal)).append("\n");
            }
            sb.append("\n");
        }

        if (order.discountAmount > 0) {
            sb.append("Giảm giá: -").append(CurrencyUtils.formatVnd(order.discountAmount)).append("\n");
        }
        sb.append("Tổng thanh toán: ").append(CurrencyUtils.formatVnd(order.totalAmount));

        new AlertDialog.Builder(this)
                .setTitle("Chi tiết đơn hàng")
                .setMessage(sb.toString())
                .setPositiveButton("Đóng", null)
                .show();
    }

    @Override
    public void onCancelOrder(OrderListItemDto item) {
        final EditText etReason = new EditText(this);
        etReason.setHint("Nhập lý do hủy đơn (tùy chọn)");
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, pad / 2);
        container.addView(etReason);

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận hủy đơn hàng")
                .setMessage("Bạn có chắc chắn muốn hủy đơn hàng #" + (item.orderCode != null ? item.orderCode : item.orderId) + "?")
                .setView(container)
                .setPositiveButton("Hủy đơn", (dialog, which) -> {
                    String reason = etReason.getText().toString().trim();
                    if (reason.isEmpty()) reason = "Khách hàng tự hủy trên ứng dụng";

                    binding.progressBar.setVisibility(View.VISIBLE);
                    ApiClient.getApi(OrderHistoryActivity.this)
                            .cancelOrder(item.orderId, new CancelOrderRequestDto(reason))
                            .enqueue(new ApiCallback<Boolean>() {
                                @Override
                                public void onSuccess(Boolean data, String message) {
                                    binding.progressBar.setVisibility(View.GONE);
                                    Toast.makeText(OrderHistoryActivity.this, "Đã hủy đơn hàng thành công!", Toast.LENGTH_SHORT).show();
                                    loadOrders(currentFilterStatus);
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    binding.progressBar.setVisibility(View.GONE);
                                    Toast.makeText(OrderHistoryActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Không", null)
                .show();
    }

    @Override
    public void onReviewOrder(OrderListItemDto item) {
        // Mở MH14 (WriteReviewActivity của Hiếu)
        Intent intent = new Intent(this, WriteReviewActivity.class);
        intent.putExtra(Constants.EXTRA_ORDER_ID, item.orderId);
        intent.putExtra(Constants.EXTRA_PRODUCT_TITLE, item.firstProductTitle);
        startActivity(intent);
    }

    @Override
    public void onReturnOrder(OrderListItemDto item) {
        // Mở MH13 (ReturnOrderActivity của Long)
        Intent intent = new Intent(this, ReturnOrderActivity.class);
        intent.putExtra(Constants.EXTRA_ORDER_ID, item.orderId);
        startActivity(intent);
    }
}
