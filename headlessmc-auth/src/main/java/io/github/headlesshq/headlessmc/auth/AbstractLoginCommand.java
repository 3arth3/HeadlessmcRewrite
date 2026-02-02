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
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

@CustomLog
public abstract class AbstractLoginCommand extends AbstractCommand {

    private final List<Thread> threads = new CopyOnWriteArrayList<>();

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

                    // SỬA LỖI TẠI ĐÂY: Sử dụng MinecraftAuth.JAVA_EDITION
                    // Hằng số này thay thế cho JAVA_EDITION_BUILDER trong bản 5.x
                    MinecraftToken mcToken = MinecraftAuth.JAVA_EDITION
                            .withHttpClient(httpClient)
                            .withDeviceCode(msaDeviceCode -> {
                                ctx.log("Please go to " + msaDeviceCode.getDirectVerificationUri() 
                                        + " and enter the code: " + msaDeviceCode.getUserCode());
                            })
                            .build();

                    MinecraftProfile mcProfile = MinecraftAuth.JAVA_EDITION
                            .withHttpClient(httpClient)
                            .getProfile(mcToken);

                    onSuccessfulLogin(mcProfile, mcToken);
                } catch (Exception e) {
                    if (e instanceof InterruptedException || e.getCause() instanceof InterruptedException) {
                        ctx.log("Login process cancelled.");
                    } else {
                        ctx.log("Login failed: " + e.getMessage());
                        log.error("Authentication error", e);
                    }
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
            
