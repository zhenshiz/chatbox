# 模组介绍

这是一款类似于Gal的对话框，为整合包或者地图的作者们制作。使用数据包编写对话框的主题样式和对话文本内容，提供更多的事件给KubeJS使用。

## 指令

ChatBox添加自定义的指令，允许开发者使用指令来与mod交互。

<br />

/chatbox skip <数据包路径: ResourceLocation> <组名: String> <序号: int> <目标实体: Target1 Target2 Target3 ...>
&#x20; 跳转到指定的对话

* 数据包路径：对话文本文件的位置

* 组名：对话文本文件内一段对话的标识符

* 序号（可选，默认为0）：一段对话中对话的序号

* 目标实体（可选）：指定对话的目标实体，多个实体之间用空格隔开，用于在对话文本或指令中引用目标实体的属性（详细介绍见[占位符](#占位符)）或进行交互。

/chatbox theme <数据包路径: ResourceLocation>  切换对话框主题

* 数据包路径：对话框主题的位置

/chatbox open  打开最近一次对话

/chatbox maxTriggerCount <数据包路径: ResourceLocation> <最大访问次数:int> 设置一段对话的最大访问次数

* 数据包路径：对话框主题的位置

* 最大访问次数：<-1表示无限访问次数，0表示无法访问，非0则为设置最大访问次数>

/chatbox maxTriggerCount reset  重置最大访问次数

/chatbox command nextDialogue 跳转下一句话

/chatbox command autoPlay <开关：Boolean> 开关自动对话

/chatbox command isScreen <开关：Boolean> true为屏幕主题模式，该模式有对话更有沉浸感可以用鼠标选择选项；false为渲染主题模式，该模式下玩家可以正常移动交互，通过鼠标和按键来交互对话框（可关，然后自己用指令交互也行）

## 数据包路径

ChatBox 将自动加载对话框中对应路径下的Json文件，其中对话框主题的文件路径在`data/<命名空间>/chatbox/theme/<文件名>.json`，而对话框文本的文件路径在`data/<命名空间>/chatbox/dialogues/<文件名>.json`。

这些文件都可以通过`/reload`指令来重载，只不过重载并不代表目前的文件直接被使用了，你还是需要重新通过指令`/chatbox theme` 和 `/chatbox skip`去更新你当前的对话框主题里的参数和对话框的文本。

# 数据包编写

你可以在本模组的jar包里面找到数据包案例参考学习。
- **需要注意的是，模组内置的数据文件并不是标准格式的数据包，如需复制出来修改，记得修改文件名！**
- **需要注意的是，模组内置的数据文件并不是标准格式的数据包，如需复制出来修改，记得加上pack.mcmeta文件！**
- **需要注意的是，模组内置的数据文件并不是标准格式的数据包，如需复制出来修改，记得修改文件名以及加上pack.mcmeta文件！**

首先你要创建一个数据包，具体去参考[McWiki](https://zh.minecraft.wiki/w/%E6%95%B0%E6%8D%AE%E5%8C%85)的数据包介绍。你根据上方数据包路径里的介绍在对应的文件路径里创建一个对话框文本的Json和对话框主题的Json。对话框主要由以下几个构成：对话框，选项，历史聊天记录按钮，立绘，而主题主要配置的是其材质，位置，大小等等。而文本主要配置的是文本，交互等等。接下来我会给你介绍2种数据包，在此之前你需要了解一下3种对字段的要求：必填（必须填写，不填写可能会导致游戏崩溃），建议填（不填写不会导致游戏崩溃，在特定的场合不填写也许能实现你的需求），可选（模组提供了默认值或者是扩展功能，你不填写也没问题）

## 对话框主题

基础架构如下：

```json
{
  "portrait": {},
  "option": {},
  "dialogBox": {},
  "functionalButton": {},
  "keyPrompt": {},
  "customAnimation": {}
}
```

### 立绘

这里的立绘是预设的状态，将在写对话文本的时候调用立绘的标识符来使用。立绘分为3种：texture(图片)，player\_head(玩家头像)，item(物品材质)。每种的参数都是不一样的。话不多说，上案例：

```json
{
  "portrait": {
    "pic": {
      "type": "texture",
      "value": "chatbox:textures/portrait/authors.png",
      "animation": "BOUNCE",
      "x": 0,
      "y": 0,
      "width": 10,
      "height": 10,
      "alignX": "left",
      "alignY": "bottom",
      "opacity": 100,
      "scale": 1,
      "renderOrder": 20,
      "angle": 0,
      "attachment": [
        {
          "value": "chatbox:textures/options/default_no_checked_option.png",
          "x": 3,
          "y": 3,
          "width": 5,
          "height": 5
        }
      ]
    },
    "player": {
      "type": "player_head",
      "value": "@s",
      "x": 0,
      "y": 0,
      "width": 10,
      "height": 10,
      "alignX": "left",
      "alignY": "bottom",
      "opacity": 100,
      "scale": 1,
      "renderOrder": 20,
      "angle": 0,
      "customAnimation": [
        {
          "time": 50,
          "x": 0,
          "y": 0,
          "scale": 1,
          "opacity": 100,
          "angle": 0,
          "easing": "EASE_OUT_SINE"
        },
        {
          "time": 100,
          "x": 0,
          "y": 0,
          "scale": 1,
          "opacity": 100,
          "angle": 360,
          "easing": "EASE_OUT_SINE"
        }
      ]
    },
    "item": {
      "type": "item",
      "value": "apple",
      "scale": 1,
      "x": 0,
      "y": 0,
      "alignX": "left",
      "alignY": "bottom",
      "opacity": 100,
      "renderOrder": 20,
      "angle": 0,
      "customAnimation": [
        {
          "time": 50,
          "x": 0,
          "y": 0,
          "scale": 1,
          "opacity": 100,
          "angle": 0,
          "easing": "EASE_OUT_SINE"
        },
        {
          "time": 50,
          "x": 0,
          "y": 0,
          "scale": 1,
          "opacity": 100,
          "angle": 360,
          "easing": "EASE_OUT_SINE"
        }
      ]
    }
  }
}
```

在上面的案例中包含了3种立绘里包含的所有的参数，接下来我们来一一介绍一下这些参数：

首先`pic player item`都是唯一的标识符，这些标识符只能出现一次，但是取名可以随便取，将在对话框文本中调用。每个立绘里都有一个type表示当前立绘的类型，是必须要填的，不然模组将无法识别你这块立绘表述的内容

**共通参数**

* x（可选，默认为0）：距离左边框的水平位置，根据用户mc屏幕宽度的百分比

* y（可选，默认为0）：距离上边框的垂直位置，根据用户mc屏幕高度的百分比

* alignX（可选，默认左对齐）：水平对齐，可选参数有`LEFT`,`CENTER`,`RIGHT`

* alignY（可选，默认上对齐）：垂直对齐，可选参数有`TOP`,`CENTER`,`BOTTOM`

* opacity（可选，默认100）：透明度，范围`0-100`

* scale（可选，默认1）：缩放比例

* renderOrder（可选，默认20）：渲染顺序的权重

* angle（可选，默认0）：旋转的角度，角度制，数值为正顺时针旋转，为负则逆时针

* animation（可选）：预设动画，直接从主题文件中预设的动画里获取对应的动画并应用。mod内置了3种可以直接使用的动画：`FADE_IN`渐入效果，`SLIDE_IN_FROM_BOTTOM`从底部滑入，`BOUNCE`弹跳。你也可以自己在主题文件中预设新的动画，配置方法见[预设动画](#预设动画)。

* customAnimation（可选）：自定义动画，采用的是关键帧动画的配置方式。外侧的配置是0帧的默认值，内侧的配置是一个数组，每一项都包括这段动画执行的时间time，第time帧时的材质texture，位置x、y，位置偏移量xOffset、yOffset，缩放大小scale，透明度opacity，旋转的角度angle以及这段动画采用的缓动函数easing，**这些参数除了time都是可选的，如果值有效，立绘相应的值就会在动画过程中渐变**。动画执行的顺序是从数组的第一项到最后一项。

* loop（可选，默认值false）：是否循环动画

  **PS：如果你同时设置了有效的animation和customAnimation，那么只有customAnimation会生效。**

**texture**

* type（必填，必须填为`TEXTURE`）：类型

* value（必填）：立绘图片的资源包路径。

* width（可选，默认10）：宽度，根据用户mc屏幕宽度的百分比，范围为`>0`

* height（可选，默认10）：高度，根据用户mc屏幕的高度的百分比，范围为`>0`

* attachment（可选）：用于将多张图片拼在一起成一个新的立绘，里面参数有`value`图片的资源包路径，`x`相对于外部图片的偏移x坐标，`y`相对于外部图片的偏移y坐标，`width`宽度，根据用户mc屏幕宽度的百分比，范围为`>0`，`height`高度，根据用户mc屏幕的高度的百分比，范围为`>0`。

**player\_head**

* type（必填，必须填为`PLAYER_HEAD`）：类型

* value（必填）：玩家的名称或者UUID，必须得是正版玩家，我这里的@s表示的是当前打开对话框的玩家id

* width（可选，默认10）：宽度，根据用户mc屏幕宽度的百分比，范围为`>0`

* height（可选，默认10）：高度，根据用户mc屏幕的高度的百分比，范围为`>0`

  **PS：因为玩家头像渲染的宽和高是必须一致的，所以实际这里的设置的width和height是共同计算后得到头像的边长的，边长=width​\*屏幕的宽+height\*屏幕的高。所以一般情况下，你只需要在width和height中选其中一个即可。**

**item**

* type（必填，必须填为`item`）：类型

* value（必填）：物品id

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

* texture（建议填）：选项默认贴图，

* selectTexture（建议填）：鼠标悬浮时的贴图

* lockTexture（建议填）：选项上锁时的贴图

* optionChatX（可选，默认0）：选项文本水平偏移

* optionChatY（可选，默认0）：选项文本垂直偏移

* textAlign（可选，默认左对齐）：文本对齐，可选参数：`LEFT`,`CENTER`,`RIGHT`

* x（可选，默认为0）、y（可选，默认为0）、width（可选，默认10）、height（可选，默认10）、alignX（可选，默认左对齐）、alignY（可选，默认上对齐）：效果同[图片立绘](#立绘)的对应参数，此处不赘述

* opacity（可选，默认100）：贴图透明度，范围`0-100`

* renderOrder（可选，默认10）：渲染顺序的权重

  ### 对话框

```json
{
  "dialogBox": {
    "texture": "chatbox:textures/chatbox/default_dialog_box.png",
    "lineWidth": 70,
    "nameX": 0,
    "nameY": 0,
    "textX": 0,
    "textY": 0,
    "textAlign": "left",
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

* texture（建议填）：对话框贴图

* lineWidth（必填）：一行文本的宽度，根据用户mc屏幕宽度的百分比，范围为`0-100`

* nameX（可选，默认0）：名称距离左边框的水平位置

* nameY（可选，默认0）：名称距离贴图上边框的垂直位置

* textX（可选，默认0）：文本距离左边框的水平位置

* textY（可选，默认0）：文本距离贴图上边框的垂直位置

* textAlign（可选，默认左对齐）：文本对齐，可选参数：`LEFT`,`CENTER`,`RIGHT`

* x（可选，默认为0）、y（可选，默认为0）、width（可选，默认10）、height（可选，默认10）、alignX（可选，默认左对齐）、alignY（可选，默认上对齐）：效果同[图片立绘](#立绘)的对应参数，此处不赘述

* opacity（可选，默认100）：贴图透明度，范围`0-100`

* renderOrder（可选，默认0）：渲染顺序的权重

  ### 功能按钮

```json
{
  "functionalButton":[
    {
      "type": "LOG",
      "texture": "chatbox:textures/button/default_log.png",
      "hoverTexture": "chatbox:textures/button/default_hover_log.png",
      "x": 0,
      "y": 0,
      "width": 5,
      "height": 8,
      "alignX": "right",
      "alignY": "bottom",
      "opacity": 100,
      "renderOrder": 30
    },
    {
      "type": "FASTFORWARD",
      "texture": "chatbox:textures/button/default_fastforward.png",
      "hoverTexture": "chatbox:textures/button/default_hover_fastforward.png",
      "x": -5,
      "y": 0,
      "width": 5,
      "height": 8,
      "alignX": "right",
      "alignY": "bottom",
      "opacity": 100,
      "renderOrder": 30
    },
    {
      "type": "AUTOPLAY",
      "texture": "chatbox:textures/button/default_autoplay.png",
      "hoverTexture": "chatbox:textures/button/default_hover_autoplay.png",
      "x": -10,
      "y": 0,
      "width": 5,
      "height": 8,
      "alignX": "right",
      "alignY": "bottom",
      "opacity": 100,
      "renderOrder": 30
    }
  ]
}
```

**PS：功能按钮的配置是一个数组，每个元素都是一个功能按钮的配置，如果你不写某个功能按钮的配置，那么该功能按钮就不会显示，按钮对应的功能也就无法使用。**

* type（必填）按钮的类型，目前支持`LOG`历史记录按钮，`FASTFORWARD`快进按钮，`AUTOPLAY`自动播放按钮

* texture（必填）：贴图

* hoverTexture（必填）：鼠标悬浮时的贴图

* x（可选，默认为0）、y（可选，默认为0）、width（可选，默认5）、height（可选，默认8）、alignX（可选，默认右对齐）、alignY（可选，默认下对齐）：效果同[图片立绘](#立绘)的对应参数，此处不赘述

* opacity（可选，默认100）：透明度，范围`0-100`

* renderOrder（可选，默认30）：渲染顺序的权重

  PS：自动播放的机制：如果在对话文件中设置了语音（`sound`定义的音效，`command`指令播放音效不算），则会在语音播放完一秒后进入下一句话；如果没有设置声音，则会在文本全部展示后的一秒后（默认值，可修改）进入下一句话。

  ### 按键提示

  该配置只会在玩家在配置文件中选择非屏幕主题对话框时才会有用

```json
{
  "keyPrompt": {
    "visible": true,
    "mouseTextureWidth": 16,
    "mouseTextureHeight": 16,
    "rightClickTexture": "chatbox:textures/key/right_mouse.png",
    "scrollTexture": "chatbox:textures/key/scroll_mouse.png",
    "x": 0,
    "y": 0,
    "alignX": "left",
    "alignY": "bottom",
    "opacity": 100,
    "renderOrder": 40
  }
}
```

* visible（可选，默认true）：是否显示按键提示

* mouseTextureWidth（可选，默认16）：鼠标材质的宽度

* mouseTextureHeight（可选，默认16）：鼠标材质的高度

* rightClickTexture（可选）：右键鼠标材质，默认值`chatbox:textures/key/right_mouse.png`

* scrollTexture（可选）：滚轮鼠标材质，默认值`chatbox:textures/key/scroll_mouse.png`

* x（可选，默认为0）、y（可选，默认为0）、alignX（可选，默认右对齐）、alignY（可选，默认下对齐）：效果同[图片立绘](#立绘)的对应参数，此处不赘述

* opacity（可选，默认100）：透明度，范围`0-100`

* renderOrder（可选，默认30）：渲染顺序的权重

  ### 预设动画

  本模组只内置了3个预设的动画，你可能觉得不够用，于是你可以在这里添加预设动画。预设的立绘在填写`animation`字段时，直接填这里的动画标识即可使用动画。\
  虽然预设动画是写在主题文件里面的，但是它并不与主题绑定，只要主题被加载了，这些动画就全局通用。\
  **动画标识区分大小写**。由于预设动画全局通用，若有同名动画，后加载的会覆盖先加载的（你问我加载顺序是由什么决定的？我也不知道，你问代码吧），因此建议采用MC通用的命名方式——命名空间:名称——以防止重名。

```json
{
  "customAnimation": {
    "TEST:ROTATE": [
      {
        "time": 360,
        "angle": -360
      }
    ]
  }
}
```

### mod资源包自带的贴图

本模组自带了一些有关于对话框的贴图，如果觉得自己做一套对话框的贴图比较麻烦的话，可以来使用本模组自带的贴图。

```md
对话框材质：
chatbox:textures/chatbox/default_dialog_box.png
chatbox:textures/chatbox/gal_dialog_box.png
选项材质：
chatbox:textures/options/default_checked_option.png
chatbox:textures/options/default_lock_checked_option.png
chatbox:textures/options/default_no_checked_option.png
chatbox:textures/options/gal_checked_option.png
chatbox:textures/options/gal_no_checked_option.png
功能按钮：
chatbox:textures/button/default_log.png
chatbox:textures/button/default_hover_log.png
chatbox:textures/button/default_fastforward.png
chatbox:textures/button/default_hover_fastforward.png
chatbox:textures/button/default_autoplay.png
chatbox:textures/button/default_hover_autoplay.png
```

### 部分参数细解

Q：renderOrder这个参数到底有什么用？

A：这个参数是决定这个组件在页面上渲染的顺序的，因为你在实际使用其它一些模组的对话框的时候可能会遇到这种问题：我无法将人物立绘显示在对话框和选项的下面，这可能是因为这些模组是锁死的渲染的顺序，先渲染的对话框和选项，再渲染的人物立绘，此时这个人物立绘就必定在上面一层了。而renderOrder就是为了解决这个问题的，默认情况下对话框为0，选项为10，立绘为20，历史聊天记录按钮为30。如果我将立绘的权重设置-10和20就会是下面2种效果，这就是这个参数的作用：

[![](https://raw.githubusercontent.com/zhenshiz/picx-images-hosting/master/3b1b2cd8-0357-462e-8b5b-4356fc24e9ee.esz2j679y.webp "renderOrder -10")]()[![](https://raw.githubusercontent.com/zhenshiz/picx-images-hosting/master/fe426f41-f0f4-4cdf-962a-8ad14ddf543e.67xxbttniw.webp "renderOrder 20")]()

**右键隐藏对话框时，也会自动将在对话框之上的立绘隐藏，也就是将所有立绘renderOrder比对话框renderOrder值大的隐藏，模组内部将这个视作区分是背景用立绘和对话框用立绘的区别。**

Q：部分参数的枚举字符串可以是小写吗？

A：所有的枚举字符串参数都是无视大小写的，你可以任意选择都可以被识别。

## 对话文本

在介绍对话文件的架构前，先介绍一下我对对话部分的架构设计，这将有利于你理解为什么要这么写这个文件。我的对话架构总结一下就是这么一张图：
[![](https://github.com/zhenshiz/picx-images-hosting/raw/master/5bc82faf-25d1-482d-bcf0-95561db0e1ea.491qlho57b.webp "对话架构")]()

将所有的对话分模块分组，每个模块都有一个唯一的标识，这也是为什么我进入一个对话，需要一个分组id和一个数字的序号。这个架构的好处是可以很好的支持多分支的剧本，不会使大量对话堆在一起而乱套了。

介绍完架构后，我们展示对话文件的基础框架：

```json
{
  "$introduce": "这是一段介绍本模组的文本",
  "dialogues": {
    "start":[{}]
  },
  "isEsc": true,
  "isPause": true,
  "isHistoricalSkip": true,
  "maxTriggerCount": -1,
  "isScreen": true,
  "theme": "",
  "criteria": {},
  "animationFPS": 60,
  "autoPlayTick": 20
}
```

**PS：所有文本都是翻译键，若你懒得写翻译键，也可以直接写文本，都无所谓。**\
**（什么？你说看这篇文档看得头大？你可以尝试把这个文档的内容直接喂给AI，让它帮你写对话json哦，亲测有效！）**

* \$introduce（可选）：纯装饰作用，不写也可以，我这里写是提供一个建议，给整个文件一个大致的主题防止开发者迷路找不到自己写的文本，比如主线，支线1，支线2等等标识。

* dialogues（必填）：包含该文件中所有的对话文本，这里的start则是分组的唯一标识，在这里可以编写这个分组里的多段对话。

* animationFPS（可选，默认60）：对话中立绘动画的帧率，默认60帧/秒。一般情况下MC的帧率并不稳定，若不限制动画帧率，不同设备看到的动画效果就不一样，也难以计时。若你的设备性能较低（不能稳定60帧以上），则会让动画以游戏实际帧率播放。

* autoPlayTick（可选，默认20）：自动播放下一句话的延迟时间，单位为游戏刻（1秒=20游戏刻）。

* isEsc（可选，默认true）：玩家是否可以通过Esc关闭对话框。

* isPause（可选，默认true）：玩家在单机模式下打开对话框是否能时停。

* isHistoricalSkip（可选，默认true）：历史记录界面是否可以回溯对话。

* maxTriggerCount（可选，默认-1）：这段对话可被触发的次数，默认为无限触发。

* isScreen（可选，默认不变化）：是否启用屏幕主题，不填变化不做变化。

* theme（可选）：进入对话时会自动设置为这个主题。

* criteria（可选）：提供一个自定义的进度触发器，写法和原版进度一模一样，具体可以参考[McWiki](https://zh.minecraft.wiki/w/%E8%BF%9B%E5%BA%A6%E5%AE%9A%E4%B9%89%E6%A0%BC%E5%BC%8F)，你也可以利用[生成器](https://misode.github.io/advancement/)去生成进度的Json。

  ***

接下来我们来介绍一下一句对话里都有哪些参数：

```json
{
  "renderEvents": [
    {"trigger": "start", "type": "hide", "value": ""}
  ],
  "dialogBox": {
    "name": "chatbox.test.name.1",
    "text": "chatbox.test.text.1",
    "renderEvents": []
  },
  "portrait": [
    "authors",
    {
      "id": "authors",
      "customAnimation": [
        {
          "time": 360,
          "angle": -360,
          "scale": 2
        }
      ],
      "renderEvents": []
    }
  ],
  "options": [
    {
      "text": "对目标造成10点伤害",
      "tooltip": "需要物品栏第一格是下界合金剑",
      "isLock": true,
      "unlockCommand": "execute if entity @s[nbt={Inventory:[{id:\"minecraft:netherite_sword\",Slot:0b}]}]",
      "click": {
        "type": "command",
        "value": "damage <target1.uuid> 10 minecraft:generic by @s from @s; give @s minecraft:diamond"
      },
      "next": "0"
    },
    {
      "text": "测试隐藏",
      "isLock": false,
      "unlockCommand": "execute if entity @s[nbt={Inventory:[{id:\"minecraft:diamond\"}]}]",
      "click": {
        "type": "command",
        "value": "say 我有钻石了"
      },
      "next": "0"
    },
    {
      "text": "退出",
      "next": "1"
    }
  ],
  "video": {
    "path": "",
    "canControl": false,
    "canSkip": true,
    "loop": false,
    "renderEvents": []
  },
  "sound": "minecraft:ambient.cave",
  "command": "say 你好; give @s minecraft:iron_ingot",
  "backgroundImage": "",
  "clearOldPortrait": true,
  "removePortrait": []
}
```

这段就包含了一段文本里的所有的参数，接下来我们一点一点来介绍吧：

* sound（可选）：音乐的路径比如`minecraft:ambient.cave`

* command（可选）：执行指令，支持多段指令，用`;`分隔，指令权限为2级

* backgroundImage（可选）：背景图片

* renderEvents（可选）：对话组件渲染事件，详见[对话组件渲染事件](#对话组件渲染事件)

  ### **对话框**

* name（可选）：名称

* text（必填）：文本

* renderEvents（可选）：对话组件渲染事件，详见[对话组件渲染事件](#对话组件渲染事件)

  ### **立绘以及立绘控制系统**

* portrait（可选）：这里可以填字符串或者json。如果是字符串填的就是我们在主题文件中预设的立绘标识符了；如果是json，就说明你是想要在主题预设的前提上覆盖部分信息，可以避免就修改一点点信息就要在主题文件里写上一堆预设，让我们的配置更加自由，且额外支持添加`renderEvents`

* clearOldPortrait（可选）：是否清除上一句对话的立绘，默认为true。

* removePortrait（可选）：如果不清除上一句对话的立绘，可以手动填写想要移除的立绘，填立绘的标识符，所有同名的立绘都会被移除

  PS：后两个参数的使用场景：有时候我希望多句连续的对话使用同样的立绘，此时每句对话都在portrait填同样的参数，有点麻烦，把clearOldPortrait设为false就好了。并且如果你的立绘有较长时间的动画（特别是背景需要缓慢移动），如果在portrait填同样的参数，就会导致动画也重新播放。而removePortrait也很简单，如果clearOldPortrait为false，这句对话就会继承上一句对话的所有立绘，若此时我想要更换某一个特定的立绘，就可以在这里填写需要更换的立绘标识，然后再在portrait填新的立绘即可。

  ### **选项**

* text（如果有选项则必填）：选项文本

* isLock（可选，默认false）：是否上锁，配合unlockCommand使用。若没有定义unlockCommand，则这个选项永远都会被锁定

* unlockCommand（可选）：用于判断选项是否会有特殊状态的指令，必须以`execute`开头才会有效。若该指令测试通过，则选项是正常可选状态，否则选项会被上锁（isLock为true）或隐藏（isLock为false或未定义）

* next（可选）：如果填的为数字则跳转到当前分组对应数字序号的对话，如果填的是字符串则跳转到对应分组的第一句话，如果不填则默认跳转到当前分组当前对话的下一句话

* click（可选）：点击按钮触发的事件，里面有2个参数`type`和`value`。若type填command，value则是要执行的指令，支持多段指令，用`;`分隔，指令权限为2级。如果你希望添加自定义的点击事件，那需要用kubejs在任意服务端脚本里调用`ChatBoxUtil`的`registerComponentEvent`函数。（详见[KubeJS](#kubejs)）

* tooltip（可选）：鼠标悬浮时弹出的文本

  ### **视频**

  本功能需要安装[WATERMeDIA](https://modrinth.com/mod/watermedia)才能生效

* path（可选）：播放视频的路径，可以是绝对路径，也可以是从当前游戏版本根目录(`saves`、`resourcepacks`所在的文件夹)开始的相对路径，推荐使用相对路径，**并且完整路径不能包含中文字符！**

* canControl（可选，默认true）：是否允许调整视频的进度，左右方向键调节播放进度，按空格暂停。

  PS：视频音量无论如何都是可以调整的：用上下方向键调节音量大小，M键静音

* canSkip（可选，默认true）：是否允许玩家跳过这段视频

* loop（可选，默认false）：是否循环播放视频

* x（可选，默认为0）、y（可选，默认为0）、width（可选，默认100）、height（可选，默认100）、alignX（可选，默认左对齐）、alignY（可选，默认上对齐）：效果同[图片立绘](#立绘)的对应参数，此处不赘述

* renderEvents（可选）：对话组件渲染事件，详见[对话组件渲染事件](#对话组件渲染事件)

## 格式化文本

具体介绍参考：<https://zh.minecraft.wiki/w/%E6%A0%BC%E5%BC%8F%E5%8C%96%E4%BB%A3%E7%A0%81?variant=zh-cn>

使用§加一个值可以设置粗体、斜体和各式各样的颜色等，比如：§1彩§2色§3字§4体，就可以实现每个字的颜色都是不一样的。

最后推荐一个VsCode插件`Minecraft Color Highlighter`可以看到格式化代码的效果

## 占位符

你可以自由地在说话者和对话文本、选项以及指令中使用占位符，目前已有的占位符如下：

* @s：当前打开对话框的玩家id，如果在@前面再加一个@将取消转义行为，比如假设你的玩家id为`player`，你用@s返回的是player，你用@@s返回的是正常文本的@s了
* <targetN.property>：第N个目标实体的属性，N从1开始，property是实体的属性名称（内置了4个属性，分别是`name`、`uuid`、`tags`、`health`）。\
比如`<target2.name>`表示第2个目标实体的名称，`<target1.health>`表示第1个目标实体的生命值，若只填`<targetN>`则表示第N个目标实体的名称，若没有对应的目标实体或者属性无法解析，则占位符会保持原样。\
如果想要自定义属性解析，需要在KubeJS中客户端环境调用`ChatBoxUtil`的`addPlaceholderResolver`函数，例子如下：
```javascript
//client_script
let LivingEntity = Java.loadClass("net.minecraft.world.entity.LivingEntity")
// 解析目标实体的药水效果
ChatBoxUtil.addPlaceholderResolver("effects", (entity) => {
    if (entity instanceof LivingEntity) {
        return entity.getActiveEffects().toString()
    }
    return "[]"
})
```
* <ruby (要注音的字符数) (注音)>：注音标签，位于要注音的文字前，例如`茕茕孑<ruby 1 lì>立`，你就能在游戏中看到立字的上方被标注了拼音`lì`

## **对话组件渲染事件**

&emsp;  &emsp;  对话组件渲染事件（`renderEvents`）是一个高级功能，指在对话播放过程中，会在组件渲染的特定时机触发一些自定义的事件效果，比如播放音效，执行指令，隐藏或显示特定组件等。`renderEvents`可以作为对话的参数，也可以写在对话组件（`dialogBox`，`portrait`，`video`）下。若作为对话的参数，则仅会在跳转到该句对话时触发事件；若写在对话组件下，则会在该组件渲染时触发事件。\
&emsp;  &emsp;  先看一个简单的例子：
```json
{
  "renderEvents": [
    {"trigger": "start", "type": "hide", "value": "@options"}
  ],
  "dialogBox": {
    "name": "凯瑟琳",
    "text": "向着星辰与深渊！欢迎来到冒险家协会。",
    "renderEvents": [
      {"trigger": "end", "type": "show", "value": "@options"}
    ]
  }
}
```
&emsp;  &emsp;  `renderEvents`是一个数组，意味着可以在对话播放过程中触发多个事件。每个事件包含3个字段：
* trigger（选填，默认`on_start`）：触发事件的时机，包括以下几种：
    * `on_start`（可简写为`start`）：组件渲染开始时触发（或者跳转到对话时）。
    * `on_end`（可简写为`end`）：组件渲染结束时触发。对于`dialogBox`组件，会在文本显示完毕时触发；对于`portrait`组件，若其有动画且不循环播放，则会在立绘动画结束时触发；对于`video`组件，若其不循环播放，则会在视频播放结束时触发。其他组件无。\
      **以下三个触发时机仅针对`portrait`组件：**
    * `on_click`（可简写为`click`）：立绘被点击时触发。（若立绘重叠，则仅会执行最上层立绘的该类事件）
    * `on_mouse_over`（可简写为`mouse_over`）：鼠标悬停在立绘上时触发。
    * `on_mouse_out`（可简写为`mouse_out`）：鼠标从立绘上移出时触发。
* type（必填）：事件类型，内置多种事件且支持用kjs注册自定义的类型。
* value（选填）：事件参数，根据事件类型的不同，参数的内容也不同。内置事件类型和参数介绍：
    * `command`：服务端执行指令。参数填指令（无需加`/`）。
    * `jump`：强制跳转对话，无视限制。不填参数则默认跳转下一句对话。填数字则跳转至指定索引对话。填组名则跳转至该组的第一句对话。
    * `goto_next`：到下一句对话，若有选项在则无效。无参数。
    * `play_sound`：播放音效。参数填音效id（比如`minecraft:ambient.cave`）。
    * `play_voice`：播放语音。同播放音效，区别是通过这个事件播放的音效类似于galgame的语音播放机制，进入下一句对话就会停止。
    * `stop_sound`：停止特定音效。
    * `hide`：隐藏特定组件，可以用`;`隔开多个组件。若参数以`@`开头则隐藏所有相关组件（`@s`指向组件本身，`@portraits`指向所有立绘，`@options`指向所有选项，`@buttons`指向所有按键，`@dialog`指向文本框，`@video`指向视频），否则隐藏指定id的立绘。
        - **需要注意的是，若组件通过事件被隐藏了，那个组件将不会被渲染、不可互动，自然也不可触发渲染事件。**
        - **立绘必须在`portrait`中被提及才可显示或隐藏，无法通过事件添加一个全新的立绘到当前的对话中!**
    * `show`：显示特定组件。参数写法同上。
    * `replace`：替换自身为特定组件。就是先隐藏自己，再显示别的组件的简单写法，参数写法同上。
    * `set_autoplay`：设置是否自动播放。参数填`"true"`或`"false"`。
    * `scale`：缩放立绘。参数填缩放比例（float类型）。\
      示例：添加两个立绘，开始时只显示其中一个，点击立绘切换成另一个，再次点击又切换回来（使用了两种不同的写法）
```json
{
  "renderEvents": [
    {"trigger": "start", "type": "hide", "value": "portrait2"}
  ],
  "portrait": [
    {
      "id": "portrait1",
      "renderEvents": [
        {"trigger": "click", "type": "hide", "value": "@s"},
        {"trigger": "click", "type": "show", "value": "portrait2"}
      ]
    },
    {
      "id": "portrait2",
      "renderEvents": [
        {"trigger": "click", "type": "replace", "value": "portrait1"}
      ]
    }
  ]
}
```

&emsp;  &emsp;  好的，介绍到这里，我们可以知道开头的示例的效果为：在对话开始时不显示选项，等到文字显示完毕后再显示选项。\
&emsp;  &emsp;  现在你已经学会文本渲染事件的用法了，去发挥你的创造力吧！（太麻烦了还搞不明白？试试把文档喂给AI，它会给你详细的解释）

# 配置文件

你可以通过NeoForge自带的可视化界面中修改配置项，目前提供的配置项包括：

* 历史记录界面滚轮速度

* 下句话如果没有新音效，是否中断前面正在播放的音效

* 对话框每秒显示的字符数

* 是否阻拦泰拉生物NPC的对话系统（需要安装泰拉生物模组）

# 隐藏按键小技巧

在本模组的屏幕模式下，还隐藏了很多参考Gal的便捷快捷键，但是由于设计问题没法很好的在游戏中提示用户这些按键，所以在这里将这些按键全部列举出来

| 界面     | 按键            | 功能              |
| :----- |:--------------|:----------------|
| 对话框界面  | Ctrl（mac为Cmd） | 快进对话（如果可以快进）    |
| 对话框界面  | 滚轮向下          | 效果同鼠标左键点击       |
| 对话框界面  | 滚轮向上          | 打开历史记录界面        |
| 对话框界面  | 右键            | 隐藏/展示对话框        |
| 历史记录界面 | 右键            | 关闭历史记录界面，返回当前对话 |

部分说明：

* 右键隐藏的包括对话框、选项、比对话框晚渲染的立绘、功能按钮。

此外，在对话框界面中，按下`f3`键可以打开调试（戏）模式。

# 关于模组联动

## KubeJS

本模组提供了3个事件给KubeJS调用来添加自己想加的东西，例子如下：

```javascript
 //client_script

//在对话框渲染前触发，提供了GuiGraphics可以添加自己想要的渲染，可取消事件
ChatBoxEvents.renderPre(event=>{
  let getGuiGraphics = event.getGuiGraphics()
  event.cancel()
})

//在对话框渲染后触发，提供了GuiGraphics可以添加自己想要的渲染
ChatBoxEvents.renderPost(event=>{
  let getGuiGraphics = event.getGuiGraphics()
})

//server_script or client_script
//在跳转对话或关闭对话（index为-1）时触发，提供了对话的数据包文件，分组和序号来方便用户对特别某一句对话添加自己想要的功能。配合ChatBoxUtil的方法可以动态修改对话
ChatBoxEvents.skipChat(event => {
  let { player, resourceLocation, group, index, targets } = event
  if (index === -1) player.tell('关闭了对话框')
})
```

除此之外，本模组还给KubeJS提供了一个全局变量`ChatBoxUtil`，这个方法中包含api化的对话框指令。~~no command行动~~

本模组还可以通过KubeJS来添加对话组件渲染事件

```javascript
//server_script

//你可以在服务端任意事件中添加对话组件渲染事件，比如我这里用的是服务端载入事件 
ServerEvents.loaded(event => {
  //第一个参数是事件的类型，是你选项写渲染事件type的值，无视大小写
  //第二个参数是事件触发时，在客户端执行的操作
  //第三个参数是决定是否需要在服务端执行操作，true的时候会自动发包触发第四个参数里的内容
  //第四个参数是事件触发时，在服务端执行的操作
  ChatBoxUtil.registerComponentEvent('test',
          (component, value) => { }, // 第一个参数为触发事件的组件，可能为空，要使用一定记得检查！第二个参数为事件的参数
          true,
          (player, value) => {
            player.tell('test click')
            // 可以在此处使用ChatBoxUtil.serverGetChatTargets(player)来获取当前对话的目标实体列表
            let targets = ChatBoxUtil.serverGetChatTargets(player)
            if (targets.length > 0) {
              let target = targets[0]
              player.tell('target1: ' + target.getName().getString())
            }
          })
})
```

## 泰拉生物

兼容版本为：`1.1.17-hotfix`
**PS：写该功能的时候发现GitHub和发布的版本的代码差距还是比较大的，所以不一定保证后续的版本的兼容性，但是我也会尽可能去兼容更新的版本的**

通过配置文件修改可以阻止泰拉生物的NPC对话系统，需要注意的是如果开启这个配置所有的NPC交互都会直接打开对应的商店，你需要自己手动给这些NPC配置对应的对话框。
那如何配置对话框呢？你可以通过KubeJS的实体交互事件取消原来的交互行为，从而跳转本模组的对话，例子如下：

```javascript
 //server_script

ItemEvents.entityInteracted(event => {
  let { player, target } = event
  //判断NPC的实体类型，比如这里是判断是否为商人
  if (target.type === 'terra_entity:merchant') {
    //跳转本模组的对话框
    ChatBoxUtil.serverSkipDialogues(player, 'test:test', 'test', target)
    //取消原先的交互行为
    event.cancel()
  }
})
```

这样就成功的将泰拉生物NPC的对话框换成本模组的，那么这时候又有人要问了：那我还如何打开这个NPC的商店呢？
你可以在对话文件中给选项的点击事件类型选择为`TERRA_ENTITY_SHOP`，这样玩家点击该选项的时候就可以打开当前交互的NPC对应的商店

## TextAnimator

你可以用这个模组来给自己的对话文本提供更好看的动画，使用可以直接参考该模组Github的readme文档：[TextAnimator](https://github.com/Snownee/TextAnimator/tree/1.20-fabric)

# 更新日志

## 1.0.9
* 对话可以添加多个目标实体，便于在对话中与实体互动和调用数据；添加与对话目标实体数据相关的占位符，格式为`<targetN.property>`，可在对话文本和指令中解析成对应的数据，详见[占位符](#占位符)
* 对话json：
  * 部分组件新增`renderEvents`字段，即[对话组件渲染事件](#对话组件渲染事件)
  * 新增`animationFPS`字段，用于限制立绘动画的帧率，默认值为60
  * 新增`autoPlayTick`字段，用于调整自动跳转下一句对话的延迟，默认值为20（对于有音效的对话，跳转延迟不会大于20）
  * 选项的隐藏和锁定改为由`unlockCommand`和`isLock`参数控制，根据`execute if`或`execute unless`指令的执行结果判断是否隐藏或锁定
  * 移除了部分字段，现在对话json中的文本都使用翻译键
* 主题中对话框配置项新增`textAlign`字段，即支持居中对齐和右对齐
* `SkipChatEvent`改为双端事件，便于在服务端修改对话（因为双端，所以`chatBoxScreen`参数又没了，哦悲悲）。关闭对话界面也会触发`SkipChatEvent`事件，此时`index`参数为-1
* 渲染调整：
  * 选项的位置现在会根据`alignY`参数自动调整，优化选项文本对齐的方式
  * 立绘现在以图像的中心为基准进行缩放，而不是左上角
* 添加简单的对话调试功能，在对话框screen按f3进入
* 添加文本注音标签，详见[占位符](#占位符)
* `ChatBoxCommandUtil`增加更多方法，便于使用kubejs动态修改对话
  ### 破坏性修改提示
* 由于优化了选项文本对齐的方式，新版选项的文字可能错位，请修改主题文件中选项的`optionChatX`参数
* 由于立绘缩放方式改为以图像中心为基准，因此缩放的立绘可能错位，请调整立绘的坐标参数（推荐使用调试模式）
* `SkipChatEvent`改为双端事件，该事件的参数也有改变
* 选项点击事件（ChatOptionClickEvent）已完全改头换面为对话组件渲染事件（ComponentEvent），因此kubejs工具类中相应的方法由`registerClickEvent`改为`registerComponentEvent`，且参数有变化
* 由于选项的隐藏和锁定改为由指令控制，因此旧的计分板相关的参数已完全移除（虽然应该没人用，但还是提一句）

## 1.0.8
* `chatbox`指令新增`command`子命令，可以通过指令进入下一句话、开关自动对话的功能以及更改对话框是屏幕(screen)还是渲染层(render)（同时移除了相应的配置项）
* render模式下，按键提示的visible若为false会同时取消按键的监听
* 添加泰拉生物的兼容，允许替换泰拉生物的对话框
* screen模式下添加右键隐藏对话框功能
* 添加调整对话框文本显示速度的配置项
* 添加立绘控制系统，可以继承上一句对话的立绘，并且可以选择删除部分立绘（添加clearOldPortrait和removePortrait参数）
* 可以将多个图片组合在一起，成为一个立绘（立绘部分添加attachment参数）
* 为立绘及其动画添加旋转角度参数
* 立绘动画添加坐标偏移量参数（xOffset和yOffset）以支持立绘相对运动
* 主题文件新增`customAnimation`字段用于预设立绘动画，便于立绘使用

## 1.0.7
* 新增配置项`下句话如果没有新音效，是否中断前面正在播放的音效`
* 修复第一次打开对话框直接崩溃的Bug
* 修复isHistoricalSkip的默认值为true
* 修复数据包最大访问次数的优先级过于高导致指令无法调试的bug
* 修复数据包文件太大导致发包异常的bug
* 取消位置的限制，大小的最大限制
* 将主题中大部分数字都支持小数
* 进入对话框，点击选项执行指令支持多条指令
* 对话json中添加theme字段，可以在进入对话时自动绑定主题
* 动画支持修改图片材质
* 对话立绘支持临时修改参数
* 支持视频
* 更新render模式对话框
* 修复一个对话中不能出现多个不同id的玩家的头像的bug

## 1.0.6
* 修复了对话框文本为1个字符的时候，游戏会崩溃的Bug
* 修复了背景图片不会因为跳转其它对话自动清除的Bug
* 更新案例数据包

## 1.0.5
* 历史记录跳转后保留的记录更加合理
* 添加参数限制一段话是否可以通过历史记录界面回溯
* 一句对话中添加一个backgroundImage参数可以设置背景图片
* 支持KubeJS样式的事件
* 修复对话框只会获取最后一个对话里的isTranslatable等参数的Bug
* 新增maxTriggerCount参数设置一段对话的最大访问次数
* 新增进度触发器，可以通过进度为条件来打开对话框
* 新增指令`/chatbox maxTriggerCount`修改和重置最大访问次数

## 1.0.4
* 修复ftb执行指令打不开对话框的bug
* 无权限玩家无法执行点击选项时应该触发的指令
* 在对话框内滚轮下滑跳转剧情，上滑打开历史记录界面。历史记录页面右键关闭

## 1.0.3
* 在进入每句话的时候都可以选择执行一条指令
* 自定义动画参数中x、y、scale、opacity都不再为必填项
* 为KubeJS提供了全局变量`ChatBoxUtil`，包括服务端调用和客户端调用的两套api
* 将翻译键参数独立出来，只需要在对话文件中配置一次即可（应该不会有人一会用翻译键一会用硬编码的吧）
* 当前对话是否是否能被esc关闭（默认能），是否允许玩家单机打开对话框的时候被时停（默认单机时停，多人正常）
* 在玩家输入`/reload`后会自动按照玩家上次切换的主题样式更新玩家当前主题的样式，不再需要每次修改主题样式的参数还需要先切换主题指令来刷新当前的样式
* 修复服务端运行失败以及服务端无法显示选项的bug
* `chatbox skip`的组名参数也支持补全

## 1.0.2
* 修复`/chatbox skip`指令跳转默认值没效果的bug
* 给`SkipChatEvent`事件添加一个`chatBoxScreen`参数
* 对话框主题的立绘配置中每一项都需添加type属性
* 对话框文本的配置中关于立绘的配置改为字符串数组，不再需要`type`和`value`
* 立绘所有的类型都支持使用透明度和自定义动画，图片额外支持3种预设的动画
* 进入对话会关闭之前播放的音乐

# 未来可能更新的功能

1.联动一个npc模组（作者正在开发中）
