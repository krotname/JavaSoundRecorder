# JavaSoundRecorder: продуктовый аудит, конкуренты и план релизов

Дата аудита: 2026-06-12.

Репозиторий: https://github.com/krotname/JavaSoundRecorder

Текущая версия: `v1.1.0`, опубликована 2026-06-10.

## 1. Что это за приложение

`JavaSoundRecorder` - небольшой Java 21 desktop-рекордер звука с двумя режимами запуска:

- CLI: один цикл записи с микрофона в WAV-файл, затем завершение процесса.
- Swing UI: минимальное окно с кнопками `Start` и `Stop`, статусом записи и тем же одноразовым workflow.

Основное назначение на текущем этапе - не полноценный пользовательский диктофон, а демонстрационный desktop-проект: чистая архитектура, Maven Wrapper, тесты, CI, CodeQL, OSSF Scorecards, SBOM, release checksums и attestations. Это сильная инженерная витрина, но слабый потребительский продукт.

## 2. Как приложение работает

### Runtime flow

1. `Main` читает переменные окружения через `AppConfig`.
2. `Main` создает `JavaSoundCaptureService`, `RecordingCoordinator` и `UploadService`.
3. Если передан аргумент `--ui`, запускается Swing-окно; иначе выполняется CLI one-shot запись.
4. `RecordingCoordinator` защищает workflow от параллельных запусков через single-flight флаг.
5. `JavaSoundCaptureService` открывает `TargetDataLine`, пишет PCM поток в WAV через `AudioSystem.write`.
6. После записи координатор либо возвращает `local-only`, либо загружает файл через Dropbox-адаптер.

### Конфигурация

- `JAVASOUNDRECORDER_RECORDING_DURATION_MS` - длительность записи.
- `JAVASOUNDRECORDER_RECORDING_DIRECTORY` - каталог WAV-файлов.
- `DROPBOX_ACCESS_TOKEN` - токен Dropbox.
- `JAVASOUNDRECORDER_DROPBOX_UPLOAD_FOLDER` - целевая папка Dropbox.
- `JAVASOUNDRECORDER_UPLOAD_ENABLED` - включает или выключает загрузку.

### Текущее качество репозитория

Сильные стороны:

- понятное разделение пакетов `config`, `audio`, `orchestration`, `storage`, `ui`;
- Maven Wrapper и Java 21;
- unit, integration, UI, contract, smoke и architecture tests;
- CI, CodeQL, Scorecards, actionlint, SpotBugs, JaCoCo;
- pinned GitHub Actions, Docker digest pinning, SBOM, checksums, artifact attestations;
- `SECURITY.md`, `CONTRIBUTING.md`, issue templates, release workflow.

Слабые стороны:

- почти нет пользовательской функциональности за пределами `Start`/`Stop`;
- настройки доступны только через env vars;
- нет выбора микрофона, индикатора уровня, waveform, pause/resume, библиотеки записей, playback, rename/delete/share;
- единственный формат записи - WAV;
- Dropbox upload включается только через токен в окружении, без UI-настройки и без user-friendly диагностики;
- релизные заметки описывают в основном инженерные улучшения, а не пользовательскую ценность.

## 3. Что было проверено вручную

### Команды

```powershell
gh auth status
gh repo view krotname/JavaSoundRecorder --json name,description,latestRelease,repositoryTopics,stargazerCount,forkCount,watchers
.\mvnw.cmd -q -DskipTests package
.\mvnw.cmd -q -Dtest=*UiTest test
java -jar target\javasoundrecorder-1.1.0-all.jar
java -jar target\javasoundrecorder-1.1.0-all.jar --ui
```

### Результаты

- GitHub CLI авторизован как `krotname`.
- Репозиторий не archived, не fork.
- GitHub metadata на дату проверки: `0` stars, `0` forks, `1` watcher.
- Последние workflow runs для `v1.1.0`: CI, CodeQL, Scorecards, Workflow lint и Release завершились успешно.
- Maven package завершился успешно.
- UI tests завершились успешно.
- CLI one-shot запись с `JAVASOUNDRECORDER_UPLOAD_ENABLED=false` и `JAVASOUNDRECORDER_RECORDING_DURATION_MS=1000` создала WAV-файл.
- Swing UI запустился, кнопка `Start` начала запись, после завершения создался WAV-файл.
- Кнопка `Stop` запросила отмену и оставила частичный WAV-файл.

