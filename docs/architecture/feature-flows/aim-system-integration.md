# エイム機能統合システム詳細

## 概要

このドキュメントは、ActionArms MODのエイム機能がどのように他のシステムと連携してリアルタイム同期を実現しているかを詳細に説明します。実装調査パターンガイドの「機能連携調査」パターンを適用した実例です。

## システム構成要素

### 調査起点（知識の泉より）

```json
// client.json から
"ClientAimManager.java": {
  "desc": "クライアント側エイム管理とトグル・プッシュ両対応",
  "dependencies": ["AimPacket.java", "HasAimManager.java"]
}

// network.json から  
"AimPacket.java": {
  "desc": "エイム状態をクライアントからサーバーに同期するパケット",
  "dependencies": ["HasAimManager.java", "ClientAimManager.java"]
}
```

## エイム機能連携フロー

### 1. リアルタイム同期シーケンス

```mermaid
sequenceDiagram
    participant User as プレイヤー
    participant CAM as ClientAimManager
    participant AP as AimPacket
    participant Server as ServerPlayerEntity<br/>(Mixin)
    participant AM as AimManager
    participant SHM as ServerHudManager
    participant HP as HudStatePacket
    participant CHM as ClientHudManager
    participant HUD as AAHudRenderer
    participant Render as MixinHeldItemRenderer
    
    User->>+CAM: エイムキー押下
    CAM->>CAM: setAiming(true)
    CAM->>+AP: sendC2S(true)
    AP->>+Server: receiveC2S()
    Server->>+AM: setAiming(true)
    AM->>AM: 状態更新・アイテム切り替え時自動解除
    
    AM->>+SHM: エイム状態変化通知
    SHM->>SHM: HUD状態更新
    SHM->>+HP: sendS2C(hudData)
    HP->>+CHM: receiveS2C()
    CHM->>CHM: 20tickタイムアウト管理
    CHM->>+HUD: HUD状態更新
    HUD->>HUD: エイム時クロスヘア描画
    
    AM->>+Render: エイム状態通知
    Render->>Render: 銃描画位置調整（中央配置）
    
    Note over User,Render: リアルタイム同期完了<br/>（エイム状態・HUD・描画位置）
    
    User->>+CAM: エイムキー解除
    CAM->>+AP: sendC2S(false)
    AP->>+Server: receiveC2S()
    Server->>+AM: setAiming(false)
    AM->>+SHM: 状態変化通知
    SHM->>+HP: sendS2C(hudData)
    HP->>+CHM: receiveS2C()
    CHM->>+HUD: 通常HUD復帰
    AM->>+Render: 通常描画位置復帰
```

### 2. エイム機能統合アーキテクチャ

```mermaid
graph TD
    subgraph "🖱️ Client Side"
        USER[プレイヤー入力]
        CAM[ClientAimManager<br/>・トグル/プッシュ対応<br/>・状態変化検出]
        CHM[ClientHudManager<br/>・20tickタイムアウト<br/>・状態受信処理]
        HUD[AAHudRenderer<br/>・エイム時クロスヘア<br/>・弾薬状況表示]
        RENDER[MixinHeldItemRenderer<br/>・エイム時描画位置調整<br/>・中央配置制御]
    end
    
    subgraph "🌐 Network Layer"
        AIMPACKET[AimPacket<br/>C2S通信<br/>boolean型エイム状態]
        HUDPACKET[HudStatePacket<br/>S2C通信<br/>NBT形式HUDデータ]
    end
    
    subgraph "🖥️ Server Side"
        SERVER[ServerPlayerEntity<br/>Mixin機能拡張]
        AM[AimManager<br/>・エイム状態管理<br/>・アイテム切り替え時自動解除]
        SHM[ServerHudManager<br/>・状態変化時のみ送信<br/>・効率的同期システム]
        GUN[LeverActionGunItem<br/>・エイム時精度向上<br/>・拡散角減少]
    end
    
    subgraph "🎮 Input Integration"
        KEYS[AAKeys<br/>エイムキーバインド]
        INPUT[ClientKeyInputManager<br/>キー状態管理]
    end
    
    %% Flow connections
    USER --> CAM
    CAM --> AIMPACKET
    AIMPACKET --> SERVER
    SERVER --> AM
    
    AM --> SHM
    AM --> GUN
    SHM --> HUDPACKET
    HUDPACKET --> CHM
    CHM --> HUD
    
    AM --> RENDER
    
    %% Input flow
    KEYS --> INPUT
    INPUT --> CAM
    
    %% Feedback loops
    HUD -.-> USER
    RENDER -.-> USER
    GUN -.-> USER
    
    %% Style definitions
    classDef clientStyle fill:#e3f2fd,stroke:#1976d2,stroke-width:2px
    classDef serverStyle fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef networkStyle fill:#e8f5e8,stroke:#388e3c,stroke-width:2px
    classDef inputStyle fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    
    class USER,CAM,CHM,HUD,RENDER clientStyle
    class SERVER,AM,SHM,GUN serverStyle
    class AIMPACKET,HUDPACKET networkStyle
    class KEYS,INPUT inputStyle
```

