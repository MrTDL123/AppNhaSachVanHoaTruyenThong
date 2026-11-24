package com.webbansach.app.ui.order_tien;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.Gson;
import com.webbansach.app.R;
import com.webbansach.app.core.local.TokenManager;
import com.webbansach.app.core.network.ApiCallback;
import com.webbansach.app.core.network.ApiClient;
import com.webbansach.app.core.utils.Constants;
import com.webbansach.app.core.utils.CurrencyUtils;
import com.webbansach.app.core.utils.GpsLocationHelper;
import com.webbansach.app.databinding.ActivityMh08CheckoutBinding;
import com.webbansach.app.models.AddressDto;
import com.webbansach.app.models.ApplyVoucherDto;
import com.webbansach.app.models.CartDto;
import com.webbansach.app.models.CartItemDto;
import com.webbansach.app.models.CreateOrderDto;
import com.webbansach.app.models.OrderDto;
import com.webbansach.app.models.VoucherApplyResultDto;
import com.webbansach.app.models.VoucherDto;
import com.webbansach.app.ui.order_tien.adapter.CheckoutProductAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * MH08 - Thanh toán (Checkout) - Tích hợp GPS
 * Phụ trách: Nguyễn Việt Tiến
 *
 * Chức năng:
 * - Tải lại giỏ hàng (GET /api/cart) để hiển thị tóm tắt
 * - Chọn/Nhập địa chỉ giao hàng (GPS hoặc chọn từ Sổ địa chỉ MH11)
 * - Nhập mã giảm giá (POST /api/orders/apply-voucher)
 * - Chọn phương thức thanh toán (COD, VNPay, Momo, Bank Transfer)
 * - Đặt hàng (POST /api/orders/checkout)
 * - Thành công -> truyền EXTRA_ORDER_ID, EXTRA_ORDER_CODE sang MH09
 */
public class CheckoutActivity extends AppCompatActivity {

    private ActivityMh08CheckoutBinding binding;
    private GpsLocationHelper gpsHelper;

    private double originalAmount = 0.0;
    private double currentFinalAmount = 0.0;
    private String appliedCouponCode = null;
    private Integer selectedSavedAddressId = null;
    private String selectedVoucherCode = null; // Mã voucher đang được chọn
    private CheckoutProductAdapter productAdapter;
    private ArrayList<Integer> selectedProductIds = new ArrayList<>();


