package net.sistr.actionarms.client.render.gltf.manager;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.client.render.gltf.data.ModelMetadata;
import net.sistr.actionarms.client.render.gltf.data.TextureSettings;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * glTFメタデータ管理クラス
 * JSONメタデータの読み込みと管理を行う
 */
public class GltfMetadataManager implements ResourceReloader, AutoCloseable {
    public static final GltfMetadataManager INSTANCE = new GltfMetadataManager();

    private final Gson gson = new Gson();

    private Map<Identifier, ModelMetadata> modelMetadata = Map.of();

    private GltfMetadataManager() {
    }

    @Override
    public CompletableFuture<Void> reload(Synchronizer synchronizer, ResourceManager manager,
                                          Profiler prepareProfiler, Profiler applyProfiler,
                                          Executor prepareExecutor, Executor applyExecutor) {
        return CompletableFuture.supplyAsync(() -> {
                    prepareProfiler.startTick();
                    prepareProfiler.push("loading_gltf_metadata");

                    var metadataResources = manager.findResources("gltf/models",
                            id -> id.getPath().endsWith(".json"));
                    Map<Identifier, ModelMetadata> metadata = new HashMap<>();

                    for (var entry : metadataResources.entrySet()) {
                        Identifier resourceId = entry.getKey();
                        Resource resource = entry.getValue();

                        try (var reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                            JsonObject json = gson.fromJson(reader, JsonObject.class);
                            ModelMetadata parsedMetadata = parseMetadata(resourceId, json);

                            // ファイル名からメタデータIDを生成 (例: gltf/models/m1873.json → m1873)
                            String path = resourceId.getPath();
                            String metadataId = path.substring("gltf/models/".length(), path.length() - ".json".length());
                            Identifier metadataKey = new Identifier(resourceId.getNamespace(), metadataId);

                            metadata.put(metadataKey, parsedMetadata);
                            ActionArms.LOGGER.debug("Loaded glTF metadata: {} -> {}", metadataKey, parsedMetadata.modelPath());
                        } catch (IOException | JsonParseException e) {
                            ActionArms.LOGGER.error("Failed to load glTF metadata: {}", resourceId, e);
                        }
                    }

                    prepareProfiler.pop();
                    prepareProfiler.endTick();
                    return metadata;
                }, prepareExecutor)
                .thenCompose(synchronizer::whenPrepared)
                .thenAcceptAsync(metadata -> {
                    applyProfiler.startTick();
                    applyProfiler.push("applying_gltf_metadata");

                    this.modelMetadata = ImmutableMap.copyOf(metadata);

                    ActionArms.LOGGER.info("Loaded {} glTF metadata entries", metadata.size());

                    applyProfiler.pop();
                    applyProfiler.endTick();
                }, applyExecutor);
    }

    /**
     * JSONからModelMetadataをパース
     * 実際のファイル形式に完全対応
     */
    private ModelMetadata parseMetadata(Identifier resourceId, JsonObject json) {
        try {
            // 1. "model"フィールドの解析（必須）
            if (!json.has("model")) {
                throw new JsonParseException("Missing required 'model' field in metadata: " + resourceId);
            }
            String modelPathStr = json.get("model").getAsString();
            Identifier modelPath = new Identifier(modelPathStr);

            // 2. "scene_index"フィールドの解析（オプション、デフォルト0）
            int sceneIndex = json.has("scene_index") ? json.get("scene_index").getAsInt() : 0;

            // 3. "hide_bone_keys"フィールドの解析
            Map<String, List<String>> hideBoneKeys = parseHideBoneKeys(json);

            // 4. "texture_settings"フィールドの解析
            TextureSettings textureSettings = parseTextureSettings(json);

            return ModelMetadata.builder(modelPath)
                    .sceneIndex(sceneIndex)
                    .hideBoneKeys(hideBoneKeys)
                    .textureSettings(textureSettings)
                    .build();

        } catch (Exception e) {
            ActionArms.LOGGER.error("Error parsing metadata for {}: {}", resourceId, e.getMessage());
            throw new JsonParseException("Failed to parse metadata: " + resourceId, e);
        }
    }

    /**
     * "hide_bone_keys"フィールドを解析
     */
    private Map<String, List<String>> parseHideBoneKeys(JsonObject json) {
        Map<String, List<String>> hideBoneKeys = new HashMap<>();

        if (!json.has("hide_bone_keys")) {
            return hideBoneKeys;
        }

        JsonObject hideBoneKeysObj = json.getAsJsonObject("hide_bone_keys");
        for (var entry : hideBoneKeysObj.entrySet()) {
            String contextKey = entry.getKey(); // "fpv", "inventory" など
            List<String> hideList = new ArrayList<>();

            for (JsonElement element : entry.getValue().getAsJsonArray()) {
                hideList.add(element.getAsString());
            }

            hideBoneKeys.put(contextKey, List.copyOf(hideList));
        }

        return hideBoneKeys;
    }

    /**
     * "texture_settings"フィールドを解析
     */
    private TextureSettings parseTextureSettings(JsonObject json) {
        TextureSettings.Builder builder = TextureSettings.builder();

        if (!json.has("texture_settings")) {
            return builder.build();
        }

        JsonObject textureSettingsObj = json.getAsJsonObject("texture_settings");

        // "texture_map"の解析
        if (textureSettingsObj.has("texture_map")) {
            JsonObject textureMapObj = textureSettingsObj.getAsJsonObject("texture_map");
            Map<String, Identifier> textureMap = new HashMap<>();

            for (var entry : textureMapObj.entrySet()) {
                String textureFileName = entry.getKey();      // "skin_alex.png"
                String resourcePathStr = entry.getValue().getAsString(); // "actionarms:textures/test/skin_alex.png"
                textureMap.put(textureFileName, new Identifier(resourcePathStr));
            }

            builder.textureMap(textureMap);
        }

        // "dynamic_textures"の解析
        if (textureSettingsObj.has("dynamic_textures")) {
            JsonObject dynamicTexturesObj = textureSettingsObj.getAsJsonObject("dynamic_textures");
            Map<String, String> dynamicTextures = new HashMap<>();

            for (var entry : dynamicTexturesObj.entrySet()) {
                String textureFileName = entry.getKey();    // "skin_alex.png"
                String contextName = entry.getValue().getAsString(); // "player_skin"
                dynamicTextures.put(textureFileName, contextName);
            }

            builder.dynamicTextures(dynamicTextures);
        }

        return builder.build();
    }

    @Override
    public void close() {
        // クリーンアップ処理（必要に応じて実装）
    }

    /**
     * メタデータIDに対応するModelMetadataを取得
     *
     * @param metadataId メタデータID
     * @return ModelMetadata、見つからない場合はOptional.empty()
     */
    public Optional<ModelMetadata> getModelMetadata(Identifier metadataId) {
        return Optional.ofNullable(modelMetadata.get(metadataId));
    }
}