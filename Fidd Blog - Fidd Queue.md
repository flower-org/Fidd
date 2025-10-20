## 1. Fidd Blog

Как указано в некоторых неструктурированных документах, простейшим и базовым прикладным Use Case, использующим Fidd Format будет являться Fidd Blog.

Fidd Blog использует базовый способ организации Fidd Message-s в виде последовательности (очереди), формируя хронологическую последовательность постов пользователя - что также известно как Блог.

Посты в Fidd Blog пишет один пользователь-автор, также являющийся хозяином/publisher-ом Fidd-а. На один Блог могут быть подписаны несколько пользователей-подписчиков в режиме Read-Only. Продвинутые фичи, как комментарии, лайки или иные виды интерактивности для подписчиков не поддерживаются, т.к. это базовый Use Case для Fidd с простейшей структурой.

Таким образом, структура Блога должна отвечать следующим требованиям:
1. Хранение сообщений в виде последовательности
    - Возможность получения списка обновлений начиная с определенного маркера (Message Number);
    - Получение контента отдельных сообщений.

2. Предоставление read-only доступа к Fidd Messages подписчикам
    - Возможность получения fidd.key-файла для конкретного Fidd Message и конкрентого подписчика.

3. НЕ ВКЛЮЧЕНО Запрос на подписку / отписку на Блог
    - не является частью структуры блога;
    - предполагается, что хозяин Fidd-а хранит информацию о подписчиках за пределами структуры блога и формирует fidd.key файлы соответственно.



## 2. Fidd Queue

Как видно, список функций, необходимых для структуры Блога достаточно несложен и универсален, и есть широкие возможности для переиспользования. Имеет смысл определить базовую структуру `Fidd Queue`, одним из вариантов практического использования которой являлся бы Fidd Blog.

Таким образом, можно сказать, что Fidd Blog это Fidd Queue, в котором владелец/паблишер является единственным автором.

Набросаем контракт FiddQueue:

```
interface FiddQueue {
    FiddQueueInfo getQueueInfo();

    List<MessageNumber> getMessageNumbersSince(MessageNumber num);
    
    BLOB getMessage(MessageNumber num);
    BLOB getMessagePublisherSignature(MessageNumber num);

    List<FiddKeyId> getAllFiddKeyIds(MessageNumber num)
                                      throws NotSupported;
    FiddKeyId getFiddKeyIdForSubscriber(MessageNumber num, SubscriberId subscriber)
                                      throws NotSupported;

    BLOB getFiddKey(MessageNumber num, FiddKeyId keyId);
    BLOB getFiddKeyPublisherSignature(MessageNumber num, FiddKeyId keyId);

    // LVL 2 only
    BLOB getMessageChunk(MessageNumber num, long offset, long length);
}

struct FiddQueueInfo {
    PublicKey publisher();
    String publisherSignatureFormat();

    String fiddKeyFormat();

    @Nullable String queueName();
    @Nullable String queueDescription();
}
```

Как можно заметить, функционал описанного выше интерфейса аналогичны `Queue.poll()`, со спецификой получения Fidd-ключей для каждого message.



## 3. Отображение Fidd Queue на файловую систему

Базовым вариантом хранилища для Fidd Queue может являться директория файловой системы. У данного варианта можно найти недостатки, но в плане простоты и распространенности трудно представить что-то превосходящее. Кроме того, те же самые принципы теоретически возможно использовать с широким спектром хранилищ, которые предоставляют интерфейс, аналогичный файловой системе - DropBox, Google Drive, Yandex Disk, S3, Ceph/Ceph FS, и т.д.

Важно заметить, что данный интерфейс, как и любой другой, может реализовываться с использованием различных storage-технологий и описываться также в разных терминах, как SDK так и сервисов. В этом разделе речь идет лишь об одном из вариантов его реализации.

### 3.1 Структура директории

Рассмотрим структуру директорий/файлов, которые будут содержать данные Fidd Queue и то, как будут реализовываться на этой основе методы вышеописанного интерфейса FiddQueue.

