package com.webbansach.app.ui.catalog_hieu;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.webbansach.app.R;
import com.webbansach.app.core.network.ApiCallback;
import com.webbansach.app.core.network.ApiClient;
import com.webbansach.app.core.utils.Constants;
import com.webbansach.app.core.utils.CurrencyUtils;
import com.webbansach.app.databinding.ActivityMh06DetailBinding;
import com.webbansach.app.models.AddToCartDto;
import com.webbansach.app.models.ApiResponse;
import com.webbansach.app.models.CartDto;
import com.webbansach.app.models.PagedResponse;
import com.webbansach.app.models.ProductDto;
import com.webbansach.app.models.ProductReviewDto;
import com.webbansach.app.ui.catalog_hieu.adapter.HomeRowAdapter;
import com.webbansach.app.ui.order_tien.CartActivity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

public class BookDetailActivity extends AppCompatActivity {
    private static final int REVIEW_PAGE_SIZE = 5;

    private ActivityMh06DetailBinding binding;
    private int productId;
    private ProductDto product;
    private Call<ApiResponse<ProductDto>> detailRequest;
    private Call<ApiResponse<PagedResponse<ProductReviewDto>>> reviewRequest;
    private boolean addingToCart;

    private final List<String> images = new ArrayList<>();
    private int imageIndex;
    private int quantity = 1;
    private boolean descriptionExpanded;
    private final List<ProductReviewDto> reviews = new ArrayList<>();
    private HomeRowAdapter<ProductReviewDto> reviewAdapter;
    private int nextReviewPage = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMh06DetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        if (savedInstanceState != null) {
            quantity = savedInstanceState.getInt("quantity", 1);
            descriptionExpanded = savedInstanceState.getBoolean("descriptionExpanded");
        }
        binding.quantityLess.setOnClickListener(view -> changeQuantity(-1));
        binding.quantityMore.setOnClickListener(view -> changeQuantity(1));
        binding.ivBookCover.setOnClickListener(view -> showCover());
        binding.ivBookCover.setFocusable(true);
        binding.toggleDescription.setOnClickListener(view -> {
            descriptionExpanded = !descriptionExpanded;
            updateDescription();
        });
        productId = getIntent().getIntExtra(Constants.EXTRA_PRODUCT_ID, -1);
        imageIndex = savedInstanceState == null ? 0 : savedInstanceState.getInt("imageIndex");
        binding.btnBack.setOnClickListener(view -> finish());
        binding.btnCart.setOnClickListener(view -> startActivity(new Intent(this, CartActivity.class)));
        binding.btnAddToCart.setOnClickListener(view -> addToCart());
        binding.btnRetry.setOnClickListener(view -> loadDetail());
        binding.btnPreviousImage.setOnClickListener(view -> showImage(imageIndex - 1));
        binding.btnNextImage.setOnClickListener(view -> showImage(imageIndex + 1));
        binding.btnMoreReviews.setOnClickListener(view -> loadReviews());

