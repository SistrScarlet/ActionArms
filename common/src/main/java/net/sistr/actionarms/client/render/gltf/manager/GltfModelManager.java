package net.sistr.actionarms.client.render.gltf.manager;

import com.google.common.collect.ImmutableMap;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.io.GltfModelReader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.client.render.gltf.converter.GltfModelConverter;
import net.sistr.actionarms.client.render.gltf.data.ProcessedGltfModel;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class GltfModelManager implements ResourceReloader, AutoCloseable {
    public static final GltfModelManager INSTANCE = new GltfModelManager();
    @Nullable
    private Map<Identifier, ProcessedGltfModel> models;

    @Override
    public CompletableFuture<Void> reload(Synchronizer synchronizer, ResourceManager manager, Profiler prepareProfiler, Profiler applyProfiler, Executor prepareExecutor, Executor applyExecutor) {
        return CompletableFuture.supplyAsync(
                        () -> manager.findResources("models/gltf", (id) -> {
                            var path = id.getPath();
                            return path.endsWith(".glb");
                        }),
                        prepareExecutor
                )
                .thenCompose(synchronizer::whenPrepared)
                .thenAcceptAsync((map) -> {
                    var modelMapBuilder = ImmutableMap.<Identifier, ProcessedGltfModel>builder();
                    var modelReader = new GltfModelReader();
                    var gltfConverter = new GltfModelConverter();
                    map.forEach((key, value) -> {
                        GltfModel original;
                        try (var inputStream = value.getInputStream()) {
                            original = modelReader.readWithoutReferences(inputStream);
                        } catch (IOException e) {
                            ActionArms.LOGGER.error("Model load error : {}", key, e);
                            return;
                        }
                        var paths = key.getPath().split("/");
                        var fileName = paths[paths.length - 1].replace(".glb", "");
                        var models = gltfConverter.convertModel(fileName, original);
                        int index = 0;
                        for (ProcessedGltfModel model : models) {
                            // Identifierはパス準拠
                            var modelKey = new Identifier(key.getNamespace(), key.getPath() + "_scene" + index);
                            modelMapBuilder.put(modelKey, model);
                            index++;
                        }
                    });
                    this.models = modelMapBuilder.build();
                });
    }

    @Override
    public void close() throws Exception {

    }

    public Optional<ProcessedGltfModel> getLoadedModel(Identifier identifier) {
        if (models == null) {
            throw new IllegalStateException("models is not initialized.");
        }
        return Optional.ofNullable(this.models.get(identifier));
    }

    public Map<Identifier, ProcessedGltfModel> getAllModels() {
        if (models == null) {
            throw new IllegalStateException("models is not initialized.");
        }
        return models;
    }
}