## 4. Конкуренты и аналоги

### Выбранная рамка сравнения

`JavaSoundRecorder` сравнивается не с DAW для профессиональной музыки, а с desktop-приложениями, которые закрывают потребность "быстро записать звук, найти запись, прослушать, переименовать/обрезать/экспортировать".

Критерии рейтинга:

- запись: стабильность, выбор устройства, pause/resume, loopback/system sound;
- UX: понятный интерфейс, waveform/level meter, feedback, shortcuts, accessibility;
- работа с файлами: форматы, библиотека, playback, rename/delete/share;
- редактирование: trim, normalize/noise cleanup, metadata;
- поставка: installer, релизы, документация, обновления;
- инженерное качество: open source, тесты, CI, supply-chain hygiene.

### Рейтинг

| Место | Приложение | Оценка | Почему выше JavaSoundRecorder |
|---:|---|---:|---|
| 1 | Audacity | 95/100 | Бесплатный open-source лидер: multi-track editor/recorder, эффекты, плагины, импорт/экспорт, активные релизы, большая аудитория. |
| 2 | Adobe Audition | 92/100 | Профессиональная рабочая станция: waveform, multitrack, spectral editing, restoration, workflow для podcast/video/audio finishing. Минус - подписка. |
| 3 | ocenaudio | 86/100 | Сильный бесплатный single-track editor: чистый UX, waveform, VST, autosave, transcription workflow, batch fixes, активный changelog. |
| 4 | WavePad | 80/100 | Понятный массовый audio editor/recorder: много форматов, базовое редактирование, эффекты, tutorials, Windows/macOS/mobile. |
| 5 | Ashampoo Audio Recorder Free | 74/100 | Ближайший простой конкурент: запись microphone/loopback, много форматов, trimming, media library, player, rename, level meter. |
| 6 | Windows Sound Recorder | 70/100 | Встроенный Windows-диктофон: pause/resume, shortcuts, rename/share, список записей, device/file-format UX в Windows 11. |
| 7 | OBS Studio | 68/100 | Не диктофон, но мощный recorder/streaming tool с развитым аудиомикшером и источниками. Слишком сложен для простой записи голоса. |
| 8 | JavaSoundRecorder `v1.1.0` | 28/100 | Инженерно аккуратный demo-recorder, но потребительски не готов: нет базовых функций диктофона и почти нет UX. |

### Входит ли JavaSoundRecorder в рейтинг

В публичные рейтинги аудиоредакторов/диктофонов JavaSoundRecorder сейчас не входит. Причины:

- нет заметной аудитории: `0` stars и `0` forks на GitHub на дату аудита;
- нет полноценного пользовательского интерфейса;
- нет installer/portable UX;
- нет функций, которые пользователи ожидают от диктофона: pause/resume, список записей, playback, rename, delete, trim, выбор устройства, форматы;
- README позиционирует проект скорее как архитектурную и supply-chain демонстрацию, чем как конечный продукт.

В узком рейтинге "маленькие open-source Java-примеры для записи WAV через Java Sound API" репозиторий может быть конкурентен благодаря тестам, архитектуре и supply-chain дисциплине. Но это ниша для разработчиков, не пользовательский рынок.

### Feature-gap матрица

Матрица ниже фиксирует разницу именно с точки зрения пользователя простого диктофона. `Да` означает, что функция доступна пользователю в обычном UI/поставке. `Частично` означает, что функция существует только технически, через env vars, CLI или без нормального UX.

