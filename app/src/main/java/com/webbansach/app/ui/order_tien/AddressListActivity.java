package com.webbansach.app.ui.order_tien;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.Gson;
import com.webbansach.app.core.network.ApiCallback;
import com.webbansach.app.core.network.ApiClient;
import com.webbansach.app.core.utils.Constants;
import com.webbansach.app.databinding.ActivityMh11AddressBinding;
import com.webbansach.app.databinding.DialogTienAddressBinding;
import com.webbansach.app.models.AddressDto;
import com.webbansach.app.models.CreateAddressDto;
import com.webbansach.app.models.UpdateAddressDto;
import com.webbansach.app.ui.order_tien.adapter.AddressAdapter;

import java.util.List;

/**
 * MH11 - Sổ địa chỉ giao hàng
 * Phụ trách: Nguyễn Việt Tiến
 *
 * Chức năng:
 * - Lấy danh sách địa chỉ (GET /api/addresses)
 * - Thêm địa chỉ mới (POST /api/addresses)
 * - Sửa địa chỉ (PUT /api/addresses/{id})
 * - Xóa địa chỉ (DELETE /api/addresses/{id})
 * - Nếu mở ở chế độ chọn (EXTRA_SELECT_ADDRESS_MODE = true):
 *   Bấm chọn -> trả về EXTRA_SELECTED_ADDRESS_JSON cho CheckoutActivity (MH08)
 */
public class AddressListActivity extends AppCompatActivity implements AddressAdapter.OnAddressActionListener {

    private ActivityMh11AddressBinding binding;
    private AddressAdapter adapter;
    private boolean isSelectMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMh11AddressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        isSelectMode = getIntent().getBooleanExtra(Constants.EXTRA_SELECT_ADDRESS_MODE, false);

        setupToolbar();
        setupRecyclerView();
        setupEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAddresses();
    }

    private void setupToolbar() {
        binding.toolbar.setTitle(isSelectMode ? "Chọn địa chỉ nhận hàng" : "Sổ địa chỉ của tôi");
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new AddressAdapter(this);
        binding.rvAddresses.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAddresses.setAdapter(adapter);
    }

    private void setupEvents() {
        binding.fabAddAddress.setOnClickListener(v -> showAddEditAddressDialog(null));
    }

    private void loadAddresses() {
        binding.progressBar.setVisibility(View.VISIBLE);
        ApiClient.getApi(this).getAddresses().enqueue(new ApiCallback<List<AddressDto>>() {
            @Override
            public void onSuccess(List<AddressDto> data, String message) {
                binding.progressBar.setVisibility(View.GONE);
                adapter.submitList(data);
                boolean isEmpty = (data == null || data.isEmpty());
                binding.tvEmptyAddress.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(AddressListActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onSelectAddress(AddressDto item) {
        if (isSelectMode) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(Constants.EXTRA_SELECTED_ADDRESS_JSON, new Gson().toJson(item));
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(this, "Địa chỉ: " + item.receiverName + " - " + item.detailAddress, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onEditAddress(AddressDto item) {
        showAddEditAddressDialog(item);
    }

    @Override
    public void onDeleteAddress(AddressDto item) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa địa chỉ này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    binding.progressBar.setVisibility(View.VISIBLE);
                    ApiClient.getApi(AddressListActivity.this).deleteAddress(item.addressId)
                            .enqueue(new ApiCallback<Boolean>() {
                                @Override
                                public void onSuccess(Boolean data, String message) {
                                    binding.progressBar.setVisibility(View.GONE);
                                    Toast.makeText(AddressListActivity.this, "Đã xóa địa chỉ thành công!", Toast.LENGTH_SHORT).show();
                                    loadAddresses();
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    binding.progressBar.setVisibility(View.GONE);
                                    Toast.makeText(AddressListActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showAddEditAddressDialog(AddressDto existing) {
        boolean isEdit = (existing != null);
        DialogTienAddressBinding dialogBinding = DialogTienAddressBinding.inflate(LayoutInflater.from(this));

        dialogBinding.tvDialogTitle.setText(isEdit ? "Cập nhật địa chỉ" : "Thêm địa chỉ mới");

        if (isEdit) {
            dialogBinding.etName.setText(existing.receiverName);
            dialogBinding.etPhone.setText(existing.receiverPhone);
            dialogBinding.etDetail.setText(existing.detailAddress);
            dialogBinding.cbIsDefault.setChecked(existing.isDefault);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(isEdit ? "Cập nhật" : "Lưu", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = dialogBinding.etName.getText() != null ? dialogBinding.etName.getText().toString().trim() : "";
                String phone = dialogBinding.etPhone.getText() != null ? dialogBinding.etPhone.getText().toString().trim() : "";
                String detail = dialogBinding.etDetail.getText() != null ? dialogBinding.etDetail.getText().toString().trim() : "";
                boolean isDefault = dialogBinding.cbIsDefault.isChecked();

                if (name.isEmpty()) {
                    dialogBinding.etName.setError("Vui lòng nhập tên người nhận");
                    return;
                }
                if (phone.isEmpty()) {
                    dialogBinding.etPhone.setError("Vui lòng nhập số điện thoại");
                    return;
                }
                if (detail.isEmpty()) {
                    dialogBinding.etDetail.setError("Vui lòng nhập địa chỉ cụ thể");
                    return;
                }

                binding.progressBar.setVisibility(View.VISIBLE);
                dialog.dismiss();

                if (isEdit) {
                    UpdateAddressDto updateDto = new UpdateAddressDto(existing.addressId, name, phone, detail, isDefault);
                    ApiClient.getApi(AddressListActivity.this).updateAddress(existing.addressId, updateDto)
                            .enqueue(new ApiCallback<AddressDto>() {
                                @Override
                                public void onSuccess(AddressDto data, String message) {
                                    binding.progressBar.setVisibility(View.GONE);
                                    Toast.makeText(AddressListActivity.this, "Cập nhật địa chỉ thành công!", Toast.LENGTH_SHORT).show();
                                    loadAddresses();
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    binding.progressBar.setVisibility(View.GONE);
                                    Toast.makeText(AddressListActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    CreateAddressDto createDto = new CreateAddressDto(name, phone, detail, isDefault);
                    ApiClient.getApi(AddressListActivity.this).createAddress(createDto)
                            .enqueue(new ApiCallback<AddressDto>() {
                                @Override
                                public void onSuccess(AddressDto data, String message) {
                                    binding.progressBar.setVisibility(View.GONE);
                                    Toast.makeText(AddressListActivity.this, "Thêm địa chỉ mới thành công!", Toast.LENGTH_SHORT).show();
                                    loadAddresses();
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    binding.progressBar.setVisibility(View.GONE);
                                    Toast.makeText(AddressListActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            });
        });

        dialog.show();
    }
}
