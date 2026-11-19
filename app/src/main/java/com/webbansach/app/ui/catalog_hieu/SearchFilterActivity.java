package com.webbansach.app.ui.catalog_hieu;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.webbansach.app.R;
import com.webbansach.app.core.network.ApiCallback;
import com.webbansach.app.core.network.ApiClient;
import com.webbansach.app.core.utils.Constants;
import com.webbansach.app.core.utils.CurrencyUtils;
import com.webbansach.app.databinding.ActivityMh05FilterBinding;
import com.webbansach.app.models.ApiResponse;
import com.webbansach.app.models.PagedResponse;
import com.webbansach.app.models.ProductListItemDto;
import com.webbansach.app.ui.catalog_hieu.adapter.HomeRowAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import retrofit2.Call;

public class SearchFilterActivity extends AppCompatActivity {
    private static final int PAGE_SIZE = 10;

    private ActivityMh05FilterBinding binding;
    private HomeRowAdapter<ProductListItemDto> products;
    private Call<ApiResponse<PagedResponse<ProductListItemDto>>> searchRequest;
    private List<FilterOption> categories;
    private List<FilterOption> authors;
    private int filterRequestsPending;
    private String keyword = "";
    private int sortChip = R.id.chipAll;
    private Integer categoryId;
    private Integer authorId;
    private Double minPrice;
    private Double maxPrice;
    private int page = 1;
    private android.os.Parcelable pendingScroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMh05FilterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets padding = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(padding.left, padding.top, padding.right, padding.bottom);
            return insets;
        });

        int initialCategory = getIntent().getIntExtra(Constants.EXTRA_CATEGORY_ID, -1);
        categoryId = initialCategory > 0 ? initialCategory : null;
        if (savedInstanceState != null) {
            keyword = savedInstanceState.getString("keyword", "");
            sortChip = savedInstanceState.getInt("sortChip", R.id.chipAll);
            categoryId = readId(savedInstanceState, "categoryId");
            authorId = readId(savedInstanceState, "authorId");
            minPrice = readPrice(savedInstanceState, "minPrice");
            maxPrice = readPrice(savedInstanceState, "maxPrice");
            page = savedInstanceState.getInt("page", 1);
            pendingScroll = savedInstanceState.getParcelable("scroll");
        }

        products = new HomeRowAdapter<>(R.layout.item_hieu_filter_book, this::bindProduct);
        int columns = Math.max(2, getResources().getConfiguration().screenWidthDp / 180);
        binding.rvProducts.setLayoutManager(new GridLayoutManager(this, columns));
        binding.rvProducts.setAdapter(products);
        setupActions();
        updateFilterButton();
        binding.clearHistory.setOnClickListener(view -> {
            new CatalogStorage(this).clearSearches();
            renderHistory();
        });
        renderHistory();
        loadPage(page);
    }

    private void setupActions() {
        binding.btnBack.setOnClickListener(view -> finish());
        binding.btnSearch.setOnClickListener(view -> submitSearch());
        binding.etSearch.setText(keyword);
        binding.etSearch.setOnEditorActionListener((view, action, event) -> {
            if (action == EditorInfo.IME_ACTION_SEARCH) {
                submitSearch();
                return true;
            }
            return false;
        });
        binding.chipGroupFilter.check(sortChip);
        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int selected = checkedIds.isEmpty() ? R.id.chipAll : checkedIds.get(0);
            if (selected != sortChip) {
                sortChip = selected;
                submitSearch();
            }
        });
        binding.btnFilters.setOnClickListener(view -> toggleFilters());
        binding.btnApply.setOnClickListener(view -> applyFilters());
        binding.btnClear.setOnClickListener(view -> clearFilters());
        binding.btnPrevious.setOnClickListener(view -> loadPage(page - 1));
        binding.btnNext.setOnClickListener(view -> loadPage(page + 1));
        binding.btnRetry.setOnClickListener(view -> loadPage(page));
    }

    private void bindProduct(HomeRowAdapter.Row row, ProductListItemDto item) {
        row.setText(R.id.homeItemTitle, item.title);
        row.setText(R.id.homeItemSubtitle, item.authorName);
        row.setText(R.id.homeItemPrice, CurrencyUtils.formatVnd(item.finalPrice));
        row.setText(R.id.homeItemExtra, item.price > item.finalPrice
                ? getString(R.string.hieu_filter_original, CurrencyUtils.formatVnd(item.price))
                : null);
        row.setText(R.id.bookDiscount, item.discountPercent > 0
                ? getString(R.string.hieu_extra_book_discount, item.discountPercent) : null);
        row.setText(R.id.bookRating, item.averageRating > 0
                ? getString(R.string.hieu_extra_book_rating, item.averageRating) : null);
        row.loadImage(item.mainImageUrl);
        row.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(this, BookDetailActivity.class);
            intent.putExtra(Constants.EXTRA_PRODUCT_ID, item.productId);
            startActivity(intent);
        });
    }

    private void submitSearch() {
        keyword = binding.etSearch.getText().toString().trim();
        pendingScroll = null;
        new CatalogStorage(this).rememberSearch(keyword);
        renderHistory();
        hideKeyboard();
        loadPage(1);
    }

    private void hideKeyboard() {
        InputMethodManager keyboard = getSystemService(InputMethodManager.class);
        keyboard.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
    }

    private String sortBy() {
        if (sortChip == R.id.chipPriceAsc) return "price-asc";
        if (sortChip == R.id.chipPriceDesc) return "price-desc";
        if (sortChip == R.id.chipRating) return "rating";
        if (sortChip == R.id.chipDiscount) return "discount";
        return null;
    }

    private void loadPage(int requestedPage) {
        if (searchRequest != null) {
            searchRequest.cancel();
        }
        page = requestedPage;
        products.submit(null);
        binding.pager.setVisibility(View.GONE);
        binding.resultsCount.setText(null);
        binding.resultStatus.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.VISIBLE);

        Call<ApiResponse<PagedResponse<ProductListItemDto>>> request = ApiClient.getApi(this)
                .getProducts(keyword.isEmpty() ? null : keyword, categoryId, authorId,
                        minPrice, maxPrice, sortBy(), requestedPage, PAGE_SIZE);
        searchRequest = request;
        request.enqueue(new ApiCallback<PagedResponse<ProductListItemDto>>() {
            @Override
            public void onSuccess(PagedResponse<ProductListItemDto> data, String message) {
                if (finishSearch(request)) {
                    renderResults(data, requestedPage);
                }
            }

            @Override
            public void onError(String message) {
                if (finishSearch(request)) {
                    showStatus(message, true);
                }
            }
        });
    }

    private boolean finishSearch(Call<?> request) {
        if (searchRequest != request || isFinishing() || isDestroyed()) {
            return false;
        }
        searchRequest = null;
        binding.progressBar.setVisibility(View.GONE);
        return true;
    }

    private void renderResults(PagedResponse<ProductListItemDto> data, int requestedPage) {
        if (data == null) {
            showStatus(getString(R.string.hieu_filter_load_error), true);
            return;
        }
        int totalPages = Math.max(1, data.totalPages);
        if (requestedPage > totalPages) {
            loadPage(totalPages);
            return;
        }
        binding.resultsCount.setText(getString(R.string.hieu_filter_results, data.totalCount));
        products.submit(data.items);
        if (products.getItemCount() == 0) {
            showStatus(getString(R.string.hieu_filter_empty), false);
        } else {
            if (pendingScroll != null) {
                binding.rvProducts.getLayoutManager().onRestoreInstanceState(pendingScroll);
                pendingScroll = null;
            } else {
                binding.rvProducts.scrollToPosition(0);
            }
        }
        binding.pager.setVisibility(totalPages > 1 ? View.VISIBLE : View.GONE);
        binding.pageLabel.setText(getString(R.string.hieu_filter_page, requestedPage, totalPages));
        binding.btnPrevious.setEnabled(requestedPage > 1);
        binding.btnNext.setEnabled(requestedPage < totalPages);
    }

    private void showStatus(String message, boolean allowRetry) {
        binding.statusMessage.setText(message);
        binding.btnRetry.setVisibility(allowRetry ? View.VISIBLE : View.GONE);
        binding.resultStatus.setVisibility(View.VISIBLE);
    }

    private void toggleFilters() {
        if (binding.filterPanel.getVisibility() == View.VISIBLE) {
            hideKeyboard();
            binding.filterPanel.setVisibility(View.GONE);
            return;
        }
        hideKeyboard();
        binding.filterPanel.setVisibility(View.VISIBLE);
        binding.minPrice.setText(minPrice == null ? "" : minPrice.toString());
        binding.maxPrice.setText(maxPrice == null ? "" : maxPrice.toString());
        populateFilter(binding.categoryFilter, categories, categoryId);
        populateFilter(binding.authorFilter, authors, authorId);
        if (filterRequestsPending == 0) {
            filterRequestsPending = (categories == null ? 1 : 0) + (authors == null ? 1 : 0);
            if (categories == null) {
                loadOptions(ApiClient.getApi(this).getCategories(), item -> item.categoryId,
                        item -> item.categoryName, true);
            }
            if (authors == null) {
                loadOptions(ApiClient.getApi(this).getAuthors(), item -> item.authorId,
                        item -> item.authorName, false);
            }
        }
        updateFilterStatus();
    }

    private <T> void loadOptions(Call<ApiResponse<List<T>>> call, Function<T, Integer> id,
                                 Function<T, String> name, boolean category) {
        call.enqueue(new ApiCallback<List<T>>() {
            @Override
            public void onSuccess(List<T> data, String message) {
                if (isFinishing() || isDestroyed()) return;
                List<FilterOption> options = new ArrayList<>();
                options.add(new FilterOption(null, getString(R.string.hieu_filter_all)));
                if (data != null) {
                    for (T item : data) {
                        if (item != null) {
                            options.add(new FilterOption(id.apply(item), name.apply(item)));
                        }
                    }
                }
                if (category) {
                    categories = options;
                    populateFilter(binding.categoryFilter, categories, categoryId);
                } else {
                    authors = options;
                    populateFilter(binding.authorFilter, authors, authorId);
                }
                filterRequestsPending--;
                updateFilterButton();
                updateFilterStatus();
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                filterRequestsPending--;
                updateFilterButton();
                updateFilterStatus();
            }
        });
    }

    private void populateFilter(Spinner spinner, List<FilterOption> options, Integer selectedId) {
        spinner.setEnabled(options != null);
        if (options == null) return;

        ArrayAdapter<FilterOption> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        for (int i = 0; i < options.size(); i++) {
            if (java.util.Objects.equals(options.get(i).id, selectedId)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void updateFilterStatus() {
        boolean ready = categories != null && authors != null;
        binding.filterStatus.setVisibility(!ready && filterRequestsPending == 0
                ? View.VISIBLE : View.GONE);
    }

    private void applyFilters() {
        binding.minPrice.setError(null);
        binding.maxPrice.setError(null);
        Double min = parsePrice(binding.minPrice);
        Double max = parsePrice(binding.maxPrice);
        if (binding.minPrice.getError() != null || binding.maxPrice.getError() != null) return;
        if (min != null && max != null && min > max) {
            binding.maxPrice.setError(getString(R.string.hieu_filter_invalid_range));
            return;
        }
        FilterOption category = (FilterOption) binding.categoryFilter.getSelectedItem();
        FilterOption author = (FilterOption) binding.authorFilter.getSelectedItem();
        if (category != null) {
            categoryId = category.id;
        }
        if (author != null) {
            authorId = author.id;
        }
        minPrice = min;
        maxPrice = max;
        finishFiltering();
    }

    private Double parsePrice(EditText input) {
        String text = input.getText().toString().trim().replace(',', '.');
        if (text.isEmpty()) return null;
        try {
            double amount = Double.parseDouble(text);
            if (Double.isFinite(amount) && amount >= 0) return amount;
        } catch (NumberFormatException ignored) {
            // Show the validation error next to the input.
        }
        input.setError(getString(R.string.hieu_filter_invalid_price));
        return null;
    }

    private void clearFilters() {
        categoryId = null;
        authorId = null;
        minPrice = null;
        maxPrice = null;
        finishFiltering();
    }

    private void finishFiltering() {
        binding.filterPanel.setVisibility(View.GONE);
        updateFilterButton();
        submitSearch();
    }

    private void renderHistory() {
        binding.historyChips.removeAllViews();
        List<String> searches = new CatalogStorage(this).searches();
        binding.historySection.setVisibility(searches.isEmpty() ? View.GONE : View.VISIBLE);
        for (String term : searches) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText(term);
            chip.setOnClickListener(view -> {
                binding.etSearch.setText(term);
                submitSearch();
            });
            binding.historyChips.addView(chip);
        }
    }

    private void addFilterChip(String label, Runnable remove) {
        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
        chip.setText(label);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(view -> {
            remove.run();
            finishFiltering();
        });
        binding.activeFilters.addView(chip);
    }

    private String optionLabel(List<FilterOption> options, Integer id, int fallback) {
        if (options != null) {
            for (FilterOption option : options) {
                if (java.util.Objects.equals(option.id, id)) return option.label;
            }
        }
        return getString(fallback, id);
    }

    private void updateFilterButton() {
        binding.activeFilters.removeAllViews();
        if (categoryId != null) addFilterChip(optionLabel(categories, categoryId,
                R.string.hieu_extra_filter_category), () -> categoryId = null);
        if (authorId != null) addFilterChip(optionLabel(authors, authorId,
                R.string.hieu_extra_filter_author), () -> authorId = null);
        if (minPrice != null) addFilterChip(getString(R.string.hieu_extra_price_from,
                CurrencyUtils.formatVnd(minPrice)), () -> minPrice = null);
        if (maxPrice != null) addFilterChip(getString(R.string.hieu_extra_price_to,
                CurrencyUtils.formatVnd(maxPrice)), () -> maxPrice = null);
        int count = (categoryId == null ? 0 : 1) + (authorId == null ? 0 : 1)
                + (minPrice == null && maxPrice == null ? 0 : 1);
        binding.btnFilters.setText(count == 0 ? getString(R.string.hieu_filter_filters)
                : getString(R.string.hieu_filter_active_filters, count));
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("keyword", keyword);
        state.putInt("sortChip", sortChip);
        state.putInt("page", page);
        state.putParcelable("scroll", binding.rvProducts.getLayoutManager().onSaveInstanceState());
        if (categoryId != null) state.putInt("categoryId", categoryId);
        if (authorId != null) state.putInt("authorId", authorId);
        if (minPrice != null) state.putDouble("minPrice", minPrice);
        if (maxPrice != null) state.putDouble("maxPrice", maxPrice);
    }

    private Integer readId(Bundle state, String key) {
        return state.containsKey(key) ? state.getInt(key) : null;
    }

    private Double readPrice(Bundle state, String key) {
        return state.containsKey(key) ? state.getDouble(key) : null;
    }

    @Override
    protected void onDestroy() {
        if (searchRequest != null) searchRequest.cancel();
        super.onDestroy();
    }

    private static final class FilterOption {
        private final Integer id;
        private final String label;

        private FilterOption(Integer id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label == null ? "" : label;
        }
    }
}
