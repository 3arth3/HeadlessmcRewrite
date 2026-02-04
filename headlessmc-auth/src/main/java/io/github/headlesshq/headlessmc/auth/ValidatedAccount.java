package io.github.headlesshq.headlessmc.auth;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import io.github.headlesshq.headlessmc.api.HasName;
import net.raphimc.minecraftauth.java.model.MinecraftProfile;
import net.raphimc.minecraftauth.java.model.MinecraftToken;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class ValidatedAccount implements HasName {

    @Nullable
    private MinecraftProfile profile;
    @Nullable
    private MinecraftToken token;
    private String xuid;
    private String name;
    private String uuid;

    public ValidatedAccount(MinecraftProfile profile, MinecraftToken token, String xuid) {
        this.profile = profile;
        this.token = token;
        this.xuid = xuid;
        this.name = profile.getName();
        this.uuid = profile.getId().toString();
    }

    public ValidatedAccount(String name, String uuid, String xuid) {
        this.profile = null;
        this.token = null;
        this.name = name;
        this.uuid = uuid;
        this.xuid = xuid;
    }

    // add getter
    public ValidatedAccount getSession() {
        return (profile != null && token != null) ? this : null;
    }

    // add getter
    public MinecraftProfile getMcProfile() {
        return profile;
    }

    public MinecraftToken getMcToken() {
        return token;
    }

    @Override
    public String getName() {
        return name;
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        if (profile != null && token != null) {
            jsonObject.add("profile", MinecraftProfile.toJson(profile));
            jsonObject.add("token", MinecraftToken.toJson(token));
        } else {
            jsonObject.addProperty("offline_name", name);
            jsonObject.addProperty("offline_uuid", uuid);
        }
        jsonObject.addProperty("xuid", xuid);
        return jsonObject;
    }

    public static ValidatedAccount fromJson(JsonObject jsonObject) {
        if (jsonObject.has("profile") && jsonObject.has("token")) {
            MinecraftProfile profile = MinecraftProfile.fromJson(jsonObject.get("profile").getAsJsonObject());
            MinecraftToken token = MinecraftToken.fromJson(jsonObject.get("token").getAsJsonObject());
            return new ValidatedAccount(profile, token, jsonObject.get("xuid").getAsString());
        } else {
            return new ValidatedAccount(
                jsonObject.get("offline_name").getAsString(),
                jsonObject.get("offline_uuid").getAsString(),
                jsonObject.get("xuid").getAsString()
            );
        }
    }
}
                
