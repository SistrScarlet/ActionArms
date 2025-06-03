package net.sistr.actionarms.client.render.gltf;

import com.google.common.collect.ImmutableMap;
import de.javagl.jgltf.model.io.GltfModelReader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.sistr.actionarms.ActionArms;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class GLTFModelManager implements ResourceReloader, AutoCloseable {
    public static final GLTFModelManager INSTANCE = new GLTFModelManager();
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
                        ProcessedGltfModel model;
                        try {
                            var original = modelReader.readWithoutReferences(value.getInputStream());
                            var name = key.toString();
                            model = gltfConverter.convertModel(name, original);
                        } catch (IOException e) {
                            ActionArms.LOGGER.error("Model load error : {}", key, e);
                            return;
                        }
                        modelMapBuilder.put(key, model);
                    });
                    this.models = modelMapBuilder.build();
                });
    }

    @Override
    public void close() throws Exception {

    }

    public Map<Identifier, ProcessedGltfModel> getModels() {
        return models;
    }
}
