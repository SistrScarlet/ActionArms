package net.sistr.actionarms.client.render.gltf.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 変換済みアニメーションrecordクラス（不変設計）
 * チャンネル管理とボーン別アクセス最適化を提供
 */
public record ProcessedAnimation(
        String name,
        List<ProcessedChannel> channels,
        Map<String, ProcessedChannel[]> nameByChannels,
        float duration
) {

    /**
     * バリデーション付きコンストラクタ
     */
    public ProcessedAnimation {
        // 名前のデフォルト値設定
        if (name == null || name.trim().isEmpty()) {
            name = "Animation_" + System.identityHashCode(this);
        }

        // チャンネルリストの防御的コピーと不変化
        if (channels == null) {
            channels = Collections.emptyList();
        } else {
            channels = List.copyOf(channels);
        }

        // 期間の計算（引数として渡されているが、検証のため再計算）
        float calculatedDuration = calculateDuration(channels);
        if (Math.abs(duration - calculatedDuration) > 0.001f) {
            // 渡された期間と計算された期間が異なる場合は計算値を使用
            duration = calculatedDuration;
        }
    }

    // === 基本情報アクセサー ===

    public int getChannelCount() {
        return channels.size();
    }

    public boolean hasChannels() {
        return !channels.isEmpty();
    }

    public boolean isValid() {
        return hasChannels() && duration > 0;
    }

    // === チャンネル検索・アクセス ===

    /**
     * インデックスでチャンネルを取得
     */
    @Nullable
    public ProcessedChannel getChannel(int index) {
        return (index >= 0 && index < channels.size()) ? channels.get(index) : null;
    }

    /**
     * ターゲットノードとパスでチャンネルを検索
     */
    public Optional<ProcessedChannel> findChannel(String targetNode, String targetPath) {
        return channels.stream()
                .filter(channel -> Objects.equals(channel.targetNode(), targetNode) &&
                        Objects.equals(channel.targetPath(), targetPath))
                .findFirst();
    }

    /**
     * ターゲットノードのすべてのチャンネルを取得
     */
    public List<ProcessedChannel> getChannelsForNode(String targetNode) {
        return channels.stream()
                .filter(channel -> Objects.equals(channel.targetNode(), targetNode))
                .toList();
    }

    // === ボーン別最適化アクセス ===

    /**
     * 指定されたボーンのチャンネル配列を取得
     * [0] = translation, [1] = rotation, [2] = scale
     */
    @Nullable
    public ProcessedChannel[] getChannels(String boneName) {
        return nameByChannels.get(boneName);
    }

    /**
     * 指定されたボーンが存在するかチェック
     */
    public boolean hasBone(String boneName) {
        return nameByChannels.containsKey(boneName);
    }

    /**
     * アニメーション対象のボーン名一覧を取得
     */
    public Set<String> getBoneNames() {
        return new HashSet<>(nameByChannels.keySet());
    }

    /**
     * アニメーション対象のボーン数を取得
     */
    public int getBoneCount() {
        return nameByChannels.size();
    }

    // === 時間正規化 ===

    /**
     * 時間の正規化（ループ対応）
     */
    public float normalizeTime(float time, boolean isLooping) {
        if (duration <= 0) return 0;

        if (isLooping) {
            return time % duration;
        } else {
            return Math.max(0, Math.min(time, duration));
        }
    }

    // === 内部ヘルパーメソッド ===

    /**
     * アニメーション全体の期間を計算する静的メソッド
     */
    private static float calculateDuration(List<ProcessedChannel> channels) {
        if (channels == null || channels.isEmpty()) return 0;

        float maxDuration = 0;
        for (ProcessedChannel channel : channels) {
            maxDuration = Math.max(maxDuration, channel.duration());
        }
        return maxDuration;
    }

    // === 統計・分析メソッド ===

    /**
     * チャンネルタイプ別の統計を取得
     */
    public Map<String, Integer> getChannelTypeStats() {
        Map<String, Integer> stats = new HashMap<>();
        for (ProcessedChannel channel : channels) {
            stats.merge(channel.targetPath(), 1, Integer::sum);
        }
        return Collections.unmodifiableMap(stats);
    }

    /**
     * 詳細な統計情報を取得
     */
    public AnimationStats getStats() {
        return new AnimationStats(
                getChannelCount(),
                getBoneCount(),
                duration,
                getChannelTypeStats(),
                isValid()
        );
    }

    /**
     * デバッグ情報を出力
     */
    public void printInfo() {
        System.out.println("Animation: " + name);
        System.out.println("  Duration: " + duration + "s");
        System.out.println("  Channels: " + getChannelCount());
        System.out.println("  Bones: " + getBoneCount());

        Map<String, Integer> typeStats = getChannelTypeStats();
        typeStats.forEach((type, count) ->
                System.out.println("    " + type + ": " + count));

        for (ProcessedChannel channel : channels) {
            System.out.println("    " + channel.toString());
        }
    }

    /**
     * デバッグ情報を取得
     */
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder(100);
        sb.append(String.format("ProcessedAnimation[%s]", name));
        sb.append(String.format("  Duration: %.3fs", duration));
        sb.append(String.format("  Channels: %d (bones: %d)", getChannelCount(), getBoneCount()));

        Map<String, Integer> typeStats = getChannelTypeStats();
        typeStats.forEach((type, count) ->
                sb.append(String.format("    %s: %d", type, count)));

        return sb.toString();
    }

    @Override
    public @NotNull String toString() {
        return String.format("Animation[%s: %.2fs, %d channels, %d bones]",
                name, duration, getChannelCount(), getBoneCount());
    }

    /**
     * アニメーション統計情報を保持するrecord
     */
    public record AnimationStats(
            int channelCount,
            int boneCount,
            float duration,
            Map<String, Integer> channelTypeStats,
            boolean isValid
    ) {
    }

    /**
     * ビルダーパターンの実装
     */
    public static class Builder {
        private String name;
        private final List<ProcessedChannel> channels = new ArrayList<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder addChannel(ProcessedChannel channel) {
            if (channel != null) {
                channels.add(channel);
            }
            return this;
        }

        public Builder addChannels(List<ProcessedChannel> channels) {
            this.channels.addAll(channels);
            return this;
        }

        public Builder channels(List<ProcessedChannel> channels) {
            this.channels.clear();
            this.channels.addAll(channels);
            return this;
        }

        /**
         * ボーン別チャンネルマッピングを構築する静的メソッド
         */
        private static Map<String, ProcessedChannel[]> buildNameByChannelsMap(List<ProcessedChannel> channels) {
            Map<String, ProcessedChannel[]> mapping = new HashMap<>(channels.size());

            for (ProcessedChannel channel : channels) {
                int index = getChannelIndex(channel.targetPath());
                if (index != -1) {
                    ProcessedChannel[] targetChannels = mapping
                            .computeIfAbsent(channel.targetNode(), s -> new ProcessedChannel[3]);
                    targetChannels[index] = channel;
                }
            }

            return mapping;
        }

        /**
         * ターゲットパスからチャンネルインデックスを取得
         */
        private static int getChannelIndex(String targetPath) {
            return switch (targetPath) {
                case "translation" -> 0;
                case "rotation" -> 1;
                case "scale" -> 2;
                default -> -1;
            };
        }

        public ProcessedAnimation build() {
            var nameByChannels = buildNameByChannelsMap(channels);
            return new ProcessedAnimation(name, channels, nameByChannels, calculateDuration(channels));
        }
    }

    /**
     * 新しいBuilderインスタンスを作成
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 既存のProcessedAnimationをベースにした新しいBuilderを作成
     */
    public Builder toBuilder() {
        return builder()
                .name(name)
                .channels(channels);
    }
}
