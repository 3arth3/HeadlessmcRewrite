package io.github.headlesshq.headlessmc.launcher.auth;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import lombok.CustomLog;
import lombok.Data;
import io.github.headlesshq.headlessmc.auth.ValidatedAccount;
import io.github.headlesshq.headlessmc.launcher.util.JsonUtil;
import io.github.headlesshq.headlessmc.launcher.util.URLs;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.requests.impl.GetRequest;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.model.MinecraftProfile;
import net.raphimc.minecraftauth.java.model.MinecraftToken;
import net.raphimc.minecraftauth.responsehandler.MinecraftResponseHandler;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import java.util.List;

/**
 * Validates that an account actually owns the game.
 */
@CustomLog
public class AccountValidator {
    private static final URL URL = URLs.url("https://api.minecraftservices.com/entitlements/mcstore");

    // Sửa tham số truyền vào từ FullJavaSession thành Profile và Token
    public ValidatedAccount validate(MinecraftProfile profile, MinecraftToken token) throws AuthException {
        log.debug("Validating session " + profile.getName() + " : " + profile.getId());
        try {
            HttpClient httpClient = MinecraftAuth.createHttpClient();
            GetRequest getRequest = new GetRequest(URL);
            
            // Sửa cách lấy Access Token: token.getToken() trả về chuỗi JWT String
            getRequest.appendHeader("Authorization", "Bearer " + token.getToken());
            
            JsonObject je = httpClient.execute(getRequest, new MinecraftResponseHandler());
            log.debug(je.toString());

            Entitlements entitlements = JsonUtil.GSON.fromJson(je, Entitlements.class);
            String xuid = null;
            for (Entitlements.Item item : entitlements.getItems()) {
                if ("game_minecraft".equals(item.getName()) || "product_minecraft".equals(item.getName())) {
                    xuid = item.parseXuid();
                    break;
                }
            }

            if (xuid == null) {
                throw new AuthException("This account does not own Minecraft!");
            }

            // Trả về ValidatedAccount mới với cấu trúc (profile, token, xuid)
            return new ValidatedAccount(profile, token, xuid);
        } catch (IOException | JsonParseException e) {
            log.error("Failed to validate " + profile.getName(), e);
            throw new AuthException("Failed to validate " + profile.getName() + ": " + e.getMessage());
        }
    }

    @Data
    @VisibleForTesting
    static class Entitlements {
        @SerializedName("items")
        private List<Item> items;

        @SerializedName("signature")
        private String signature;

        @SerializedName("keyId")
        private String keyId;

        @Data
        static class Item {
            @SerializedName("name")
            private String name;

            @SerializedName("signature")
            private String signature;

            public String parseXuid() throws AuthException, JsonSyntaxException {
                String[] split = signature.split("\\.");
                if (split.length != 3) {
                    throw new AuthException("Invalid JWT " + signature);
                }

                String payload = new String(Base64.getDecoder().decode(split[1]));
                JsonElement jsonElement = JsonParser.parseString(payload);
                String xuid = JsonUtil.getString(jsonElement, "signerId");
                if (xuid == null) {
                    throw new AuthException("Failed to find xuid for " + signature);
                }

                return xuid;
            }
        }
    }
}
        