| Возможность | JavaSoundRecorder 1.1.0 | Windows Sound Recorder | Ashampoo Audio Recorder Free | ocenaudio | Audacity | Вывод для JavaSoundRecorder |
|---|---|---|---|---|---|---|
| Быстрый старт записи | Частично: `Start`, но окно слишком маленькое | Да | Да | Да | Да | Нужно сохранить простоту, но исправить размер и feedback. |
| Stop/cancel с понятным финалом | Нет: остается `Cancel requested` | Да | Да | Да | Да | Нужна явная state machine и финальные статусы. |
| Pause/resume | Нет | Да | Да | Да | Да | Минимальный consumer baseline не достигнут. |
| Выбор микрофона | Нет | Да | Да | Да | Да | Нужен device selector и preflight check. |
| System audio / loopback | Нет | Нет/зависит от ОС | Да | Частично | Частично | Для Windows-рекордера это сильное конкурентное улучшение, но не v1.2.0. |
| Level meter | Нет | Да | Да | Да | Да | Без индикатора пользователь не понимает, пишется ли звук. |
| Waveform/visualization | Нет | Да | Нет/частично | Да | Да | Требуется для доверия к записи и последующего редактирования. |
| Список записей | Нет | Да | Да | Да | Да | Сейчас файл фактически исчезает для пользователя после записи. |
| Playback | Нет | Да | Да | Да | Да | Без playback приложение не закрывает полный диктофонный сценарий. |
| Rename/delete/share/open folder | Нет | Да | Да | Да | Да | Нужна библиотека с file actions. |
| Trim | Нет | Нет/ограниченно | Да | Да | Да | Важный следующий шаг после библиотеки и playback. |
| Несколько форматов | Нет: только WAV | Да | Да | Да | Да | Нужны export profiles; запись можно оставить WAV internally. |
| Metadata | Нет | Нет/ограниченно | Да | Да | Да | Полезно после появления библиотеки и экспорта. |
| Autosave/recovery | Нет | Да/системно | Частично | Да | Да | При отмене и сбоях сейчас нет прозрачной политики. |
| Installer/portable app | Нет: Maven/JAR | Да | Да | Да | Да | Для публичного рейтинга нужен release asset без Maven. |
| Тесты/CI/supply-chain | Да, сильнее многих простых диктофонов | Не публично | Не публично | Не полностью публично | Да | Главное конкурентное преимущество проекта как open-source repo. |

Итог по матрице: JavaSoundRecorder выигрывает только по прозрачности инженерного процесса и supply-chain evidence. По пользовательскому UX он уступает даже встроенному Windows Sound Recorder, потому что не закрывает полный цикл "записал -> увидел -> прослушал -> переименовал/нашел -> поделился/экспортировал".

## 5. Что делают лидеры по changelog

### Audacity

Наблюдаемый вектор развития:

- надежность редактирования и waveform/selection;
- исправления paste, macros, scripting, FLAC/import/export;
- улучшения HiDPI UI;
- Podcast 2.0 chapters export;
- FFmpeg 8 support, OGG/Opus export через FFmpeg;
- cloud/audio.com import/export;
- Audacity 4 beta: envelope editing, label tracks, spectral editing, cloud projects, metadata editor, plugin support, lead-in recording.

Вывод для JavaSoundRecorder:

- даже бесплатный лидер конкурирует не "кнопкой записи", а полным циклом записи, редактирования, навигации, экспорта и публикации;
- минимальный путь сближения - waveform/level UI, библиотека записей, базовый trim, несколько форматов и нормальный export/share flow.

### ocenaudio

Наблюдаемый вектор развития:

- crash/freeze fixes в UI и batch workflows;
- MP3 metadata и ID3 fixes;
- фиксы non-ASCII paths на Windows;
- autosave для защиты сессии;
- transcription mode;
- waveform canvas precision, pan/zoom, marker/region UX;
- channel routing, trim/split, annotation tracks, templates;
- loudness/statistics fixes.

Вывод для JavaSoundRecorder:

- для desktop audio важны не только "записать файл", но и сохранность сессии, устойчивость к Unicode paths, визуальная навигация, markers/regions и понятная диагностика.

### Windows Sound Recorder

Наблюдаемый вектор развития:

- pause/resume;
- shortcuts `Ctrl+R`, Space, Esc;
- rename/share;
- список записей внутри приложения;
- до трех часов на один recording file;
- Windows 11 UX: визуализация, markers, выбор recording device и file format.

Вывод для JavaSoundRecorder:

- ближайший baseline для простого диктофона - не Audacity, а Windows Sound Recorder. JavaSoundRecorder уступает ему почти по всем UX-пунктам.

