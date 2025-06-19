# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ActionArms is a sophisticated Minecraft mod that adds realistic firearms mechanics to the game. It's built using the Architectury platform to support both Fabric and Forge mod loaders simultaneously.

- **Mod ID**: `actionarms`
- **Minecraft Version**: 1.20.1
- **Multi-Loader**: Fabric + Forge via Architectury
- **Primary Language**: Java 17

## Development Commands

### Building and Running
```bash
# Build all platforms
./gradlew build

# Build specific platform
./gradlew :fabric:build
./gradlew :forge:build

# Run development environment
./gradlew :fabric:runClient
./gradlew :fabric:runServer
./gradlew :forge:runClient
./gradlew :forge:runServer

# Clean build
./gradlew clean build
```

### Development Environment
- Uses Yarn mappings for Minecraft 1.20.1
- Java 17 required
- Access widener file: `common/src/main/resources/actionarms.accesswidener`

## Architecture Overview

### Multi-Platform Structure
```
common/     # Shared code between Fabric and Forge
fabric/     # Fabric-specific implementations
forge/      # Forge-specific implementations
docs/       # Project documentation and specifications
```

### Core Systems

#### 1. Component-Based Item System
The mod uses a sophisticated NBT-based component system for gun mechanics:

**Key Interfaces:**
- `IItemComponent` - Base component with NBT serialization
- `ExecuteFunction<T>` - Functional interface for component modifications
- `ComponentResult` - Enum tracking component state (NO_CHANGE/MODIFIED)

**Component Types:**
- `BulletComponent` - Bullet properties and data
- `LeverActionGunComponent` - Lever-action rifle mechanics
- `MagazineComponent` - Magazine/tube loading system
- `UniqueComponent` - Item uniqueness tracking

#### 2. Gun Mechanics System
**Core Classes:**
- `LeverActionGunItem` - Main lever-action rifle implementation
- `BulletItem` - Ammunition items with physics properties
- `GunController` - Player gun operation management
- `AimManager` - Aiming system with toggle/hold modes

**Mechanical Components:**
- `FireTrigger` - Shooting mechanics and timing
- `CyclingLever` - Lever operation (down/up cycle)
- `Chamber` - Bullet chamber management
- `Cartridge` - Shell/cartridge state tracking

#### 3. Client-Side Rendering
**glTF System:**
- Custom 3D model rendering using jglTF library
- `GltfRenderer` - Direct OpenGL rendering bypassing Minecraft's model system
- `ItemAnimationManager` - UUID-based animation synchronization
- Mixins for held item and item renderer integration

**HUD System:**
- `AAHudRenderer` - Ammunition status display
- Two display modes: vertical (bottom-right) and horizontal (top-center)
- Real-time chamber and magazine visualization

#### 4. Input Management
- `ClientKeyInputManager` - Custom key binding system
- `AAKeys` - Key binding definitions
- Replaces vanilla right-click with dedicated aim/fire keys
- Platform-specific implementations in fabric/forge directories

#### 5. Audio System
- 4 sound types: FIRE, DRY_FIRE, RELOAD, CYCLE
- 4-8 sound variations per type
- `LeverActionPlaySoundContext` - Centralized sound management

#### 6. Networking
**Packet System:**
- `KeyInputPacket` - Key press synchronization
- `AimPacket` - Aiming state synchronization
- `RecoilPacket` - Recoil effect synchronization
- `HudStatePacket` - HUD update synchronization

#### 7. Entity System
- `BulletEntity` - Physics-based projectile with gravity and air resistance
- Hybrid hit detection: raycast + ProjectileUtil
- Headshot detection with size-responsive hitboxes
- Player capability systems via mixins

## Key Packages

### Core (`/net/sistr/actionarms/`)
- `ActionArms.java` - Main mod initialization
- `config/` - Configuration management (Cloth Config)
- `setup/Registration.java` - Item/entity/sound registration

### Client (`/client/`)
- `key/` - Input management and key bindings
- `render/` - All rendering systems (glTF, HUD, entities)

### Entity (`/entity/`)
- `BulletEntity.java` - Projectile physics and collision
- `util/` - Player capability systems (aim, key input, gun control)

### Item (`/item/`)
- Core item implementations and component system
- `component/` - Component definitions and data types
- `util/` - Item utility classes

