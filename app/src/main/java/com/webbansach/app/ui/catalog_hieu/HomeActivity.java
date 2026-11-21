package com.webbansach.app.ui.catalog_hieu;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.webbansach.app.R;
import com.webbansach.app.core.network.ApiCallback;
import com.webbansach.app.core.network.ApiClient;
import com.webbansach.app.core.utils.Constants;
import com.webbansach.app.core.utils.CurrencyUtils;
import com.webbansach.app.databinding.ActivityMh04HomeBinding;
import com.webbansach.app.models.ApiResponse;
import com.webbansach.app.models.BannerDto;
import com.webbansach.app.models.CategoryDto;
import com.webbansach.app.models.FreeBookDto;
import com.webbansach.app.models.HomePageDataDto;
import com.webbansach.app.models.ProductListItemDto;
import com.webbansach.app.ui.auth_long.ProfileActivity;
import com.webbansach.app.ui.catalog_hieu.adapter.HomeRowAdapter;
import com.webbansach.app.ui.order_tien.CartActivity;

import retrofit2.Call;

public class HomeActivity extends AppCompatActivity {
    private ActivityMh04HomeBinding binding;
    private Call<ApiResponse<HomePageDataDto>> request;
    private HomeRowAdapter<BannerDto> banners;
    private HomeRowAdapter<CategoryDto> categories;
    private HomeRowAdapter<ProductListItemDto> newBooks;
    private HomeRowAdapter<ProductListItemDto> bestSellers;
    private HomeRowAdapter<FreeBookDto> freeBooks;
    private boolean hasContent;
    private HomeRowAdapter<ProductListItemDto> recentBooks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMh04HomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });
        setupRows();
        binding.layoutSearchBar.setOnClickListener(view -> openSearch());
        binding.homeNavSearch.setOnClickListener(view -> openSearch());
        binding.homeNavCart.setOnClickListener(view -> startActivity(new Intent(this, CartActivity.class)));
        binding.homeNavProfile.setOnClickListener(view -> startActivity(new Intent(this, ProfileActivity.class)));
        binding.swipeRefresh.setColorSchemeResources(R.color.primary_red);
        binding.swipeRefresh.setOnRefreshListener(() -> loadHome(true));
        binding.homeRetry.setOnClickListener(view -> loadHome(false));
        loadHome(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        recentBooks.submit(new CatalogStorage(this).recentBooks());
        showSection(recentBooks, binding.recentSection);
    }

    private void updateBannerDots() {
        int count = banners.getItemCount();
        binding.bannerDots.setVisibility(count > 1 ? View.VISIBLE : View.GONE);
        int position = ((LinearLayoutManager) binding.rvBanners.getLayoutManager())
                .findFirstVisibleItemPosition();
        position = Math.max(0, position);
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < count; i++) {
            dots.append(i == position ? "● " : "○ ");
        }
        binding.bannerDots.setText(dots);
        binding.bannerDots.setContentDescription(getString(R.string.hieu_extra_banner_position, position + 1, count));
    }

    private void openSearch() {
        startActivity(new Intent(this, SearchFilterActivity.class));
    }

    private void setupRows() {
        banners = new HomeRowAdapter<>(R.layout.item_hieu_home_banner, (row, item) -> {
            row.setText(R.id.homeItemTitle, item.title);
            row.loadImage(item.imageUrl);
        });
        categories = new HomeRowAdapter<>(R.layout.item_hieu_home_category, (row, item) -> {
            row.setText(R.id.homeItemTitle, item.categoryName);
            row.setText(R.id.homeItemSubtitle,
                    getString(R.string.hieu_home_category_count, item.totalProducts));
            row.itemView.setOnClickListener(view -> {
                Intent intent = new Intent(this, SearchFilterActivity.class);
                intent.putExtra(Constants.EXTRA_CATEGORY_ID, item.categoryId);
                startActivity(intent);
            });
        });
        newBooks = new HomeRowAdapter<>(R.layout.item_hieu_home_book, this::bindProduct);
        bestSellers = new HomeRowAdapter<>(R.layout.item_hieu_home_book, this::bindProduct);
        freeBooks = new HomeRowAdapter<>(R.layout.item_hieu_home_book, (row, item) ->
                bindBook(row, item.title, item.authorName, item.coverImageUrl,
                        getString(R.string.hieu_home_free_price),
                        getString(R.string.hieu_home_remaining, item.availableQty)));

        recentBooks = new HomeRowAdapter<>(R.layout.item_hieu_home_book, this::bindProduct);
        attachRow(binding.rvRecent, recentBooks);
        attachRow(binding.rvBanners, banners);
        binding.rvBanners.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView view, int dx, int dy) {
                updateBannerDots();
            }
        });
        new PagerSnapHelper().attachToRecyclerView(binding.rvBanners);
        attachRow(binding.rvCategories, categories);
        attachRow(binding.rvNewProducts, newBooks);
        attachRow(binding.rvBestSellers, bestSellers);
        attachRow(binding.rvFreeBooks, freeBooks);
    }

    private void attachRow(RecyclerView row, RecyclerView.Adapter<?> adapter) {
        row.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        row.setAdapter(adapter);
    }

    private void bindProduct(HomeRowAdapter.Row row, ProductListItemDto book) {
        String originalPrice = book.price > book.finalPrice
                ? getString(R.string.hieu_home_original_price, CurrencyUtils.formatVnd(book.price))
                : null;
        bindBook(row, book.title, book.authorName, book.mainImageUrl,
                CurrencyUtils.formatVnd(book.finalPrice), originalPrice);
        row.setText(R.id.bookDiscount, book.discountPercent > 0
                ? getString(R.string.hieu_extra_book_discount, book.discountPercent) : null);
        row.setText(R.id.bookRating, book.averageRating > 0
                ? getString(R.string.hieu_extra_book_rating, book.averageRating) : null);
        row.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(this, BookDetailActivity.class);
            intent.putExtra(Constants.EXTRA_PRODUCT_ID, book.productId);
            startActivity(intent);
        });
    }

    private void bindBook(HomeRowAdapter.Row row, String title, String author, String image,
                          String price, String extra) {
        row.setText(R.id.homeItemTitle, title);
        row.setText(R.id.homeItemSubtitle, author);
        row.setText(R.id.homeItemPrice, price);
        row.setText(R.id.homeItemExtra, extra);
        row.loadImage(image);
    }

    private void loadHome(boolean refreshing) {
        if (request != null) {
            return;
        }
        binding.homeStatus.setVisibility(View.GONE);
        binding.progressBar.setVisibility(refreshing ? View.GONE : View.VISIBLE);
        request = ApiClient.getApi(this).getHomePage();
        request.enqueue(new ApiCallback<HomePageDataDto>() {
            @Override
            public void onSuccess(HomePageDataDto data, String message) {
                if (finishLoading()) {
                    render(data == null ? new HomePageDataDto() : data);
                }
            }

            @Override
            public void onError(String message) {
                if (finishLoading()) {
                    if (hasContent || recentBooks.getItemCount() > 0) {
                        Toast.makeText(HomeActivity.this, message, Toast.LENGTH_LONG).show();
                    } else {
                        showStatus(message);
                    }
                }
            }
        });
    }

    private boolean finishLoading() {
        if (isFinishing() || isDestroyed()) {
            return false;
        }
        request = null;
        binding.progressBar.setVisibility(View.GONE);
        binding.swipeRefresh.setRefreshing(false);
        return true;
    }

    private void render(HomePageDataDto data) {
        banners.submit(data.banners);
        updateBannerDots();
        categories.submit(data.categories);
        newBooks.submit(data.newProducts);
        bestSellers.submit(data.bestSellerProducts);
        freeBooks.submit(data.freeBooks);
        showSection(banners, binding.rvBanners);
        showSection(categories, binding.homeCategorySection);
        showSection(newBooks, binding.homeNewHeading, binding.rvNewProducts);
        showSection(bestSellers, binding.homeBestHeading, binding.rvBestSellers);
        showSection(freeBooks, binding.homeFreeHeading, binding.rvFreeBooks);
        hasContent = banners.getItemCount() + categories.getItemCount()
                + newBooks.getItemCount() + bestSellers.getItemCount() + freeBooks.getItemCount() > 0;
        if (!hasContent && recentBooks.getItemCount() == 0) {
            showStatus(getString(R.string.hieu_home_empty));
        }
    }

    private void showSection(HomeRowAdapter<?> adapter, View... views) {
        for (View view : views) {
            view.setVisibility(adapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
        }
    }

    private void showStatus(String message) {
        binding.homeStatusText.setText(message);
        binding.homeStatus.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        if (request != null) {
            request.cancel();
        }
        super.onDestroy();
    }
}
