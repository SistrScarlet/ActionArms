package net.sistr.actionarms.client.render.gltf.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessedAnimation {
    private final String name;
    private final List<ProcessedChannel> channels;
    private final Map<String, ProcessedChannel> channelsByTarget;
    private final float duration;
    private final boolean isLooping;

    public ProcessedAnimation(String name, List<ProcessedChannel> channels) {
        this.name = name != null ? name : "Animation";
        this.channels = new ArrayList<>(channels);
        this.channelsByTarget = new HashMap<>();
        this.isLooping = true; // デフォルトでループ

        // ターゲット別のチャンネルマッピングを作成
        for (ProcessedChannel channel : channels) {
            String targetKey = channel.getTargetNode() + ":" + channel.getTargetPath();
            channelsByTarget.put(targetKey, channel);
        }

        // アニメーション全体の長さを計算
        this.duration = calculateDuration();
    }

    public String getName() {
        return name;
    }

    public List<ProcessedChannel> getChannels() {
        return new ArrayList<>(channels);
    }

    public float getDuration() {
        return duration;
    }

    public boolean isLooping() {
        return isLooping;
    }

    // 特定のノードとパスのチャンネルを取得
    public ProcessedChannel getChannel(String nodeName, String path) {
        return channelsByTarget.get(nodeName + ":" + path);
    }

    // 指定時間での値を取得
    public Object getValueAt(String nodeName, String path, float time) {
        ProcessedChannel channel = getChannel(nodeName, path);
        return channel != null ? channel.getValueAt(time) : null;
    }

    // 時間の正規化（ループ対応）
    public float normalizeTime(float time) {
        if (duration <= 0) return 0;

        if (isLooping) {
            return time % duration;
        } else {
            return Math.max(0, Math.min(time, duration));
        }
    }

    private float calculateDuration() {
        float maxDuration = 0;
        for (ProcessedChannel channel : channels) {
            maxDuration = Math.max(maxDuration, channel.getDuration());
        }
        return maxDuration;
    }

    // デバッグ情報
    public void printInfo() {
        System.out.println("Animation: " + name);
        System.out.println("  Duration: " + duration + "s");
        System.out.println("  Channels: " + channels.size());
        for (ProcessedChannel channel : channels) {
            System.out.println("    " + channel.toString());
        }
    }

    @Override
    public String toString() {
        return String.format("Animation[%s: %.2fs, %d channels]",
                name, duration, channels.size());
    }

    public int getChannelCount() {
        return this.channels.size();
    }
}
