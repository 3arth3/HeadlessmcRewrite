package io.github.headlesshq.headlessmc.auth;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import io.github.headlesshq.headlessmc.api.HasName;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a validated Minecraft account, supporting both Premium (Microsoft) 
 * and Offline (Cracked) authentication modes.
 */
@Getter
@Setter
@AllArgsConstructor
public class ValidatedAccount implements HasName {

    @Nullable
    private StepFullJavaSession.FullJavaSession session;
    private String xuid;
    private String name;
    private String uuid;

    /**
     * Constructor for Premium accounts.
     * @param session The authenticated session from Microsoft/Mojang.
     * @param xuid The Xbox User ID.
     */
    public ValidatedAccount(StepFullJavaSession.FullJavaSession session, String xuid) {
        this.session = session;
        this.xuid = xuid;
        this.name = session.getMcProfile().getName();
        this.uuid = session.getMcProfile().getId().toString();
    }

    /**
     * Constructor for Offline (Cracked) accounts.
     * @param name The display name for the offline player.
     * @param uuid The generated UUID for the offline player.
     * @param xuid The dummy or zeroed XUID.
     */
    public ValidatedAccount(String name, String uuid, String xuid) {
        this.session = null;
        this.name = name;
        this.uuid = uuid;
        this.xuid = xuid;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }

    /**
     * Serializes the account information to a JsonObject for persistence.
     * @return JsonObject containing session or offline player data.
     */
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        if (session != null) {
            jsonObject.add("session", MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.toJson(session));
        } else {
            jsonObject.addProperty("offline_name", name);
            jsonObject.addProperty("offline_uuid", uuid);
        }
        jsonObject.addProperty("xuid", xuid);
        return jsonObject;
    }

    /**
     * Deserializes an account from a JsonObject.
     * @param jsonObject The stored account data.
     * @return A ValidatedAccount instance (Premium or Offline).
     */
    public static ValidatedAccount fromJson(JsonObject jsonObject) {
        if (jsonObject.has("session")) {
            StepFullJavaSession.FullJavaSession session = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN
                .fromJson(jsonObject.get("session").getAsJsonObject());
            return new ValidatedAccount(session, jsonObject.get("xuid").getAsString());
        } else {
            return new ValidatedAccount(
                jsonObject.get("offline_name").getAsString(),
                jsonObject.get("offline_uuid").getAsString(),
                jsonObject.get("xuid").getAsString()
            );
        }
    }
 }
        