## 技術的実装詳細

### 1. ClientAimManager の状態管理

```java
// 実装パターン例（documentation-creation-guide.md より）
public class ClientAimManager {
    private boolean aiming = false;
    private boolean toggleMode = false; // 設定により切り替え可能
    
    public void setAiming(boolean aiming) {
        if (this.aiming != aiming) {
            this.aiming = aiming;
            AimPacket.sendC2S(aiming); // 即座にサーバー同期
        }
    }
    
    // トグル・プッシュ両対応
    public void handleAimInput(boolean keyPressed) {
        if (toggleMode) {
            if (keyPressed) setAiming(!aiming); // トグル
        } else {
            setAiming(keyPressed); // プッシュ
        }
    }
}
```

### 2. ネットワーク効率化

```java
// HudStatePacket の効率化実装
public class ServerHudManager {
    // 状態変化時のみ送信（効率化）
    public void updateHud(ServerPlayerEntity player, String stateId, HudState<?> newState) {
        HudState<?> oldState = getState(player, stateId);
        if (!Objects.equals(oldState, newState)) {
            HudStatePacket.sendS2C(player, stateId, newState);
            setState(player, stateId, newState);
        }
    }
}
```

### 3. Mixin統合パターン

```java
// MixinHeldItemRenderer でのエイム時描画制御
@Mixin(HeldItemRenderer.class)
public class MixinHeldItemRenderer {
    @Inject(method = "applyEquipOffset", at = @At("HEAD"), cancellable = true)
    public void onApplyEquipOffset(CallbackInfo ci) {
        if (isPlayerAiming() && isGltfModelItem()) {
            // エイム時の中央配置
            applyAimingOffset();
            ci.cancel(); // デフォルト処理をキャンセル
        }
    }
}
```

## パフォーマンス特性

### 1. ネットワーク効率

| 項目 | 従来システム | エイム機能統合 |
|------|-------------|-------------|
| エイム状態同期 | 毎tick送信 | 変化時のみ |
| HUD更新 | 強制送信 | 差分のみ |
| データサイズ | 大きい | 最小限（boolean） |
| レスポンス | 遅延あり | 即座 |

### 2. メモリ使用量

```
エイム状態: boolean 1bit
HUD状態: NBTCompound（必要分のみ）
タイムアウト管理: 20tick（1秒）自動クリーンアップ
```

## トラブルシューティング

### よくある問題と解決法

| 問題 | 原因 | 解決方法 |
|------|------|---------|
| エイム状態が同期されない | ネットワークパケット登録漏れ | Networking.java でのパケット登録確認 |
| HUDが更新されない | ServerHudManager の状態変化検出漏れ | 状態比較ロジックの確認 |
| 描画位置がずれる | MixinHeldItemRenderer の条件分岐 | エイム状態判定の確認 |
| エイムが自動解除される | AimManager のアイテム切り替え検出 | 意図的な動作（仕様） |

## 拡張ポイント

### 将来的な機能追加

1. **スコープ機能**: エイム時のズーム機能
2. **呼吸エフェクト**: エイム時の視点ブレ
3. **疲労システム**: 長時間エイム時の精度低下
4. **カスタムクロスヘア**: プレイヤー設定可能なクロスヘア

### 実装時の考慮点

```java
// 拡張時のパターン
public interface AdvancedAimManager extends AimManager {
    void setZoomLevel(float zoom);
    void applyFatigueEffect(float fatigue);
    void setCustomCrosshair(CrosshairType type);
}
```

## まとめ

このエイム機能統合システムは、以下の原則に基づいて設計されています：

1. **リアルタイム性**: 即座の状態同期
2. **効率性**: 変化時のみの通信
3. **拡張性**: 将来機能への対応
4. **統合性**: 他システムとの密な連携

この実装により、マルチプレイヤー環境でも遅延のないエイム体験と視覚的フィードバックを実現しています。

---

**作成日**: 2025-06-19  
**調査パターン**: 機能連携調査（implementation-investigation-patterns.md）  
**情報源**: 知識の泉（client.json, network.json, root.json）  
**関連ドキュメント**: system-overview.md