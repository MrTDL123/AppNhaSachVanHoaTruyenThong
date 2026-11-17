package com.webbansach.app.ui.order_tien.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.webbansach.app.core.utils.CurrencyUtils;
import com.webbansach.app.databinding.ItemTienCheckoutProductBinding;
import com.webbansach.app.models.CartItemDto;

import java.util.ArrayList;
import java.util.List;

public class CheckoutProductAdapter extends RecyclerView.Adapter<CheckoutProductAdapter.ProductViewHolder> {

    private List<CartItemDto> items = new ArrayList<>();

    public void submitList(List<CartItemDto> newItems) {
        this.items = (newItems != null) ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTienCheckoutProductBinding binding = ItemTienCheckoutProductBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ProductViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        private final ItemTienCheckoutProductBinding binding;

        public ProductViewHolder(ItemTienCheckoutProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(CartItemDto item) {
            binding.tvBookTitle.setText(item.title);
            binding.tvBookQuantity.setText("x" + item.quantity);

            // Thành tiền từng cuốn = đơn giá x số lượng
            double itemTotal = item.getDisplayPrice() * item.quantity;
            binding.tvBookPrice.setText(CurrencyUtils.formatVnd(itemTotal));

            Glide.with(itemView.getContext())
                    .load(CurrencyUtils.resolveImageUrl(item.mainImageUrl))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_delete)
                    .into(binding.ivBookThumb);
        }
    }
}