```
FiddQueue\
    0-QueueInfo\
        keys\
            subscriberId1.fidd.key
            subscriberId1.fidd.key.publ.sign
            subscriberId2.fidd.key
            subscriberId2.fidd.key.publ.sign
            subscriberId3.fidd.key
            subscriberId3.fidd.key.publ.sign
            ...
        0-QueueInfo.fidd
        0-QueueInfo.fidd.publ.sign

    1\
        keys\
            subscriberId1.fidd.key
            subscriberId1.fidd.key.publ.sign
            subscriberId2.fidd.key
            subscriberId2.fidd.key.publ.sign
            subscriberId3.fidd.key
            subscriberId3.fidd.key.publ.sign
            ...
        1.fidd
        1.fidd.publ.sign

    2\
        keys\
            subscriberId1.fidd.key
            subscriberId1.fidd.key.publ.sign
            subscriberId2.fidd.key
            subscriberId2.fidd.key.publ.sign
            subscriberId3.fidd.key
            subscriberId3.fidd.key.publ.sign
            ...
        2.fidd
        2.fidd.publ.sign

    3\
        ...
    25\
        ...
    100\
        ...
    ...
    300\
        ...
    400\
        ...
    ...
etc. 
```

### 3.2 Реализация методов интерфейса FiddQueue

- `FiddQueueInfo getQueueInfo();`
    - Данная информация содержится в специальном сообщении **0-QueueInfo**.

- `List<MessageNumber> getAllMessageNumbers();`
    - Просто перечислить все подпапки, кроме **0-QueueInfo**.

- `List<MessageNumber> getMessageNumbersSince(MessageNumber num);`
    - Перечислить все подпапки, кроме **0-QueueInfo**, номер (название) которых больше параметра *num*.

- `BLOB getMessage(MessageNumber num);`
    - Найти подпапку с номером *num*, вернуть fidd-файл.

- `BLOB getMessagePublisherSignature(MessageNumber num);`
    - Найти подпапку с номером *num*, вернуть fidd.publ.sign-файл.

- `List<FiddKeyId> getAllFiddKeyIds(MessageNumber num)`
    - Найти подпапку с номером *num*, вернуть список fidd.key-файлов из ее подпапки *num/keys*.

- `FiddKeyId getFiddKeyIdForSubscriber(MessageNumber num, SubscriberId subscriber)`
    - В зависимости от того, как будет формироваться FiddKeyId, возможны варианты, но идея в том, чтобы найти сответствующий FiddKeyId в *num/keys*.

- `BLOB getFiddKey(MessageNumber num, FiddKeyId keyId);`
    - Вернуть соответствующий fidd.key-файл из подпапки *num/keys*.

- `BLOB getFiddKeyPublisherSignature(MessageNumber num, FiddKeyId keyId);`
    - Вернуть соответствующий fidd.key.publ.sign-файл из подпапки *num/keys*.

- `BLOB getMessageChunk(MessageNumber num, long offset, long length);`
    - То же, что `getFiddKey`, но возвращаем не весь файл, а его часть, начиная с указанного оффсета и указанной длины.


### 3.3 Заключение 

Снова замечу, что здесь мы рассматриваем простейший вариант структуры директории, и даже на ФС можно отобразить данную структуру по-разному. Например, необязательно в явном виде указывать MessageNumber в папке и имени fidd-файла, т.к. его можно альтернативно прочитать из FiddFileMetadata. Однако это бы усложнило или же сделало невозможной эффективную реализацию метода `getMessageNumbersSince`.

Нашей главной целью в этом разделе является описать простой практический вариант хранения Fidd Queue, доступный для использования и понимания любому, и который можно разместить где угодно (хоть на флешке). В целях сохранения простоты и нежелательности переусложнения, было принято решение оставить Message Number-s в открытом виде.

Учитывая, что Message Number-s - это, в принципе, служебная информация, и поле является монотонно прирастающим значением, то для атакующего, не имеющего ключей становится очевидным лишь порядок сообщений в последовательности, но не их контент. Теоретически, данный порядок можно выяснить путем наблюдения за папкой или же анализируя время создания файлов другими способами, то есть какой-то критической информации в этом нет, и навряд ли это можно считать существенной утечкой, которая могла бы как-то упростить атаку на Fidd Queue.

Мы не говорим о том, что во всех реализациях надо так expos-ить MessageNumber; напротив - если можно этого избежать - то следует избегать, но в данном конкретном случае мы считаем эту неэффективность приемлемой.