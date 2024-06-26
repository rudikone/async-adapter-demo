# Демо асинхронного проекта для сервисных тестов

Подробнее читай [статью](https://habr.com/ru/articles/824594/) на Хабр

Для локального запуска необходимо выполнить docker-compose up в папке docker. См. [docker-compose](docker/docker-compose.yml)

## Тесты

- ThreadSleepTest - тест с засыпанием тестового потока
- CountDownLatchWithSpyBeanTest - тест с SpryBean и CountDownLatch для синхронизации
- ListenerWithBlockingQueueTest - тест с MessageListener и BlockingQueue для синхронизации
- AcknowledgingListenerWithBlockingQueueTest - тест с AcknowledgingMessageListener и BlockingQueue для синхронизации
- AwaitilityTest - тест с AcknowledgingMessageListener и библиотекой Awaitility для синхронизации
