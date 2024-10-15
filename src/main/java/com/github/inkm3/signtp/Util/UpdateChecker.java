package com.github.inkm3.signtp.Util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class UpdateChecker {

    private static Plugin plugin;
    private static UpdateChecker instance;
    private final String repositoryUrl;
    private final String currentVersion;
    private final boolean updateCheckDisabled;

    private UpdateChecker(Plugin plugin) {
        UpdateChecker.plugin = plugin;
        this.repositoryUrl = "https://github.com/Inkm3/SignTP-Modoki";
        this.currentVersion = plugin.getPluginMeta().getVersion();
        this.updateCheckDisabled = Boolean.getBoolean("disableUpdateCheck");
    }

    public static UpdateChecker getInstance(JavaPlugin plugin) {
        if (instance == null) {
            instance = new UpdateChecker(plugin);
        }
        return instance;
    }

    public void checkForUpdates() {
        if (updateCheckDisabled) {
            return;
        }

        try {
            // リポジトリURLをAPIのURLに変換
            String apiUrl = convertToApiUrl(repositoryUrl);
            // GitHub APIからリリース情報を取得
            String jsonResponse = getApiResponse(apiUrl);
            // バージョン一覧を解析
            List<String> versions = parseVersions(jsonResponse);

            // 最新バージョンを取得
            Optional<String> latestVersion = versions.stream()
                    .max(this::compareVersions);

            // アップデートがある場合はコンソールに表示
            if (latestVersion.isPresent() && compareVersions(latestVersion.get(), currentVersion) > 0) {
                plugin.getLogger().info("新しいバージョン " + latestVersion.get() + " が利用可能です。現在のバージョン: " + currentVersion);
            }

        } catch (Exception e) {

            plugin.getLogger().warning("アップデートの確認中にエラーが発生しました: " + e.getMessage());
        }
    }

    private String convertToApiUrl(String repositoryUrl) {
        // リポジトリのオーナーと名前を抽出
        String[] parts = repositoryUrl.replace("https://github.com/", "").split("/");
        String owner = parts[0];
        String repo = parts[1];
        // GitHub APIのURLを生成
        return "https://api.github.com/repos/" + owner + "/" + repo + "/releases";
    }

    private String getApiResponse(String apiUrl) throws IOException, InterruptedException {
        try(HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/vnd.github.v3+json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        }
    }

    private List<String> parseVersions(String jsonResponse) {
        List<String> versions = new ArrayList<>();

        int index = 0;
        while ((index = jsonResponse.indexOf("\"tag_name\"", index)) != -1) {
            index = jsonResponse.indexOf(":", index) + 1;
            while (Character.isWhitespace(jsonResponse.charAt(index))) {
                index++;
            }
            if (jsonResponse.charAt(index) == '"') {
                index++;
                int end = jsonResponse.indexOf('"', index);
                if (end != -1) {
                    String tagName = jsonResponse.substring(index, end);
                    versions.add(tagName);
                    index = end;
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        return versions;
    }

    private int compareVersions(String v1, String v2) {
        // バージョン文字列を数値部分だけに変換し、ドットで分割
        String[] v1Parts = v1.replaceAll("[^0-9.]", "").split("\\.");
        String[] v2Parts = v2.replaceAll("[^0-9.]", "").split("\\.");

        int length = Math.max(v1Parts.length, v2Parts.length);

        for (int i = 0; i < length; i++) {
            int v1Num = i < v1Parts.length && !v1Parts[i].isEmpty() ? Integer.parseInt(v1Parts[i]) : 0;
            int v2Num = i < v2Parts.length && !v2Parts[i].isEmpty() ? Integer.parseInt(v2Parts[i]) : 0;

            if (v1Num != v2Num) {
                return Integer.compare(v1Num, v2Num);
            }
        }
        return 0;
    }
}