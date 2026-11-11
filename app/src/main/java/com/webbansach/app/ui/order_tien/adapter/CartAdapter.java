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

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    // 1. Interface để bắn sự kiện ngược lại cho CartActivity xử lý gọi API
    public interface OnCartItemActionListener {
        void onIncreaseQuantity(CartItemDto item);
        void onDecreaseQuantity(CartItemDto item);
        void onRemoveItem(CartItemDto item);
    }

    private List<CartItemDto> items = new ArrayList<>();
    private final OnCartItemActionListener listener;

    public CartAdapter(OnCartItemActionListener listener) {
        this.listener = listener;
    }

    // Hàm nhận danh sách mới khi gọi API xong và vẽ lại toàn bộ
    public void submitList(List<CartItemDto> newItems) {
        this.items = (newItems != null) ? newItems : new ArrayList<>();
        notifyDataSetChanged();
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

//        public void bind(CartItemDto item) {
//            // Đổ tên sách & số lượng
//            binding.tvBookTitle.setText(item.title);
//            binding.tvQuantity.setText(String.valueOf(item.quantity));
//
//            // Định dạng tiền tệ VNĐ (ví dụ 150000 -> "150.000 đ")
//            binding.tvBookPrice.setText(CurrencyUtils.formatVnd(item.finalPrice));
//
//            // Dùng Glide tải ảnh từ URL do backend trả về
//            Glide.with(itemView.getContext())
//                    .load(CurrencyUtils.resolveImageUrl(item.mainImageUrl))
//                    .placeholder(android.R.drawable.ic_menu_gallery)
//                    .error(android.R.drawable.ic_delete)
//                    .into(binding.ivBookThumb);
//
//            // Bắt sự kiện bấm nút Tăng (+)
//            binding.btnPlus.setOnClickListener(v -> {
//                if (listener != null) listener.onIncreaseQuantity(item);
//            });
//
//            // Bắt sự kiện bấm nút Giảm (-)
//            binding.btnMinus.setOnClickListener(v -> {
//                if (listener != null) listener.onDecreaseQuantity(item);
//            });
//
//            // Bắt sự kiện bấm nút Xóa (Thùng rác)
//            binding.btnRemoveItem.setOnClickListener(v -> {
//                if (listener != null) listener.onRemoveItem(item);
//            });
//        }

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
        }
    }
}
