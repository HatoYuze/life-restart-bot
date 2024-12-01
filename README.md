# life-restart-bot

---

## 人生重开模拟器

一个基于 [mirai](https://github.com/mamoe/mirai) 的人生重开模拟器

### 如何使用？

- 下载 [release](https://github.com/HatoYuze/life-restart-bot/releases/latest) 中的 `life-restart-bot-xxx-mirai2.jar`
- 随后丢进你的 [mcl](https://github.com/iTXTech/mirai-console-loader) 的 `plugins` 中
- ~即可成功安装插件~

在授予权限后，发送指令 `/remake start` 开始人生模拟器
> 您可能需要 [project-mirai/chat-command](https://github.com/project-mirai/chat-command) 插件

首先，`bot`会发送随机抽取的天赋列表,如：

> 请在抽取的随机天赋中共选择3个：
>
> > 直接输入对应的序号即可，数字间由英文逗号相隔(如: 1,2,3)
>
>1：乡间微风
>
> 你出生在农村
>
>2：宠物大师
>
>宠物不会意外死亡
>
>3：十死无生
>
>体质-10
>
>4：钻石健身卡
>
>家境>10时体质+3
>
>5：转世重修
>
>渡劫失败重生
>
>...

共有 `10` 个天赋，
使用者需要发送**用逗号分割**的序号列表

如:`1,2,3` 或 `1，2，3` **(但 `1，2,3`这类混合使用中英文逗号的是无效的)**

随后，bot会按照所提供的序号选择天赋，并发送人生模拟器结果

> 人生模拟器结果是一个聊天记录，_未来会尝试更改为 图片 形式_
>
> 聊天记录分为三个部分
> - 天赋 & 初始属性点
> - 模拟经历： `x岁: 事件` 直到 `你死了。`
> - 结算 (分为 `颜值,家境,乐观,智力,力量,总分` 几项)

### 权限

若想要插件正常运行，你需要给予用户 `com.github.hatoyuze.restarter.life-restarter:command-execute` 权限

你可以使用以下指令，使所有用户**都可以**正常使用相关指令
> perm add u* com.github.hatoyuze.restarter.life-restarter:command-execute

如果你希望只有指定的用户可以使用，将`u*`改为`u对应qq号`即可，例如：
> perm add u114514 com.github.hatoyuze.restarter.life-restarter:command-execute

如果没有该权限，**任何**有关本插件的指令**都无法**正常调用哦！

### 特别鸣谢

- [VickScarlet/lifeRestart](https://github.com/VickScarlet/lifeRestart)
- [Scabiosa/lifeRestart](https://github.com/Scabiosa/lifeRestart)