### Ashampoo Audio Recorder Free

Наблюдаемый вектор развития:

- microphone и soundcard/loopback recording;
- output formats MP3/WMA/OGG/WAV/FLAC/OPUS/APE;
- media library;
- built-in player;
- trimming;
- metadata edit;
- sort by length/date/filename;
- level meter.

Вывод для JavaSoundRecorder:

- для простого recorder-продукта нужно минимум: источник, формат, качество, уровень, библиотека, playback, trim и file actions.

### OBS Studio

Наблюдаемый вектор развития:

- audio mixer overhaul;
- pinning audio sources;
- monitoring controls;
- visibility/ordering fixes for sources;
- WebRTC support.

Вывод для JavaSoundRecorder:

- OBS не прямой конкурент по простоте, но показывает, что управление источниками и микшер/monitoring становятся конкурентным ожиданием даже в recorder-сценариях.

## 6. UI/UX тестирование

Область UX/UI тестирования: текущий функциональный объем `v1.1.0`, то есть CLI one-shot и Swing UI с `Start`/`Stop`. Playback, trim, device picker, export formats и library не тестировались как работающие функции, потому что их нет в приложении; они зафиксированы как gap и roadmap items.

### Проверенные сценарии

1. Запуск Swing UI.
2. Первичное состояние: `Idle`, `Start` enabled, `Stop` disabled.
3. `Start`: статус `Recording`, `Start` disabled, `Stop` enabled.
4. Автоматическое завершение после заданной длительности.
5. `Stop` во время записи.
6. Проверка созданных WAV-файлов.
7. CLI one-shot запись.

### UX/UI test matrix

| ID | Проверка | Метод | Ожидаемый UX | Фактический результат | Статус | Следствие |
|---|---|---|---|---|---|---|
| UI-001 | Первое открытие окна | `java -jar ... --ui`, визуальная проверка | Окно читаемое, базовые controls видны без resize | Окно около `236x105`, controls видны, но запас под статусы отсутствует | Fail | BUG-001 |
| UI-002 | Idle state | Визуально и через существующий `RecorderPanelUiTest` | `Idle`, `Start` включен, `Stop` выключен | Соответствует | Pass | Базовое состояние корректно. |
| UI-003 | Start recording | Ручной click `Start`, запись 3000 ms | Состояние меняется на recording, stop доступен, пользователь видит прогресс | Статус `Recording`, кнопки меняются, но прогресса/таймера/уровня нет | Partial | BUG-005 |
| UI-004 | Successful finish | Дождаться завершения записи | Пользователь видит понятный финальный статус и путь/действие | WAV создан, но `Done: <long filename>` обрезается | Fail | BUG-001 |
| UI-005 | Stop/cancel | Нажать `Stop` во время записи | Промежуточное `Stopping...`, затем финальное `Cancelled` или `Saved partial` | Статус остается `Cancel requested` | Fail | BUG-002 |
| UI-006 | Partial artifact after cancel | Проверить каталог после UI-005 | Либо partial удален, либо явно помечен и объяснен | Остался обычный WAV размером `63432` bytes | Fail | BUG-003 |
| UI-007 | CLI one-shot | `java -jar ...` с env duration/upload disabled | Лог сообщает путь, файл создан, процесс завершается | WAV создан, процесс завершился | Pass | CLI baseline работает. |
| UI-008 | No-upload mode | `JAVASOUNDRECORDER_UPLOAD_ENABLED=false` | Запись не требует Dropbox, target понятен | Лог показывает `uploaded: false target: local-only` | Pass | Технический UX CLI приемлем. |
| UI-009 | Error message strategy | Анализ кода `Main.showFailure` | Короткая понятная ошибка + technical details отдельно | UI показывает `Error: ` + технический `cause.toString()` | Fail | BUG-004 |
| UI-010 | Accessibility baseline | Анализ Swing components | Accessible names, keyboard shortcuts, focus order documented | Нет explicit accessible names и shortcuts | Fail | BUG-007 |
| UI-011 | Discoverability of saved file | Ручной successful finish | Есть список записей или `Open folder` | Только обрезанный status label | Fail | BUG-006 |
| UI-012 | Existing automated UI coverage | `.\mvnw.cmd -q -Dtest=*UiTest test` | Тесты проходят и покрывают state transitions | Тесты проходят, но не покрывают long status/cancel final state | Partial | Нужно расширить v1.2.0 tests. |

