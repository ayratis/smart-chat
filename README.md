# smart-chat

# Инструкция
1) Скомпилировать library.aar и импортировать в свой проект.
2) Для запуска чата необходимо создать intent для запуска с помощью статической функции [SmartChatLibrary.createLaunchIntent(...)](https://github.com/ayratis/smart-chat/blob/master/library/src/main/java/gb/smartchat/library/SmartChatActivity.kt), где:
    - smartUserId - UUID идентификатор пользователя (в Smart приходит в настройках в /shops, поле СсылкаПользователя);
    - storeInfoList - список объектов [StoreInfo](https://github.com/ayratis/smart-chat/blob/master/library/src/main/java/gb/smartchat/library/entity/StoreInfo.kt) (в Smart это shops и partners);
    - baseUrl - необзятельный параметр, нужен только для отладки.
3) Для отображения пуш уведомлений необходимо проверить поле is_chat_push_message, которое придет в data сообщения. Если is_chat_push_message == true, необходимо передать обработку данного сообщения библиотеке, используя статическую функцию [ChatPushNotificationManager.proceedRemoteMessage(...)](https://github.com/ayratis/smart-chat/blob/master/library/src/main/java/gb/smartchat/library/ChatPushNotificationManager.kt), где:
    - dataMap - data сообщения, полученная в сообщении сервиса;
    - iconRes - id ресурса иконки для уведомления (например R.drawable.icon_24).
