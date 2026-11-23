package com.webbansach.app.ui.catalog_hieu.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.webbansach.app.R;
import com.webbansach.app.core.utils.CurrencyUtils;

import java.util.ArrayList;
import java.util.List;

public final class HomeRowAdapter<T> extends RecyclerView.Adapter<HomeRowAdapter.Row> {
    public interface Binder<T> {
        void bind(Row row, T item);
    }

    private final List<T> items = new ArrayList<>();
    private final int layout;
    private final Binder<T> binder;

    public HomeRowAdapter(@LayoutRes int layout, Binder<T> binder) {
        this.layout = layout;
        this.binder = binder;
    }

    public void submit(List<T> data) {
        items.clear();
        if (data != null) {
            for (T item : data) {
                if (item != null) {
                    items.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Row onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Row(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Row holder, int position) {
        binder.bind(holder, items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static final class Row extends RecyclerView.ViewHolder {
        Row(View view) {
            super(view);
        }

        public void setText(int id, String value) {
            TextView text = itemView.findViewById(id);
            text.setText(value);
            text.setVisibility(TextUtils.isEmpty(value) ? View.GONE : View.VISIBLE);
        }

        public void loadImage(String url) {
            ImageView image = itemView.findViewById(R.id.homeItemImage);
            Glide.with(image)
                    .load(CurrencyUtils.resolveImageUrl(url))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(image);
        }
    }
}
