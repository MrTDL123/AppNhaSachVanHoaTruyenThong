package com.webbansach.app.ui.order_tien.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.webbansach.app.core.utils.CurrencyUtils;
import com.webbansach.app.databinding.ItemTienCartBinding;
import com.webbansach.app.models.CartItemDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    // 1. Interface để bắn sự kiện ngược lại cho CartActivity xử lý gọi API
    public interface OnCartItemActionListener {
        void onIncreaseQuantity(CartItemDto item);
        void onDecreaseQuantity(CartItemDto item);
        void onRemoveItem(CartItemDto item);
        void onSelectionChanged();
    }

    private List<CartItemDto> items = new ArrayList<>();
    private final OnCartItemActionListener listener;
    private final java.util.Set<Integer> selectedProductIds = new java.util.HashSet<>();

    public CartAdapter(OnCartItemActionListener listener) {
        this.listener = listener;
    }

    // Hàm nhận danh sách mới khi gọi API xong và vẽ lại toàn bộ
    public void submitList(List<CartItemDto> newItems) {
        this.items = (newItems != null) ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<CartItemDto> getSelectedItems() {
        List<CartItemDto> selected = new ArrayList<>();
        for (CartItemDto item : items) {
            if (selectedProductIds.contains(item.productId)) {
                selected.add(item);
            }
        }
        return selected;
    }

    public void setSelectAll(boolean isSelectAll) {
        selectedProductIds.clear();
        if (isSelectAll) {
            for (CartItemDto item : items) {
                selectedProductIds.add(item.productId);
            }
        }
        notifyDataSetChanged();
    }

    public boolean isAllSelected() {
        if (items == null || items.isEmpty()) return false;
        return selectedProductIds.size() == items.size();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Nạp layout item_tien_cart.xml thông qua ViewBinding
        ItemTienCartBinding binding = ItemTienCartBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new CartViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItemDto item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // Lớp giữ các view của từng dòng
    class CartViewHolder extends RecyclerView.ViewHolder {
        private final ItemTienCartBinding binding;

        public CartViewHolder(ItemTienCartBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(CartItemDto item) {
            binding.tvBookTitle.setText(item.title);
            binding.tvQuantity.setText(String.valueOf(item.quantity));

            // 1. Hiển thị giá tiền chuẩn xác từ FinalUnitPrice hoặc UnitPrice
            double priceToDisplay = item.getDisplayPrice();
            binding.tvBookPrice.setText(CurrencyUtils.formatVnd(priceToDisplay));

            // 2. Nối đường dẫn ảnh đầy đủ
            String fullImageUrl = CurrencyUtils.resolveImageUrl(item.mainImageUrl);

            // 3. Tải ảnh bằng Glide (đã hỗ trợ bỏ qua lỗi SSL nếu máy ảo chặn)
            Glide.with(itemView.getContext())
                    .load(fullImageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_delete)
                    .into(binding.ivBookThumb);

            // Bắt sự kiện nút bấm
            binding.btnPlus.setOnClickListener(v -> {
                if (listener != null) listener.onIncreaseQuantity(item);
            });

            binding.btnMinus.setOnClickListener(v -> {
                if (listener != null) listener.onDecreaseQuantity(item);
            });

            binding.btnRemoveItem.setOnClickListener(v -> {
                if (listener != null) listener.onRemoveItem(item);
            });
            // Đặt trạng thái checkbox — phải gỡ listener trước để tránh trigger vòng lặp
            binding.cbSelectItem.setOnCheckedChangeListener(null);
            binding.cbSelectItem.setChecked(selectedProductIds.contains(item.productId));
            binding.cbSelectItem.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedProductIds.add(item.productId);
                } else {
                    selectedProductIds.remove(item.productId);
                }

                if (listener != null) listener.onSelectionChanged();
            });
        }
    }
}
