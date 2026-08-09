package io.github.pengzixuan30.gamesai.tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletionFunctionTool;
import com.openai.models.chat.completions.ChatCompletionTool;

import io.github.pengzixuan30.gamesai.GamesAI;

public final class GamesAIToolsRegister {

    private GamesAIToolsRegister() {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface RegisterTool {

        String name();

        String description();

        String parameters() default "{}";
    }

    private static final Map<String, ToolHandler> REGISTRY = new LinkedHashMap<>();

    public record ToolHandler(
            Method method,
            Object instance,
            String name,
            String description,
            FunctionParameters parameters,
            String[] paramNames
    ) {}

    public static void register(String name, Object instance, Method method,
                                String description, FunctionParameters params) {
        if (REGISTRY.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate tool name: " + name);
        }

        String[] paramNames = buildParamNames(method);
        ToolHandler handler = new ToolHandler(method, instance, name, description, params, paramNames);
        REGISTRY.put(name, handler);
        GamesAI.LOGGER.debug("[GamesAIToolsRegister] Registered tool: {}", name);
    }

    public static void register(String name, Method method,
                                String description, FunctionParameters params) {
        register(name, null, method, description, params);
    }

    public static List<String> scanAndRegister(Object target) {
        List<String> registered = new ArrayList<>();
        Class<?> clazz;
        Object instance;

        if (target instanceof Class<?>) {
            clazz = (Class<?>) target;
            instance = null;
        } else {
            clazz = target.getClass();
            instance = target;
        }

        for (Method method : clazz.getDeclaredMethods()) {
            RegisterTool anno = method.getAnnotation(RegisterTool.class);
            if (anno == null) continue;

            boolean isStatic = Modifier.isStatic(method.getModifiers());

            Object methodInstance = instance;
            if (methodInstance == null && !isStatic) {
                try {
                    methodInstance = clazz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    GamesAI.LOGGER.warn("[GamesAIToolsRegister] Cannot instantiate {} for non-static method {}",
                            clazz.getName(), method.getName(), e);
                    continue;
                }
            }
            if (methodInstance != null && isStatic) {
                GamesAI.LOGGER.warn("[GamesAIToolsRegister] Skipping static method {} on instance scan", method.getName());
                continue;
            }

            try {
                FunctionParameters funcParams = parseParametersJson(anno.parameters());
                register(anno.name(), methodInstance, method, anno.description(), funcParams);
                registered.add(anno.name());
            } catch (Exception e) {
                GamesAI.LOGGER.error("[GamesAIToolsRegister] Failed to register tool: {}", anno.name(), e);
            }
        }
        return registered;
    }

    public static List<ChatCompletionTool> buildToolList() {
        List<ChatCompletionTool> tools = new ArrayList<>();
        for (ToolHandler h : REGISTRY.values()) {
            var funcBuilder = FunctionDefinition.builder()
                    .name(h.name())
                    .description(h.description());
            if (h.parameters() != null) {
                funcBuilder.parameters(h.parameters());
            }
            tools.add(ChatCompletionTool.ofFunction(
                    ChatCompletionFunctionTool.builder()
                            .function(funcBuilder.build())
                            .build()
            ));
        }
        return tools;
    }

    public static String dispatch(String name, Consumer<String> feedback,
                                   String aiName, String rawArgs) throws Exception {
        ToolHandler handler = REGISTRY.get(name);
        if (handler == null) {
            return "Unknown tool: " + name;
        }

        Object[] callArgs = buildCallArgs(handler, feedback, aiName, rawArgs);
        Object result = handler.method().invoke(handler.instance(), callArgs);
        return result == null ? "" : result.toString();
    }

    public static boolean contains(String name) {
        return REGISTRY.containsKey(name);
    }

    public static int size() {
        return REGISTRY.size();
    }

    public static void clear() {
        REGISTRY.clear();
    }

    private static String[] buildParamNames(Method method) {
        Parameter[] params = method.getParameters();
        String[] names = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            names[i] = params[i].getName();
        }
        return names;
    }

    static FunctionParameters parseParametersJson(String json) {
        if (json == null || json.isBlank() || "{}".equals(json.trim())) {
            return null;
        }
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return jsonObjectToFunctionParameters(root);
    }

    private static FunctionParameters jsonObjectToFunctionParameters(JsonObject obj) {
        FunctionParameters.Builder builder = FunctionParameters.builder();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            builder.putAdditionalProperty(entry.getKey(), gsonToJsonValue(entry.getValue()));
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static JsonValue gsonToJsonValue(JsonElement element) {
        if (element.isJsonNull()) {
            return JsonValue.from((Object) null);
        } else if (element.isJsonPrimitive()) {
            JsonPrimitive prim = element.getAsJsonPrimitive();
            if (prim.isBoolean()) return JsonValue.from(prim.getAsBoolean());
            if (prim.isNumber())  return JsonValue.from(prim.getAsNumber());
            return JsonValue.from(prim.getAsString());
        } else if (element.isJsonArray()) {
            List<JsonValue> list = new ArrayList<>();
            for (JsonElement e : element.getAsJsonArray()) {
                list.add(gsonToJsonValue(e));
            }
            return JsonValue.from(list);
        } else if (element.isJsonObject()) {
            Map<String, JsonValue> map = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> e : element.getAsJsonObject().entrySet()) {
                map.put(e.getKey(), gsonToJsonValue(e.getValue()));
            }
            return JsonValue.from(map);
        }
        return JsonValue.from(element.toString());
    }

    private static Object[] buildCallArgs(ToolHandler handler, Consumer<String> feedback,
                                           String aiName, String rawArgs) {
        String[] paramNames = handler.paramNames();
        Object[] args = new Object[paramNames.length];

        JsonObject argsJson = null;
        if (rawArgs != null && !rawArgs.isBlank()) {
            try {
                argsJson = JsonParser.parseString(rawArgs).getAsJsonObject();
            } catch (Exception e) {
                GamesAI.LOGGER.warn("[GamesAIToolsRegister] Failed to parse tool args JSON: {}", rawArgs, e);
            }
        }

        for (int i = 0; i < paramNames.length; i++) {
            String name = paramNames[i];
            Class<?> type = handler.method().getParameters()[i].getType();

            if (("feedback".equals(name) || i == 0) && Consumer.class.isAssignableFrom(type)) {
                args[i] = feedback;
                continue;
            }
            if (("aiName".equals(name) || i == 1) && type == String.class) {
                args[i] = aiName;
                continue;
            }
            if (argsJson != null && argsJson.has(name)) {
                args[i] = coerceJsonValue(argsJson.get(name), type);
            } else {
                args[i] = defaultValue(type);
            }
        }
        return args;
    }

    private static Object coerceJsonValue(JsonElement element, Class<?> targetType) {
        if (element.isJsonNull()) return defaultValue(targetType);
        if (targetType == String.class) return element.getAsString();
        if (targetType == int.class || targetType == Integer.class) return element.getAsInt();
        if (targetType == long.class || targetType == Long.class) return element.getAsLong();
        if (targetType == double.class || targetType == Double.class) return element.getAsDouble();
        if (targetType == boolean.class || targetType == Boolean.class) return element.getAsBoolean();
        if (targetType == List.class && element.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonElement e : element.getAsJsonArray()) {
                list.add(coerceJsonValue(e, Object.class));
            }
            return list;
        }
        return new Gson().fromJson(element, targetType);
    }

    private static Object defaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == boolean.class) return false;
        return null;
    }
}
