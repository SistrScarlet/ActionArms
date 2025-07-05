package net.sistr.actionarms.client.render.gltf.manager;

import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.client.render.gltf.renderer.GltfObjectRenderer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * glTFオブジェクトレンダラーのレジストリクラス
 * レンダラーの管理・検索機能を提供
 */
public class GltfObjectRendererRegistry implements SynchronousResourceReloader {
    public static final GltfObjectRendererRegistry INSTANCE = new GltfObjectRendererRegistry();
    private final Map<Identifier, Factory<?>> registry = new HashMap<>();
    private final Map<Identifier, GltfObjectRenderer<?>> renderers = new HashMap<>();

    /**
     * レンダラーを登録
     */
    public <T> void registerRenderer(Identifier id, Factory<T> factory) {
        registry.put(id, factory);
    }

    /**
     * IDでレンダラーを取得
     *
     * @param id  レンダラーID
     * @param <T> レンダリング対象の型
     * @return レンダラー、見つからない場合はOptional.empty()
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<GltfObjectRenderer<T>> getRenderer(Identifier id) {
        return Optional.ofNullable((GltfObjectRenderer<T>) renderers.get(id));
    }

    @FunctionalInterface
    public interface Factory<T> {
        GltfObjectRenderer<T> create(Context context);
    }

    public record Context(GltfModelManager modelManager, GltfMetadataManager metadataManager) {
    }

    @Override
    public void reload(ResourceManager manager) {
        this.renderers.clear();
        var context = new Context(GltfModelManager.INSTANCE, GltfMetadataManager.INSTANCE);
        this.registry.forEach((id, factory) -> {
            try {
                var renderer = factory.create(context);
                this.renderers.put(id, renderer);
            } catch (Exception e) {
                ActionArms.LOGGER.error("Error during renderer creation : {}", id, e);
            }
        });
    }
}