### Mixin (`/mixin/`)
- Minecraft integration via mixins
- Rendering modifications, player behavior, world access

## Development Patterns

### Component Pattern
Items use a component-based architecture where functionality is modular and data-driven through NBT serialization. Components are immutable and modifications return new instances.

### Mixin Integration
Heavy use of mixins for non-invasive Minecraft integration, particularly for rendering and player behavior modifications. Access widener used for protected/private field access.

### Functional Programming
Extensive use of functional interfaces (`ExecuteFunction`) and lambdas for component operations and event handling.

### Platform Abstraction
Architectury pattern with common code and platform-specific implementations for Fabric/Forge compatibility.

## Important Dependencies

- **jglTF**: 3D model loading and rendering (`de.javagl:jgltf-*`)
- **Architectury**: Multi-platform mod development
- **Cloth Config**: Configuration GUI system
- **ModMenu**: Fabric configuration integration

## Current Implementation Status

The mod currently focuses on lever-action rifles with complete mechanics including:
- M1873 lever-action rifle fully implemented
- Medium caliber ammunition system
- Tube magazine loading
- Complete client-server synchronization
- glTF model rendering with animations
- HUD and audio systems

Future expansion will include additional gun types, ammunition varieties, and crafting systems as detailed in `/docs/item-system-specification.md`.

## Testing

No automated test framework is currently implemented. Testing is done through the development environment using `runClient` and `runServer` gradle tasks.

## Coding Standards

### Language and Documentation
- **Primary Language**: Japanese should be used for all communication, documentation, and code comments
- **Documentation**: Write all documentation files and code comments in Japanese
- **Code Comments**: Use Japanese for inline comments and JavaDoc documentation

### Null Safety
- **Null Annotations**: Use `@org.jetbrains.annotations.Nullable` to explicitly mark nullable types
- **Non-null Default**: All parameters, return values, and class fields are assumed non-null unless explicitly marked with `@Nullable`
- **Annotation Targets**: Apply `@Nullable` to:
  - Method parameters that can accept null values
  - Method return values that may return null
  - Class fields that can be null
- **Optional Usage**: When a method return value is nullable, prefer returning `Optional<T>` instead of null to make the API more explicit and safe

### Example
```java
// Good: Explicit null safety
public Optional<BulletComponent> findBullet(@Nullable ItemStack stack) {
    if (stack == null) return Optional.empty();
    // 実装...
}

// Nullable field example
@Nullable
private PlayerEntity cachedPlayer;
```

When working on this codebase, pay special attention to:
- Component state management and immutability
- Client-server synchronization for multiplayer compatibility
- NBT serialization for data persistence
- Platform-specific implementations in fabric/forge directories
- Proper null safety annotations and Optional usage

## ドキュメント管理システム

### 知識の泉（Knowledge Spring）- 効率的な実装調査システム

ActionArmsプロジェクトでは、実装調査の効率化のために「知識の泉」と呼ばれる構造化された知識管理システムを導入しています。

#### システム構成
```
docs/agent/
├── files-index.json          # 全体概要とカテゴリ分類
├── crud-guide.md            # 知識の泉操作指示書
├── common/                  # 詳細情報（カテゴリ別）
│   ├── root.json           # メインクラス・設定管理
│   ├── client.json         # クライアント・キーバインド
│   ├── config.json         # MOD設定
│   ├── item.json           # 銃アイテム・コンポーネント
│   ├── network.json        # ネットワーク通信
│   ├── setup.json          # 登録処理
│   ├── gltf.json           # glTFレンダリングシステム
│   ├── component.json      # 移行完了ファイル
│   └── entity.json         # エンティティシステム
└── [ガイドライン・実例文書] # 以下参照
```

#### 活用方法

**1. 実装調査の開始点として使用**
```bash
# 全体把握
Read docs/agent/files-index.json

# 特定システム調査
Read docs/agent/common/gltf.json    # レンダリング調査時
Read docs/agent/common/item.json    # アイテムシステム調査時
Read docs/agent/common/network.json # 通信システム調査時
```

