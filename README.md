# scala_server
Сделанно по примеру lightbend play rest api example

`app/repo/DataRepository` определение репозитория и данных
`app/repo/CSVRepository` DataRepository из CSV файла
`app/controllers/Resources` Handler для репо, и типы данных которые он возвращает
`app/controllers/Controller` Основной контроллер, и MainControllerComponents с ResourceHandler и ActionBuilder
`app/controllers/BaseRouter` Основной Router, транслирует запросы контроллеру

Запросы
`localhost:9000/` Весь список данных
`localhost:9000/get/1` Получить для ID 1
`localhost:9000/avg?lo=YYYY-MM-DD&hi=YYYY-MM-DD` Получить среднее за период lo .. hi
`localhost:9000/min_max?lo_YYYY-MM-DD&hi=YYYY-MM-DD` Получить максимальное и минимальное значение за пероид lo .. hi
