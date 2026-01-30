Логирование реализовано через Logback.
Функциональность:
- логирование HTTP-запросов;
- security-события (login, logout);
- ошибки;
- бизнес-операции;

Мониторинг и метрики
Для мониторинга используется Spring Boot Actuator.
Доступные endpoints:
- /actuator/health
- /actuator/info
- /actuator/metrics

Для интеграции с Prometheus:
- /actuator/prometheus