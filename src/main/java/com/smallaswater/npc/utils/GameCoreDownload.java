package com.smallaswater.npc.utils;

import org.powernukkitx.Server;
import org.powernukkitx.math.NukkitMath;
import org.powernukkitx.plugin.Plugin;
import com.google.common.util.concurrent.AtomicDouble;
import com.smallaswater.npc.RsNPC;
import lombok.NonNull;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Utility class for automatically downloading the GameCore dependency
 */
public class GameCoreDownload {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36";

    // each task downloads 128 kb of data
    private static final int THRESHOLD = 128 * 1024;

    public static final String MINIMUM_GAME_CORE_VERSION = "1.6.11";
    private static String ACTUAL_MINIMUM_GAME_CORE_VERSION;

    private static final String MAVEN_URL_CENTRAL = "https://repo1.maven.org/maven2/";
    private static final String MAVEN_URL_HUAWEI = "https://repo.huaweicloud.com/repository/maven/";
    private static final String MAVEN_URL_LANINK = "https://repo.lanink.cn/repository/maven-public/";

    private static final List<String> GAME_CORE_URL_LIST;

    static {
        //re-check the full GameCore version here to account for differences between the compile-time dependency and the actual runtime environment
        ACTUAL_MINIMUM_GAME_CORE_VERSION = MINIMUM_GAME_CORE_VERSION.split("-")[0];
        String codename = Server.getInstance().getCodename();
        if ("PowerNukkitX".equalsIgnoreCase(codename)/* || "PowerNukkit".equalsIgnoreCase(codename)*/) {
            ACTUAL_MINIMUM_GAME_CORE_VERSION += "-PNX";
        } else if ("MOT".equalsIgnoreCase(codename) || "PM1E".equalsIgnoreCase(codename)) {
            ACTUAL_MINIMUM_GAME_CORE_VERSION += "-PM1E";
        }

        GAME_CORE_URL_LIST = Collections.unmodifiableList(Arrays.asList(
                getGameCoreUrl(MAVEN_URL_CENTRAL),
                getGameCoreUrl(MAVEN_URL_HUAWEI),
                getGameCoreUrl(MAVEN_URL_LANINK)
        ));
    }

    private static String getGameCoreUrl(String mavenUrl) {
        //full plugin download URL
        return mavenUrl + "cn/lanink/MemoriesOfTime-GameCore/" + ACTUAL_MINIMUM_GAME_CORE_VERSION + "/MemoriesOfTime-GameCore-" + ACTUAL_MINIMUM_GAME_CORE_VERSION + ".jar";
    }

    private GameCoreDownload() {
        throw new RuntimeException("error");
    }

    /**
     * Check and download the GameCore dependency
     *
     * @return 0 - GameCore is loaded and up to date    1 - GameCore could not be downloaded     2 - downloaded successfully
     */
    public static int checkAndDownload() {
        return checkAndDownload(0);
    }

    /**
     * Check and download the GameCore dependency
     *
     * @param retry the retry count (download URL index)
     * @return 0 - GameCore is loaded and up to date    1 - GameCore could not be downloaded     2 - downloaded successfully
     */
    private static int checkAndDownload(int retry) {
        if (retry >= GAME_CORE_URL_LIST.size()) {
            return 1;
        }
        String url = GAME_CORE_URL_LIST.get(retry);

        Plugin plugin = Server.getInstance().getPluginManager().getPlugin("MemoriesOfTime-GameCore");

        if (plugin != null) {
            if (!VersionUtils.checkMinimumVersion(plugin, ACTUAL_MINIMUM_GAME_CORE_VERSION)) {
                RsNPC.getInstance().getLogger().warning("MemoriesOfTime-GameCore依赖版本太低！正在尝试更新版本...");
                File file = getPluginFile(plugin);
                if (file != null) {
                    Server.getInstance().getPluginManager().disablePlugin(plugin);
                    ClassLoader classLoader = plugin.getClass().getClassLoader();
                    try {
                        if (classLoader instanceof URLClassLoader) {
                            ((URLClassLoader) classLoader).close();
                        }
                    } catch (IOException ignored) {

                    }
                    file.delete();
                }else {
                    RsNPC.getInstance().getLogger().error("删除旧版本失败！请手动删除！");
                }
            }
        }

        if (plugin == null || plugin.isDisabled()) {
            RsNPC.getInstance().getLogger().info("尝试从 " + url + " 下载 MemoriesOfTime-GameCore 中...");

            File file = new File(Server.getInstance().getFilePath() + "/plugins/MemoriesOfTime-GameCore-" + ACTUAL_MINIMUM_GAME_CORE_VERSION + ".jar");

            try {
                AtomicDouble last = new AtomicDouble(-16);
                download(url, file, (len, fullLength) -> {
                    double d = NukkitMath.round(len * 1.0 / fullLength * 100, 2);
                    if (d - last.get() > 15) { // report progress every 15%
                        RsNPC.getInstance().getLogger().info("已下载：" + d + "%");
                        last.set(d);
                    }
                });
            } catch (Exception e) {
                RsNPC.getInstance().getLogger().error("MemoriesOfTime-GameCore依赖下载失败！");
                return checkAndDownload(++retry);
            }

            RsNPC.getInstance().getLogger().info("MemoriesOfTime-GameCore依赖下载成功！");
            Server.getInstance().getPluginManager().loadPlugin(file);
            return 2;
        }
        return 0;
    }

