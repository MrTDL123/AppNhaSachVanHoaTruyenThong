package com.webbansach.app.ui.auth_long;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.webbansach.app.core.local.TokenManager;
import com.webbansach.app.core.network.ApiCallback;
import com.webbansach.app.core.network.ApiClient;
import com.webbansach.app.databinding.ActivityMh01SplashBinding;
import com.webbansach.app.models.AuthResponseDto;
import com.webbansach.app.models.CustomerLoginDto;
import com.webbansach.app.ui.catalog_hieu.HomeActivity;

/**
 * MH01 - Splash Screen (Màn hình khởi động)
 * Phụ trách: Trần Duy Long
 *
 * Chức năng:
 * - Hiển thị logo và tên ứng dụng trong 2 giây
 * - Kiểm tra trạng thái đăng nhập (TokenManager.isLoggedIn())
 * - Nếu đã đăng nhập -> chuyển thẳng đến HomeActivity (MH04)
 * - Nếu chưa đăng nhập -> chuyển đến LoginActivity (MH02)
 */
public class SplashActivity extends AppCompatActivity {

    private ActivityMh01SplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMh01SplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Delay 2 giây rồi kiểm tra đăng nhập
//        new Handler(Looper.getMainLooper()).postDelayed(() -> {
//            TokenManager tokenManager = TokenManager.getInstance(this);
//            Intent intent;
//            if (tokenManager.isLoggedIn()) {
//                intent = new Intent(SplashActivity.this, HomeActivity.class);
//            } else {
//                intent = new Intent(SplashActivity.this, LoginActivity.class);
//            }
//            startActivity(intent);
//            finish();
//        }, 2000);

        // Tự động gọi API đăng nhập tài khoản test để lấy Token thật
        autoLoginAndOpenHome();
    }

    private void autoLoginAndOpenHome() {
        Log.d("TEST_AUTH", "Đang tự động đăng nhập tài khoản test...");
        // Đóng gói email và password mẫu
        CustomerLoginDto loginDto = new CustomerLoginDto("jackacevn@gmail.com", "123456");
        // Gọi API POST /api/Auth/mobile-login
        ApiClient.getApi(this).mobileLogin(loginDto).enqueue(new ApiCallback<AuthResponseDto>() {
            @Override
            public void onSuccess(AuthResponseDto data, String message) {
                // 1. Lưu AccessToken & RefreshToken thật vào TokenManager
                TokenManager tokenManager = TokenManager.getInstance(SplashActivity.this);
                tokenManager.saveTokens(data.accessToken, data.refreshToken);
                tokenManager.saveUserInfo(data);
                Log.d("TEST_AUTH", "Đăng nhập thành công! Token: " + data.accessToken);
                Toast.makeText(SplashActivity.this, "Đăng nhập tự động thành công!", Toast.LENGTH_SHORT).show();
                // 2. Chuyển thẳng sang HomeActivity với phiên đăng nhập hợp lệ
                Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
            @Override
            public void onError(String errorMessage) {
                Log.e("TEST_AUTH", "Đăng nhập thất bại: " + errorMessage);
                Toast.makeText(SplashActivity.this, "Lỗi đăng nhập: " + errorMessage, Toast.LENGTH_LONG).show();
                // Nếu có lỗi mạng vẫn mở HomeActivity để bạn quan sát lỗi
                Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
