# TODO

## 高

## 中

- エイム状態を GunComponent に統合する（現状 AimManager が Player 側に分離しており、SAAGunComponent 等から状態参照が煩雑。3本目の銃の抽象化時に対応）

## 低

- HUDステータス同期（HudStatePacket）とアニメーション再生管理（ItemAnimationEventPacket）の統合検討（どちらもサーバー→クライアントのデータ同期という共通基盤がある）
