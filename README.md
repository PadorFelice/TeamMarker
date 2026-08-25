# TeamMarker — 客户端队友标识模组

仅客户端运行的 Fabric 模组，**服务端无需安装任何东西**。安装本模组的玩家可以在自己屏幕上看到队友头顶前缀标识和穿墙发光轮廓，其他玩家完全看不见，不发任何数据包到服务器。

- **Minecraft 版本**：1.21.8
- **加载器**：Fabric Loader 0.19.3+
- **依赖**：Fabric API 0.136.1+1.21.8
- **环境**：仅客户端（`environment: client`）

---

## 一、安装

1. 把 `teammarker-1.0.0.jar` 复制到 `.minecraft/mods/`
2. 同时确保 `mods/` 里有对应 1.21.8 的 Fabric API jar
3. 用 Fabric Loader 启动 1.21.8
4. 服务器无需做任何改动，加入任何服务器都生效

---

## 二、配置文件

路径：`config/teammarker.json`

首次启动自动生成。所有字段都有空值兜底，删坏了也不会让游戏崩溃。

| 字段 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `enable` | 布尔 | `true` | 总开关。`false` 时所有标识和发光都关闭 |
| `glowEnabled` | 布尔 | `true` | 发光子开关。`false` 时只显示头顶前缀，不穿墙发光 |
| `prefixText` | 字符串 | `"[队友]"` | 头顶名字前缀。可填任意字符，如 `"★"`、`"[T]"` |
| `color` | 字符串 | `"e"` | 颜色代码（单字符）。前缀文字和发光轮廓都使用这个颜色 |
| `playerNameList` | 字符串数组 | `[]` | 队友名单。区分大小写写入，但匹配时大小写不敏感 |

### 配置示例

```json
{
  "enable": true,
  "glowEnabled": true,
  "prefixText": "[队友]",
  "color": "e",
  "playerNameList": ["Steve", "Alex", "Notch"]
}
```

---

## 三、颜色代码对照表

`color` 字段填下面任一单字符即可（不区分大小写）：

| 代码 | 颜色 | 视觉示例 | 代码 | 颜色 | 视觉示例 |
|---|---|---|---|---|---|
| `0` | 黑色 Black | ███ | `8` | 深灰 Dark Gray | ███ |
| `1` | 深蓝 Dark Blue | ███ | `9` | 蓝 Blue | ███ |
| `2` | 深绿 Dark Green | ███ | `a` | 绿 Green | ███ |
| `3` | 深青 Dark Aqua | ███ | `b` | 青 Aqua | ███ |
| `4` | 深红 Dark Red | ███ | `c` | 红 Red | ███ |
| `5` | 深紫 Dark Purple | ███ | `d` | 浅紫 Light Purple | ███ |
| `6` | 金 Gold | ███ | `e` | 黄 Yellow | ███ |
| `7` | 灰 Gray | ███ | `f` | 白 White | ███ |

**推荐配色**：
- 默认 `e`（黄色）— 最醒目
- `a`（绿色）— 友军视觉惯例
- `b`（青色）— 不撞色，多人服推荐
- `c`（红色）— 敌对玩家列表（反向标记）

---

## 四、客户端指令

所有指令**仅在本地执行**，不发送任何数据包到服务器。在聊天框输入：

| 指令 | 作用 |
|---|---|
| `/teammarker add <玩家名>` | 加入队友名单，自动保存到 JSON |
| `/teammarker remove <玩家名>` | 移出队友名单，自动保存 |
| `/teammarker list` | 在聊天框打印当前全部队友列表 |
| `/teammarker toggle` | 切换总开关（开 ↔ 关） |
| `/teammarker prefix <文字>` | 修改前缀标识文字并保存 |

**示例**：
```
/teammarker add Steve
/teammarker prefix [好友]
/teammarker list
```

---

## 五、快捷键

可在 **选项 → 控制设置 → 按键绑定 → TeamMarker 分类** 中修改默认键。

| 默认键 | 作用 | 反馈 |
|---|---|---|
| **K** | 加准星指向的玩家到名单 | `已添加队友: xxx (按 K 加下一个)` |
| **L** | 删准星指向的玩家 | `已移除队友: xxx` 或 `xxx 不在名单中` |
| **M** | 4 格半径内批量加入 | `范围添加完成：新增 N 个，已在名单中 M 个` |

### 快捷键使用场景

- **K**：单人加入。准星对准玩家按 K，准星自动移到下一个人继续按
- **L**：临时撤销某人的标识。准星对准按 L 即可
- **M**：队友站一起时一次全加。4 格半径球体内所有玩家都加进名单

---

## 六、修改颜色

颜色无法通过游戏内指令修改，需要直接改配置文件：

### 步骤
1. 退出游戏（或切到主菜单）
2. 用文本编辑器打开 `.minecraft/config/teammarker.json`
3. 修改 `color` 字段为上面颜色表的代码（如改成红色：`"color": "c"`）
4. 保存
5. 重启游戏

### 示例：改成红色 + 前缀变 `[友]`

```json
{
  "enable": true,
  "glowEnabled": true,
  "prefixText": "[友]",
  "color": "c",
  "playerNameList": ["Steve"]
}
```

或者用指令改前缀（不用退出游戏）：
```
/teammarker prefix [友]
```

---

## 七、常见问题

### 1. 与 HUD 模组冲突
所有 Mixin 逻辑包在 `try-catch (Throwable)` 中，冲突时会静默失败，不会让游戏崩溃。最坏情况是某个功能不生效，但游戏可正常运行。

### 2. 有bug请提出issue
仔细阅读readme后，游戏若依旧有报错可上报issue！

---

## 八、卸载

直接删除 `mods/teammarker-1.0.0.jar` 即可。配置文件 `config/teammarker.json` 不会被自动删除，如要彻底清理：

```powershell
Remove-Item "$env:APPDATA\.minecraft\mods\teammarker-1.0.0.jar"
Remove-Item "$env:APPDATA\.minecraft\config\teammarker.json"
```