    public static File getPluginFile(Plugin plugin) {
        File file = null;
        ClassLoader PluginClass = plugin.getClass().getClassLoader();
        try {
            if (PluginClass instanceof URLClassLoader) {
                URLClassLoader pluginClass = (URLClassLoader) PluginClass;
                URL url = pluginClass.getURLs()[0];
                file = new File(URLDecoder.decode(url.getFile(), "UTF-8"));
            }
        } catch (UnsupportedEncodingException ignored) {

        }
        return file;
    }

    /**
     * Download
     *
     * @param strUrl   the target URL
     * @param saveFile the file to save to
     * @param callback the callback invoked as the download progresses
     */
    private static void download(String strUrl, File saveFile, BiConsumer<Long, Long> callback) throws Exception {
        URL url = new URL(strUrl);
        HttpURLConnection connection = ((HttpURLConnection) url.openConnection());
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Connection", "keep-alive");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setReadTimeout(5000);


        long fullLength = connection.getContentLength();
        if ("chunked".equals(connection.getHeaderField("Transfer-Encoding"))) { // chunked transfer uses single-threaded download
            RandomAccessFile out = new RandomAccessFile(saveFile, "rw");
            out.seek(0);
            byte[] b = new byte[1024];
            InputStream in = connection.getInputStream();
            int read;
            long count = 0;
            while ((read = in.read(b)) >= 0) {
                out.write(b, 0, read);
                count += read;
                if (callback != null) {
                    callback.accept(count, fullLength);
                }
            }
            in.close();
            out.close();
            return;
        }
        ForkJoinPool pool = new ForkJoinPool();
        AtomicLong atomicLong = new AtomicLong();
        pool.submit(new DownloadTask(strUrl,0, fullLength, saveFile, (l) -> {
            atomicLong.addAndGet(l);
            callback.accept(atomicLong.get(), fullLength);
        }));
        pool.shutdown();
        // synchronize: wait for all threads to finish
        while (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
        }
        if (fullLength < 1 || saveFile.length() < 1) {
            throw new Exception("下载失败");
        }
    }

    private static class DownloadTask extends RecursiveAction {

        private final String strUrl;
        private final File file;
        private final long start;
        private final long end;

        private final Consumer<Integer> callback;

        public DownloadTask(@NonNull String strUrl, long start, long end, File file, Consumer<Integer> callback) {
            this.strUrl = strUrl;
            this.start = start;
            this.end = end;
            this.file = file;
            this.callback = callback;
        }

        @Override
        protected void compute() {
            RandomAccessFile out = null;
            InputStream in = null;
            try {
                long l = end - start;
                if (l < THRESHOLD) {
                    HttpURLConnection connection = getConnection();
                    connection.setRequestProperty("Range", "bytes=" + start + "-" + end);

                    out = new RandomAccessFile(file, "rw");
                    out.seek(start);
                    in = connection.getInputStream();
                    byte[] b = new byte[1024];
                    int len;
                    while ((len = in.read(b)) >= 0) {
                        out.write(b, 0, len);
                        callback.accept(len);
                    }
                    in.close();
                    out.close();
                } else {
                    long mid = (start + end) / 2;
                    new SubDownloadTask(strUrl, start, mid, file, callback).fork();
                    new SubDownloadTask(strUrl, mid, end, file, callback).fork();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public HttpURLConnection getConnection() throws IOException {
            HttpURLConnection connection = (HttpURLConnection) new URL(strUrl).openConnection();
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("User-Agent", USER_AGENT);

            return connection;
        }
    }

    private static class SubDownloadTask extends DownloadTask {

        public SubDownloadTask(@NonNull String strUrl, long start, long end, File file, Consumer<Integer> callback) {
            super(strUrl, start, end, file, callback);
        }

    }

}

