# smart-chat

# Инструкция
1. Скомпилировать library.aar и импортировать в свой проект.
2. Для запуска чата необходимо создать intent для запуска с помощью статической функции SmartChatLibrary.createLaunchIntent(cntext: Context, smartUserId: String, storeInfoList: List<StoreInfo>, baseUrl: String), где:
  -smartUserId - UUID идентификатор пользователя (в Smart приходит в настройках в /shops, поле СсылкаПользователя);
  -storeInfoList - список объектов StoreInfo https://github.com/ayratis/smart-chat/blob/master/library/src/main/java/gb/smartchat/library/entity/StoreInfo.kt (в Smart это shops и partners);
  -baseUrl - необзятельный параметр, нужен только для отладки.
3. Когда чат не запущен, для отображения пуш уведомлений, необходимо проверить поле is_chat_push_message, которое придет в data сообщения. Если is_chat_push_message == true, необходимо передать обработку данного сообщения библиотеке, используя статическую функцию ChatPushNotificationManager.proceedRemoteMessage(context: Context, smartUserId: String, storeInfoList: List<StoreInfo>, dataMap: Map<String, String>, iconRes: Int), где:
  -dataMap - data сообщения, полученная в сообщении сервиса;
  -iconRes - id ресурса иконки для уведомления (например R.drawable.icon_24).
