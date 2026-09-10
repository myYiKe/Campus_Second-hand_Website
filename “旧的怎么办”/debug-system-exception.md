# Debug Session: system-exception

- Status: OPEN
- Symptom: 页面出现“系统异常”提示
- Goal: 定位触发异常的具体接口、SQL 或运行时错误，并给出最小修复

## Hypotheses

1. 某个接口 SQL 与本地旧数据库结构不兼容。
2. 收藏足迹相关接口返回字段与前端读取字段不一致。
3. 运行时存在空值数据，Mapper 或页面渲染阶段未做好兼容。
4. 真正失败的不是当前可见页面，而是同页面并行加载的其他接口。

## Evidence Log

- `/api/orders`、`/api/reviews`、`/api/disputes` 返回正常，`/api/messages` 返回 `INTERNAL_ERROR / 系统异常`
- 后端日志命中：`Unknown column 'm.from_user' in 'field list'`
- 实库 `message` 表字段为 `sender_id/receiver_id/is_read`，不包含 `from_user/to_user/read_status/conversation_id`
- 发送消息二次回归命中：旧外键 `message_ibfk_1/message_ibfk_2` 仍引用历史 `user` 表，导致写入失败
- 修复后回归：`GET /api/messages` 正常，`POST /api/messages` 正常

## Root Cause

- 代码中的消息模块按新表字段实现，但本地数据库沿用了旧版 `message` 结构。
- 旧库还保留了指向历史 `user` 表的外键，和当前 `user_account` 用户体系不兼容。

## Fix

- `MessageMapper` 改为兼容旧字段 `sender_id/receiver_id/is_read`
- 系统消息改为以兼容文本前缀持久化，再在查询层还原 `REVIEW/DISPUTE`
- `SchemaUpgradeRunner` 启动时自动清理旧消息外键，避免发送消息失败

## Next Step

- 保留调试记录，待用户页面确认消息/订单页不再出现“系统异常”后再收尾。
