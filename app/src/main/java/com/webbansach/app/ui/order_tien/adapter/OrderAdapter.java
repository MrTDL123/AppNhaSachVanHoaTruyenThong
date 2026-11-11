package com.webbansach.app.ui.order_tien.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.webbansach.app.core.utils.CurrencyUtils;
import com.webbansach.app.databinding.ItemTienOrderBinding;
import com.webbansach.app.models.OrderListItemDto;

import java.util.ArrayList;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    public interface OnOrderActionListener {
        void onViewOrderDetail(OrderListItemDto item);
        void onCancelOrder(OrderListItemDto item);
        void onReviewOrder(OrderListItemDto item);
        void onReturnOrder(OrderListItemDto item);
    }

    private List<OrderListItemDto> items = new ArrayList<>();
    private final OnOrderActionListener listener;

    public OrderAdapter(OnOrderActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<OrderListItemDto> newItems) {
        this.items = (newItems != null) ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTienOrderBinding binding = ItemTienOrderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new OrderViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        private final ItemTienOrderBinding binding;

        public OrderViewHolder(ItemTienOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(OrderListItemDto item) {
            binding.tvOrderCode.setText("#" + (item.orderCode != null ? item.orderCode : String.valueOf(item.orderId)));
            binding.tvOrderStatus.setText(item.statusText != null ? item.statusText : getStatusLabel(item.status));
            binding.tvOrderDate.setText(item.orderDate != null ? item.orderDate : "");

            binding.tvProductTitle.setText(item.firstProductTitle != null ? item.firstProductTitle : "Sản phẩm");

            String qtyInfo = "Số lượng: " + item.firstProductQuantity;
            if (item.otherProductsCount > 0) {
                qtyInfo += " (+" + item.otherProductsCount + " sản phẩm khác)";
            }
            binding.tvProductQuantityInfo.setText(qtyInfo);

            binding.tvTotalAmount.setText(CurrencyUtils.formatVnd(item.totalAmount));

            // Tải ảnh bìa sách đầu tiên
            if (item.firstProductImageUrl != null && !item.firstProductImageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(CurrencyUtils.resolveImageUrl(item.firstProductImageUrl))
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_delete)
                        .into(binding.ivProductThumb);
            } else {
                binding.ivProductThumb.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            // Trạng thái 0 (Pending) -> Hiển thị nút Hủy đơn
            boolean isPending = (item.status == 0);
            binding.btnCancelOrder.setVisibility(isPending ? View.VISIBLE : View.GONE);

            // Trạng thái 3 (Completed) -> Hiển thị Đánh giá & Trả hàng
            boolean isCompleted = (item.status == 3);
            binding.btnReviewOrder.setVisibility(isCompleted ? View.VISIBLE : View.GONE);
            binding.btnReturnOrder.setVisibility(isCompleted ? View.VISIBLE : View.GONE);

            // Ẩn thanh hành động nếu không có nút nào hiển thị
            boolean hasActions = isPending || isCompleted;
            binding.layoutActions.setVisibility(hasActions ? View.VISIBLE : View.GONE);

            // Sự kiện click item -> Xem chi tiết
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onViewOrderDetail(item);
            });

            binding.btnCancelOrder.setOnClickListener(v -> {
                if (listener != null) listener.onCancelOrder(item);
            });

            binding.btnReviewOrder.setOnClickListener(v -> {
                if (listener != null) listener.onReviewOrder(item);
            });

            binding.btnReturnOrder.setOnClickListener(v -> {
                if (listener != null) listener.onReturnOrder(item);
            });
        }

        private String getStatusLabel(int status) {
            switch (status) {
                case 0: return "Chờ xử lý";
                case 1: return "Đang xử lý";
                case 2: return "Đang giao hàng";
                case 3: return "Hoàn thành";
                case 4: return "Đã hủy";
                case 5: return "Đã trả hàng";
                default: return "Đơn hàng";
            }
        }
    }
}