### UX findings summary

- Functional baseline подтвержден: приложение действительно записывает WAV из CLI и Swing UI.
- Основной UX-провал не в capture pipeline, а в обратной связи: пользователь не видит прогресс, не видит полный результат, не получает финальный cancel status и не понимает судьбу partial-файла.
- Текущие automated UI tests полезны, но проверяют компоненты в изоляции и не доказывают качество реального окна после `frame.pack()`.
- Для следующего релиза сначала нужно исправить UX дефекты, которые уже проявляются в двухкнопочном UI, и только потом добавлять новые функции.

### Найденные баги и UX-дефекты

#### BUG-001. Статус завершения обрезается в маленьком окне

Серьезность: P1.

Факты:

- окно после `frame.pack()` остается очень маленьким;
- строка `Done: recording_20260612_...wav` визуально не помещается;
- пользователь не видит полный путь/имя файла и не получает action для открытия файла.

Связанные места:

- `RecorderPanel`: фиксированный `PREF_WIDTH = 220`, `PREF_HEIGHT = 40`;
- `Main`: `frame.pack()`;
- `Main`: `panel.setStatus("Done: " + result.recordingPath().getFileName())`.

Исправить в следующем релизе:

- задать минимальный размер окна не меньше `420x180`;
- сделать статус многострочным или использовать `JTextArea`/`JLabel` с HTML wrapping;
- показывать короткий статус `Saved` и отдельную строку/tooltip с полным путем;
- добавить кнопку `Open folder` после успешной записи;
- добавить UI-тест на длинное имя файла.

#### BUG-002. После Stop статус остается `Cancel requested`

Серьезность: P1.

Факты:

- после нажатия `Stop` UI возвращает кнопки в idle-состояние;
- статус остается промежуточным `Cancel requested`, хотя пользователь ожидает финальное `Cancelled` или `Saved partial`.

Связанные места:

- `Main.runAsyncAndUpdateState`: ветка stop напрямую ставит `Cancel requested`;
- `showFailure` умеет показывать `Cancelled`, но ручной сценарий не доводит UI до этого состояния.

Исправить в следующем релизе:

- ввести явную модель состояния `Idle`, `Recording`, `Stopping`, `Cancelled`, `Saved`, `Failed`;
- при stop показывать `Stopping...`, а затем финальное `Cancelled`;
- не переводить кнопки в idle до завершения cancel cleanup;
- добавить UI/integration тест, где fake capture блокируется, затем отменяется и UI получает финальное состояние.

#### BUG-003. При отмене остается частичный WAV без объяснения

Серьезность: P1.

Факты:

- после Stop в тестовом прогоне остался частичный файл размером `63432` bytes;
- UI не сообщает, сохранен ли partial intentionally;
- нет политики cleanup.

Связанные места:

- `RecordingCoordinator.requestStop` отменяет future/worker, но не управляет partial artifact;
- `JavaSoundCaptureService` закрывает line, и `AudioSystem.write` успевает оставить файл.

Исправить в следующем релизе:

- выбрать политику по умолчанию: удалять partial при cancel;
- если partial нужно сохранить, переименовывать в `*.partial.wav` и явно показывать `Saved partial`;
- добавить настройку/константу в coordinator tests;
- добавить regression test на cleanup/partial policy.

#### BUG-004. Нет preflight проверки устройства записи

Серьезность: P2.

Факты:

- пользователь узнает о проблеме микрофона только после `Start`;
- ошибка формируется через `cause.toString()`, может быть слишком длинной и технической.

Исправить в следующем релизе:

- при запуске UI проверять доступность хотя бы одного `TargetDataLine`;
- если устройства нет, отключить `Start` и показать понятный статус;
- добавить кнопку `Refresh devices`;
- показывать короткую user-facing ошибку и `Details` для технической причины.

