## Project Overview

ActionArms はリアルな銃火器メカニクスを追加する Minecraft mod。Architectury API で Fabric/Forge 両対応。

## Architecture

- Architectury Loom multi-module: `common/`, `fabric/`, `forge/`
- 共通コードは `common/src/main/java/net/sistr/actionarms/`
- コンポーネントベースアイテムシステム: `IItemComponent` + NBT シリアライゼーション
- 銃メカニクス: `LeverActionGunItem`, `GunController`, `GunPhase`(状態enum), `AimManager`, `FireTrigger`, `CyclingLever`, `Chamber`
- SAA（コルトSAA）: `SAAGunComponent`, `SAAGunController`, `Cylinder`（firingIndex/gateIndex モデル）
- シリンダー回転: cockRotate（時計回り、firingIndex--）、loadRotate（反時計回り、firingIndex++）
- glTF レンダリング: `GltfObjectRenderer`（Minecraft モデルシステムをバイパス）, `AnimationLayer`（Clip/Procedural）, `ItemAnimationManager`
- SAA 描画: `SAAItemRenderer`（Procedural でシリンダー回転、hideBones で弾丸表示制御）
- glTF メタデータ: `ModelMetadata.properties()`（モデル固有設定を JSON で定義、例: `cylinder_bone`, `cylinder_axis`）
- HUD: `AAHudRenderer`（弾薬表示、操作ヒント表示（Config で ON/OFF）、`KeyBinding.getBoundKeyLocalizedText()` でキー名動的取得）
- 入力: `ClientKeyInputManager`, `AAKeys`（バニラ右クリックをエイム/射撃キーに置換）
- ネットワーク: `KeyInputPacket`, `AimPacket`, `RecoilPacket`, `HudStatePacket`
- エンティティ: `BulletEntity`（レイキャスト + ProjectileUtil ハイブリッド、ヘッドショット判定）
- 依存: jglTF（3Dモデル）, Cloth Config, ModMenu

## Environment

- Minecraft 1.20.1, Gradle 7.4, Architectury API
- Java 17 が必要（`~/.gradle/gradle.properties` で `org.gradle.java.home` 設定済み）

## Build & Test

- `./gradlew spotlessApply` - コード整形 (google-java-format, AOSP style)
- `./gradlew spotlessCheck` - 整形チェック
- `./gradlew checkstyleMain` - Checkstyle スタイルチェック
- `./gradlew spotbugsMain` - SpotBugs バグ検出
- `python3 .claude/scripts/spotbugs-report.py` - SpotBugs レポート解析（`--summary`, `--priority N`）
- `./gradlew :common:test` - ユニットテスト (JUnit 5)
- `./gradlew build` - ビルド
- サウンドファイル (.ogg) は Gamemaster Audio ライセンスにより git 除外。Windows 側からコピー: `cp /mnt/v/.../sounds/item/gun/*.ogg common/.../sounds/item/gun/`

## TODO 管理

- `TODO.md` をタスクリストとして自律管理する
- 開発中に発見した課題・技術的負債・リファクタ候補などを随時追記する
- 完了したタスクは削除する（履歴は不要）
- 優先度（高/中/低）でカテゴリ分けする

## 作業記録

- 設計判断や重要な技術的決定を行った際は `docs/` に作業記録を残す
- ユーザーの指示がなくても、記録に値する判断をした場合は自律的に `/doc` スキルで記録する
- 形式: `docs/{category}/yyyy-mm-dd_{タイトル}.md`
- カテゴリ: `adr/`（設計判断）, `plan/`（作業プラン）, `research/`（調査メモ）等

## Cross-Environment Workflow

- WSL2 から Windows リポジトリへローカルremote経由で転送可能
- Windows側でチェックアウト中のブランチにはpush不可。別ブランチ名にpush: `git push local <branch>:wsl/{branch-name}`

### Code Editing Guidelines

- DamageType の挙動変更はコードではなく `data/minecraft/tags/damage_type/` の JSON タグで行う（例: `bypasses_cooldown.json`）
- 既存ファイルを Write で全体書き換えする際は、既存の内容が失われないよう注意する（Edit で差分追加を優先）
- 返り値にOptionalを使用し、フィールドや引数には@Nullableを使用する
- org.jetbrains.annotations.Nullableを使用する
- @Nullable フィールドはローカル変数にキャッシュしてから使用する（SpotBugs NP_NULL_PARAM_DEREF 対策）
- Mixin Accessor は `util/` に配置し、メソッド名に `_AA` サフィックスを付ける（例: `getBrewTime_AA()`）
- protected フィールド/メソッドへの外部アクセスが必要な場合、同パッケージ内ならパッケージプライベートゲッターを追加する（Mixin Accessor より簡潔）
- レコードクラスに薄いメソッドは作らない
- コードに変更内容を書かない
- float タイマーの完了判定には `== 0` ではなく `<= 0` を使用する（浮動小数点精度問題）
- コンポーネントの tick() は状態変更の有無を boolean で返す（NBT 保存判定に使用）

### Testing Guidelines

- `./gradlew :common:test` で JUnit 5 テスト実行（MC 起動不要）
- MC レジストリに依存する enum/クラスはテストから直接参照しない（static 初期化で NPE）
- Registration への参照を含む enum は SoundSuppliers パターンで遅延ロード化する
- コンテキストオブジェクト（PlaySoundContext 等）はラムダスタブで差し替え可能
- float タイマーのテストは `tickAndExpect(expectedTicks, condition)` パターンで FP 精度マージンを許容
- 複雑なロジック（smartGateRotate 等）はパラメタライズドテストで全パターン網羅する

### Initialization Order

- `preInit()`: Config 登録 → Registration.init()（この順序厳守。Registration が AADataRegistry を参照し、config 値を使用するため）

### API Research

- Minecraft バニラ・Fabric・Forge などの前提 Mod の API 調査には必ず `mc-api-research` エージェントを使用する
- `.gradle` キャッシュの jar を直接検索しない

### Design Review Guidelines

- レビュー観点: SOLID原則, Effective Java, Law of Demeter / Tell Don't Ask, OOPアンチパターン
- `super` 呼び出しを含む override メソッドは外部クラスに委譲できない — 本体に残す
- 状態を持たないオーケストレーション/ファクトリは static ユーティリティクラスで可（過度なオブジェクト化を避ける）