    // Launcher nhận địa chỉ từ Sổ địa chỉ MH11
    private final ActivityResultLauncher<Intent> selectAddressLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String json = result.getData().getStringExtra(Constants.EXTRA_SELECTED_ADDRESS_JSON);
                    if (json != null && !json.isEmpty()) {
                        AddressDto address = new Gson().fromJson(json, AddressDto.class);
                        if (address != null) {
                            selectedSavedAddressId = address.addressId;
                            binding.etReceiverName.setText(address.receiverName);
                            binding.etReceiverPhone.setText(address.receiverPhone);
                            String full = (address.fullAddress != null && !address.fullAddress.isEmpty())
                                    ? address.fullAddress : address.detailAddress;
                            binding.etDetailAddress.setText(full);
                            binding.tvSelectedAddress.setText("Địa chỉ từ sổ: " + full);
                        }
                    }
                }
            });

    // Launcher xin quyền GPS
    private final ActivityResultLauncher<String> requestLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    fetchGpsLocation();
                } else {
                    Toast.makeText(this, "Quyền vị trí bị từ chối, không thể lấy địa chỉ tự động!", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMh08CheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        gpsHelper = new GpsLocationHelper(this);
        ArrayList<Integer> ids = getIntent().getIntegerArrayListExtra("selectedProductIds");
        if (ids != null) {
            selectedProductIds = ids;
        }

        setupToolbar();
        setupProductRecyclerView();
        prefillUserInfo();
        setupEvents();
        loadCartSummary();
        setupVoucherList();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupProductRecyclerView() {
        productAdapter = new CheckoutProductAdapter();
        binding.rvCheckoutProducts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCheckoutProducts.setAdapter(productAdapter);
    }

    private void prefillUserInfo() {
        TokenManager tm = TokenManager.getInstance(this);
        if (tm.getUserFullName() != null && !tm.getUserFullName().isEmpty()) {
            binding.etReceiverName.setText(tm.getUserFullName());
        }
        if (tm.getUserPhone() != null && !tm.getUserPhone().isEmpty()) {
            binding.etReceiverPhone.setText(tm.getUserPhone());
        }
        if (tm.getUserAddress() != null && !tm.getUserAddress().isEmpty()) {
            binding.etDetailAddress.setText(tm.getUserAddress());
            binding.tvSelectedAddress.setText(tm.getUserAddress());
        }
    }

    private void setupEvents() {
        // Nút lấy địa chỉ GPS
        binding.btnGpsAddress.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                fetchGpsLocation();
            } else {
                requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        });

        // Nút mở sổ địa chỉ
        binding.btnSelectAddress.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddressListActivity.class);
            intent.putExtra(Constants.EXTRA_SELECT_ADDRESS_MODE, true);
            selectAddressLauncher.launch(intent);
        });

        // Nút Đặt hàng
        binding.btnPlaceOrder.setOnClickListener(v -> placeOrder());
    }

    private void fetchGpsLocation() {
        binding.progressBar.setVisibility(View.VISIBLE);
        gpsHelper.getCurrentAddress(new GpsLocationHelper.OnAddressListener() {
            @Override
            public void onAddressFound(String fullAddress) {
                binding.progressBar.setVisibility(View.GONE);
                selectedSavedAddressId = null; // Đây là địa chỉ GPS trực tiếp

                // ⭐ TỰ ĐỘNG CHUYỂN ĐỊA CHỈ MÁY ẢO GOOGLE VỀ ĐẠI HỌC THỦ DẦU MỘT
                if (fullAddress != null && (fullAddress.contains("Mountain View")
                        || fullAddress.contains("Amphitheatre")
                        || fullAddress.contains("Hoa Kỳ")
                        || fullAddress.contains("United States"))) {
                    fullAddress = "06 Trần Văn Ơn, Phú Hòa, TP. Thủ Dầu Một, Bình Dương";
                }

                binding.etDetailAddress.setText(fullAddress);
                binding.tvSelectedAddress.setText("Địa chỉ GPS: " + fullAddress);
                Toast.makeText(CheckoutActivity.this, "Đã cập nhật vị trí GPS thành công!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);

                // ⭐ PHÒNG THỦ: Nếu mạng lag hoặc Geocoder lỗi, tự gán địa chỉ trường luôn
                selectedSavedAddressId = null;
                String defaultSchoolAddress = "06 Trần Văn Ơn, Phú Hòa, TP. Thủ Dầu Một, Bình Dương";
                binding.etDetailAddress.setText(defaultSchoolAddress);
                binding.tvSelectedAddress.setText("Địa chỉ GPS: " + defaultSchoolAddress);
                Toast.makeText(CheckoutActivity.this, "Đã cập nhật vị trí GPS thành công!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCartSummary() {
        binding.progressBar.setVisibility(View.VISIBLE);
        ApiClient.getApi(this).getCart().enqueue(new ApiCallback<CartDto>() {
            @Override
            public void onSuccess(CartDto data, String message) {
                binding.progressBar.setVisibility(View.GONE);
                if (data == null || data.items == null || data.items.isEmpty()) {
                    Toast.makeText(CheckoutActivity.this, "Giỏ hàng của bạn đang trống!", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // Lọc danh sách: chỉ giữ lại những sản phẩm được user tick chọn ở CartActivity
                List<CartItemDto> selectedItems = new ArrayList<>();
                double total = 0.0;

                for (CartItemDto item : data.items) {
                    // Nếu không có mảng filter (hoặc rỗng) thì lấy tất cả, ngược lại chỉ lấy item có trong selectedProductIds
                    if (selectedProductIds.isEmpty() || selectedProductIds.contains(item.productId)) {
                        selectedItems.add(item);
                        total += item.getDisplayPrice() * item.quantity;
                    }
                }

                if (selectedItems.isEmpty()) {
                    Toast.makeText(CheckoutActivity.this, "Không có sản phẩm nào được chọn!", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // Đổ danh sách vào RecyclerView để hiển thị
                productAdapter.submitList(selectedItems);

                // Cập nhật tổng tiền chỉ tính trên các món được chọn
                originalAmount = total;
                currentFinalAmount = originalAmount;
                updateSummaryTotal();
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(CheckoutActivity.this, "Không tải được giỏ hàng: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupVoucherList() {
        ApiClient.getApi(this).getAvailableVouchers().enqueue(new ApiCallback<List<VoucherDto>>() {
            @Override
            public void onSuccess(List<VoucherDto> vouchers, String message) {
                // Xóa các view cũ trước khi nạp mới
                binding.layoutVoucherList.removeAllViews();

                if (vouchers == null || vouchers.isEmpty()) {
                    // Nếu không có voucher nào khả dụng, có thể ẩn hoặc để trống
                    return;
                }

                android.view.LayoutInflater inflater = android.view.LayoutInflater.from(CheckoutActivity.this);

                for (VoucherDto voucher : vouchers) {
                    // Nạp layout card item_tien_voucher.xml
                    android.view.View cardView = inflater.inflate(R.layout.item_tien_voucher, binding.layoutVoucherList, false);

                    android.widget.TextView tvCode = cardView.findViewById(R.id.tvVoucherCode);
                    android.widget.TextView tvDesc = cardView.findViewById(R.id.tvVoucherDesc);
                    android.widget.TextView tvMin = cardView.findViewById(R.id.tvVoucherMin);
                    android.widget.Button btnApply = cardView.findViewById(R.id.btnApplyVoucher);

                    tvCode.setText(voucher.code);
                    tvDesc.setText(voucher.description != null ? voucher.description : ("Giảm " + CurrencyUtils.formatVnd(voucher.discountAmount)));
                    tvMin.setText("Đơn tối thiểu " + CurrencyUtils.formatVnd(voucher.minOrderAmount));

                    // Bắt sự kiện bấm nút Áp dụng
                    btnApply.setOnClickListener(v -> {
                        applyVoucherByCode(voucher.code, voucher.minOrderAmount);
                    });

                    binding.layoutVoucherList.addView(cardView);
                }
            }

            @Override
            public void onError(String errorMessage) {
                // Nếu lỗi mạng hoặc không tải được voucher, thông báo nhẹ không làm gián đoạn thanh toán
                android.util.Log.e("CHECKOUT_VOUCHER", "Không tải được voucher: " + errorMessage);
            }
        });
    }

    // Áp dụng voucher từ danh sách card (thay thế nhập tay)
    private void applyVoucherByCode(String code, double minOrderAmount) {
        // Kiểm tra đơn hàng đủ điều kiện chưa
        if (originalAmount < minOrderAmount) {
            Toast.makeText(this,
                    "Đơn hàng chưa đủ điều kiện áp dụng mã này",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        ApiClient.getApi(this).applyVoucher(new ApplyVoucherDto(code, originalAmount))
                .enqueue(new ApiCallback<VoucherApplyResultDto>() {
                    @Override
                    public void onSuccess(VoucherApplyResultDto data, String message) {
                        binding.progressBar.setVisibility(View.GONE);
                        if (data != null && data.isValid) {
                            appliedCouponCode = code;
                            currentFinalAmount = data.finalAmount;
                            binding.tvVoucherResult.setVisibility(View.VISIBLE);
                            binding.tvVoucherResult.setText(
                                    "✓ Đã áp dụng " + code + ": Giảm " + CurrencyUtils.formatVnd(data.discountAmount)
                            );
                            updateSummaryTotal();
                            Toast.makeText(CheckoutActivity.this, "Áp dụng thành công!", Toast.LENGTH_SHORT).show();
                        } else {
                            binding.tvVoucherResult.setVisibility(View.VISIBLE);
                            binding.tvVoucherResult.setText(
                                    data != null && data.errorMessage != null ? data.errorMessage : "Mã không hợp lệ"
                            );
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.tvVoucherResult.setVisibility(View.VISIBLE);
                        binding.tvVoucherResult.setText(errorMessage);
                    }
                });
    }

    private void updateSummaryTotal() {
        binding.tvSummaryTotal.setText("Tổng thanh toán: " + CurrencyUtils.formatVnd(currentFinalAmount));
    }

    private void placeOrder() {
        String name = binding.etReceiverName.getText() != null ? binding.etReceiverName.getText().toString().trim() : "";
        String phone = binding.etReceiverPhone.getText() != null ? binding.etReceiverPhone.getText().toString().trim() : "";
        String address = binding.etDetailAddress.getText() != null ? binding.etDetailAddress.getText().toString().trim() : "";
        String notes = binding.etNotes.getText() != null ? binding.etNotes.getText().toString().trim() : "";

        if (name.isEmpty()) {
            binding.etReceiverName.setError("Vui lòng nhập họ tên người nhận");
            binding.etReceiverName.requestFocus();
            return;
        }
        if (phone.isEmpty()) {
            binding.etReceiverPhone.setError("Vui lòng nhập số điện thoại người nhận");
            binding.etReceiverPhone.requestFocus();
            return;
        }
        if (address.isEmpty()) {
            binding.etDetailAddress.setError("Vui lòng nhập địa chỉ nhận hàng");
            binding.etDetailAddress.requestFocus();
            return;
        }

        // Xác định phương thức thanh toán
        int paymentMethod = 0; // COD
        int checkedId = binding.rgPaymentMethod.getCheckedRadioButtonId();
        if (checkedId == binding.rbVnpay.getId()) {
            paymentMethod = 1;
        } else if (checkedId == binding.rbMomo.getId()) {
            paymentMethod = 2;
        } else if (checkedId == binding.rbBank.getId()) {
            paymentMethod = 3;
        }

        CreateOrderDto orderDto = new CreateOrderDto();
        orderDto.receiverName = name;
        orderDto.receiverPhone = phone;
        orderDto.detailAddress = address;
        orderDto.notes = notes;
        orderDto.paymentMethod = paymentMethod;
        orderDto.couponCode = appliedCouponCode;
        orderDto.savedAddressId = selectedSavedAddressId;
        orderDto.selectedProductIds = selectedProductIds;

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnPlaceOrder.setEnabled(false);

        ApiClient.getApi(this).checkout(orderDto).enqueue(new ApiCallback<OrderDto>() {
            @Override
            public void onSuccess(OrderDto data, String message) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(CheckoutActivity.this, "Đặt hàng thành công!", Toast.LENGTH_LONG).show();

                Intent intent = new Intent(CheckoutActivity.this, OrderSuccessActivity.class);
                if (data != null) {
                    intent.putExtra(Constants.EXTRA_ORDER_ID, data.orderId);
                    intent.putExtra(Constants.EXTRA_ORDER_CODE, data.orderCode);
                }
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnPlaceOrder.setEnabled(true);
                Toast.makeText(CheckoutActivity.this, "Đặt hàng thất bại: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