#### BUG-005. Нет индикатора прогресса, таймера и уровня входа

Серьезность: P2.

Факты:

- во время записи UI статичен;
- пользователь не понимает, идет ли реальный захват звука или приложение зависло.

Исправить после bugfix-релиза:

- добавить elapsed/remaining timer;
- добавить level meter;
- позже добавить waveform preview.

#### BUG-006. Нет базовой библиотеки записей

Серьезность: P2.

Факты:

- после записи нет списка файлов;
- нельзя прослушать, переименовать, удалить, открыть папку;
- путь к файлу фактически скрыт из-за обрезанного статуса.

Исправить после bugfix-релиза:

- добавить панель recent recordings;
- добавить actions: play, rename, delete, reveal in folder;
- хранить metadata локально или выводить из файловой системы.

#### BUG-007. UI не локализован и не имеет accessibility contract

Серьезность: P3.

Факты:

- тексты только на английском;
- нет accessible names/descriptions в Swing components;
- нет documented shortcuts.

Исправить итерационно:

- ввести resource bundle `en`/`ru`;
- задать accessible names/descriptions;
- добавить keyboard shortcuts и тесты.

## 7. Следующий релиз: v1.2.0

Тип релиза: bugfix/minor.

Цель: закрыть найденные UI/UX-баги, сделать приложение честным простым recorder demo без обрезанных статусов, зависших cancel-состояний и неявных partial-файлов.

### v1.2.0 Patch Notes

Добавлено:

- Минимальный размер Swing-окна и адаптивная зона статуса.
- Кнопка `Open folder` после успешной записи.
- Явные состояния UI: `Idle`, `Recording`, `Stopping`, `Cancelled`, `Saved`, `Failed`.
- Короткие user-facing сообщения об ошибках и раскрываемые technical details.
- Preflight-проверка доступности устройства записи при старте UI.
- Regression tests для длинных имен файлов, cancel-state и partial-file policy.

Изменено:

- После успешной записи UI показывает короткое `Saved` и полный путь отдельно, без обрезки.
- После `Stop` UI сначала показывает `Stopping...`, затем финальное `Cancelled` или `Saved partial` в зависимости от выбранной политики.
- Кнопки не возвращаются в idle-состояние до завершения cleanup.

Исправлено:

- Длинное имя WAV больше не ломает читаемость UI.
- Cancel больше не остается в промежуточном статусе.
- Partial WAV при отмене либо удаляется, либо явно маркируется и показывается пользователю.

Acceptance criteria:

- `.\mvnw.cmd -q verify` проходит локально.
- Ручной запуск `--ui` с `JAVASOUNDRECORDER_RECORDING_DURATION_MS=3000` показывает полный успешный result без обрезки.
- Ручной Stop во время записи приводит к финальному статусу `Cancelled` или `Saved partial`, не `Cancel requested`.
- В каталоге записи нет немаркированных partial-файлов после cancel.
- UI tests покрывают state transitions.

Out of scope:

- waveform;
- playback;
- multi-format export;
- installer.

## 8. Патчноуты на пять версий после исправления багов

### v1.3.0 - Настройки и устройства

Цель: убрать зависимость обычного пользователя от env vars.

Добавить:

- Settings dialog в Swing UI.
- Выбор каталога записи через file chooser.
- Настройку длительности записи.
- Переключатель upload enabled.
- Список доступных input devices.
- `Refresh devices`.
- Сохранение локальных настроек в user config file.

Изменить:

- `AppConfig` разделить на immutable runtime config и user preferences.
- `Main` должен собирать config из env vars + preferences, где env vars имеют приоритет для CI/CLI.

Тесты:

- unit tests для merge env/preferences;
- UI tests для settings dialog;
- fake audio device provider для тестируемого выбора устройств.

Acceptance criteria:

- Пользователь может выбрать папку и длительность без переменных окружения.
- Если устройство исчезло, UI показывает понятное состояние и предлагает refresh.
- CLI остается backward-compatible с текущими env vars.

### v1.4.0 - Библиотека записей и playback

Цель: превратить результат записи из "файл где-то на диске" в управляемую запись внутри приложения.

Добавить:

