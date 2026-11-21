package com.webbansach.app.ui.catalog_hieu;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.webbansach.app.R;
import com.webbansach.app.core.network.ApiCallback;
import com.webbansach.app.core.network.ApiClient;
import com.webbansach.app.core.utils.Constants;
import com.webbansach.app.databinding.ActivityMh14ReviewBinding;
import com.webbansach.app.models.ApiResponse;
import com.webbansach.app.models.CreateReviewDto;
import com.webbansach.app.models.OrderDto;
import com.webbansach.app.models.OrderItemDto;
import com.webbansach.app.models.ProductReviewDto;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

public class WriteReviewActivity extends AppCompatActivity {
    private static final int COMPLETED_ORDER = 3;

    private ActivityMh14ReviewBinding binding;
    private Submission submission;
    private Call<ApiResponse<OrderDto>> orderRequest;
    private int orderId;
    private int productId;
    private CatalogStorage storage;
    private int draftProductId = -1;
    private boolean restoringDraft;
    private boolean reviewSent;
    private boolean discardDraft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMh14ReviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        orderId = getIntent().getIntExtra(Constants.EXTRA_ORDER_ID, -1);
        int incomingProduct = getIntent().getIntExtra(Constants.EXTRA_PRODUCT_ID, -1);
        productId = savedInstanceState == null
                ? incomingProduct : savedInstanceState.getInt("productId", incomingProduct);
        storage = new CatalogStorage(this);
        submission = new ViewModelProvider(this).get(Submission.class);
        submission.state.observe(this, this::renderSubmission);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmExit();
            }
        });
        binding.etComment.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {
                saveDraft();
            }
            @Override public void afterTextChanged(android.text.Editable text) {}
        });
        binding.toolbar.setNavigationOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
        binding.btnRetryOrder.setOnClickListener(view -> loadOrder());
        binding.btnSubmitReview.setOnClickListener(view -> submitReview());
        binding.ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            updateRatingMeaning();
            if (fromUser) {
                binding.submitError.setVisibility(View.GONE);
                saveDraft();
            }
        });

        if (orderId <= 0) {
            showOrderError(getString(R.string.hieu_review_missing_order), false);
        } else if (incomingProduct > 0) {
            showBook(incomingProduct, getIntent().getStringExtra(Constants.EXTRA_PRODUCT_TITLE));
        } else {
            loadOrder();
        }
    }

    private void switchDraft(int id) {
        if (draftProductId == id) {
            productId = id;
            return;
        }
        saveDraft();
        productId = id;
        draftProductId = id;
        restoringDraft = true;
        binding.ratingBar.setRating(id > 0 ? storage.draftRating(orderId, id) : 0);
        binding.etComment.setText(id > 0 ? storage.draftComment(orderId, id) : "");
        restoringDraft = false;
        updateRatingMeaning();
    }

    private void updateRatingMeaning() {
        int[] labels = {R.string.hieu_extra_rating_0, R.string.hieu_extra_rating_1,
                R.string.hieu_extra_rating_2, R.string.hieu_extra_rating_3,
                R.string.hieu_extra_rating_4, R.string.hieu_extra_rating_5};
        int stars = Math.max(0, Math.min(5, Math.round(binding.ratingBar.getRating())));
        binding.ratingMeaning.setText(labels[stars]);
    }

    private void saveDraft() {
        if (submission != null && submission.state.getValue() != null
                && submission.state.getValue().success) return;
        if (storage == null || restoringDraft || reviewSent || discardDraft
                || draftProductId <= 0 || orderId <= 0) return;
        String comment = binding.etComment.getText() == null ? "" : binding.etComment.getText().toString();
        storage.saveDraft(orderId, draftProductId, Math.round(binding.ratingBar.getRating()), comment);
    }

    private void confirmExit() {
        boolean hasDraft = draftProductId > 0 && (binding.ratingBar.getRating() > 0
                || !TextUtils.isEmpty(binding.etComment.getText()));
        if (!hasDraft || reviewSent) {
            finish();
            return;
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.hieu_extra_draft_exit)
                .setMessage(R.string.hieu_extra_draft_hint)
                .setPositiveButton(R.string.hieu_extra_save_exit, (dialog, which) -> {
                    saveDraft();
                    finish();
                })
                .setNegativeButton(R.string.hieu_extra_stay, null)
                .setNeutralButton(R.string.hieu_extra_discard, (dialog, which) -> {
                    discardDraft = true;
                    storage.clearDraft(orderId, draftProductId);
                    finish();
                }).show();
    }

    @Override
    protected void onPause() {
        saveDraft();
        super.onPause();
    }

    private void loadOrder() {
        if (orderRequest != null) {
            return;
        }
        binding.orderStatus.setVisibility(View.GONE);
        binding.btnRetryOrder.setVisibility(View.GONE);
        binding.orderProgress.setVisibility(View.VISIBLE);

        orderRequest = ApiClient.getApi(this).getOrderDetail(orderId);
        orderRequest.enqueue(new ApiCallback<OrderDto>() {
            @Override
            public void onSuccess(OrderDto order, String message) {
                if (finishOrderLoad()) {
                    renderOrder(order);
                }
            }

            @Override
            public void onError(String message) {
                if (finishOrderLoad()) {
                    showOrderError(message, true);
                }
            }
        });
    }

    private boolean finishOrderLoad() {
        if (isFinishing() || isDestroyed()) {
            return false;
        }
        orderRequest = null;
        binding.orderProgress.setVisibility(View.GONE);
        return true;
    }

    private void renderOrder(OrderDto order) {
        if (order == null) {
            showOrderError(getString(R.string.hieu_review_order_error), true);
            return;
        }
        if (order.status != COMPLETED_ORDER) {
            showOrderError(getString(R.string.hieu_review_incomplete), false);
            return;
        }

        List<OrderItemDto> books = new ArrayList<>();
        if (order.items != null) {
            for (OrderItemDto item : order.items) {
                if (item != null && item.productId > 0) {
                    books.add(item);
                }
            }
        }
        if (books.isEmpty()) {
            showOrderError(getString(R.string.hieu_review_no_books), false);
        } else if (books.size() == 1) {
            OrderItemDto item = books.get(0);
            showBook(item.productId, item.title);
        } else {
            showBookPicker(books);
        }
    }

    private void showBook(int id, String title) {
        switchDraft(id);
        binding.tvProductTitle.setText(bookTitle(id, title));
        binding.reviewForm.setVisibility(View.VISIBLE);
        renderSubmission(submission.state.getValue());
    }

    private void showBookPicker(List<OrderItemDto> books) {
        List<String> titles = new ArrayList<>();
        titles.add(getString(R.string.hieu_review_select));
        int selected = 0;
        for (OrderItemDto book : books) {
            titles.add(bookTitle(book.productId, book.title));
            if (book.productId == productId && selected == 0) {
                selected = titles.size() - 1;
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, titles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.tvProductTitle.setVisibility(View.GONE);
        binding.bookPicker.setVisibility(View.VISIBLE);
        binding.bookPicker.setAdapter(adapter);
        binding.bookPicker.setSelection(selected);
        switchDraft(selected == 0 ? -1 : books.get(selected - 1).productId);
        binding.bookPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedProduct = position == 0 ? -1 : books.get(position - 1).productId;
                if (productId != selectedProduct) {
                    switchDraft(selectedProduct);
                    renderSubmission(null);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                switchDraft(-1);
                renderSubmission(null);
            }
        });
        binding.reviewForm.setVisibility(View.VISIBLE);
        renderSubmission(submission.state.getValue());
    }

    private String bookTitle(int id, String title) {
        return TextUtils.isEmpty(title) ? getString(R.string.hieu_review_unknown_book, id) : title;
    }

    private void showOrderError(String message, boolean retry) {
        binding.orderStatus.setText(message);
        binding.orderStatus.setVisibility(View.VISIBLE);
        binding.btnRetryOrder.setVisibility(retry ? View.VISIBLE : View.GONE);
    }

    private void submitReview() {
        if (productId <= 0 || orderId <= 0 || submission.isSending()) {
            return;
        }
        int rating = Math.round(binding.ratingBar.getRating());
        if (rating < 1 || rating > 5) {
            showSubmitError(getString(R.string.hieu_review_rating_required));
            return;
        }
        String comment = binding.etComment.getText() == null
                ? "" : binding.etComment.getText().toString().trim();

        InputMethodManager keyboard = getSystemService(InputMethodManager.class);
        keyboard.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
        CreateReviewDto review = new CreateReviewDto(productId, orderId, rating, comment);
        submission.sentProductId = productId;
        CatalogStorage draftStorage = storage;
        int submittedOrder = orderId;
        int submittedProduct = productId;
        submission.send(ApiClient.getApi(getApplicationContext()).createReview(review),
                () -> draftStorage.clearDraft(submittedOrder, submittedProduct));
    }

    private void renderSubmission(SubmissionResult result) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        boolean sending = submission.isSending();
        binding.ratingBar.setEnabled(productId > 0 && !sending);
        binding.etComment.setEnabled(productId > 0 && !sending);
        binding.bookPicker.setEnabled(!sending);
        binding.btnSubmitReview.setEnabled(productId > 0 && !sending);
        binding.btnSubmitReview.setText(sending ? R.string.hieu_review_sending : R.string.hieu_review_submit);
        binding.submitProgress.setVisibility(sending ? View.VISIBLE : View.GONE);
        binding.submitError.setVisibility(View.GONE);
        if (result == null || sending) {
            return;
        }
        if (result.success) {
            reviewSent = true;
            storage.clearDraft(orderId, submission.sentProductId);
            Toast.makeText(this, R.string.hieu_review_sent, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            showSubmitError(TextUtils.isEmpty(result.message)
                    ? getString(R.string.hieu_review_submit_error) : result.message);
        }
    }

    private void showSubmitError(String message) {
        binding.submitError.setText(message);
        binding.submitError.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("productId", productId);
    }

    @Override
    protected void onDestroy() {
        if (orderRequest != null) {
            orderRequest.cancel();
        }
        super.onDestroy();
    }

    public static final class Submission extends ViewModel {
        private final MutableLiveData<SubmissionResult> state = new MutableLiveData<>();
        private boolean sending;
        private int sentProductId;

        private boolean isSending() {
            return sending;
        }

        private void send(Call<ApiResponse<ProductReviewDto>> call, Runnable clearDraft) {
            sending = true;
            state.setValue(null);
            call.enqueue(new ApiCallback<ProductReviewDto>() {
                @Override
                public void onSuccess(ProductReviewDto data, String message) {
                    clearDraft.run();
                    sending = false;
                    state.setValue(new SubmissionResult(true, null));
                }

                @Override
                public void onError(String message) {
                    sending = false;
                    state.setValue(new SubmissionResult(false, message));
                }
            });
        }
    }

    private static final class SubmissionResult {
        private final boolean success;
        private final String message;

        private SubmissionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}
