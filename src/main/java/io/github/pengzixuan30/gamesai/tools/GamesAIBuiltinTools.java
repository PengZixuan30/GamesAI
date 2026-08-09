package io.github.pengzixuan30.gamesai.tools;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.pengzixuan30.gamesai.GamesAI;
import io.github.pengzixuan30.gamesai.translations.GamesAITranslations;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;

public class GamesAIBuiltinTools {

    @GamesAIToolsRegister.RegisterTool(
        name = "get_online_players",
        description = "获取服务器当前的在线玩家列表"
    )
    public static String getOnlinePlayers(Consumer<String> feedback, String aiName) {
        feedback.accept(aiName + GamesAITranslations.tr("tools.games_ai.get_online_players"));

        MinecraftServer server = GamesAI.getServer();
        if (server == null) return "服务器尚未启动";

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return "当前没有在线玩家";

        List<String> names = new ArrayList<>();
        for (ServerPlayer player : players) {
            names.add(player.getGameProfile().name());
        }

        return "在线玩家 (" + names.size() + " 人): " + String.join(", ", names);
    }

    @GamesAIToolsRegister.RegisterTool(
        name = "get_whitelist_name",
        description = "获取服务器的白名单列表"
    )
    public static String getWhitelistName(Consumer<String> feedback, String aiName) {
        feedback.accept(aiName + GamesAITranslations.tr("tools.games_ai.get_whitelist_name"));

        MinecraftServer server = GamesAI.getServer();
        if (server == null) return "服务器尚未启动";

        UserWhiteList whitelist = server.getPlayerList().getWhiteList();
        List<String> names = new ArrayList<>();
        for (UserWhiteListEntry entry : whitelist.getEntries()) {
            names.add(entry.getUser().name());
        }

        if (names.isEmpty()) return "白名单为空";
        return "白名单 (" + names.size() + " 人): " + String.join(", ", names);
    }

    @GamesAIToolsRegister.RegisterTool(
        name = "add_to_whitelist",
        description = "在白名单中添加一名玩家, 推荐在添加之前先查询白名单",
        parameters = """
            {
              "type": "object",
              "properties": {
                "player": {
                  "type": "string",
                  "description": "要添加到白名单的玩家名称"
                }
              },
              "required": ["player"]
            }
            """
    )
    public static String addToWhitelist(Consumer<String> feedback, String aiName, String player) {
        feedback.accept(aiName + GamesAITranslations.tr("tools.games_ai.add_to_whitelist", player));

        MinecraftServer server = GamesAI.getServer();
        if (server == null) return "服务器尚未启动";

        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack(), "whitelist add " + player);

