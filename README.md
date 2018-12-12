# BLE蓝牙调试助手

## 代码托管
[![](https://jitpack.io/v/wandersnail/easyble.svg)](https://jitpack.io/#wandersnail/easyble)
[![Download](https://api.bintray.com/packages/wandersnail/android/easyble/images/download.svg) ](https://bintray.com/wandersnail/android/easyble/_latestVersion)

## 使用

1. 在project的build.gradle里的repositories添加内容，最好两个都加上，有时jitpack会抽风，同步不下来。
```
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
		maven { url 'https://dl.bintray.com/wandersnail/android/' }
	}
}
```
2. module的build.gradle中的添加依赖：
```
dependencies {
	...
	implementation 'com.github.wandersnail:easyble:1.0.0'
	implementation 'org.greenrobot:eventbus:3.1.1'
}
```

## 功能
- 支持多设备同时连接
- 支持连接同时配对
- 支持搜索已连接设备
- 支持搜索器设置
- 支持自定义搜索过滤条件
- 支持自动重连、最大重连次数限制、直接重连或搜索到设备再重连控制
- 支持请求延时及发送延时设置
- 支持分包大小设置、最大传输单元设置
- 支持注册和取消通知监听，使用EventBus通知状态及数据
- 支持回调方式，支持使用注解@InvokeThread控制回调线程。注意：EventBus和回调只能取其一！
- 支持发送设置（是否等待发送结果回调再发送下一包）
- 支持写入模式设置
- 支持设置连接的传输方式
- 支持连接超时设置

## 详细使用方法

[![](https://img.shields.io/badge/Tutorial-CSDN-red.svg)](https://blog.csdn.net/fszeng2011/article/details/80999342)	

## 基于此库的BLE调试app
[![](https://img.shields.io/badge/Download-App%20Store-yellow.svg)](http://app.mi.com/details?id=cn.zfs.bledebugger)
[![](https://img.shields.io/badge/Download-APK-blue.svg)](https://github.com/wandersnail/easyble/releases/download/1.0.0/bledebugger_v1.15.apk)

## 调试app截图
![image](https://github.com/wandersnail/easyble/blob/master/screenshot/0d12b411b69c21f97460983f0e22280e5ec424032.jpg)
![image](https://github.com/wandersnail/easyble/blob/master/screenshot/0d12b411b69c22f978609b3f0e222b0e5fc424032.jpg)
![image](https://github.com/wandersnail/easyble/blob/master/screenshot/0d12b411b69c23f97e609c3f09222f0e54c424032.jpg)
![image](https://github.com/wandersnail/easyble/blob/master/screenshot/0e623b5f536864d7b1ef7881cdfdd6f6c420eb5a9.jpg)
![image](https://github.com/wandersnail/easyble/blob/master/screenshot/02b5d84bc72bd4cfa34f80bb3e5ef7439a4ba476b.jpg)