**2. 調査効率の向上**
- **71%の時間短縮**: 従来255分 → 75分
- **依存関係追跡**: `dependencies`フィールドで関連システム特定
- **実装状況把握**: `importance`/`key_features`/`note`で詳細確認
- **最新変更確認**: `recent_major_changes`で履歴把握

### ドキュメント作成ガイドライン

#### 利用可能なガイドライン
```
docs/agent/
├── documentation-creation-guide.md        # 包括的作成指針
├── implementation-investigation-patterns.md # 5つの調査パターン
└── architecture-diagram-guidelines.md     # 図表作成標準
```

#### 実装調査の5つのパターン

**1. システム全体把握調査**
```
files-index.json → importance: high特定 → アーキテクチャ図作成
適用例: docs/architecture/system-overview.md
```

**2. 機能連携調査**
```
dependencies追跡 → データフロー分析 → シーケンス図作成
適用例: docs/architecture/feature-flows/aim-system-integration.md
```

**3. パフォーマンス調査**
```
recent_major_changes確認 → 最適化手法理解 → 技術詳細文書化
適用例: docs/architecture/data-flows/gltf-rendering-system.md
```

**4. コンポーネントシステム調査**
```
コンポーネント構造把握 → 実装パターン抽出 → サンプル集作成
適用例: docs/implementation/component-system-samples.md
```

**5. バグ調査・デバッグ**
```
問題箇所特定 → 関連システム確認 → 最近の変更確認 → 解決策文書化
```

### ドキュメント更新プロセス

#### 必須更新タイミング
- 新しいJavaファイル追加時
- 重要な機能変更時
- システム間の依存関係変更時
- アーキテクチャ変更時

#### 更新手順
```bash
# 1. 知識の泉の更新
Read docs/agent/crud-guide.md  # 操作方法確認
# 該当するcommon/*.jsonファイルを更新

# 2. 関連ドキュメントの更新
# アーキテクチャ図、実装サンプル等の更新

# 3. 実例文書の参照
Read docs/case-studies/implementation-investigation-example.md
# 実際の更新プロセス例を確認
```

### 実際のドキュメント構成

#### アーキテクチャ文書
```
docs/architecture/
├── system-overview.md                      # システム全体概要図
├── feature-flows/
│   └── aim-system-integration.md          # エイム機能連携詳細
└── data-flows/
    └── gltf-rendering-system.md           # glTFレンダリング技術詳細
```

#### 実装資料
```
docs/implementation/
└── component-system-samples.md            # コンポーネント実装パターン集
```

#### 実例文書
```
docs/case-studies/
└── implementation-investigation-example.md # 調査・作成プロセス実例
```

### 開発ワークフローへの統合

#### 新機能開発時
```
1. 類似機能の調査（知識の泉活用）
2. 依存関係の特定（dependencies確認）
3. 実装パターンの参照（サンプル集活用）
4. 実装完了後の知識の泉更新
```

#### バグ修正時
```
1. 問題箇所の特定（knowledge spring検索）
2. 関連システムの確認（dependencies追跡）
3. 最近の変更確認（recent_major_changes）
4. 修正完了後の情報更新
```

#### 新規開発者オンボーディング
```
Day 1: system-overview.md で全体把握
Day 2: investigation-patterns.md で調査手法学習
Day 3: component-system-samples.md で実装パターン学習
Day 4: 実際の開発タスクへの適用
```

## Memories

### Development Process
- When updating the knowledge base, read `docs/agent/crud-guide.md` and follow the specified process for updating the knowledge base
- プロジェクトの大まかな情報を得たいときは、知識の泉`docs/agent`ディレクトリ内の情報を利用してください
- 実装調査時は必ず`docs/agent/implementation-investigation-patterns.md`の5つのパターンを活用してください
- ドキュメント作成時は`docs/agent/documentation-creation-guide.md`のガイドラインに従ってください

### Knowledge Management Guidelines
- **調査の起点**: 必ず`files-index.json`から開始し、全体像を把握してから詳細調査に進む
- **効率化**: `importance: high`のファイルを優先的に調査し、`dependencies`で関連システムを特定
- **最新性**: `recent_major_changes`を確認して最新の実装状況を把握
- **ドキュメント品質**: Mermaid記法を使用した視覚的表現と実践的なコードサンプルを提供
- **継続的更新**: システム変更時には関連する知識の泉とドキュメントを同時に更新