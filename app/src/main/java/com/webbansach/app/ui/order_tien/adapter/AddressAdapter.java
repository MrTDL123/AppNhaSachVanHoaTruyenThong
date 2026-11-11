package com.webbansach.app.ui.order_tien.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.webbansach.app.databinding.ItemTienAddressBinding;
import com.webbansach.app.models.AddressDto;

import java.util.ArrayList;
import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.AddressViewHolder> {

    public interface OnAddressActionListener {
        void onSelectAddress(AddressDto item);
        void onEditAddress(AddressDto item);
        void onDeleteAddress(AddressDto item);
    }

    private List<AddressDto> items = new ArrayList<>();
    private final OnAddressActionListener listener;

    public AddressAdapter(OnAddressActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<AddressDto> newItems) {
        this.items = (newItems != null) ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTienAddressBinding binding = ItemTienAddressBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new AddressViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class AddressViewHolder extends RecyclerView.ViewHolder {
        private final ItemTienAddressBinding binding;

        public AddressViewHolder(ItemTienAddressBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(AddressDto item) {
            binding.tvReceiverName.setText(item.receiverName);
            binding.tvReceiverPhone.setText(item.receiverPhone);

            String full = (item.fullAddress != null && !item.fullAddress.isEmpty())
                    ? item.fullAddress : item.detailAddress;
            binding.tvFullAddress.setText(full);

            binding.tvDefaultBadge.setVisibility(item.isDefault ? View.VISIBLE : View.GONE);

            // Bấm vào card -> Chọn địa chỉ này
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onSelectAddress(item);
            });

            // Nút sửa
            binding.btnEditAddress.setOnClickListener(v -> {
                if (listener != null) listener.onEditAddress(item);
            });

            // Nút xóa
            binding.btnDeleteAddress.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteAddress(item);
            });
        }
    }
}
