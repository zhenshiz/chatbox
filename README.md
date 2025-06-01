# 模组介绍

这是一款类似于Gal的对话框，为整合包或者地图的作者们制作。使用数据包编写对话框的主题样式和对话文本内容，提供更多的事件给KubeJS使用。

# 运行方式

## 指令

ChatBox添加自定义的指令，允许开发者使用指令来与mod交互。

<br />

/chatbox skip <数据包路径: ResourceLocation> <组名: String> <序号: int>
&#x20; 跳转到指定的对话

* 数据包路径：对话文本文件的位置

* 组名：对话文本文件内一段对话的标识符

* 序号（可选，默认为0）：一段对话中对话的序号

/chatbox theme <数据包路径: ResourceLocation>  切换对话框主题

* 数据包路径：对话框主题的位置

/chatbox open  打开最近一次对话

## 数据包路径

ChatBox 将自动加载对话框中对应路径下的Json文件，其中对话框主题的文件路径在`data/<命名空间>/chatbox/theme/<文件名>.json`，而对话框文本的文件路径在`data/<命名空间>/chatbox/dialogues/<文件名>.json`。

这些文件都可以通过`/reload`指令来重载，只不过重载并不代表目前的文件直接被使用了，你还是需要重新通过指令`/chatbox theme 和 /chatbox skip`去更新你当前的对话框主题里的参数和对话框的文本。

# 数据包编写

