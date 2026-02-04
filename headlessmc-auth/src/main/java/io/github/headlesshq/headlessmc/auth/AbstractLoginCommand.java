package io.github.headlesshq.headlessmc.auth;

import lombok.CustomLog;
import lombok.Setter;
import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.command.AbstractCommand;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.model.MinecraftProfile;
import net.raphimc.minecraftauth.java.model.MinecraftToken;
import net.raphimc.minecraftauth.java.request.MinecraftProfileRequest;
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.model.MsaToken;
import net.raphimc.minecraftauth.msa.request.MsaDeviceCodeRequest;
import net.raphimc.minecraftauth.msa.request.MsaDeviceCodeTokenRequest;
import net.raphimc.minecraftauth.xbl.data.XblConstants;
import net.raphimc.minecraftauth.xbl.model.XblUserToken;
import net.raphimc.minecraftauth.xbl.model.XblXstsToken;
import net.raphimc.minecraftauth.xbl.request.XblUserAuthenticateRequest;
import net.raphimc.minecraftauth.xbl.request.XblXstsAuthorizeRequest;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

@CustomLog
public abstract class AbstractLoginCommand extends AbstractCommand {

    private final List<Thread> threads = new CopyOnWriteArrayList<>();
    private final MsaApplicationConfig JAVA_CONFIG = new MsaApplicationConfig("00000000402b5328", "service::user.auth.xboxlive.com::MBI_SSL");

    @Setter
    protected Supplier<HttpClient> httpClientFactory = MinecraftAuth::createHttpClient;

    public AbstractLoginCommand(HeadlessMc ctx) {
        this(ctx, "login", "Logs you into an account.");
    }

    public AbstractLoginCommand(HeadlessMc ctx, String name, String description) {
        super(ctx, name, description);
    }

    protected abstract void onSuccessfulLogin(MinecraftProfile profile, MinecraftToken token);

    @Override
    public void execute(String line, String... args) throws CommandException {
        if (args.length > 1 && args[1].equalsIgnoreCase("-cancel")) {
            cancelLoginProcess(args);
        } else {
            loginWithDeviceCode(args);
        }
    }

    protected void loginWithDeviceCode(String... args) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    HttpClient httpClient = httpClientFactory.get();

                    MsaDeviceCodeRequest deviceCodeReq = new MsaDeviceCodeRequest(JAVA_CONFIG);
                    MsaDeviceCode deviceCode = deviceCodeReq.handle(httpClient.execute(deviceCodeReq));
                    
                    ctx.log("Please go to " + deviceCode.getDirectVerificationUri() + " and enter the code: " + deviceCode.getUserCode());

                    MsaToken msaToken = null;
                    while (msaToken == null && !isInterrupted()) {
                        try {
                            MsaDeviceCodeTokenRequest msaTokenReq = new MsaDeviceCodeTokenRequest(JAVA_CONFIG, deviceCode);
                            msaToken = msaTokenReq.handle(httpClient.execute(msaTokenReq));
                        } catch (Exception e) {
                            Thread.sleep(5000);
                        }
                    }

                    if (msaToken == null) return;

                    XblUserAuthenticateRequest xblReq = new XblUserAuthenticateRequest(JAVA_CONFIG, msaToken);
                    XblUserToken xblToken = xblReq.handle(httpClient.execute(xblReq));

                    XblXstsAuthorizeRequest xstsReq = new XblXstsAuthorizeRequest(null, xblToken, null, XblConstants.JAVA_XSTS_RELYING_PARTY);
                    XblXstsToken xstsToken = xstsReq.handle(httpClient.execute(xstsReq));

                    // SỬA LỖI TẠI ĐÂY: Sử dụng constructor đúng (long, String, String)
                    // Vì không có MinecraftJavaTokenRequest, ta khởi tạo MinecraftToken từ dữ liệu XSTS
                    MinecraftToken mcToken = new MinecraftToken(
                        xstsToken.getExpireTimeMs(), 
                        "Bearer", 
                        xstsToken.getToken()
                    );

                    MinecraftProfileRequest profileReq = new MinecraftProfileRequest(mcToken);
                    MinecraftProfile profile = profileReq.handle(httpClient.execute(profileReq));

                    ctx.log("Logged in as: " + profile.getName() + " (" + profile.getId() + ")");
                    onSuccessfulLogin(profile, mcToken);

                } catch (InterruptedException e) {
                    ctx.log("Login process cancelled.");
                } catch (Exception e) {
                    ctx.log("Login failed: " + e.getMessage());
                    log.error("Authentication error", e);
                } finally {
                    threads.remove(this);
                }
            }
        };
        startLoginThread(thread);
    }

    protected void cancelLoginProcess(String... args) throws CommandException {
        if (args.length <= 2) {
            throw new CommandException("Please specify the login process id!");
        }
        String targetName = "HMC Login Thread - " + args[2];
        for (Thread thread : threads) {
            if (targetName.equals(thread.getName())) {
                thread.interrupt();
                threads.remove(thread);
                ctx.log("Cancelled login process " + args[2] + ".");
                return;
            }
        }
        ctx.log("Failed to find login process with id " + args[2] + "!");
    }

    protected void startLoginThread(Thread thread) {
        int threadId = 0;
        String name;
        synchronized (threads) {
            do {
                name = "HMC Login Thread - " + threadId++;
            } while (hasThreadWithName(name));
            thread.setName(name);
            thread.setDaemon(true);
            threads.add(thread);
        }
        ctx.log("Starting login process " + (threadId - 1) + ". Use 'login -cancel " + (threadId - 1) + "' to abort.");
        thread.start();
        ctx.getExitManager().addTaskThread(thread);
    }

    protected boolean hasThreadWithName(String threadName) {
        return threads.stream().anyMatch(t -> threadName.equals(t.getName()));
    }
}
    