        return "已将 " + player + " 添加到白名单";
    }

    @GamesAIToolsRegister.RegisterTool(
        name = "remove_from_whitelist",
        description = "删除一名白名单中的玩家, 推荐在删除之前先查询白名单",
        parameters = """
            {
              "type": "object",
              "properties": {
                "player": {
                  "type": "string",
                  "description": "要从白名单移除的玩家名称"
                }
              },
              "required": ["player"]
            }
            """
    )
    public static String removeFromWhitelist(Consumer<String> feedback, String aiName, String player) {
        feedback.accept(aiName + GamesAITranslations.tr("tools.games_ai.remove_from_whitelist", player));

        MinecraftServer server = GamesAI.getServer();
        if (server == null) return "服务器尚未启动";

        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack(), "whitelist remove " + player);

        return "已将 " + player + " 从白名单移除";
    }

    @GamesAIToolsRegister.RegisterTool(
        name = "search_minecraft_wiki",
        description = "搜索Minecraft Wiki以获取相关信息, 请不要使用此方法搜索与Minecraft无关的东西。如果返回了Search results页面, 你可以通过先浏览此页面, 再进行一次精确查询",
        parameters = """
            {
              "type": "object",
              "properties": {
                "query": {
                  "type": "string",
                  "description": "要搜索的内容，例如某个物品、怪物、机制等的名称"
                }
              },
              "required": ["query"]
            }
            """
    )
    public static String searchMinecraftWiki(Consumer<String> feedback, String aiName, String query) throws Exception {
        feedback.accept(aiName + GamesAITranslations.tr("tools.games_ai.search_minecraft_wiki", query));

        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://minecraft.wiki/api.php?action=query&list=search"
                + "&srsearch=" + encoded
                + "&format=json&srlimit=5";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "GamesAI-Minecraft-Mod/1.0")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonObject queryObj = root.getAsJsonObject("query");
        JsonArray results = queryObj.getAsJsonArray("search");

        if (results.isEmpty()) {
            return "未找到与 \"" + query + "\" 相关的结果";
        }

        StringBuilder sb = new StringBuilder("Minecraft Wiki 搜索结果:\n\n");
        for (int i = 0; i < results.size(); i++) {
            JsonObject r = results.get(i).getAsJsonObject();
            String title = r.get("title").getAsString();
            String snippet = r.get("snippet").getAsString().replaceAll("<[^>]+>", "");
            sb.append(i + 1).append(". ").append(title).append("\n");
            sb.append("   ").append(snippet).append("\n\n");
        }

        return sb.toString().trim();
    }

    @GamesAIToolsRegister.RegisterTool(
        name = "calculator",
        description = "计算一个数学表达式, 只能使用数字和+-*/()运算符",
        parameters = """
            {
              "type": "object",
              "properties": {
                "expression": {
                  "type": "string",
                  "description": "要计算的数学表达式"
                }
              },
              "required": ["expression"]
            }
            """
    )
    public static String calculator(Consumer<String> feedback, String aiName, String expression) {
        feedback.accept(aiName + GamesAITranslations.tr("tools.games_ai.calculator", expression));

        if (!expression.matches("[0-9+\\-*/().%\\s]+")) {
            return "表达式包含不允许的字符，只能使用数字和 + - * / ( ) 运算符";
        }

        try {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
            if (engine != null) {
                Object result = engine.eval(expression);
                return expression + " = " + result;
            }
            return expression + " = " + evaluateSimple(expression);
        } catch (Exception e) {
            return "计算错误: " + e.getMessage();
        }
    }

    @GamesAIToolsRegister.RegisterTool(
        name = "item_calculator",
        description = "计算一个数学表达式, 并将结果转换为Minecraft物品单位（潜影盒、组、个）。自动适用堆叠数，未指定时默认为 64",
        parameters = """
            {
              "type": "object",
              "properties": {
                "expression": {
                  "type": "string",
                  "description": "要计算的数学表达式"
                },
                "single_limit": {
                  "type": "integer",
                  "description": "单个物品的最大堆叠数，默认为 64"
                }
              },
              "required": ["expression"]
            }
            """
    )
    public static String itemCalculator(Consumer<String> feedback, String aiName,
                                         String expression, double singleLimit) {
        feedback.accept(aiName + GamesAITranslations.tr("tools.games_ai.item_calculator", expression));

        int limit = (singleLimit > 0) ? (int) singleLimit : 64;

        try {
            double raw;
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
            if (engine != null) {
                raw = ((Number) engine.eval(expression)).doubleValue();
            } else {
                raw = evaluateSimple(expression);
            }

            int total = (int) Math.round(raw);
            int perBox = 27 * limit;
            int boxes = total / perBox;
            int rem = total % perBox;
            int stacks = rem / limit;
            int items = rem % limit;

            StringBuilder sb = new StringBuilder(expression);
            sb.append(" = ").append(total).append(" 个");
            sb.append("\n换算 (每组 ").append(limit).append(" 个):\n");
            if (boxes > 0) sb.append("  ").append(boxes).append(" 潜影盒");
            if (stacks > 0) sb.append(boxes > 0 ? " + " : "  ").append(stacks).append(" 组");
            if (items > 0 || (boxes == 0 && stacks == 0)) {
                sb.append((boxes > 0 || stacks > 0) ? " + " : "  ").append(items).append(" 个");
            }

            return sb.toString();
        } catch (Exception e) {
            return "计算错误: " + e.getMessage();
        }
    }

    @GamesAIToolsRegister.RegisterTool(
        name = "ai_read_data",
        description = "读取公共数据中的键值对, 输入key以获取对应的value, 推荐在读取之前先查看现有的key都有哪些",
        parameters = """
            {
              "type": "object",
              "properties": {
                "key": {
                  "type": "string",
                  "description": "要读取的数据的键"
                }
              },
              "required": ["key"]
            }
            """
    )
    public static String aiReadData(Consumer<String> feedback, String aiName, String key) {
        feedback.accept(aiName + GamesAITranslations.tr("tools.games_ai.ai_read_data", key));

        String value = GamesAI.getDatabase().readData(key);
        if (value == null) return "键 " + key + " 不存在";
        return "键 " + key + " 的值为 " + value;
    }

    @GamesAIToolsRegister.RegisterTool(
        name = "ai_read_all_keys",
        description = "读取公共数据中的所有键"
    )
    public static String aiReadAllKeys(Consumer<String> feedback, String aiName) {
        feedback.accept(aiName + GamesAITranslations.tr("tools.games_ai.ai_read_all_keys"));

        List<String> keys = GamesAI.getDatabase().getAllKeys();
        if (keys.isEmpty()) return "数据库中没有数据";
        return "当前所有的键有: " + String.join(", ", keys);
    }

    @GamesAIToolsRegister.RegisterTool(
        name = "ai_write_data",
        description = "向公共数据中写入键值对(新增/覆写模式), 输入key和value以写入数据, 注意写入方式为覆写, 需避免覆盖重要数据, 数据不存在时将自动创建",
        parameters = """
            {
              "type": "object",
              "properties": {
                "key": {
                  "type": "string",
                  "description": "要写入的数据的键"
                },
                "value": {
                  "type": "string",
                  "description": "要写入的数据的值"
                }
              },
              "required": ["key", "value"]
            }
            """
    )
    public static String aiWriteData(Consumer<String> feedback, String aiName, String key, String value) {
        feedback.accept(aiName + GamesAITranslations.tr("tools.games_ai.ai_write_data", key, value));
        if (!GamesAI.getDatabase().writeData(key, value)) return "写入数据失败";
        return "已将键 " + key + " 的值写入 " + value;
    }

    @GamesAIToolsRegister.RegisterTool(
        name = "ai_add_data",
        description = "向公共数据中追加数据(新增/追加模式), 输入key和value以追加数据, 数据将被追加到原数据的末尾, 不存在时自动创建",
        parameters = """
            {
              "type": "object",
              "properties": {
                "key": {
                  "type": "string",
                  "description": "要追加数据的键"
                },
                "value": {
                  "type": "string",
                  "description": "要追加的数据的值"
                }
              },
              "required": ["key", "value"]
            }
            """
    )
    public static String aiAddData(Consumer<String> feedback, String aiName, String key, String value) {
        feedback.accept(aiName + GamesAITranslations.tr("tools.games_ai.ai_add_data", key, value));

        String oldValue = GamesAI.getDatabase().readData(key);
        String newValue = (oldValue == null) ? value : oldValue + value;
        if (!GamesAI.getDatabase().writeData(key, newValue)) return "追加数据失败";
        return "已将键 " + key + " 的值增加 " + value + ", 当前值为 " + newValue;
    }

    @GamesAIToolsRegister.RegisterTool(
        name = "ai_del_data",
        description = "从公共数据中删除数据, 输入key以删除对应的数据, 注意删除后无法恢复, 即使key不存在, 也仍然会进行删除",
        parameters = """
            {
              "type": "object",
              "properties": {
                "key": {
                  "type": "string",
                  "description": "要删除的数据的键"
                }
              },
              "required": ["key"]
            }
            """
    )
    public static String aiDelData(Consumer<String> feedback, String aiName, String key) {
        feedback.accept(aiName + GamesAITranslations.tr("tools.games_ai.ai_del_data", key));
        if (!GamesAI.getDatabase().deleteData(key)) return "删除数据失败";
        return "已删除键 " + key + " 的数据";
    }

    @GamesAIToolsRegister.RegisterTool(
        name = "read_skills",
        description = "阅读技能指导文件, 调用多个工具前必备, 每次只能读取一个 skills, 读取前请先通过已注册的 Skills 列表确认文件名",
        parameters = """
            {
              "type": "object",
              "properties": {
                "skills": {
                  "type": "string",
                  "description": "你要阅读的技能文件的文件名"
                }
              },
              "required": ["skills"]
            }
            """
    )
    public static String readSkills(Consumer<String> feedback, String aiName, String skills) {
        feedback.accept(aiName + GamesAITranslations.tr("tools.games_ai.read_skills", skills));

        java.nio.file.Path skillsDir = net.fabricmc.loader.api.FabricLoader.getInstance()
                .getConfigDir().resolve("games_ai").resolve("skills");
        java.nio.file.Path file = skillsDir.resolve(skills);

        // 安全检查：防止路径穿越
        if (!file.normalize().startsWith(skillsDir.normalize())) {
            return "非法的文件名: " + skills;
        }

        if (!java.nio.file.Files.exists(file)) {
            return "技能文件不存在: " + skills;
        }

        try {
            return java.nio.file.Files.readString(file);
        } catch (java.io.IOException e) {
            return "读取技能文件失败: " + e.getMessage();
        }
    }

    @GamesAIToolsRegister.RegisterTool(
        name = "setting_timer",
        description = "设置一个计时器, 等待这段时间之后再执行下一步操作",
        parameters = """
            {
              "type": "object",
              "properties": {
                "duration": {
                  "type": "number",
                  "description": "等待的时长（秒）"
                }
              },
              "required": ["duration"]
            }
            """
    )
    public static String settingTimer(Consumer<String> feedback, String aiName, double duration) {
        long millis = (long) (duration * 1000);
        feedback.accept(aiName + GamesAITranslations.tr("tools.games_ai.setting_timer", duration));
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "计时器被中断";
        }
        return "计时器结束，已等待 " + duration + " 秒";
    }

    @GamesAIToolsRegister.RegisterTool(
        name = "reload_plugin",
        description = "热重载插件以应用配置、工具和语言文件的更改"
    )
    public static String reloadPlugin(Consumer<String> feedback, String aiName) {
        feedback.accept(aiName + GamesAITranslations.tr("tools.games_ai.reload_plugin"));
        GamesAI.reload();
        return "插件已重载";
    }

    private static double evaluateSimple(String expr) {
        return new SimpleParser(expr.replaceAll("\\s+", "")).parse();
    }

    private static final class SimpleParser {
        private final String s;
        private int i;

        SimpleParser(String s) { this.s = s; this.i = 0; }

        double parse() { return expr(); }

        private double expr() {
            double x = term();
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == '+') { i++; x += term(); }
                else if (c == '-') { i++; x -= term(); }
                else break;
            }
            return x;
        }

        private double term() {
            double x = factor();
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == '*') { i++; x *= factor(); }
                else if (c == '/') { i++; x /= factor(); }
                else if (c == '%') { i++; x %= factor(); }
                else break;
            }
            return x;
        }

        private double factor() {
            if (i >= s.length()) return 0;
            char c = s.charAt(i);
            if (c == '(') {
                i++;
                double v = expr();
                if (i < s.length() && s.charAt(i) == ')') i++;
                return v;
            }
            if (c == '-') { i++; return -factor(); }
            int start = i;
            while (i < s.length()
                    && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) {
                i++;
            }
            return Double.parseDouble(s.substring(start, i));
        }
    }
}