首先你要创建一个数据包，具体去参考[McWiki](https://zh.minecraft.wiki/w/%E6%95%B0%E6%8D%AE%E5%8C%85)的数据包介绍。你根据上方数据包路径里的介绍在对应的文件路径里创建一个对话框文本的Json和对话框主题的Json。对话框主要由以下几个构成：对话框，选项，历史聊天记录按钮，立绘，而主题主要配置的是其材质，位置，大小等等。而文本主要配置的是文本，交互等等。

## 对话框主题

基础架构如下：

```json
{
  "portrait": {},
  "option": {},
  "dialogBox": {},
  "logButton": {}
}
```

### 立绘

这里的立绘是预设的状态，将在写对话文本的时候调用立绘的标识符来使用。立绘分为3种：texture(图片)，player\_head(玩家头像)，item(物品材质)。每种的参数都是不一样的。话不多说，上案例：

```json
"portrait": {
"pic":{
"value": "chatbox:textures/portrait/authors.png",
"animation": "none",
"duration" : 20,
"easing": "EASE_IN_SINE",
"x": 0,
"y": 0,
"width": 10,
"height": 10,
"alignX": "left",
"alignY": "bottom",
"opacity": 100,
"renderOrder": 20
},
"player":{
"value": "@s",
"x": 0,
"y": 0,
"width": 10,
"height": 10,
"alignX": "left",
"alignY": "bottom",
"opacity": 100,
"renderOrder": 20
},
"item":{
"value": "apple",
"scale": 1,
"x": 0,
"y": 0,
"alignX": "left",
"alignY": "bottom",
"opacity": 100,
"renderOrder": 20
}
},
```

在上面的案例中包含了3种立绘里包含的所有的参数，接下来我们来一一介绍一下这些参数：

首先`pic player item`都是唯一的标识符，这些标识符只能出现一次，但是取名可以随便取，将在对话框文本中调用

**texture**

* value（必填）：立绘图片的资源包路径。

* animation（可选，默认无动画）：预设动画，可选参数: `NONE`无动画,`FADE_IN`渐入效果,`SLIDE_IN_FROM_BOTTOM`从底部滑入,`BOUNCE`弹射。

* duration（可选，默认20）：动画执行的时间，单位和用户电脑的帧率有关，具体时长为`duration/帧率`秒

* easing（可选，默认EASE\_IN\_SINE）：缓动函数，可选参数有`EASE_IN_SINE`,`EASE_OUT_SINE`,`EASE_IN_OUT_SINE`,`EASE_IN_QUAD`,`EASE_OUT_QUAD`,`EASE_IN_OUT_QUAD`,`EASE_IN_CUBIC`,`EASE_OUT_CUBIC`,`EASE_IN_OUT_CUBIC`,`EASE_IN_QUART`,`EASE_OUT_QUART`,`EASE_IN_OUT_QUART`,`EASE_IN_QUINT`,`EASE_OUT_QUINT`,`EASE_IN_OUT_QUINT`,`EASE_IN_EXPO`,`EASE_OUT_EXPO`,`EASE_IN_OUT_EXPO`,`EASE_IN_CIRC`,`EASE_OUT_CIRC`,`EASE_IN_OUT_CIRC`,`EASE_IN_BACK`,`EASE_OUT_BACK`,`EASE_IN_OUT_BACK`,`EASE_IN_ELASTIC`,`EASE_OUT_ELASTIC`,`EASE_IN_OUT_ELASTIC`,`EASE_IN_BOUNCE`,`EASE_OUT_BOUNCE`,`EASE_IN_OUT_BOUNCE`。具体每个参数代表的意义可以参考[缓动函数网站](https://easings.net/zh-cn)。

* x（可选，默认为0）：距离左边框的水平位置，根据用户mc屏幕宽度的百分比，范围为`-100-100`

* y（可选，默认为0）：距离上边框的垂直位置，根据用户mc屏幕高度的百分比，范围为`-100-100`

* width（可选，默认10）：宽度，根据用户mc屏幕宽度的百分比，范围为`0-100`

* height（可选，默认10）：高度，根据用户mc屏幕的高度的百分比，范围为`0-100`

* alignX（可选，默认左对齐）：水平对齐，可选参数有`LEFT`,`CENTER`,`RIGHT`

* alignY（可选，默认上对齐）：垂直对齐，可选参数有`TOP`,`CENTER`,`BOTTOM`

* opacity（可选，默认100）：透明度，范围`0-100`

* renderOrder（可选，默认20）：渲染顺序的权重

  **player\_head**

* value（必填）：玩家的名称或者UUID，必须得是正版玩家，我这里的@s表示的是当前打开对话框的玩家id

* x（可选，默认为0）：距离左边框的水平位置，根据用户mc屏幕宽度的百分比，范围为`-100-100`

* y（可选，默认为0）：距离上边框的垂直位置，根据用户mc屏幕高度的百分比，范围为`-100-100`

* width（可选，默认10）：宽度，根据用户mc屏幕宽度的百分比，范围为`0-100`

* height（可选，默认10）：高度，根据用户mc屏幕的高度的百分比，范围为`0-100`

* alignX（可选，默认左对齐）：水平对齐，可选参数有`LEFT`,`CENTER`,`RIGHT`

* alignY（可选，默认上对齐）：垂直对齐，可选参数有`TOP`,`CENTER`,`BOTTOM`

* opacity（可选，默认100）：透明度，范围`0-100`

* renderOrder（可选，默认20）：渲染顺序的权重

  **item**

* value（必选）：物品id

* scale（可选，默认1）：缩放比例

* x（可选，默认为0）：距离左边框的水平位置，根据用户mc屏幕宽度的百分比，范围为`-100-100`

* y（可选，默认为0）：距离上边框的垂直位置，根据用户mc屏幕高度的百分比，范围为`-100-100`

* alignX（可选，默认左对齐）：水平对齐，可选参数有`LEFT`,`CENTER`,`RIGHT`

* alignY（可选，默认上对齐）：垂直对齐，可选参数有`TOP`,`CENTER`,`BOTTOM`

* opacity（可选，默认100）：透明度，范围`0-100`

* renderOrder（可选，默认20）：渲染顺序的权重

### 选项

```json
{
  "option":{
    "texture": "chatbox:textures/options/default_no_checked_option.png",
    "selectTexture": "chatbox:textures/options/default_checked_option.png",
    "lockTexture": "chatbox:textures/options/default_lock_checked_option.png",
    "optionChatX": 0,
    "optionChatY": 0,
    "textAlign": "left",
    "x": 0,
    "y": 0,
    "width": 10,
    "height": 10,
    "alignX": "left",
    "alignY": "bottom",
    "opacity": 100,
    "renderOrder": 10
  }
}
```

* texture（必填）：选项默认贴图，

* selectTexture（必填）：鼠标悬浮时的贴图

* lockTexture（必填）：选项上锁时的贴图

* optionChatX（可选，默认0）：选项文本水平偏移

* optionChatY（可选，默认0）：选项文本垂直偏移

* textAlign（可选，默认左对齐）：文本对齐，可选参数：`LEFT`,`CENTER`,`RIGHT`

* x（可选，默认为0）：贴图距离左边框的水平位置，根据用户mc屏幕宽度的百分比，范围为`-100-100`

* y（可选，默认为0）：第一个选项贴图距离上边框的垂直位置，其它贴图位置会自动根据高度自动生成，根据用户mc屏幕高度的百分比，范围为`-100-100`

* width（可选，默认10）：贴图宽度，根据用户mc屏幕宽度的百分比，范围为`0-100`

* height（可选，默认10）：贴图高度，根据用户mc屏幕的高度的百分比，范围为`0-100`

* alignX（可选，默认左对齐）：水平对齐，可选参数有`LEFT`,`CENTER`,`RIGHT`

* alignY（可选，默认上对齐）：垂直对齐，可选参数有`TOP`,`CENTER`,`BOTTOM`

* opacity（可选，默认100）：贴图透明度，范围`0-100`

* renderOrder（可选，默认10）：渲染顺序的权重

### 对话框

```javascript
{
  "dialogBox":{
    "texture": "chatbox:textures/chatbox/default_dialog_box.png",
    "lineWidth": 70,
    "nameX": 0,
    "nameY": 0,
    "textX": 0,
    "textY": 0,
    "x": 0,
    "y": 0,
    "width": 10,
    "height": 10,
    "alignX": "left",
    "alignY": "bottom",
    "opacity": 100,
    "renderOrder": 0
  }
}
```

* texture（必填）：对话框贴图

* lineWidth（必填）：一行文本的宽度，根据用户mc屏幕宽度的百分比，范围为`0-100`

* nameX（可选，默认0）：名称距离左边框的水平位置

* nameY（可选，默认0）：名称距离贴图上边框的垂直位置

* textX（可选，默认0）：文本距离左边框的水平位置

* textY（可选，默认0）：文本距离贴图上边框的垂直位置

* x（可选，默认为0）：贴图距离左边框的水平位置，根据用户mc屏幕宽度的百分比，范围为`-100-100`

* y（可选，默认为0）：贴图距离上边框的垂直位置，根据用户mc屏幕高度的百分比，范围为`-100-100`

* width（可选，默认10）：贴图宽度，根据用户mc屏幕宽度的百分比，范围为`0-100`

* height（可选，默认10）：贴图高度，根据用户mc屏幕的高度的百分比，范围为`0-100`

* alignX（可选，默认左对齐）：水平对齐，可选参数有`LEFT`,`CENTER`,`RIGHT`

* alignY（可选，默认上对齐）：垂直对齐，可选参数有`TOP`,`CENTER`,`BOTTOM`

* opacity（可选，默认100）：贴图透明度，范围`0-100`

* renderOrder（可选，默认0）：渲染顺序的权重

### 历史聊天记录按钮

```json
{
  "logButton":{
    "texture": "chatbox:textures/log/default_log.png",
    "hoverTexture": "chatbox:textures/log/default_hover_log.png",
    "x": 0,
    "y": 0,
    "width": 10,
    "height": 10,
    "alignX": "left",
    "alignY": "bottom",
    "opacity": 100,
    "renderOrder": 40
  }
}
```

* texture（必填）：贴图

* hoverTexture（必填）：鼠标悬浮时的贴图

* x（可选，默认为0）：距离左边框的水平位置，根据用户mc屏幕宽度的百分比，范围为`-100-100`

* y（可选，默认为0）：距离上边框的垂直位置，根据用户mc屏幕高度的百分比，范围为`-100-100`

* width（可选，默认10）：宽度，根据用户mc屏幕宽度的百分比，范围为`0-100`

* height（可选，默认10）：高度，根据用户mc屏幕的高度的百分比，范围为`0-100`

* alignX（可选，默认左对齐）：水平对齐，可选参数有`LEFT`,`CENTER`,`RIGHT`

* alignY（可选，默认上对齐）：垂直对齐，可选参数有`TOP`,`CENTER`,`BOTTOM`

* opacity（可选，默认100）：透明度，范围`0-100`

* renderOrder（可选，默认40）：渲染顺序的权重

### mod资源包自带的贴图

本模组自带了一些有关于对话框的贴图，如果觉得自己做一套对话框的贴图比较麻烦的话，可以来使用本模组自带的贴图。本模组内的贴图一共分为2套风格的主题：一种是RPG，另一种是GalGame。

```json
RPG：
chatbox:textures/chatbox/default_dialog_box.png
chatbox:textures/options/default_checked_option.png
chatbox:textures/options/default_lock_checked_option.png
chatbox:textures/options/default_no_checked_option.png
chatbox:textures/log/default_log.png
chatbox:textures/log/default_hover_log.png
GalGame：
chatbox:textures/chatbox/gal_dialog_box.png
chatbox:textures/options/gal_checked_option.png
chatbox:textures/options/gal_no_checked_option.png
chatbox:textures/log/default_log.png
chatbox:textures/log/default_hover_log.png
```

### 部分参数细解

Q：renderOrder这个参数到底有什么用？

A：这个参数是决定这个组件在页面上渲染的顺序的，因为你在实际使用其它一些模组的对话框的时候可能会遇到这种问题：我无法将人物立绘显示在对话框和选项的下面，这可能是因为这些模组是锁死的渲染的顺序，先渲染的对话框和选项，再渲染的人物立绘，此时这个人物立绘就必定在上面一层了，。而renderOrder就是为了解决这个问题的，默认情况下对话框为0，选项为10，立绘为20，历史聊天记录按钮为30。如果我将立绘的权重设置-10和20就会是下面2种效果，这就是这个参数的作用：

[![78e3859c d8b6 4dba ab7d 9aae82bc5cfc](https://img.picgo.net/2025/05/30/78e3859c-d8b6-4dba-ab7d-9aae82bc5cfcd891e02b685dbb79.png "78e3859c d8b6 4dba ab7d 9aae82bc5cfc")](https://www.picgo.net/image/78e3859c-d8b6-4dba-ab7d-9aae82bc5cfc.y4NIsb)[![c5cef4ac 7fcf 45d4 bf98 2f0a93a61259](https://img.picgo.net/2025/05/30/c5cef4ac-7fcf-45d4-bf98-2f0a93a61259b8fb10ba5af577e2.png "c5cef4ac 7fcf 45d4 bf98 2f0a93a61259")](https://www.picgo.net/image/c5cef4ac-7fcf-45d4-bf98-2f0a93a61259.y4NPMC)

Q：部分参数的枚举字符串可以是小写吗？

A：所有的枚举字符串参数都是无视大小写的，你可以任意选择都可以被识别。

## 对话文本

基础框架如下：

```javascript
{
  "$introduce": "这是一段介绍本模组的文本",
  "dialogues": {
    "start":[{}]
  }
}
```

其中\$introduce纯装饰作用，不写也可以，我这里写是提供一个建议，给整个文件一个大致的主题防止开发者迷路找不到自己写的文本，比如主线，支线1，支线2等等标识。而dialogues将包含该文件中所有的对话文本，这里的start则是分组的唯一标识，在这里可以编写这个分组里的多段对话。

接下来我们来介绍一下一段对话里都有哪些参数：

```javascript
{
  "dialogBox":{
    "name": "chatbox.test.name.1",
    "text": "chatbox.test.text.1",
    "isTranslatable": true
  },
  "portrait": [
    {
      "type": "texture",
      "value": "authors"
    }
  ],
  "options":[
    {
      "text": "chatbox.test.option.1",
      "isTranslatable": true,
      "isLock": true,
      "lock": {
        "objective" : "",
        "value" : ""
      },
      "hidden": {
        "objective" : "",
        "value" : ""
      },
      "isHidden" : false,
      "next": 1
      "click": {
        "type" : "",
        "value" : ""
      }
      "tooltip": "chatbox.test.tooltip.1",
    }
  ],
  "sound": "minecraft:ambient.cave",
  "volume": 1,
  "pitch": 1
}
```

这段就包含了一段文本里的所有的参数，接下来我们一点一点来介绍吧：

**音乐**

* sound（可选）：音乐的路径比如`minecraft:ambient.cave`

* volume（可选）：音量

* pitch（可选）：音高

  **对话框**

* name（可选）：名称

* text（可选）：文本

* isTranslatable（可选，默认为false）：是否为翻译键

  **立绘**

* type（必填）：立绘类型，可选参数有：`TEXTURE`,`PLAYER_HEAD`,`ITEM`

* value（必填）：这里填的就是我们在主题文件中预设的立绘标识符了

  **选项**

* text（选填）：选项文本

* isTranslatable（选填，默认false）：是否为翻译键

* isLock（选填，默认false）：是否上锁

* lock（选填）：设置解锁的条件，里面有2个参数`objective`和`value`，其中objective指的是计分板的名字，value是这个计分板的key值，如果值为1则解锁，反之上锁

* isHidden（选填，默认false）：是否隐藏

* hidden（选填）：设置解锁的条件，里面有2个参数`objective`和`value`，其中objective指的是计分板的名字，value是这个计分板的key值，如果值为1则解锁，反之上锁

* next（选填）：如果填的为数字则跳转到当前分组对应数字序号的对话，如果填的是字符串则跳转到对应分组的第一句话，如果不填则默认跳转到当前分组当前对话的下一句话

* click（选填）：点击按钮触发的自定义事件，里面有2个参数`type`和`value`，其中type目前只支持command（有好的建议可以给我Github发发issues），value则是要执行的指令名

* tooltip（选题）：鼠标悬浮时弹出的文本

## 占位符

你可以自由地在说话者和对话文本、选项中使用占位符，目前已有的占位符如下：

* @s：当前打开对话框的玩家id，如果在@前面再加一个@将取消转义行为，比如假设你的玩家id为`player`，你用@s返回的是player，你用@@s返回的是正常文本的@s了

  # 关于KubeJS联动

  本模组提供了3个事件给KubeJS调用来添加自己想加的东西，你需要在NeoForge的事件中调用它们，例子如下：

  ```javascript
  let $ChatBoxRender$Post = Java.loadClass("com.zhenshiz.chatbox.event.ChatBoxRender$Post");

  //在对话框渲染前触发，提供了GuiGraphics可以添加自己想要的渲染，可取消事件
  NativeEvents.onEvent($ChatBoxRender$Post,event=>{
    let getGuiGraphics = event.getGuiGraphics()
  })

  let $ChatBoxRender$Pre = Java.loadClass("com.zhenshiz.chatbox.event.ChatBoxRender$Pre");

  //在对话框渲染后触发，提供了GuiGraphics可以添加自己想要的渲染
  NativeEvents.onEvent($ChatBoxRender$Pre,event=>{
    let getGuiGraphics = event.getGuiGraphics()
  })

  let $SkipChatEvent = Java.loadClass("com.zhenshiz.chatbox.event.SkipChatEvent");

  //在跳转对话时触发，提供了对话的数据包文件，分组和序号来方便用户对特别某一句对话添加自己想要的功能
  NativeEvents.onEvent($SkipChatEvent, event => {
    let { resourceLocation, group, index } = event
  })
  ```

  # Q & A

  Q：这个模组未来会移植到其它版本或者其它模组加载器吗？

  A：至少我不会，但是欢迎其它感兴趣的开发者来fork我的模组

# 未来可能更新的功能

1. 给立绘图片提供自定义动画支持
2. 联动一个字体动画的mod（欢迎提提建议）
3. 联动一个npc模组（欢迎提提建议）