- Панель `Recordings`.
- Список последних WAV-файлов из выбранного каталога.
- Playback внутри приложения.
- Rename.
- Delete с подтверждением.
- Reveal/Open in folder.
- Sort by date/name/duration/size.
- Отображение duration и size.

Изменить:

- После записи автоматически добавлять файл в список.
- Статус успешной записи должен ссылаться на выбранный элемент библиотеки.

Тесты:

- unit tests для scanning/sorting;
- UI tests для list actions;
- integration test: record -> appears in library -> delete removes file.

Acceptance criteria:

- После записи файл виден в UI без открытия проводника.
- Пользователь может прослушать и переименовать запись.
- Delete не удаляет файл без подтверждения.

### v1.5.0 - Живая обратная связь записи

Цель: сделать процесс записи наблюдаемым.

Добавить:

- Elapsed timer.
- Remaining timer для bounded recording.
- Level meter по входному сигналу.
- Pause/resume.
- Keyboard shortcuts: `Ctrl+R` start, Space pause/resume, Esc stop/cancel.
- Basic accessibility names/descriptions для controls.

Изменить:

- `AudioCaptureService` должен отдавать progress/level events через callback или observable interface.
- `RecordingCoordinator` должен различать stop, pause и cancel.

Тесты:

- unit tests для state machine;
- UI tests для shortcuts;
- fake capture service с predictable level/progress events.

Acceptance criteria:

- Во время записи видно, что звук реально поступает.
- Pause/resume не создает новый файл без явного намерения пользователя.
- Shortcuts работают только когда UI в подходящем состоянии.

### v1.6.0 - Форматы, экспорт и upload UX

Цель: приблизиться к простым consumer-рекордерам по file workflow.

Статус реализации в текущей финальной итерации:

- Реализованы export profiles как явная модель форматов.
- Поддержаны реальные `WAV` и `FLAC` export paths с SHA-256 checksum.
- `MP3` и `OGG/Opus` оставлены видимыми профилями, но возвращают понятную ошибку до добавления codec backend.
- Добавлен metadata editor для `title`, `artist`, `comment`; данные хранятся UTF-8 sidecar-файлом рядом с WAV.
- Добавлен ручной `Upload`/retry для выбранной записи через текущую Dropbox-конфигурацию.
- Rename/delete синхронизируют sidecar metadata с аудиофайлом.
- Принятое ограничение: sample rate/channels/bitrate UI вынесены в следующий шаг, потому что текущий recorder пишет
  фиксированный PCM WAV, а ложные bitrate controls ухудшили бы UX.

Добавить:

- Export profiles: WAV, MP3, FLAC, OGG/Opus.
- Настройки sample rate, channels, bitrate/quality там, где формат поддерживает.
- Metadata editor: title, artist, comment.
- Upload panel: Dropbox status, target folder, retry, last error.
- Optional HTTP/local upload target в UI, если они остаются в архитектуре.
- Checksums для локальных exported files.

Изменить:

- `UploadService` должен возвращать user-facing status и technical diagnostics отдельно.
- Release artifacts должны включать user guide с форматами и upload setup.

Тесты:

- contract tests для exporters;
- metadata round-trip tests;
- upload retry tests с fake service.

Acceptance criteria:

- Пользователь может экспортировать запись минимум в WAV и один compressed формат.
- Ошибка upload не скрывает локально созданный файл.
- UI объясняет, что было сохранено локально и что не удалось загрузить.

### v1.7.0 - Редактирование и подготовка записи

Цель: дать минимальный editing flow, который нужен после записи голоса.

Добавить:

- Trim start/end.
- Normalize volume.
- Silence trim.
- Simple noise reduction или documented integration с внешним инструментом, если встроенная реализация слишком рискованна.
- Markers.
- Notes/comment per recording.
- Export selected region.

Изменить:

- Ввести отдельный слой `editing`, не смешивать editing с `audio` capture.
- UI разделить на `Record`, `Library`, `Edit`.

Тесты:

- golden-file или signal-level tests для trim/normalize;
- tests на сохранение markers/notes;
- UI smoke для edit workflow.

Acceptance criteria:

