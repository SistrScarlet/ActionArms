## Project Overview

ActionArms はリアルな銃火器メカニクスを追加する Minecraft mod。Architectury API で Fabric/Forge 両対応。

## Architecture

- Architectury Loom multi-module: `common/`, `fabric/`, `forge/`
- 共通コードは `common/src/main/java/net/sistr/actionarms/`
- コンポーネントベースアイテムシステム: `IItemComponent` + NBT シリアライゼーション
- 銃メカニクス: `LeverActionGunItem`, `GunController`, `AimManager`, `FireTrigger`, `CyclingLever`, `Chamber`
- glTF レンダリング: `GltfRenderer`（Minecraft モデルシステムをバイパス）, `ItemAnimationManager`
- HUD: `AAHudRenderer`（弾薬表示、vertical/horizontal モード）
- 入力: `ClientKeyInputManager`, `AAKeys`（バニラ右クリックをエイム/射撃キーに置換）
- ネットワーク: `KeyInputPacket`, `AimPacket`, `RecoilPacket`, `HudStatePacket`
- エンティティ: `BulletEntity`（レイキャスト + ProjectileUtil ハイブリッド、ヘッドショット判定）
- 依存: jglTF（3Dモデル）, Cloth Config, ModMenu

## Environment

- Minecraft 1.20.1, Gradle 7.4, Architectury API
- Java 17 が必要（`~/.gradle/gradle.properties` で `org.gradle.java.home` 設定済み）

## Build & Test

- `./gradlew spotlessApply` - コード整形 (google-java-format)
- `./gradlew spotlessCheck` - 整形チェック
- `./gradlew checkstyleMain` - Checkstyle スタイルチェック
- `./gradlew spotbugsMain` - SpotBugs バグ検出
- `python3 .claude/scripts/spotbugs-report.py` - SpotBugs レポート解析（`--summary`, `--priority N`）
- `./gradlew :common:test` - ユニットテスト (JUnit 5)
- `./gradlew build` - ビルド

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

- 既存ファイルを Write で全体書き換えする際は、既存の内容が失われないよう注意する（Edit で差分追加を優先）
- 返り値にOptionalを使用し、フィールドや引数には@Nullableを使用する
- org.jetbrains.annotations.Nullableを使用する
- @Nullable フィールドはローカル変数にキャッシュしてから使用する（SpotBugs NP_NULL_PARAM_DEREF 対策）
- Mixin Accessor は `util/` に配置し、メソッド名に `_AA` サフィックスを付ける（例: `getBrewTime_AA()`）
- protected フィールド/メソッドへの外部アクセスが必要な場合、同パッケージ内ならパッケージプライベートゲッターを追加する（Mixin Accessor より簡潔）
- レコードクラスに薄いメソッドは作らない
- コードに変更内容を書かない

### API Research

- Minecraft バニラ・Fabric・Forge などの前提 Mod の API 調査には必ず `mc-api-research` エージェントを使用する
- `.gradle` キャッシュの jar を直接検索しない

### Design Review Guidelines

- レビュー観点: SOLID原則, Effective Java, Law of Demeter / Tell Don't Ask, OOPアンチパターン
- `super` 呼び出しを含む override メソッドは外部クラスに委譲できない — 本体に残す
- 状態を持たないオーケストレーション/ファクトリは static ユーティリティクラスで可（過度なオブジェクト化を避ける）