        reviewAdapter = new HomeRowAdapter<>(R.layout.item_hieu_detail_review, this::bindReview);
        binding.rvReviews.setAdapter(reviewAdapter);
        updateCartButton();
        if (productId > 0) {
            loadDetail();
        } else {
            showDetailError(getString(R.string.hieu_detail_missing), false);
        }
    }

    private void loadDetail() {
        if (detailRequest != null) {
            return;
        }
        binding.detailStatus.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.VISIBLE);
        detailRequest = ApiClient.getApi(this).getProductDetail(productId);
        detailRequest.enqueue(new ApiCallback<ProductDto>() {
            @Override
            public void onSuccess(ProductDto data, String message) {
                if (!finishDetail()) {
                    return;
                }
                if (data == null) {
                    showDetailError(getString(R.string.hieu_detail_load_error), true);
                    return;
                }
                product = data;
                renderProduct();
                loadReviews();
            }

            @Override
            public void onError(String message) {
                if (finishDetail()) {
                    showDetailError(message, true);
                }
            }
        });
    }

    private boolean finishDetail() {
        if (isClosing()) {
            return false;
        }
        detailRequest = null;
        binding.progressBar.setVisibility(View.GONE);
        return true;
    }

    private void showDetailError(String message, boolean canRetry) {
        binding.statusMessage.setText(message);
        binding.btnRetry.setVisibility(canRetry ? View.VISIBLE : View.GONE);
        binding.detailStatus.setVisibility(View.VISIBLE);
    }

    private void changeQuantity(int change) {
        if (!canAddToCart() || addingToCart) return;
        quantity = Math.max(1, Math.min(product.stockQuantity, quantity + change));
        updateCartButton();
    }

    private void showCover() {
        if (images.isEmpty()) return;
        android.widget.ImageView image = new android.widget.ImageView(this);
        image.setAdjustViewBounds(true);
        image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription(binding.ivBookCover.getContentDescription());
        Glide.with(this).load(images.get(imageIndex)).into(image);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(product.title)
                .setView(image)
                .setPositiveButton(R.string.hieu_extra_close, null)
                .show();
    }

    private void updateDescription() {
        binding.tvDescription.setMaxLines(descriptionExpanded ? Integer.MAX_VALUE : 5);
        binding.toggleDescription.setText(descriptionExpanded
                ? R.string.hieu_extra_read_less : R.string.hieu_extra_read_more);
    }

    private void renderProduct() {
        new CatalogStorage(this).rememberBook(product);
        quantity = Math.max(1, Math.min(quantity, product.stockQuantity));
        binding.tvTitle.setText(product.title);
        if (TextUtils.isEmpty(product.authorName)) {
            binding.tvAuthor.setVisibility(View.GONE);
        } else {
            binding.tvAuthor.setText(getString(R.string.hieu_detail_author, product.authorName));
        }
        binding.tvFinalPrice.setText(CurrencyUtils.formatVnd(product.finalPrice));
        boolean discounted = product.price > product.finalPrice;
        binding.tvOriginalPrice.setVisibility(discounted ? View.VISIBLE : View.GONE);
        binding.tvOriginalPrice.setText(CurrencyUtils.formatVnd(product.price));
        binding.tvOriginalPrice.setPaintFlags(
                binding.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        binding.tvDiscount.setVisibility(discounted && product.discountPercent > 0
                ? View.VISIBLE : View.GONE);
        binding.tvDiscount.setText(getString(R.string.hieu_detail_discount,
                NumberFormat.getNumberInstance(getResources().getConfiguration().getLocales().get(0))
                        .format(product.discountPercent)));
        binding.ratingBar.setRating((float) product.averageRating);
        binding.tvRating.setText(getString(R.string.hieu_detail_rating,
                product.averageRating, product.reviewCount));
        binding.tvDescription.setText(TextUtils.isEmpty(product.description)
                ? getString(R.string.hieu_detail_no_description) : product.description);
        binding.stockStatus.setText(!product.isActive ? R.string.hieu_detail_inactive
                : R.string.hieu_detail_out);
        if (canAddToCart()) {
            binding.stockStatus.setText(getString(R.string.hieu_detail_stock, product.stockQuantity));
        }
        updateDescription();
        binding.tvDescription.post(() -> {
            android.text.Layout layout = binding.tvDescription.getLayout();
            boolean longText = layout != null && (layout.getLineCount() > 5
                    || (layout.getLineCount() > 0 && layout.getEllipsisCount(layout.getLineCount() - 1) > 0));
            binding.toggleDescription.setVisibility(longText ? View.VISIBLE : View.GONE);
        });
        renderSpecs();
        renderImages();
        updateCartButton();
        binding.scrollView.setVisibility(View.VISIBLE);
    }

    private void renderSpecs() {
        List<String> specs = new ArrayList<>();
        addSpec(specs, R.string.hieu_detail_category, product.categoryName);
        addSpec(specs, R.string.hieu_detail_publisher, product.publisher);
        addSpec(specs, R.string.hieu_detail_year, product.publishYear);
        if (product.pageCount > 0) {
            specs.add(getString(R.string.hieu_detail_pages, product.pageCount));
        }
        addSpec(specs, R.string.hieu_detail_isbn, product.isbn);
        addSpec(specs, R.string.hieu_detail_language, product.language);
        addSpec(specs, R.string.hieu_detail_dimensions, product.dimensions);
        binding.tvSpecs.setText(TextUtils.join("\n", specs));
        binding.specsCard.setVisibility(specs.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void addSpec(List<String> specs, int label, String value) {
        if (!TextUtils.isEmpty(value)) {
            specs.add(getString(label, value));
        }
    }

    private void renderImages() {
        addImage(product.mainImageUrl);
        if (product.imageUrls != null) {
            for (String url : product.imageUrls) {
                addImage(url);
            }
        }
        binding.ivBookCover.setContentDescription(getString(R.string.hieu_detail_cover, product.title));
        binding.galleryControls.setVisibility(images.size() > 1 ? View.VISIBLE : View.GONE);
        showImage(Math.min(imageIndex, Math.max(0, images.size() - 1)));
    }

    private void addImage(String path) {
        if (!TextUtils.isEmpty(path)) {
            String url = CurrencyUtils.resolveImageUrl(path);
            if (!images.contains(url)) {
                images.add(url);
            }
        }
    }

    private void showImage(int index) {
        imageIndex = index;
        Glide.with(this).load(images.isEmpty() ? null : images.get(index))
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(binding.ivBookCover);
        binding.imagePosition.setText(getString(R.string.hieu_detail_image_position,
                index + 1, images.size()));
        binding.btnPreviousImage.setEnabled(index > 0);
        binding.btnNextImage.setEnabled(index < images.size() - 1);
    }

    private void loadReviews() {
        if (reviewRequest != null) {
            return;
        }
        binding.reviewStatus.setVisibility(View.GONE);
        binding.reviewProgress.setVisibility(View.VISIBLE);
        binding.btnMoreReviews.setVisibility(View.GONE);
        int requestedPage = nextReviewPage;
        reviewRequest = ApiClient.getApi(this)
                .getProductReviews(productId, requestedPage, REVIEW_PAGE_SIZE);
        reviewRequest.enqueue(new ApiCallback<PagedResponse<ProductReviewDto>>() {
            @Override
            public void onSuccess(PagedResponse<ProductReviewDto> data, String message) {
                if (!finishReviews()) {
                    return;
                }
                if (data == null) {
                    showReviewError(message);
                    return;
                }
                if (data.items != null) {
                    reviews.addAll(data.items);
                }
                reviewAdapter.submit(reviews);
                if (reviewAdapter.getItemCount() == 0) {
                    binding.reviewStatus.setText(R.string.hieu_detail_no_reviews);
                    binding.reviewStatus.setVisibility(View.VISIBLE);
                }
                nextReviewPage = requestedPage + 1;
                binding.btnMoreReviews.setText(R.string.hieu_detail_more);
                binding.btnMoreReviews.setVisibility(requestedPage < data.totalPages
                        ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                if (finishReviews()) {
                    showReviewError(message);
                }
            }
        });
    }

    private boolean finishReviews() {
        if (isClosing()) {
            return false;
        }
        reviewRequest = null;
        binding.reviewProgress.setVisibility(View.GONE);
        return true;
    }

    private void showReviewError(String message) {
        binding.reviewStatus.setText(getString(R.string.hieu_detail_review_error, message));
        binding.reviewStatus.setVisibility(View.VISIBLE);
        binding.btnMoreReviews.setText(R.string.hieu_detail_retry);
        binding.btnMoreReviews.setVisibility(View.VISIBLE);
    }

    private void bindReview(HomeRowAdapter.Row row, ProductReviewDto review) {
        row.setText(R.id.reviewAuthor, TextUtils.isEmpty(review.customerName)
                ? getString(R.string.hieu_detail_review_author) : review.customerName);
        row.setText(R.id.reviewRating, getString(R.string.hieu_detail_review_rating, review.rating));
        row.setText(R.id.reviewComment, review.comment);
    }

    private boolean canAddToCart() {
        return product != null && product.isActive && product.stockQuantity > 0;
    }

    private void updateCartButton() {
        binding.quantityValue.setText(String.valueOf(quantity));
        binding.quantityLess.setEnabled(canAddToCart() && !addingToCart && quantity > 1);
        binding.quantityMore.setEnabled(canAddToCart() && !addingToCart && quantity < product.stockQuantity);
        binding.btnAddToCart.setEnabled(canAddToCart() && !addingToCart);
        int label = R.string.hieu_detail_add;
        if (addingToCart) {
            label = R.string.hieu_detail_adding;
        } else if (product != null && !canAddToCart()) {
            label = product.isActive ? R.string.hieu_detail_out : R.string.hieu_detail_inactive;
        }
        binding.btnAddToCart.setText(label);
    }

    private void addToCart() {
        if (!canAddToCart() || addingToCart) {
            return;
        }
        addingToCart = true;
        updateCartButton();
        ApiClient.getApi(this).addToCart(new AddToCartDto(productId, quantity))
                .enqueue(new ApiCallback<CartDto>() {
                    @Override
                    public void onSuccess(CartDto data, String message) {
                        finishAddToCart(getString(R.string.hieu_detail_added));
                    }

                    @Override
                    public void onError(String message) {
                        finishAddToCart(message);
                    }
                });
    }

    private void finishAddToCart(String message) {
        if (!isClosing()) {
            addingToCart = false;
            updateCartButton();
            com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), message,
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.layoutBottomAction)
                    .setAction(R.string.hieu_detail_cart, view ->
                            startActivity(new Intent(this, CartActivity.class))).show();
        }
    }

    private boolean isClosing() {
        return isFinishing() || isDestroyed();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("imageIndex", imageIndex);
        state.putInt("quantity", quantity);
        state.putBoolean("descriptionExpanded", descriptionExpanded);
    }

    @Override
    protected void onDestroy() {
        if (detailRequest != null) detailRequest.cancel();
        if (reviewRequest != null) reviewRequest.cancel();
        super.onDestroy();
    }
}