- Пользователь может быстро обрезать тишину в начале/конце записи.
- Исходный файл не перезаписывается без подтверждения.
- Export selected region создает новый файл и добавляет его в библиотеку.

### v1.8.0 - Дистрибуция, локализация и продуктовая полировка

Цель: сделать проект installable desktop-продуктом, а не только Maven/JAR demo.

Добавить:

- Windows installer или portable ZIP с `javaw` launcher.
- App icon и нормальные screenshots.
- RU/EN localization через resource bundles.
- First-run screen с выбором каталога и устройства.
- In-app About dialog: version, license, GitHub link, logs path.
- Crash/error log export для bug reports.
- Accessibility checklist в docs.

Изменить:

- README перепозиционировать: отдельно "для пользователей" и "для разработчиков".
- Release notes делить на `User changes`, `Developer changes`, `Security/supply-chain`.

Тесты:

- smoke test packaged artifact запускается без Maven;
- visual regression screenshots для основных UI states;
- manual release checklist для installer/portable ZIP.

Acceptance criteria:

- Новый пользователь может скачать release asset и запустить UI без Maven.
- UI доступен на русском и английском.
- README за первые 60 секунд объясняет, как записать, найти и прослушать файл.

## 9. Приоритетный backlog для другой LLM

### Немедленно: v1.2.0

1. Не менять архитектуру радикально.
2. Начать с тестов на текущие дефекты:
   - long status text;
   - stop/cancel final state;
   - partial file cleanup/marking;
   - no-device friendly error.
3. Исправить `RecorderPanel` layout.
4. Ввести маленькую UI state model.
5. Исправить `RecordingCoordinator` cancel artifact policy.
6. Обновить `docs/USAGE.md`, `docs/TEST_PLAN.md`, `CHANGELOG.md`.
7. Запустить `.\mvnw.cmd -q verify`.
8. Вручную проверить CLI и `--ui`.

### Не делать в v1.2.0

- Не добавлять полноценный audio editor.
- Не добавлять FFmpeg, если это не нужно для bugfix.
- Не ломать env var compatibility.
- Не переписывать Swing на другой UI toolkit.

### Риски

- `AudioSystem.write` блокирующий; cancel/partial cleanup нужно тестировать через fake capture service и отдельно вручную.
- Java Sound device enumeration зависит от ОС и драйверов; нужен интерфейс-обертка для тестируемости.
- MP3/Opus export может принести licensing/dependency вопросы; для v1.6.0 сначала сделать design note.
- Dropbox token UX нельзя улучшать через сохранение plaintext token без security decision.

## 10. Источники

- JavaSoundRecorder GitHub: https://github.com/krotname/JavaSoundRecorder
- Audacity home/download/changelog: https://www.audacityteam.org/ , https://www.audacityteam.org/download/ , https://github.com/audacity/audacity/releases
- Audacity 4 beta notes: https://www.audacityteam.org/next/
- Adobe Audition product page: https://www.adobe.com/products/audition.html
- ocenaudio home/download/changelog: https://www.ocenaudio.com/ , https://www.ocenaudio.com/download , https://www.ocenaudio.com/changelog
- Microsoft Sound Recorder FAQ: https://support.microsoft.com/en-us/windows/sound-recorder-app-for-windows-faq-5c208478-2141-bd07-fe1d-d6d1356c1d56
- Windows 11 Sound Recorder feature coverage: https://www.windowscentral.com/microsoft-rolls-out-new-sound-recorder-preview-app-insiders-windows-11
- Ashampoo Audio Recorder Free: https://www.ashampoo.com/en-us/audio-recorder-free
- Ashampoo manual: https://support.ashampoo.com/hc/en-us/articles/360024612173-Audio-Recorder-Free-Manual
- OBS Studio 32.1 release notes: https://obsproject.com/blog/obs-studio-32-1-release-notes
- WavePad product/version pages: https://www.nch.com.au/wavepad/index.html , https://www.nch.com.au/wavepad/versions.html
- Zapier audio editor ranking: https://zapier.com/blog/best-audio-editor/
- TechRadar audio editor rankings: https://www.techradar.com/best/best-audio-editor , https://www.techradar.com/best/best-free-audio-editors
