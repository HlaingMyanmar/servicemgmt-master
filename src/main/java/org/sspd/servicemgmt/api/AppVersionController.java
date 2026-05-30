package org.sspd.servicemgmt.api;

import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/app")
public class AppVersionController {

    /**
     * Mobile app မှ version check လုပ်ရန် endpoint။
     * versionCode နှင့် versionName ကို release တင်တိုင်း update လုပ်ပါ။
     */
    @GetMapping("/version")
    public ResponseEntity<ApiResponse<AppVersionResponse>> getVersion() {
        AppVersionResponse v = new AppVersionResponse();
        v.setVersionCode(4);                   // ← release တင်တိုင်း increment
        v.setVersionName("1.0.4-stable");      // ← versionName နဲ့ ကိုက်ညီရမည်
        v.setForceUpdate(false);               // true → dialog dismiss မရ
        v.setChangelog(
            "• ကုန်ကျစရိတ် / ဝင်ငွေ date bug ပြင်ဆင်ပြီ\n" +
            "• Login screen animation အသစ်\n" +
            "• Loading animation (ကွန်ပျူတာ animation) ထည့်ပြီ\n" +
            "• Security update"
        );
        // APK download URL — internal server မှ serve လုပ်လျှင် ဖြည့်ပါ
        v.setDownloadUrl("");

        return ResponseEntity.ok(new ApiResponse<>(true, "OK", v));
    }

    @Data
    public static class AppVersionResponse {
        private int    versionCode;
        private String versionName;
        private boolean forceUpdate;
        private String changelog;
        private String downloadUrl;
    }
}
